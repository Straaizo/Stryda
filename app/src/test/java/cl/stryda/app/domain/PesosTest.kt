package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PesosTest {

    private var n = 0

    private fun corrida(diasAtras: Long, posicion: Int?, distancia: Int = 1200): CarreraDelCaballo {
        val id = "car${n++}"
        return CarreraDelCaballo(
            Carrera(id, "Club Hípico de Santiago", HOY.minusDays(diasAtras), null, 5, distancia, null, null, null, null),
            Participacion("p$id", id, "k01", 4, null, 56.0, posicion, null, null),
            caballo(),
            corredores = 12,
        )
    }

    @Test
    fun `cruza cada carrera con el pesaje del mismo dia`() {
        val mediciones = listOf(peso(HOY.minusDays(40), 470.0), peso(HOY.minusDays(20), 462.0), peso(HOY.minusDays(5), 466.0))
        val r = pesosEnCarreras(mediciones, listOf(corrida(40, 1), corrida(20, 9), corrida(10, 3)))
        assertEquals(listOf(470.0, 462.0), r.map { it.kg })
        assertEquals(listOf(1, 9), r.map { it.posicion })
    }

    @Test
    fun `carrera sin resultado no cuenta`() {
        val r = pesosEnCarreras(listOf(peso(HOY, 470.0)), listOf(corrida(0, null)))
        assertEquals(emptyList<PesoEnCarrera>(), r)
    }

    @Test
    fun `mejor carrera elige la mejor posicion y a igualdad la mas reciente`() {
        val pesos = listOf(
            PesoEnCarrera(HOY.minusDays(60), 470.0, 1, 10, 1200),
            PesoEnCarrera(HOY.minusDays(30), 465.0, 4, 10, 1200),
            PesoEnCarrera(HOY.minusDays(10), 468.0, 1, 12, 1000),
        )
        assertEquals(468.0, mejorCarreraConPeso(pesos)!!.kg, 0.001)
        assertNull(mejorCarreraConPeso(emptyList()))
    }

    @Test
    fun `rango de peso en la ventana y null con un solo pesaje`() {
        val mediciones = listOf(peso(HOY.minusDays(300), 430.0), peso(HOY.minusDays(50), 462.0), peso(HOY.minusDays(5), 470.0))
        val r = rangoPeso(mediciones, HOY)!!
        assertEquals(462.0, r.start, 0.001)
        assertEquals(470.0, r.endInclusive, 0.001)
        assertNull(rangoPeso(listOf(peso(HOY, 470.0)), HOY))
    }

    @Test
    fun `historial ignora otras mediciones y ordena por fecha`() {
        val temp = Medicion("t", "k01", HOY, TipoMedicion.TEMPERATURA, 37.8)
        val h = historialPeso(listOf(peso(HOY, 2.0), temp, peso(HOY.minusDays(3), 1.0)))
        assertEquals(listOf(1.0, 2.0), h.map { it.valor })
    }
}
