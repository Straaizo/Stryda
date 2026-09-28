package cl.stryda.app.domain

import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.sqrt

data class Estimacion(
    val distanciaM: Int,
    val tiempoCentralSeg: Double,
    val tiempoMinSeg: Double,
    val tiempoMaxSeg: Double,
    val carrerasUsadas: Int,
    /** Segundos por carrera a la distancia objetivo. Negativo = sus tiempos bajan (mejora). */
    val tendenciaSegPorCarrera: Double,
)

sealed interface ResultadoEstimacion {
    data class Disponible(val estimacion: Estimacion) : ResultadoEstimacion
    data class Insuficiente(val carrerasConTiempo: Int, val minimo: Int) : ResultadoEstimacion
}

/**
 * Contrato para cualquier estimador. Hoy: [EstimadorEstadistico].
 * Mañana: un modelo entrenado con datos reales, sin tocar la UI.
 */
fun interface Estimador {
    fun estimar(carreras: List<CarreraDelCaballo>, distanciaObjetivo: Int, hoy: LocalDate): ResultadoEstimacion
}

/**
 * Tiempo esperado a una distancia según el propio historial del caballo. Nunca lo compara con otros.
 *
 * 1. Toma sus últimas carreras corridas con tiempo.
 * 2. Lleva cada una a la distancia objetivo con la fórmula de Riegel (t2 = t1 · (d2/d1)^1.06).
 * 3. Ajusta una recta sobre la velocidad en el orden de las carreras y proyecta la siguiente.
 * 4. El margen es la dispersión de sus propios resultados alrededor de esa recta.
 */
object EstimadorEstadistico : Estimador {
    const val MINIMO_CARRERAS = 3
    private const val MAXIMO_CARRERAS = 8
    private const val EXPONENTE_RIEGEL = 1.06
    private const val DISPERSION_MINIMA_MS = 0.08

    override fun estimar(carreras: List<CarreraDelCaballo>, distanciaObjetivo: Int, hoy: LocalDate): ResultadoEstimacion {
        val muestras = carreras
            .filter { c ->
                c.participacion.posicion != null &&
                    (c.participacion.tiempoSeg ?: 0.0) > 0 &&
                    c.carrera.distanciaM > 0 &&
                    !c.carrera.fecha.isAfter(hoy)
            }
            .sortedBy { it.carrera.fecha }
            .takeLast(MAXIMO_CARRERAS)
        if (muestras.size < MINIMO_CARRERAS || distanciaObjetivo <= 0) {
            return ResultadoEstimacion.Insuficiente(muestras.size, MINIMO_CARRERAS)
        }

        // Velocidad equivalente a la distancia objetivo
        val v = muestras.map { c ->
            val tObjetivo = c.participacion.tiempoSeg!! * (distanciaObjetivo.toDouble() / c.carrera.distanciaM).pow(EXPONENTE_RIEGEL)
            distanciaObjetivo / tObjetivo
        }
        val n = v.size
        val x = (0 until n).map { it.toDouble() }
        val mx = x.average()
        val mv = v.average()
        val sxx = x.sumOf { (it - mx).pow(2) }
        val pendiente = x.indices.sumOf { (x[it] - mx) * (v[it] - mv) } / sxx
        val intercepto = mv - pendiente * mx

        val residuos = x.indices.map { v[it] - (intercepto + pendiente * x[it]) }
        val dispersion = maxOf(sqrt(residuos.sumOf { it * it } / (n - 2).coerceAtLeast(1)), DISPERSION_MINIMA_MS)

        // Se proyecta una carrera hacia adelante, sin salirse de lo que el caballo ya mostró
        val vProyectada = (intercepto + pendiente * n).coerceIn(v.min() - dispersion, v.max() + dispersion)

        val central = distanciaObjetivo / vProyectada
        return ResultadoEstimacion.Disponible(
            Estimacion(
                distanciaM = distanciaObjetivo,
                tiempoCentralSeg = central,
                tiempoMinSeg = distanciaObjetivo / (vProyectada + dispersion),
                tiempoMaxSeg = distanciaObjetivo / (vProyectada - dispersion),
                carrerasUsadas = n,
                tendenciaSegPorCarrera = -distanciaObjetivo * pendiente / (vProyectada * vProyectada),
            ),
        )
    }
}
