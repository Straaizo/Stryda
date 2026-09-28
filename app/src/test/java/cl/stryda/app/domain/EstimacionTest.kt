package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class EstimacionTest {

    private val caballo = caballo()
    private var n = 0

    private fun corrida(diasAtras: Long, distancia: Int, tiempo: Double?, posicion: Int? = 3): CarreraDelCaballo {
        val id = "car${n++}"
        return CarreraDelCaballo(
            Carrera(id, "Club Hípico de Santiago", HOY.minusDays(diasAtras), null, 5, distancia, null, null, null, null),
            Participacion("p$id", id, "k01", 4, null, 56.0, posicion, tiempo, null),
            caballo,
        )
    }

    private fun estimar(carreras: List<CarreraDelCaballo>, distancia: Int) =
        EstimadorEstadistico.estimar(carreras, distancia, HOY)

    @Test
    fun `con menos de tres carreras no estima`() {
        val r = estimar(listOf(corrida(60, 1200, 72.0), corrida(30, 1200, 71.5)), 1200)
        assertEquals(ResultadoEstimacion.Insuficiente(2, 3), r)
    }

    @Test
    fun `ignora carreras sin tiempo, sin posicion y futuras`() {
        val r = estimar(
            listOf(
                corrida(90, 1200, 72.0),
                corrida(60, 1200, null),
                corrida(30, 1200, 71.0, posicion = null),
                corrida(-3, 1200, null, posicion = null),
                corrida(10, 1200, 71.5),
            ),
            1200,
        )
        assertEquals(ResultadoEstimacion.Insuficiente(2, 3), r)
    }

    @Test
    fun `tiempos parejos estiman el mismo tiempo sin tendencia`() {
        val r = estimar(listOf(corrida(90, 1200, 72.0), corrida(60, 1200, 72.0), corrida(30, 1200, 72.0)), 1200)
        val e = (r as ResultadoEstimacion.Disponible).estimacion
        assertEquals(72.0, e.tiempoCentralSeg, 0.01)
        assertEquals(0.0, e.tendenciaSegPorCarrera, 0.01)
        assertTrue(e.tiempoMinSeg < e.tiempoCentralSeg && e.tiempoCentralSeg < e.tiempoMaxSeg)
    }

    @Test
    fun `caballo mejorando proyecta un tiempo menor y tendencia negativa`() {
        val r = estimar(
            listOf(corrida(120, 1200, 73.0), corrida(90, 1200, 72.5), corrida(60, 1200, 72.0), corrida(30, 1200, 71.5)),
            1200,
        )
        val e = (r as ResultadoEstimacion.Disponible).estimacion
        assertTrue(e.tiempoCentralSeg < 71.5)
        assertTrue(e.tendenciaSegPorCarrera < -0.3)
        assertEquals(4, e.carrerasUsadas)
    }

    @Test
    fun `lleva otras distancias a la objetivo con Riegel`() {
        val r = estimar(listOf(corrida(90, 1000, 60.0), corrida(60, 1000, 60.0), corrida(30, 1000, 60.0)), 1200)
        val e = (r as ResultadoEstimacion.Disponible).estimacion
        assertEquals(60.0 * 1.2.pow(1.06), e.tiempoCentralSeg, 0.01)
    }

    @Test
    fun `la proyeccion no se escapa de lo que el caballo ya mostro`() {
        // Mejora fuerte: la recta proyectada iría más allá de su mejor marca
        val r = estimar(listOf(corrida(90, 1200, 76.0), corrida(60, 1200, 74.0), corrida(30, 1200, 72.0)), 1200)
        val e = (r as ResultadoEstimacion.Disponible).estimacion
        val vMejor = 1200 / 72.0
        assertTrue(1200 / e.tiempoCentralSeg <= vMejor + 0.08 + 1e-9)
    }

    @Test
    fun `usa solo las ultimas ocho carreras`() {
        val viejas = (0 until 6).map { corrida(400L - it * 10, 1200, 80.0) }
        val recientes = (0 until 8).map { corrida(100L - it * 10, 1200, 72.0) }
        val e = (estimar(viejas + recientes, 1200) as ResultadoEstimacion.Disponible).estimacion
        assertEquals(8, e.carrerasUsadas)
        assertEquals(72.0, e.tiempoCentralSeg, 0.01)
    }

}
