package cl.stryda.app.ui.screens.caballo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Medicion
import cl.stryda.app.domain.RangosMedicion
import cl.stryda.app.domain.Sesion
import cl.stryda.app.domain.TipoTrabajo
import cl.stryda.app.domain.mejorCarreraConPeso
import cl.stryda.app.ui.components.GraficoPeso
import cl.stryda.app.ui.components.TransformacionTiempo
import cl.stryda.app.ui.components.aKmH
import cl.stryda.app.ui.components.decimal
import cl.stryda.app.ui.components.digitosASegundos
import cl.stryda.app.ui.components.fechaCorta
import cl.stryda.app.ui.components.fechaRelativaFutura
import cl.stryda.app.ui.components.fechaRelativaPasada
import cl.stryda.app.ui.components.hora
import cl.stryda.app.ui.components.tiempoCarrera
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.FormaPill
import cl.stryda.app.ui.theme.StrydaTema
import kotlin.math.abs

/** Velocidades fuera de este rango en un trabajo son casi seguro un error de tipeo. */
private val RANGO_KMH_TRABAJO = 35.0..75.0
private val DISTANCIAS_TRABAJO = listOf(400, 600, 800, 1000, 1200)

// ---------- Peso ----------

@Composable
fun HojaPeso(
    d: FichaContenido,
    onGuardar: (peso: Double?, temperatura: Double?, fc: Double?) -> Unit,
    onCerrar: () -> Unit,
) {
    var peso by rememberSaveable { mutableStateOf("") }
    var temperatura by rememberSaveable { mutableStateOf("") }
    var fc by rememberSaveable { mutableStateOf("") }
    var otras by rememberSaveable { mutableStateOf(false) }

    val errorPeso = errorRango(peso, RangosMedicion.PESO_KG, "kg")
    val errorTemp = errorRango(temperatura, RangosMedicion.TEMPERATURA, "°C")
    val errorFc = errorRango(fc, RangosMedicion.FC_REPOSO, "lpm")
    val valido = listOf(peso, temperatura, fc).any { it.isNotBlank() } && listOf(errorPeso, errorTemp, errorFc).all { it == null }

    Hoja("Peso de ${d.caballo.nombre}", onCerrar) {
        CampoNumero("Peso de hoy (kg)", peso, { peso = it }, errorPeso, "Por ejemplo 485,5")
        if (otras) {
            CampoNumero("Temperatura (°C)", temperatura, { temperatura = it }, errorTemp, "Opcional")
            CampoNumero("Frecuencia en reposo (lpm)", fc, { fc = it }, errorFc, "Opcional", decimales = false)
        } else {
            TextButton(onClick = { otras = true }, modifier = Modifier.heightIn(min = Espacio.tactil)) {
                Text("Agregar temperatura o frecuencia")
            }
        }
        BotonGuardar("Guardar pesaje", valido) {
            onGuardar(leerNumero(peso), leerNumero(temperatura), leerNumero(fc))
        }

        HorizontalDivider(color = StrydaTema.colores.border, modifier = Modifier.padding(vertical = Espacio.s))
        Subtitulo("Historial de peso")
        if (d.pesos.isEmpty()) {
            TextoApagado("Todavía no hay pesajes. El primero que registres aparece acá.")
            return@Hoja
        }
        ResumenPeso(d)
        if (d.pesos.size >= 2) GraficoPeso(d.pesos)
        ListaPesos(d)
    }
}

/** Observaciones sobre su propio peso. Nunca dice cuánto debería pesar. */
@Composable
private fun ResumenPeso(d: FichaContenido) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        d.rangoPeso?.let {
            Text("En los últimos 6 meses se movió entre ${decimal(it.start)} y ${decimal(it.endInclusive)} kg.", style = MaterialTheme.typography.bodyLarge)
        }
        mejorCarreraConPeso(d.pesosEnCarreras)?.let { m ->
            Text(
                "En su mejor resultado reciente (${m.posicion}° el ${fechaCorta(m.fecha)}) pesó ${decimal(m.kg)} kg.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ListaPesos(d: FichaContenido) {
    val carreraPorFecha = d.pesosEnCarreras.associateBy { it.fecha }
    val recientes = d.pesos.reversed()
    Column {
        recientes.forEachIndexed { i, m ->
            if (i > 0) HorizontalDivider(color = StrydaTema.colores.border)
            val anterior = recientes.getOrNull(i + 1)
            val variacion = anterior?.let { m.valor - it.valor }
            val contexto = listOfNotNull(
                carreraPorFecha[m.fecha]?.let { c -> "Día de carrera · ${c.posicion}°" + ((c.corredores ?: 0).takeIf { it >= 2 }?.let { " de $it" } ?: "") },
                "Registrado en el teléfono".takeIf { m.pendiente },
            ).joinToString(" · ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(vertical = Espacio.s)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(fechaRelativaPasada(m.fecha, d.hoy).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
                    if (contexto.isNotEmpty()) TextoApagado(contexto)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${decimal(m.valor)} kg", style = MaterialTheme.typography.titleMedium)
                    variacion?.let {
                        TextoApagado(if (abs(it) < 0.05) "igual" else "${if (it > 0) "+" else "−"}${decimal(abs(it))} kg")
                    }
                }
            }
        }
    }
}

// ---------- Velocidad ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HojaVelocidad(
    d: FichaContenido,
    onGuardar: (TipoTrabajo, Int, Double) -> Unit,
    onCerrar: () -> Unit,
) {
    var tipo by rememberSaveable { mutableStateOf(TipoTrabajo.APRONTE) }
    var distancia by rememberSaveable { mutableIntStateOf(800) }
    var digitos by rememberSaveable { mutableStateOf("") }

    val segundos = digitosASegundos(digitos)
    val kmh = segundos?.takeIf { it > 0 }?.let { aKmH(distancia / it) }
    val error = when {
        digitos.isNotBlank() && segundos == null -> "Revisa los segundos: no pueden pasar de 59"
        kmh != null && kmh !in RANGO_KMH_TRABAJO -> "${decimal(kmh)} km/h parece un error de tipeo. Revisa el tiempo o la distancia."
        else -> null
    }

    Hoja("Velocidad de ${d.caballo.nombre}", onCerrar) {
        d.trabajosRapidos.firstOrNull()?.let { s ->
            Text("Máxima: ${decimal(aKmH(s.distanciaM!! / s.tiempoSeg!!))} km/h", style = StrydaTema.cifras.media)
            TextoApagado("${s.tipo.etiqueta} ${s.distanciaM} m en ${tiempoCarrera(s.tiempoSeg)} · ${fechaRelativaPasada(s.fecha, d.hoy)}")
        }

        Subtitulo("Registrar trabajo cronometrado")
        Etiqueta("Tipo")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
            listOf(TipoTrabajo.APRONTE, TipoTrabajo.HUINCHADA).forEach { t ->
                FilterChip(
                    selected = tipo == t,
                    onClick = {
                        tipo = t
                        if (t == TipoTrabajo.HUINCHADA) distancia = 400
                    },
                    label = { Text(t.etiqueta, style = MaterialTheme.typography.labelLarge) },
                    shape = FormaPill,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Etiqueta("Distancia")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            DISTANCIAS_TRABAJO.forEach { m ->
                FilterChip(
                    selected = distancia == m,
                    onClick = { distancia = m },
                    label = { Text("$m m", style = MaterialTheme.typography.labelLarge) },
                    shape = FormaPill,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        OutlinedTextField(
            value = digitos,
            onValueChange = { nuevo -> if (nuevo.length <= 5 && nuevo.all(Char::isDigit)) digitos = nuevo },
            label = { Text("Tiempo") },
            isError = error != null,
            supportingText = {
                Text(error ?: kmh?.let { "= ${decimal(it)} km/h" } ?: "Solo números: 486 es 48.6 y 1024 es 1:02.4")
            },
            visualTransformation = TransformacionTiempo,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        BotonGuardar("Guardar ${tipo.etiqueta.lowercase()}", segundos != null && error == null && kmh != null) {
            onGuardar(tipo, distancia, segundos!!)
        }

        if (d.trabajosRapidos.isNotEmpty()) {
            HorizontalDivider(color = StrydaTema.colores.border, modifier = Modifier.padding(vertical = Espacio.s))
            Subtitulo("Sus trabajos más rápidos")
            d.trabajosRapidos.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(color = StrydaTema.colores.border)
                FilaTrabajo(s, d)
            }
        }
    }
}

@Composable
private fun FilaTrabajo(s: Sesion, d: FichaContenido) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = Espacio.s)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${s.tipo.etiqueta} ${s.distanciaM} m", style = MaterialTheme.typography.bodyLarge)
            TextoApagado(
                listOfNotNull(fechaRelativaPasada(s.fecha, d.hoy).replaceFirstChar { it.uppercase() }, "Registrado en el teléfono".takeIf { s.pendiente })
                    .joinToString(" · "),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${decimal(aKmH(s.distanciaM!! / s.tiempoSeg!!))} km/h", style = MaterialTheme.typography.titleMedium)
            TextoApagado(tiempoCarrera(s.tiempoSeg))
        }
    }
}

// ---------- Peak en carrera ----------

@Composable
fun HojaPeak(d: FichaContenido, onCerrar: () -> Unit) {
    Hoja("Peak en carrera", onCerrar) {
        Text(
            "Es la velocidad más alta que midió el sistema GMAX del Club Hípico en un tramo de 200 m durante una carrera.",
            style = MaterialTheme.typography.bodyLarge,
        )
        val peak = d.peak
        if (peak == null) {
            TextoApagado(
                "No hay parciales cargados para sus carreras. El programa oficial no los trae; " +
                    "se publican en clubhipico.cl, en la sección GMAX de cada carrera.",
            )
        } else {
            Text("${decimal(aKmH(peak.parcial.velocidadMs))} km/h", style = StrydaTema.cifras.media)
            TextoApagado(
                listOfNotNull(
                    "A los ${peak.parcial.metros} m",
                    peak.carrera?.let { "${fechaCorta(it.carrera.fecha)} · ${it.carrera.distanciaM} m · ${it.carrera.hipodromo}" },
                ).joinToString(" · "),
            )
        }
    }
}

// ---------- Carreras ----------

@Composable
fun HojaCarreras(d: FichaContenido, onCerrar: () -> Unit) {
    val pesoPorFecha = d.pesos.associateBy { it.fecha }
    Hoja("Carreras de ${d.caballo.nombre}", onCerrar) {
        if (d.carreras.isEmpty()) {
            TextoApagado("Todavía no tiene carreras. Cuando se inscriba en una, aparece acá.")
            return@Hoja
        }
        d.carreras.forEachIndexed { i, c ->
            if (i > 0) HorizontalDivider(color = StrydaTema.colores.border)
            FilaCarrera(c, d, pesoPorFecha[c.carrera.fecha])
        }
        TextoApagado("Los tiempos son aproximados: tiempo de la carrera más 0,2 s por cuerpo de diferencia.")
    }
}

@Composable
private fun FilaCarrera(c: CarreraDelCaballo, d: FichaContenido, peso: Medicion?) {
    val p = c.participacion
    val futura = !c.carrera.fecha.isBefore(d.hoy) && p.posicion == null
    val cuando = if (futura) {
        listOfNotNull(fechaRelativaFutura(c.carrera.fecha, d.hoy), c.carrera.hora?.let(::hora)).joinToString(", ")
    } else {
        fechaRelativaPasada(c.carrera.fecha, d.hoy)
    }.replaceFirstChar { it.uppercase() }
    val carrera = listOfNotNull(
        c.carrera.numero?.let { "${it}ª carrera" },
        listOfNotNull("${c.carrera.distanciaM} m", c.carrera.superficie).joinToString(" "),
        c.carrera.hipodromo,
    ).joinToString(" · ")
    val resultado = when {
        futura -> "Inscrito"
        p.posicion != null -> "${p.posicion}°" + ((c.corredores ?: 0).takeIf { it >= 2 }?.let { " de $it" } ?: "")
        else -> "Resultado aún no cargado"
    }
    val detalle = listOfNotNull(
        p.tiempoSeg?.let { "Tiempo aprox. ${tiempoCarrera(it)}" },
        p.numero?.let { "Nº $it" },
        p.jinete,
        peso?.let { "${decimal(it.valor, 0)} kg" },
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Espacio.m)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cuando, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Espacio.s))
            Text(
                resultado,
                style = MaterialTheme.typography.titleMedium,
                color = if (futura) StrydaTema.colores.info else MaterialTheme.colorScheme.onSurface,
            )
        }
        TextoApagado(carrera)
        if (detalle.isNotEmpty()) TextoApagado(detalle)
    }
}

// ---------- Piezas comunes ----------

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun TextoApagado(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
}
