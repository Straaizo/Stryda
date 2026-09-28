package cl.stryda.app.domain

import java.time.LocalDate

/**
 * Último día del período de retiro. El día del tratamiento cuenta como día 0.
 * Supuesto de la demo: con 10 días de retiro aplicados el 1 de marzo, el retiro dura hasta el 11
 * y el ejemplar queda apto el 12.
 */
fun ultimoDiaRetiro(tratamiento: Tratamiento): LocalDate =
    tratamiento.fecha.plusDays(tratamiento.diasRetiro.toLong())

/** Tratamiento con el retiro más largo aún vigente hoy. */
fun retiroVigente(tratamientos: List<Tratamiento>, hoy: LocalDate): Tratamiento? =
    tratamientos
        .filter { it.diasRetiro > 0 && !it.fecha.isAfter(hoy) && !ultimoDiaRetiro(it).isBefore(hoy) }
        .maxByOrNull { ultimoDiaRetiro(it) }

sealed interface Elegibilidad {
    data object Apto : Elegibilidad
    data class EnRetiro(val hasta: LocalDate) : Elegibilidad
    data object Lesionado : Elegibilidad
    data object NoDisponible : Elegibilidad
}

fun elegibilidad(caballo: Caballo, tratamientos: List<Tratamiento>, hoy: LocalDate): Elegibilidad = when {
    caballo.estado == EstadoCaballo.LESIONADO -> Elegibilidad.Lesionado
    caballo.estado == EstadoCaballo.RETIRADO -> Elegibilidad.NoDisponible
    else -> retiroVigente(tratamientos, hoy)
        ?.let { Elegibilidad.EnRetiro(ultimoDiaRetiro(it)) }
        ?: Elegibilidad.Apto
}
