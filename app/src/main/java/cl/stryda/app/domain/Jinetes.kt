package cl.stryda.app.domain

import java.time.LocalDate

/** Un jinete que ya corrió al caballo. */
data class JineteDelCaballo(
    val nombre: String,
    val montas: Int,
    val ultimaMonta: LocalDate,
    /** Mejor llegada con él. `null` si ninguna de sus montas tiene resultado cargado. */
    val mejorPosicion: Int?,
)

/** Quiénes lo han corrido: el que más lo montó primero, a igualdad el más reciente. */
fun jinetesDelCaballo(carreras: List<CarreraDelCaballo>, hoy: LocalDate): List<JineteDelCaballo> =
    carreras
        .filter { !it.participacion.jinete.isNullOrBlank() && (it.carrera.fecha.isBefore(hoy) || it.participacion.posicion != null) }
        .groupBy { it.participacion.jinete!!.trim() }
        .map { (nombre, montas) ->
            JineteDelCaballo(
                nombre = nombre,
                montas = montas.size,
                ultimaMonta = montas.maxOf { it.carrera.fecha },
                mejorPosicion = montas.mapNotNull { it.participacion.posicion }.minOrNull(),
            )
        }
        .sortedWith(compareByDescending<JineteDelCaballo> { it.montas }.thenByDescending { it.ultimaMonta })
