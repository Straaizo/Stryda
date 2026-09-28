package cl.stryda.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class CargaSemanal(
    val inicioSemana: LocalDate,
    val porTipo: Map<TipoTrabajo, Double>,
) {
    val total: Double get() = porTipo.values.sum()
}

/** Unidades de carga de una sesión: peso del tipo ajustado por esfuerzo (3 = neutro). */
fun cargaSesion(sesion: Sesion): Double {
    val factor = sesion.esfuerzo?.coerceIn(1, 5)?.let { it / 3.0 } ?: 1.0
    return sesion.tipo.pesoCarga * factor
}

/**
 * Carga por semana (lunes a domingo) de las últimas [semanas], de la más antigua a la actual.
 * Las semanas sin sesiones aparecen con total 0: un hueco también es información.
 */
fun cargaSemanal(sesiones: List<Sesion>, hoy: LocalDate, semanas: Int = 12): List<CargaSemanal> {
    val lunesActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val primerLunes = lunesActual.minusWeeks((semanas - 1).toLong())
    val porSemana = sesiones
        .filter { !it.fecha.isBefore(primerLunes) && !it.fecha.isAfter(hoy) }
        .groupBy { it.fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

    return (0 until semanas).map { i ->
        val lunes = primerLunes.plusWeeks(i.toLong())
        val porTipo = porSemana[lunes].orEmpty()
            .groupBy { it.tipo }
            .mapValues { (_, lista) -> lista.sumOf(::cargaSesion) }
        CargaSemanal(lunes, porTipo)
    }
}

const val DIAS_VENTANA_RECIENTE = 14
const val DIAS_LINEA_BASE = 90
const val UMBRAL_CARGA_ALTA = 1.3
private const val MIN_SESIONES_LINEA_BASE = 10

/**
 * Carga de los últimos 14 días dividida por el promedio quincenal de los 90 días anteriores.
 * `null` si no hay línea base suficiente para comparar.
 */
fun ratioCargaReciente(sesiones: List<Sesion>, hoy: LocalDate): Double? {
    val inicioReciente = hoy.minusDays((DIAS_VENTANA_RECIENTE - 1).toLong())
    val inicioBase = inicioReciente.minusDays(DIAS_LINEA_BASE.toLong())

    val recientes = sesiones.filter { !it.fecha.isBefore(inicioReciente) && !it.fecha.isAfter(hoy) }
    val base = sesiones.filter { !it.fecha.isBefore(inicioBase) && it.fecha.isBefore(inicioReciente) }
    if (base.size < MIN_SESIONES_LINEA_BASE) return null

    val promedioQuincenal = base.sumOf(::cargaSesion) * DIAS_VENTANA_RECIENTE / DIAS_LINEA_BASE
    if (promedioQuincenal <= 0.0) return null
    return recientes.sumOf(::cargaSesion) / promedioQuincenal
}

fun diasEntre(desde: LocalDate, hasta: LocalDate): Long = ChronoUnit.DAYS.between(desde, hasta)
