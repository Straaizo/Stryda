package cl.stryda.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SaludEIndicadorTest {

    @Test
    fun `dias de retiro que cruzan de mes`() {
        val t = tratamiento(LocalDate.of(2026, 1, 28), diasRetiro = 10)
        assertEquals(LocalDate.of(2026, 2, 7), ultimoDiaRetiro(t))
        assertEquals(t, retiroVigente(listOf(t), LocalDate.of(2026, 2, 7)))
        assertNull(retiroVigente(listOf(t), LocalDate.of(2026, 2, 8)))
    }

    @Test
    fun `tratamiento sin dias de retiro nunca bloquea`() {
        assertNull(retiroVigente(listOf(tratamiento(HOY, diasRetiro = 0)), HOY))
    }

    @Test
    fun `elegibilidad prioriza lesion sobre retiro`() {
        val t = tratamiento(HOY.minusDays(1), diasRetiro = 5)
        assertEquals(Elegibilidad.Lesionado, elegibilidad(caballo(EstadoCaballo.LESIONADO), listOf(t), HOY))
        assertEquals(Elegibilidad.EnRetiro(HOY.plusDays(4)), elegibilidad(caballo(), listOf(t), HOY))
        assertEquals(Elegibilidad.Apto, elegibilidad(caballo(), emptyList(), HOY))
    }

    @Test
    fun `corre pronto gana a lesionado y tratamiento`() {
        val ind = indicadorEstado(
            caballo(EstadoCaballo.LESIONADO), HOY.plusDays(2), listOf(tratamiento(HOY, 10)), emptyList(), HOY,
        )
        assertEquals(IndicadorEstado.CorreEn(2), ind)
    }

    @Test
    fun `carrera a mas de 7 dias no cuenta como corre pronto`() {
        assertEquals(
            IndicadorEstado.EntrenaNormal,
            indicadorEstado(caballo(), HOY.plusDays(8), emptyList(), emptyList(), HOY),
        )
    }

    @Test
    fun `lesionado gana a tratamiento`() {
        assertEquals(
            IndicadorEstado.Lesionado,
            indicadorEstado(caballo(EstadoCaballo.LESIONADO), null, listOf(tratamiento(HOY, 10)), emptyList(), HOY),
        )
    }

    @Test
    fun `tratamiento gana a carga alta`() {
        val sesiones = cargaAltaDeEjemplo()
        val ind = indicadorEstado(caballo(), null, listOf(tratamiento(HOY.minusDays(1), 10)), sesiones, HOY)
        assertEquals(IndicadorEstado.EnTratamiento(HOY.plusDays(9)), ind)
    }

    @Test
    fun `carga alta dispara sobre 30 por ciento y no por debajo`() {
        assertEquals(IndicadorEstado.CargaAlta, indicadorEstado(caballo(), null, emptyList(), cargaAltaDeEjemplo(), HOY))

        val parejo = sesionesDiarias(HOY.minusDays(103), HOY)
        assertEquals(IndicadorEstado.EntrenaNormal, indicadorEstado(caballo(), null, emptyList(), parejo, HOY))
    }

    @Test
    fun `en descanso no se muestra como entrena normal`() {
        assertEquals(
            IndicadorEstado.EnDescanso,
            indicadorEstado(caballo(EstadoCaballo.DESCANSANDO), null, emptyList(), emptyList(), HOY),
        )
    }

    /** Base de galope suave (3/día) y últimas 2 semanas en apronte (5/día): ratio 1.67. */
    private fun cargaAltaDeEjemplo() =
        sesionesDiarias(HOY.minusDays(103), HOY.minusDays(14)) +
            sesionesDiarias(HOY.minusDays(13), HOY, TipoTrabajo.APRONTE)
}
