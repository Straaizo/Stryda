package cl.stryda.app.ui.screens.caballo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.Carrera
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Elegibilidad
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.Estimacion
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.Participacion
import cl.stryda.app.domain.PesoActual
import cl.stryda.app.domain.ResultadoEstimacion
import cl.stryda.app.domain.Sexo
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.theme.StrydaTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class FichaScreenTest {

    @get:Rule val regla = createComposeRule()

    private val hoy = LocalDate.of(2026, 9, 25)
    private val retamo = Caballo("k01", "c01", "Gran Retamo", Sexo.MACHO, 4, "Castaño", null, null, EstadoCaballo.ACTIVO, null)

    private val ultima = CarreraDelCaballo(
        Carrera("car1", "Club Hípico de Santiago", hoy.minusDays(26), null, 7, 1400, "arena", null, null, null),
        Participacion("p1", "car1", "k01", 1, "Diego Carrasco", 56.0, 1, 84.0, null),
        retamo,
        corredores = 12,
    )

    private fun mostrar(contenido: FichaContenido) {
        regla.setContent {
            StrydaTheme(oscuro = false) { FichaContenidoUi(UiState.Contenido(contenido), {}, {}) }
        }
    }

    @Test
    fun muestraIdentidadCifrasUltimaCarreraYEstimacion() {
        mostrar(
            FichaContenido(
                caballo = retamo,
                indicador = IndicadorEstado.EntrenaNormal,
                elegibilidad = Elegibilidad.Apto,
                peso = PesoActual(485.0, hoy.minusDays(4), 4, 1.7),
                sesionMasRapida = null,
                peak = null,
                ultimaCarrera = ultima,
                diasDesdeUltimaCarrera = 26,
                proximaCarrera = null,
                estimacion = ResultadoEstimacion.Disponible(Estimacion(1400, 83.2, 82.6, 83.9, 5, -0.6)),
                hoy = hoy,
            ),
        )
        regla.onNodeWithText("Gran Retamo").assertIsDisplayed()
        regla.onNodeWithText("Castaño · Macho · 4 años").assertIsDisplayed()
        regla.onNodeWithText("Apto para correr").assertIsDisplayed()
        regla.onNodeWithContentDescription("Peso, 485,0 kg, +1,7 kg vs. mes pasado").assertIsDisplayed()
        regla.onNodeWithText("Jinete: Diego Carrasco · Nº 1 · 56 kg").performScrollTo().assertIsDisplayed()
        regla.onNodeWithText("de 12").assertExists()
        regla.onNodeWithText("1:23.2").performScrollTo().assertIsDisplayed()
        regla.onNodeWithText("Sus tiempos vienen bajando unos 0,6 s por carrera.").assertExists()
    }

    @Test
    fun debutanteSinDatosNoMuestraCerosFalsos() {
        mostrar(
            FichaContenido(
                caballo = retamo.copy(nombre = "Cóndor Andino", pelaje = null),
                indicador = IndicadorEstado.EntrenaNormal,
                elegibilidad = Elegibilidad.Apto,
                peso = null,
                sesionMasRapida = null,
                peak = null,
                ultimaCarrera = null,
                diasDesdeUltimaCarrera = null,
                proximaCarrera = null,
                estimacion = ResultadoEstimacion.Insuficiente(0, 3),
                hoy = hoy,
            ),
        )
        regla.onNodeWithContentDescription("Peso, sin dato, Sin pesajes registrados").assertIsDisplayed()
        regla.onNodeWithContentDescription("Peak en carrera, sin dato, Sin datos de parciales").assertIsDisplayed()
        regla.onNodeWithContentDescription("Desde su última carrera, sin dato, Aún no debuta").assertIsDisplayed()
        regla.onNodeWithText("Última carrera").assertDoesNotExist()
        regla.onNodeWithText("Todavía no tiene carreras con tiempo", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tocarElPesoAbreSuHistorial() {
        mostrar(
            FichaContenido(
                caballo = retamo, indicador = IndicadorEstado.EntrenaNormal, elegibilidad = Elegibilidad.Apto,
                peso = PesoActual(485.0, hoy.minusDays(4), 4, 1.7), sesionMasRapida = null, peak = null,
                ultimaCarrera = null, diasDesdeUltimaCarrera = null, proximaCarrera = null,
                estimacion = ResultadoEstimacion.Insuficiente(0, 3), hoy = hoy,
            ),
        )
        regla.onNodeWithContentDescription("Peso, 485,0 kg", substring = true).performClick()
        regla.onNodeWithText("Peso de Gran Retamo").assertIsDisplayed()
        regla.onNodeWithText("Guardar pesaje").assertExists()
    }

    @Test
    fun enRetiroMuestraHastaCuando() {
        mostrar(
            FichaContenido(
                caballo = retamo,
                indicador = IndicadorEstado.EnTratamiento(hoy.plusDays(7)),
                elegibilidad = Elegibilidad.EnRetiro(LocalDate.of(2026, 10, 2)),
                peso = null, sesionMasRapida = null, peak = null, ultimaCarrera = null,
                diasDesdeUltimaCarrera = null, proximaCarrera = null,
                estimacion = ResultadoEstimacion.Insuficiente(0, 3), hoy = hoy,
            ),
        )
        regla.onNodeWithText("En período de retiro hasta el 2 de octubre").assertIsDisplayed()
    }
}
