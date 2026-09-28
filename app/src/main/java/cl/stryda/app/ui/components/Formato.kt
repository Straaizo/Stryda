package cl.stryda.app.ui.components

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val MESES_LARGOS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

/** "12 mar" */
fun fechaCorta(fecha: LocalDate): String = "${fecha.dayOfMonth} ${MESES[fecha.monthValue - 1]}"

/** "14 de marzo" */
fun fechaLarga(fecha: LocalDate): String = "${fecha.dayOfMonth} de ${MESES_LARGOS[fecha.monthValue - 1]}"

/** Pasado: hoy · ayer · hace 3 días · 12 mar */
fun fechaRelativaPasada(fecha: LocalDate, hoy: LocalDate): String {
    val dias = ChronoUnit.DAYS.between(fecha, hoy)
    return when {
        dias == 0L -> "hoy"
        dias == 1L -> "ayer"
        dias in 2..6 -> "hace $dias días"
        else -> fechaCorta(fecha)
    }
}

/** Futuro: hoy · mañana · sáb 27 sep */
fun fechaRelativaFutura(fecha: LocalDate, hoy: LocalDate): String {
    val dias = ChronoUnit.DAYS.between(hoy, fecha)
    return when (dias) {
        0L -> "hoy"
        1L -> "mañana"
        else -> "${DIAS[fecha.dayOfWeek.value - 1]} ${fechaCorta(fecha)}"
    }
}

fun hora(hora: LocalTime): String = "%02d:%02d".format(hora.hour, hora.minute)

/** Segundos → "1:23.4" (mm:ss.d). */
fun tiempoCarrera(segundos: Double): String {
    val decimas = Math.round(segundos * 10)
    val min = decimas / 600
    val seg = (decimas % 600) / 10
    val dec = decimas % 10
    return if (min > 0) "%d:%02d.%d".format(min, seg, dec) else "%d.%d".format(seg, dec)
}

/** "hoy 06:12" · "ayer 18:03" · "12 mar 06:12" */
fun momento(instante: Instant, hoy: LocalDate, zona: ZoneId = ZoneId.systemDefault()): String {
    val local = instante.atZone(zona)
    val dia = when (ChronoUnit.DAYS.between(local.toLocalDate(), hoy)) {
        0L -> "hoy"
        1L -> "ayer"
        else -> fechaCorta(local.toLocalDate())
    }
    return "$dia ${hora(local.toLocalTime())}"
}

/** "hace un momento" · "hace 5 min" · "hace 2 h" · "ayer" · "12 mar" */
fun instanteRelativo(instante: Instant, ahora: Instant, zona: ZoneId = ZoneId.systemDefault()): String {
    val minutos = Duration.between(instante, ahora).toMinutes()
    return when {
        minutos < 1 -> "hace un momento"
        minutos < 60 -> "hace $minutos min"
        minutos < 24 * 60 && instante.atZone(zona).toLocalDate() == ahora.atZone(zona).toLocalDate() -> "hace ${minutos / 60} h"
        else -> fechaRelativaPasada(instante.atZone(zona).toLocalDate(), ahora.atZone(zona).toLocalDate())
    }
}

private val LOCALE_CL = java.util.Locale.forLanguageTag("es-CL")

/** Coma decimal chilena: 485.24 → "485,2". */
fun decimal(valor: Double, decimales: Int = 1): String = "%.${decimales}f".format(LOCALE_CL, valor)

/** m/s → km/h, la unidad que se usa en la pista. */
fun aKmH(metrosPorSegundo: Double): Double = metrosPorSegundo * 3.6

/** Iniciales para el avatar: "Gran Retamo" → "GR", "Malulo" → "MA". */
fun iniciales(nombre: String): String {
    val palabras = nombre.split(" ").filter { it.isNotBlank() && it.lowercase() !in setOf("de", "del", "la", "el", "los", "las") }
    return when {
        palabras.size >= 2 -> "${palabras[0].first()}${palabras[1].first()}"
        palabras.size == 1 -> palabras[0].take(2)
        else -> "?"
    }.uppercase()
}
