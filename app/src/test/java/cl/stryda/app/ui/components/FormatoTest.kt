package cl.stryda.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FormatoTest {
    private val hoy = LocalDate.of(2026, 9, 25)

    @Test
    fun `fechas pasadas relativas`() {
        assertEquals("hoy", fechaRelativaPasada(hoy, hoy))
        assertEquals("ayer", fechaRelativaPasada(hoy.minusDays(1), hoy))
        assertEquals("hace 3 días", fechaRelativaPasada(hoy.minusDays(3), hoy))
        assertEquals("12 mar", fechaRelativaPasada(LocalDate.of(2026, 3, 12), hoy))
    }

    @Test
    fun `fechas futuras relativas`() {
        assertEquals("mañana", fechaRelativaFutura(hoy.plusDays(1), hoy))
        assertEquals("dom 27 sep", fechaRelativaFutura(hoy.plusDays(2), hoy))
    }

    @Test
    fun `tiempo en formato mm ss d`() {
        assertEquals("1:23.4", tiempoCarrera(83.4))
        assertEquals("48.6", tiempoCarrera(48.6))
        assertEquals("1:00.0", tiempoCarrera(59.99))
    }

    @Test
    fun `iniciales ignoran articulos`() {
        assertEquals("GR", iniciales("Gran Retamo"))
        assertEquals("TS", iniciales("Tronador del Sur"))
        assertEquals("MA", iniciales("Malulo"))
    }

    @Test
    fun `instante relativo`() {
        val ahora = Instant.parse("2026-09-25T15:00:00Z")
        assertEquals("hace un momento", instanteRelativo(ahora, ahora, ZoneOffset.UTC))
        assertEquals("hace 5 min", instanteRelativo(ahora.minusSeconds(300), ahora, ZoneOffset.UTC))
        assertEquals("hace 2 h", instanteRelativo(ahora.minusSeconds(7200), ahora, ZoneOffset.UTC))
        assertEquals("ayer", instanteRelativo(ahora.minusSeconds(86400), ahora, ZoneOffset.UTC))
    }
}
