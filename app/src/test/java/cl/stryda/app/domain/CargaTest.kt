package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CargaTest {

    @Test
    fun `carga semanal incluye semanas vacias intercaladas`() {
        val sesiones = listOf(
            sesion(HOY, TipoTrabajo.APRONTE),                 // semana actual
            sesion(HOY.minusWeeks(2), TipoTrabajo.VAREO),     // hace 2 semanas
            // hace 1 semana: vacía a propósito
        )
        val semanas = cargaSemanal(sesiones, HOY, semanas = 3)

        assertEquals(3, semanas.size)
        assertEquals(1.0, semanas[0].total, 0.001)
        assertEquals(0.0, semanas[1].total, 0.001)
        assertTrue(semanas[1].porTipo.isEmpty())
        assertEquals(5.0, semanas[2].total, 0.001)
    }

    @Test
    fun `semanas empiezan en lunes y van de la mas antigua a la actual`() {
        val semanas = cargaSemanal(emptyList(), HOY, semanas = 12)
        assertEquals(12, semanas.size)
        assertTrue(semanas.all { it.inicioSemana.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(LocalDate.of(2026, 9, 21), semanas.last().inicioSemana)
        assertTrue(semanas.zipWithNext().all { (a, b) -> a.inicioSemana.isBefore(b.inicioSemana) })
    }

    @Test
    fun `carga separa por tipo de trabajo`() {
        val sesiones = listOf(sesion(HOY, TipoTrabajo.VAREO), sesion(HOY.minusDays(1), TipoTrabajo.VAREO), sesion(HOY, TipoTrabajo.TROTE))
        val actual = cargaSemanal(sesiones, HOY, semanas = 1).single()
        assertEquals(2.0, actual.porTipo[TipoTrabajo.VAREO]!!, 0.001)
        assertEquals(2.0, actual.porTipo[TipoTrabajo.TROTE]!!, 0.001)
    }

    @Test
    fun `esfuerzo ajusta la carga y sin esfuerzo cuenta como medio`() {
        assertEquals(5.0, cargaSesion(sesion(HOY, TipoTrabajo.APRONTE)), 0.001)
        assertEquals(5.0, cargaSesion(sesion(HOY, TipoTrabajo.APRONTE, esfuerzo = 3)), 0.001)
        assertEquals(5.0 * 5 / 3, cargaSesion(sesion(HOY, TipoTrabajo.APRONTE, esfuerzo = 5)), 0.001)
        assertEquals(0.0, cargaSesion(sesion(HOY, TipoTrabajo.DESCANSO, esfuerzo = 5)), 0.001)
    }

    @Test
    fun `ratio de carga detecta dos semanas 40 por ciento sobre su promedio`() {
        val base = sesionesDiarias(HOY.minusDays(103), HOY.minusDays(14), TipoTrabajo.GALOPE_SUAVE)
        val recientes = sesionesDiarias(HOY.minusDays(13), HOY, TipoTrabajo.GALOPE_SUAVE) +
            sesionesDiarias(HOY.minusDays(13), HOY.minusDays(8), TipoTrabajo.VAREO) // +6 unidades sobre 42
        val ratio = ratioCargaReciente(base + recientes, HOY)
        assertNotNull(ratio)
        assertEquals(48.0 / 42.0, ratio!!, 0.001)
    }

    @Test
    fun `ratio de carga es null sin linea base suficiente`() {
        val soloRecientes = sesionesDiarias(HOY.minusDays(13), HOY)
        assertNull(ratioCargaReciente(soloRecientes, HOY))
    }
}
