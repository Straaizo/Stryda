package cl.stryda.app.domain

import java.time.LocalDate

/** Velocidad máxima registrada en m/s, en sesiones cronometradas o parciales de carrera. */
fun velocidadMaxima(sesiones: List<Sesion>, parciales: List<Parcial> = emptyList()): Double? {
    val deSesiones = sesiones.mapNotNull { s ->
        val d = s.distanciaM
        val t = s.tiempoSeg
        if (d != null && d > 0 && t != null && t > 0) d / t else null
    }
    val deParciales = parciales.map { it.velocidadMs }.filter { it > 0 }
    return (deSesiones + deParciales).maxOrNull()
}

/** La sesión cronometrada con mejor velocidad media (la que define la velocidad máxima en trabajos). */
fun sesionMasRapida(sesiones: List<Sesion>): Sesion? =
    sesiones
        .filter { (it.distanciaM ?: 0) > 0 && (it.tiempoSeg ?: 0.0) > 0 }
        .maxByOrNull { it.distanciaM!! / it.tiempoSeg!! }

/** Velocidad instantánea más alta medida en carrera (parciales GMAX). Nulo si no hay parciales. */
fun peakEnCarrera(parciales: List<Parcial>): Parcial? = parciales.filter { it.velocidadMs > 0 }.maxByOrNull { it.velocidadMs }

/** Mejor (menor) tiempo por distancia en aprontes y huinchadas. */
fun mejoresTiemposPorDistancia(sesiones: List<Sesion>): Map<Int, Sesion> =
    sesiones
        .filter { it.tipo.esCronometrado && it.distanciaM != null && (it.tiempoSeg ?: 0.0) > 0 }
        .groupBy { it.distanciaM!! }
        .mapValues { (_, lista) -> lista.minBy { it.tiempoSeg!! } }
        .toSortedMap()

data class PesoActual(
    val valorKg: Double,
    val fecha: LocalDate,
    val diasDesdeMedicion: Long,
    /** Diferencia contra la medición de hace un mes o más. `null` si no hay con qué comparar. */
    val variacionMesKg: Double?,
) {
    val esAntiguo: Boolean get() = diasDesdeMedicion > DIAS_PESO_VIGENTE
}

const val DIAS_PESO_VIGENTE = 30
private const val DIAS_COMPARACION_PESO = 28

fun pesoActual(mediciones: List<Medicion>, hoy: LocalDate): PesoActual? {
    val pesos = mediciones
        .filter { it.tipo == TipoMedicion.PESO_KG && !it.fecha.isAfter(hoy) }
        .sortedBy { it.fecha }
    val ultimo = pesos.lastOrNull() ?: return null
    val referencia = pesos.lastOrNull { !it.fecha.isAfter(ultimo.fecha.minusDays(DIAS_COMPARACION_PESO.toLong())) }
    return PesoActual(
        valorKg = ultimo.valor,
        fecha = ultimo.fecha,
        diasDesdeMedicion = diasEntre(ultimo.fecha, hoy),
        variacionMesKg = referencia?.let { ultimo.valor - it.valor },
    )
}

/** Días desde la última carrera corrida (con posición). `null` si nunca ha corrido. */
fun diasDesdeUltimaCarrera(carreras: List<CarreraDelCaballo>, hoy: LocalDate): Long? =
    carreras
        .filter { it.participacion.posicion != null && !it.carrera.fecha.isAfter(hoy) }
        .maxOfOrNull { it.carrera.fecha }
        ?.let { diasEntre(it, hoy) }

/** Próxima carrera programada desde hoy (inclusive). */
fun proximaCarrera(carreras: List<CarreraDelCaballo>, hoy: LocalDate): CarreraDelCaballo? =
    carreras
        .filter { it.participacion.posicion == null && !it.carrera.fecha.isBefore(hoy) }
        .minWithOrNull(compareBy({ it.carrera.fecha }, { it.carrera.hora }))
