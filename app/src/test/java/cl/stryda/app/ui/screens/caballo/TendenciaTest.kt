package cl.stryda.app.ui.screens.caballo

import org.junit.Assert.assertEquals
import org.junit.Test

class TendenciaTest {
    @Test
    fun `describe la tendencia sin recomendar`() {
        assertEquals("Sus tiempos se mantienen parejos en las últimas 5 carreras.", textoTendencia(0.05, 5))
        assertEquals("Sus tiempos vienen bajando unos 0,6 s por carrera.", textoTendencia(-0.6, 5))
        assertEquals("Sus tiempos vienen subiendo unos 1,2 s por carrera.", textoTendencia(1.2, 4))
    }
}
