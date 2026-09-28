package cl.stryda.app.ui.screens.caballo

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CampoCaballo
import cl.stryda.app.domain.Elegibilidad
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.Medicion
import cl.stryda.app.domain.ResultadoEstimacion
import cl.stryda.app.domain.TipoMedicion
import cl.stryda.app.domain.TipoTrabajo
import cl.stryda.app.ui.theme.StrydaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HojasFichaTest {

    @get:Rule val regla = createComposeRule()

    private val hoy = LocalDate.of(2026, 9, 25)
    private val caballo = Caballo("cg-sos-de-boca", "c01", "Sos de Boca", null, 5, null, "Robert Bruce", null, EstadoCaballo.ACTIVO, null)

    private fun ficha(pesos: List<Medicion> = emptyList()) = FichaContenido(
        caballo = caballo, indicador = IndicadorEstado.EntrenaNormal, elegibilidad = Elegibilidad.Apto,
        peso = null, sesionMasRapida = null, peak = null, ultimaCarrera = null, diasDesdeUltimaCarrera = null,
        proximaCarrera = null, estimacion = ResultadoEstimacion.Insuficiente(0, 3), hoy = hoy, pesos = pesos,
    )

    @Test
    fun cambiarEstadoEntregaElNuevoEstadoYLaNota() {
        var guardado: Pair<EstadoCaballo, String?>? = null
        regla.setContent {
            StrydaTheme(oscuro = false) { HojaEstado(caballo, onGuardar = { e, n -> guardado = e to n }, onCerrar = {}) }
        }
        regla.onNodeWithText("Sin cambios").assertIsNotEnabled()
        regla.onNodeWithText("Lesionado").performClick()
        regla.onNodeWithText("Nota (opcional)").performTextInput("Tendón mano derecha")
        regla.onNodeWithText("Guardar: Lesionado").performClick()
        assertEquals(EstadoCaballo.LESIONADO to "Tendón mano derecha", guardado)
    }

    @Test
    fun pesoFueraDeRangoAvisaYNoDejaGuardar() {
        var peso: Double? = null
        regla.setContent {
            StrydaTheme(oscuro = false) { HojaPeso(ficha(), onGuardar = { p, _, _ -> peso = p }, onCerrar = {}) }
        }
        regla.onNodeWithText("Peso de hoy (kg)").performTextInput("48")
        regla.onNodeWithText("parece un error de tipeo", substring = true).assertExists()
        regla.onNodeWithText("Guardar pesaje").assertIsNotEnabled()

        regla.onNodeWithText("Peso de hoy (kg)").performTextReplacement("485,5")
        regla.onNodeWithText("Guardar pesaje").assertIsEnabled().performClick()
        assertEquals(485.5, peso!!, 0.001)
    }

    @Test
    fun historialDePesoMuestraRangoYVariacion() {
        val pesos = listOf(
            Medicion("a", caballo.id, hoy.minusDays(30), TipoMedicion.PESO_KG, 462.0),
            Medicion("b", caballo.id, hoy.minusDays(10), TipoMedicion.PESO_KG, 470.0, pendiente = true),
        )
        regla.setContent {
            StrydaTheme(oscuro = false) {
                HojaPeso(ficha(pesos).copy(rangoPeso = 462.0..470.0), onGuardar = { _, _, _ -> }, onCerrar = {})
            }
        }
        regla.onNodeWithText("Historial de peso").performScrollTo()
        regla.onNodeWithText("se movió entre 462,0 y 470,0 kg", substring = true).assertExists()
        regla.onNodeWithText("+8,0 kg").assertExists()
        regla.onNodeWithText("Registrado en el teléfono").assertExists()
    }

    @Test
    fun registrarAprontePorDigitosCalculaLaVelocidad() {
        var guardado: Triple<TipoTrabajo, Int, Double>? = null
        regla.setContent {
            StrydaTheme(oscuro = false) { HojaVelocidad(ficha(), onGuardar = { t, m, s -> guardado = Triple(t, m, s) }, onCerrar = {}) }
        }
        regla.onNodeWithText("Tiempo").performTextInput("486")
        regla.onNodeWithText("= 59,3 km/h").assertExists()
        regla.onNodeWithText("Guardar apronte").assertIsEnabled().performClick()
        assertEquals(TipoTrabajo.APRONTE, guardado!!.first)
        assertEquals(800, guardado!!.second)
        assertEquals(48.6, guardado!!.third, 0.001)
    }

    @Test
    fun editarDatosEntregaSoloLoQueCambio() {
        var cambios: Map<CampoCaballo, String?>? = null
        regla.setContent {
            StrydaTheme(oscuro = false) { HojaDatos(caballo, onGuardar = { cambios = it }, onCerrar = {}) }
        }
        regla.onNodeWithText("Alazán").performClick()
        regla.onNodeWithText("Macho").performClick()
        regla.onNodeWithText("Guardar 2 cambios").performClick()
        assertEquals(mapOf(CampoCaballo.PELAJE to "Alazán", CampoCaballo.SEXO to "macho"), cambios)
    }
}
