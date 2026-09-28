package cl.stryda.app.ui.screens.caballo

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import cl.stryda.app.R
import cl.stryda.app.domain.Diseno
import cl.stryda.app.domain.Elegibilidad
import cl.stryda.app.domain.ResultadoEstimacion
import cl.stryda.app.domain.interpretarChaquetilla
import cl.stryda.app.ui.components.aKmH
import cl.stryda.app.ui.components.decimal
import cl.stryda.app.ui.components.fechaCorta
import cl.stryda.app.ui.components.fechaLarga
import cl.stryda.app.ui.components.hora
import cl.stryda.app.ui.components.tiempoCarrera
import cl.stryda.app.ui.theme.ColoresSeda
import cl.stryda.app.ui.theme.PaletaClara
import java.io.File
import kotlin.math.abs

/**
 * Ficha del ejemplar en PDF (A4), pensada para enviarla al propietario.
 * Arriba el corral y su preparador; Stryda va discreta al pie.
 * Nunca incluye tratamientos, fármacos ni notas: solo el estado y si está apto para correr.
 */
class FichaPdf(private val context: Context) {

    private val regular = ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.DEFAULT
    private val semibold = ResourcesCompat.getFont(context, R.font.inter_semibold) ?: Typeface.DEFAULT_BOLD

    private val texto = PaletaClara.text.toArgb()
    private val apagado = PaletaClara.textMuted.toArgb()
    private val primario = PaletaClara.primary.toArgb()
    private val borde = PaletaClara.border.toArgb()
    private val suave = PaletaClara.primarySoft.toArgb()

    private val documento = PdfDocument()
    private var pagina: PdfDocument.Page? = null
    private lateinit var lienzo: Canvas
    private var y = 0f
    private var numeroPagina = 0

    fun generar(d: FichaContenido): File {
        nuevaPagina(d)
        encabezadoCaballo(d)
        cifras(d)
        d.proximaCarrera?.let { proximaCarrera(d) }
        jinetes(d)
        campana(d)
        estimacion(d)
        historialPeso(d)
        cerrarPagina(d)

        val carpeta = File(context.cacheDir, "pdf").apply { mkdirs() }
        val archivo = File(carpeta, "Ficha ${d.caballo.nombre}.pdf")
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return archivo
    }

    // ---------- Páginas ----------

    private fun nuevaPagina(d: FichaContenido) {
        pagina?.let { cerrarPagina(d) }
        numeroPagina++
        val p = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create())
        pagina = p
        lienzo = p.canvas
        y = MARGEN

        // Encabezado del corral: es su preparador quien le habla al dueño
        val corral = d.corral
        texto(corral?.nombre ?: "Ficha del ejemplar", MARGEN, y + 16f, 16f, semibold, texto)
        val sub = listOfNotNull(corral?.preparador?.let { "Preparador: $it" }, corral?.hipodromo).joinToString(" · ")
        if (sub.isNotEmpty()) texto(sub, MARGEN, y + 32f, 9.5f, regular, apagado)
        textoDerecha("Ficha al ${fechaLarga(d.hoy)} de ${d.hoy.year}", ANCHO - MARGEN, y + 16f, 9.5f, regular, apagado)
        y += 44f
        linea(y, primario, 1.5f)
        y += 18f
    }

    private fun cerrarPagina(d: FichaContenido) {
        val p = pagina ?: return
        val pie = ALTO - MARGEN + 12f
        linea(pie - 14f, borde, 0.8f)
        texto(
            "Generado con Stryda · Carreras: programa oficial del Club Hípico de Santiago; tiempos aproximados. " +
                "Trabajos de demostración.",
            MARGEN, pie, 7.5f, regular, apagado,
        )
        textoDerecha("Página $numeroPagina", ANCHO - MARGEN, pie, 7.5f, regular, apagado)
        documento.finishPage(p)
        pagina = null
    }

    /** Si lo que viene no cabe, sigue en una página nueva. */
    private fun asegurar(alto: Float, d: FichaContenido) {
        if (y + alto > ALTO - MARGEN - 16f) nuevaPagina(d)
    }

    // ---------- Secciones ----------

    private fun encabezadoCaballo(d: FichaContenido) {
        val k = d.caballo
        texto(k.nombre, MARGEN, y + 26f, 26f, semibold, texto)
        y += 38f
        val datos = listOfNotNull(k.pelaje, k.sexo?.etiqueta, k.edad?.let { "$it años" }).joinToString(" · ")
        if (datos.isNotEmpty()) {
            texto(datos, MARGEN, y + 12f, 11f, regular, texto)
            y += 18f
        }
        val genealogia = listOfNotNull(k.padre, k.madre).joinToString(" y ")
        if (genealogia.isNotEmpty()) {
            texto("Por $genealogia", MARGEN, y + 12f, 10f, regular, apagado)
            y += 18f
        }
        k.colores?.let { colores ->
            chaquetilla(colores, MARGEN, y + 2f, 26f)
            texto("Colores: $colores", MARGEN + 34f, y + 18f, 10f, regular, texto)
            y += 34f
        }
        val estado = etiquetaEstado(k.estado)
        val apto = when (val e = d.elegibilidad) {
            Elegibilidad.Apto -> "Apto para correr"
            is Elegibilidad.EnRetiro -> "En período de retiro hasta el ${fechaLarga(e.hasta)}"
            Elegibilidad.Lesionado -> "No puede correr mientras esté lesionado"
            Elegibilidad.NoDisponible -> "Retirado de las carreras"
        }
        caja(MARGEN, y, ANCHO - 2 * MARGEN, 30f, suave)
        texto("Estado: $estado", MARGEN + 10f, y + 19f, 11f, semibold, texto)
        texto(apto, MARGEN + 170f, y + 19f, 11f, regular, texto)
        y += 44f
    }

    private fun cifras(d: FichaContenido) {
        titulo("Rendimiento", d)
        val ancho = (ANCHO - 2 * MARGEN - 3 * 8f) / 4
        val items = listOf(
            Triple("Peso", d.peso?.let { "${decimal(it.valorKg)} kg" }, d.peso?.let { p ->
                p.variacionMesKg?.let { v -> "${if (v >= 0) "+" else "−"}${decimal(abs(v))} kg en el mes" } ?: "al ${fechaCorta(p.fecha)}"
            }),
            Triple("Velocidad máxima", d.sesionMasRapida?.let { "${decimal(aKmH(it.distanciaM!! / it.tiempoSeg!!))} km/h" },
                d.sesionMasRapida?.let { "${it.tipo.etiqueta} ${it.distanciaM} m" }),
            Triple("Peak en carrera", d.peak?.let { "${decimal(aKmH(it.parcial.velocidadMs))} km/h" }, if (d.peak == null) "Sin datos GMAX" else "GMAX"),
            Triple("Última carrera", d.diasDesdeUltimaCarrera?.let { "$it días" }, d.ultimaCarrera?.let { "${it.participacion.posicion}° el ${fechaCorta(it.carrera.fecha)}" } ?: "Aún no debuta"),
        )
        items.forEachIndexed { i, (etiqueta, valor, detalle) ->
            val x = MARGEN + i * (ancho + 8f)
            marco(x, y, ancho, 58f)
            texto(etiqueta, x + 8f, y + 14f, 8.5f, regular, apagado)
            texto(valor ?: "—", x + 8f, y + 34f, 15f, semibold, texto)
            detalle?.let { texto(it, x + 8f, y + 49f, 8f, regular, apagado) }
        }
        y += 72f
    }

    private fun proximaCarrera(d: FichaContenido) {
        val c = d.proximaCarrera ?: return
        titulo("Próxima carrera", d)
        asegurar(40f, d)
        val cuando = listOfNotNull(fechaLarga(c.carrera.fecha), c.carrera.hora?.let(::hora)).joinToString(", ")
        val carrera = listOfNotNull(c.carrera.numero?.let { "${it}ª carrera" }, "${c.carrera.distanciaM} m", c.carrera.superficie, c.carrera.hipodromo)
            .joinToString(" · ")
        val partida = listOfNotNull(
            c.participacion.numero?.let { "Número $it" },
            c.participacion.jinete?.let { "Jinete: $it" },
            c.participacion.pesoKg?.let { "${decimal(it, 0)} kg" },
        ).joinToString(" · ")
        texto(cuando, MARGEN, y + 12f, 11f, semibold, texto)
        texto(carrera, MARGEN, y + 27f, 10f, regular, texto)
        if (partida.isNotEmpty()) texto(partida, MARGEN, y + 41f, 10f, regular, texto)
        y += 54f
    }

    private fun jinetes(d: FichaContenido) {
        if (d.jinetes.isEmpty()) return
        titulo("Jinetes que lo han corrido", d)
        d.jinetes.take(5).forEach { j ->
            asegurar(16f, d)
            val detalle = listOfNotNull(
                if (j.montas == 1) "1 carrera" else "${j.montas} carreras",
                j.mejorPosicion?.let { "mejor llegada ${it}°" },
                "última el ${fechaCorta(j.ultimaMonta)}",
            ).joinToString(" · ")
            texto(j.nombre, MARGEN, y + 11f, 10f, semibold, texto)
            texto(detalle, MARGEN + 130f, y + 11f, 10f, regular, apagado)
            y += 16f
        }
        y += 10f
    }

    private fun campana(d: FichaContenido) {
        val corridas = d.carreras.filter { it.participacion.posicion != null || it.carrera.fecha.isBefore(d.hoy) }
        if (corridas.isEmpty()) return
        titulo("Campaña", d)
        val columnas = listOf("Fecha" to 52f, "Hipódromo" to 118f, "Dist." to 42f, "Pista" to 38f, "Lugar" to 42f, "Tiempo" to 46f, "Nº" to 24f, "Jinete" to 90f, "Peso" to 43f)
        val pesos = d.pesos.associateBy { it.fecha }
        fun fila(valores: List<String>, fuente: Typeface, color: Int) {
            var x = MARGEN
            valores.forEachIndexed { i, v ->
                texto(recortar(v, columnas[i].second - 4f, 8.5f, fuente), x, y + 11f, 8.5f, fuente, color)
                x += columnas[i].second
            }
            y += 16f
        }
        asegurar(34f, d)
        fila(columnas.map { it.first }, semibold, apagado)
        linea(y - 3f, borde, 0.6f)
        corridas.forEach { c ->
            asegurar(16f, d)
            val p = c.participacion
            fila(
                listOf(
                    fechaCorta(c.carrera.fecha),
                    c.carrera.hipodromo.replace("Club Hípico de Santiago", "Club Hípico"),
                    "${c.carrera.distanciaM} m",
                    c.carrera.superficie ?: "—",
                    p.posicion?.let { pos -> "$pos°" + ((c.corredores ?: 0).takeIf { it >= 2 }?.let { "/$it" } ?: "") } ?: "—",
                    p.tiempoSeg?.let(::tiempoCarrera) ?: "—",
                    p.numero?.toString() ?: "—",
                    p.jinete ?: "—",
                    pesos[c.carrera.fecha]?.let { "${decimal(it.valor, 0)} kg" } ?: "—",
                ),
                regular, texto,
            )
        }
        texto("Tiempo aproximado: tiempo de la carrera más 0,2 s por cuerpo de diferencia. Peso: peso físico del programa.", MARGEN, y + 8f, 7.5f, regular, apagado)
        y += 22f
    }

    private fun historialPeso(d: FichaContenido) {
        if (d.pesos.size < 2) return
        titulo("Historial de peso", d, bloque = 100f)
        d.rangoPeso?.let {
            texto("En los últimos 6 meses se movió entre ${decimal(it.start)} y ${decimal(it.endInclusive)} kg.", MARGEN, y + 10f, 9.5f, regular, texto)
            y += 16f
        }
        val alto = 50f
        val ancho = ANCHO - 2 * MARGEN
        marco(MARGEN, y, ancho, alto + 20f)
        val valores = d.pesos.map { it.valor }
        val min = valores.min() - 3
        val max = valores.max() + 3
        val primeraFecha = d.pesos.first().fecha.toEpochDay()
        val dias = (d.pesos.last().fecha.toEpochDay() - primeraFecha).coerceAtLeast(1)
        fun px(i: Int) = MARGEN + 40f + (ancho - 60f) * (d.pesos[i].fecha.toEpochDay() - primeraFecha) / dias
        fun py(v: Double) = y + 10f + (alto - 10f) * (1 - ((v - min) / (max - min))).toFloat()

        texto("${decimal(max, 0)} kg", MARGEN + 6f, y + 16f, 7.5f, regular, apagado)
        texto("${decimal(min, 0)} kg", MARGEN + 6f, y + alto, 7.5f, regular, apagado)
        val trazo = Path()
        d.pesos.indices.forEach { i -> if (i == 0) trazo.moveTo(px(i), py(valores[i])) else trazo.lineTo(px(i), py(valores[i])) }
        lienzo.drawPath(trazo, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primario; style = Paint.Style.STROKE; strokeWidth = 1.8f })
        d.pesos.indices.forEach { i ->
            lienzo.drawCircle(px(i), py(valores[i]), 2.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primario })
        }
        texto(fechaCorta(d.pesos.first().fecha), MARGEN + 40f, y + alto + 14f, 7.5f, regular, apagado)
        textoDerecha(fechaCorta(d.pesos.last().fecha), MARGEN + ancho - 20f, y + alto + 14f, 7.5f, regular, apagado)
        y += alto + 32f
    }

    private fun estimacion(d: FichaContenido) {
        titulo("Estimación", d)
        when (val r = d.estimacion) {
            is ResultadoEstimacion.Insuficiente -> parrafo(
                "Se necesita al menos ${r.minimo} carreras con tiempo para estimar; lleva ${r.carrerasConTiempo}.", d,
            )
            is ResultadoEstimacion.Disponible -> {
                val e = r.estimacion
                parrafo(
                    "Tiempo esperado para ${e.distanciaM} m: ${tiempoCarrera(e.tiempoCentralSeg)} " +
                        "(entre ${tiempoCarrera(e.tiempoMinSeg)} y ${tiempoCarrera(e.tiempoMaxSeg)}). " +
                        textoTendencia(e.tendenciaSegPorCarrera, e.carrerasUsadas) + " " +
                        "Calculado con sus últimas ${e.carrerasUsadas} carreras, comparado solo con él mismo; es una estimación estadística, no un pronóstico.",
                    d,
                )
            }
        }
    }

    // ---------- Dibujo ----------

    /** [bloque] = alto de lo que viene bajo el título: así un título nunca queda solo al pie de la página. */
    private fun titulo(t: String, d: FichaContenido, bloque: Float = 20f) {
        asegurar(22f + bloque, d)
        y += 4f
        texto(t.uppercase(), MARGEN, y + 10f, 9f, semibold, primario)
        y += 18f
    }

    private fun parrafo(t: String, d: FichaContenido) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular; textSize = 10f; color = texto }
        val layout = StaticLayout.Builder.obtain(t, 0, t.length, paint, (ANCHO - 2 * MARGEN).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1f).build()
        asegurar(layout.height.toFloat(), d)
        lienzo.withTranslation(MARGEN, y) { layout.draw(this) }
        y += layout.height + 10f
    }

    private fun pincel(tamano: Float, fuente: Typeface, color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = tamano; typeface = fuente; this.color = color }

    private fun texto(t: String, x: Float, yBase: Float, tamano: Float, fuente: Typeface, color: Int) {
        lienzo.drawText(t, x, yBase, pincel(tamano, fuente, color))
    }

    private fun textoDerecha(t: String, xDer: Float, yBase: Float, tamano: Float, fuente: Typeface, color: Int) {
        val p = pincel(tamano, fuente, color).apply { textAlign = Paint.Align.RIGHT }
        lienzo.drawText(t, xDer, yBase, p)
    }

    private fun recortar(t: String, ancho: Float, tamano: Float, fuente: Typeface): String {
        val p = pincel(tamano, fuente, texto)
        if (p.measureText(t) <= ancho) return t
        var s = t
        while (s.isNotEmpty() && p.measureText("$s…") > ancho) s = s.dropLast(1)
        return "$s…"
    }

    private fun linea(yLinea: Float, color: Int, grosor: Float) {
        lienzo.drawLine(MARGEN, yLinea, ANCHO - MARGEN, yLinea, Paint().apply { this.color = color; strokeWidth = grosor })
    }

    private fun caja(x: Float, yCaja: Float, ancho: Float, alto: Float, color: Int) {
        lienzo.drawRoundRect(RectF(x, yCaja, x + ancho, yCaja + alto), 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun marco(x: Float, yMarco: Float, ancho: Float, alto: Float) {
        lienzo.drawRoundRect(
            RectF(x, yMarco, x + ancho, yMarco + alto), 6f, 6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = borde; style = Paint.Style.STROKE; strokeWidth = 0.8f },
        )
    }

    /** Chaquetilla simplificada con los colores del stud. */
    private fun chaquetilla(descripcion: String, x: Float, yTop: Float, lado: Float) {
        val ch = interpretarChaquetilla(descripcion) ?: return
        val principal = ColoresSeda[ch.principal]?.toArgb() ?: return
        val secundario = ch.secundario?.let { ColoresSeda[it]?.toArgb() }
        fun poligono(puntos: List<Pair<Float, Float>>) = Path().apply {
            puntos.forEachIndexed { i, (px, py) -> if (i == 0) moveTo(x + lado * px, yTop + lado * py) else lineTo(x + lado * px, yTop + lado * py) }
            close()
        }
        val torso = poligono(listOf(0.24f to 0.14f, 0.38f to 0.08f, 0.5f to 0.18f, 0.62f to 0.08f, 0.76f to 0.14f, 0.76f to 0.95f, 0.24f to 0.95f))
        val manga = listOf(0.24f to 0.14f, 0.08f to 0.26f, 0.02f to 0.46f, 0.15f to 0.52f, 0.24f to 0.36f)
        val colorMangas = if (ch.diseno == Diseno.MANGAS && secundario != null) secundario else principal
        val relleno = Paint(Paint.ANTI_ALIAS_FLAG)
        listOf(poligono(manga), poligono(manga.map { (a, b) -> 1 - a to b })).forEach { lienzo.drawPath(it, relleno.apply { color = colorMangas }) }
        lienzo.drawPath(torso, relleno.apply { color = principal })
        if (secundario != null && (ch.diseno == Diseno.FRANJA || ch.diseno == Diseno.CRUZ)) {
            lienzo.withClip(torso) {
                drawRect(x + lado * 0.42f, yTop, x + lado * 0.58f, yTop + lado, relleno.apply { color = secundario })
                if (ch.diseno == Diseno.CRUZ) drawRect(x, yTop + lado * 0.40f, x + lado, yTop + lado * 0.52f, relleno)
            }
        }
        lienzo.drawPath(torso, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = apagado; style = Paint.Style.STROKE; strokeWidth = 0.7f })
    }

    private companion object {
        const val ANCHO = 595 // A4 en puntos
        const val ALTO = 842
        const val MARGEN = 40f
    }
}

/** Abre el menú de Android para enviar el PDF (WhatsApp, correo, Drive...). */
fun compartirPdf(context: Context, archivo: File, nombreCaballo: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Ficha de $nombreCaballo")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(envio, "Enviar ficha de $nombreCaballo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
