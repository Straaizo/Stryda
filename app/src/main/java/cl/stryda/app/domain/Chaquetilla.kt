package cl.stryda.app.domain

import java.text.Normalizer

/** Colores de seda que aparecen en los programas. La clave es el nombre sin tildes, en minúscula. */
val COLORES_SEDA = listOf(
    "amarillo", "azul", "blanco", "burdeo", "cafe", "calipso", "celeste", "dorado", "fucsia", "granate",
    "gris", "lila", "morado", "naranjo", "negro", "plateado", "rojo", "rosado", "turquesa", "verde",
)

enum class Diseno { LISA, MANGAS, FRANJA, CRUZ, LUNARES, HOMBRERAS }

/**
 * Chaquetilla interpretada desde la descripción del programa.
 * "Naranjo franja y puños dorado" → principal naranjo, secundario dorado, franja y puños.
 */
data class Chaquetilla(
    val principal: String,
    val secundario: String?,
    val diseno: Diseno,
    val punos: Boolean,
)

private fun normalizar(palabra: String): String =
    Normalizer.normalize(palabra.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

fun interpretarChaquetilla(descripcion: String?): Chaquetilla? {
    val palabras = descripcion?.split(Regex("[\\s,.]+"))?.map(::normalizar)?.filter { it.isNotEmpty() } ?: return null
    val colores = palabras.map { if (it == "dorada") "dorado" else it }.filter { it in COLORES_SEDA }
    val principal = colores.firstOrNull() ?: return null
    val diseno = when {
        "cruz" in palabras -> Diseno.CRUZ
        palabras.any { it.startsWith("franja") || it == "banda" || it == "faja" } -> Diseno.FRANJA
        palabras.any { it.startsWith("lunar") || it.startsWith("estrella") } -> Diseno.LUNARES
        "hombreras" in palabras -> Diseno.HOMBRERAS
        "mangas" in palabras -> Diseno.MANGAS
        else -> Diseno.LISA
    }
    return Chaquetilla(
        principal = principal,
        secundario = colores.drop(1).firstOrNull { it != principal },
        diseno = diseno,
        punos = "punos" in palabras,
    )
}
