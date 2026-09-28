package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RendimientoTest {

    @Test
    fun `velocidad maxima es null si ninguna sesion tiene tiempo`() {
        val sesiones = listOf(
            sesion(HOY, TipoTrabajo.APRONTE, distanciaM = 800, tiempoSeg = null),
            sesion(HOY, TipoTrabajo.GALOPE_SUAVE, distanciaM = 2400),
        )
        assertNull(velocidadMaxima(sesiones))
    }

    @Test
    fun `velocidad maxima toma el mejor entre sesiones y parciales`() {
        val sesiones = listOf(sesion(HOY, TipoTrabajo.APRONTE, distanciaM = 800, tiempoSeg = 50.0)) // 16.0 m/s
        val parciales = listOf(Parcial("p1", 200, 12.0, 17.2), Parcial("p1", 400, 24.0, 16.5))
        assertEquals(16.0, velocidadMaxima(sesiones)!!, 0.001)
        assertEquals(17.2, velocidadMaxima(sesiones, parciales)!!, 0.001)
    }

    @Test
    fun `tiempo record por distancia con una sola muestra`() {
        val unica = sesion(HOY, TipoTrabajo.APRONTE, distanciaM = 1000, tiempoSeg = 61.4)
        val records = mejoresTiemposPorDistancia(listOf(unica))
        assertEquals(1, records.size)
        assertEquals(61.4, records[1000]!!.tiempoSeg!!, 0.001)
    }

    @Test
    fun `tiempo record ignora trabajos no cronometrados y elige el menor`() {
        val records = mejoresTiemposPorDistancia(
            listOf(
                sesion(HOY, TipoTrabajo.APRONTE, distanciaM = 800, tiempoSeg = 50.1),
                sesion(HOY, TipoTrabajo.APRONTE, distanciaM = 800, tiempoSeg = 49.3),
                sesion(HOY, TipoTrabajo.GALOPE_FUERTE, distanciaM = 800, tiempoSeg = 40.0),
            ),
        )
        assertEquals(49.3, records[800]!!.tiempoSeg!!, 0.001)
    }

    @Test
    fun `peso actual con ultima medicion de hace tres meses queda marcado como antiguo`() {
        val mediciones = listOf(peso(HOY.minusDays(120), 480.0), peso(HOY.minusDays(92), 484.0))
        val actual = pesoActual(mediciones, HOY)!!
        assertEquals(484.0, actual.valorKg, 0.001)
        assertEquals(92L, actual.diasDesdeMedicion)
        assertTrue(actual.esAntiguo)
        assertEquals(4.0, actual.variacionMesKg!!, 0.001)
    }

    @Test
    fun `peso actual sin medicion anterior no inventa variacion`() {
        val actual = pesoActual(listOf(peso(HOY.minusDays(2), 470.0)), HOY)!!
        assertNull(actual.variacionMesKg)
        assertEquals(false, actual.esAntiguo)
    }

    @Test
    fun `peso actual es null sin mediciones de peso`() {
        val temperatura = Medicion("m", "k01", HOY, TipoMedicion.TEMPERATURA, 37.8)
        assertNull(pesoActual(listOf(temperatura), HOY))
    }
}
