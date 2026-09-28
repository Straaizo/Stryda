package cl.stryda.app.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Una hoja ya limpia: cada fila es columna → texto (o null si la celda está vacía). */
data class TablaSheets(val filas: List<FilaSheets>)

class FilaSheets(private val celdas: Map<String, String?>) {

    fun texto(columna: String): String? = celdas[columna]?.trim()?.takeIf { it.isNotEmpty() }

    fun entero(columna: String): Int? = decimal(columna)?.let { if (it % 1.0 == 0.0) it.toInt() else null }

    fun largo(columna: String): Long? = decimal(columna)?.toLong()

    /** Acepta punto o coma decimal ("3,5" → 3.5). Separador de miles con punto no se interpreta. */
    fun decimal(columna: String): Double? = texto(columna)?.replace(',', '.')?.toDoubleOrNull()

    /** Acepta ISO (2026-09-25), dd-MM-yyyy, dd/MM/yyyy y el formato gviz Date(2026,8,25). */
    fun fecha(columna: String): LocalDate? = texto(columna)?.let(::parsearFecha)
}

class RespuestaSheetsInvalida(mensaje: String) : Exception(mensaje)

private val json = Json { ignoreUnknownKeys = true }
private val regexFechaGviz = Regex("""Date\((\d+),(\d+),(\d+)(?:,.*)?\)""")
// STRICT: un "31/02" se rechaza en vez de convertirse en silencio en 28 de febrero
private val formatosFecha = listOf("uuuu-MM-dd", "dd-MM-uuuu", "dd/MM/uuuu", "d/M/uuuu")
    .map { DateTimeFormatter.ofPattern(it).withResolverStyle(ResolverStyle.STRICT) }

internal fun parsearFecha(texto: String): LocalDate? {
    regexFechaGviz.matchEntire(texto.trim())?.let { m ->
        val (a, mes, d) = m.destructured
        // gviz usa meses base 0
        return try {
            LocalDate.of(a.toInt(), mes.toInt() + 1, d.toInt())
        } catch (_: java.time.DateTimeException) {
            null
        }
    }
    for (formato in formatosFecha) {
        try {
            return LocalDate.parse(texto.trim(), formato)
        } catch (_: DateTimeParseException) {
            // se prueba el siguiente formato
        }
    }
    return null
}

/**
 * Recorta el envoltorio `google.visualization.Query.setResponse(...);` y convierte la tabla gviz
 * en filas nombradas por la cabecera. Filas más cortas que la cabecera se completan con null.
 */
fun parsearRespuestaGviz(cuerpo: String): TablaSheets {
    val inicio = cuerpo.indexOf('{')
    val fin = cuerpo.lastIndexOf('}')
    if (inicio < 0 || fin <= inicio) throw RespuestaSheetsInvalida("La respuesta no contiene JSON")

    val raiz = try {
        json.parseToJsonElement(cuerpo.substring(inicio, fin + 1)).jsonObject
    } catch (e: IllegalArgumentException) {
        throw RespuestaSheetsInvalida("JSON mal formado: ${e.message}")
    }
    if ((raiz["status"] as? JsonPrimitive)?.contentOrNull == "error") {
        throw RespuestaSheetsInvalida("La planilla devolvió un error")
    }
    val tabla = raiz["table"]?.jsonObject ?: return TablaSheets(emptyList())

    val etiquetas = tabla["cols"]?.jsonArray.orEmpty().map { col ->
        col.jsonObject["label"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    }
    val filasCrudas = tabla["rows"]?.jsonArray.orEmpty().map { fila ->
        val celdas = (fila as? JsonObject)?.get("c") as? JsonArray ?: JsonArray(emptyList())
        celdas.map(::valorCelda)
    }

    // Si gviz no detectó la cabecera, viene como primera fila
    val (cabecera, datos) = if (etiquetas.all { it.isEmpty() } && filasCrudas.isNotEmpty()) {
        filasCrudas.first().map { it?.trim().orEmpty() } to filasCrudas.drop(1)
    } else {
        etiquetas to filasCrudas
    }

    val filas = datos
        .filter { fila -> fila.any { !it.isNullOrBlank() } }
        .map { fila ->
            FilaSheets(cabecera.withIndex().filter { it.value.isNotEmpty() }.associate { (i, col) -> col to fila.getOrNull(i) })
        }
    return TablaSheets(filas)
}

private fun valorCelda(celda: kotlinx.serialization.json.JsonElement): String? {
    val obj = celda as? JsonObject ?: return null
    val v = obj["v"]
    if (v == null || v is JsonNull) return null
    val primitivo = v as? JsonPrimitive ?: return null
    if (primitivo.isString) return primitivo.content
    // Números enteros sin ".0" para que "3.0" se lea como 3
    return primitivo.doubleOrNull?.let { d -> if (d % 1.0 == 0.0) d.toLong().toString() else d.toString() }
        ?: primitivo.content
}
