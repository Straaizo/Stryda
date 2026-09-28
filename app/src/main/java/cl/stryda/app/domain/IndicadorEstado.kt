package cl.stryda.app.domain

import java.time.LocalDate

const val DIAS_CORRE_PRONTO = 7

/**
 * El único indicador que lleva la fila de un ejemplar en Mi corral.
 * Prioridad: corre pronto > lesionado > tratamiento > carga alta > descanso > normal.
 */
sealed interface IndicadorEstado {
    data class CorreEn(val dias: Long) : IndicadorEstado
    data object Lesionado : IndicadorEstado
    data class EnTratamiento(val retiroHasta: LocalDate) : IndicadorEstado
    data object CargaAlta : IndicadorEstado

    /** No está en la lista del documento: un ejemplar en descanso no "entrena normal". Supuesto a validar. */
    data object EnDescanso : IndicadorEstado
    data object EntrenaNormal : IndicadorEstado
}

fun indicadorEstado(
    caballo: Caballo,
    proximaCarrera: LocalDate?,
    tratamientos: List<Tratamiento>,
    sesiones: List<Sesion>,
    hoy: LocalDate,
): IndicadorEstado {
    val diasACarrera = proximaCarrera?.let { diasEntre(hoy, it) }
    if (diasACarrera != null && diasACarrera in 0..DIAS_CORRE_PRONTO) {
        return IndicadorEstado.CorreEn(diasACarrera)
    }
    if (caballo.estado == EstadoCaballo.LESIONADO) return IndicadorEstado.Lesionado
    retiroVigente(tratamientos, hoy)?.let { return IndicadorEstado.EnTratamiento(ultimoDiaRetiro(it)) }
    val ratio = ratioCargaReciente(sesiones, hoy)
    if (ratio != null && ratio >= UMBRAL_CARGA_ALTA) return IndicadorEstado.CargaAlta
    if (caballo.estado == EstadoCaballo.DESCANSANDO) return IndicadorEstado.EnDescanso
    return IndicadorEstado.EntrenaNormal
}
