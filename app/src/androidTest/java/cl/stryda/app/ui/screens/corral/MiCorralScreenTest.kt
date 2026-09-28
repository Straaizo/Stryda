package cl.stryda.app.ui.screens.corral

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cl.stryda.app.data.local.EstadoDatos
import cl.stryda.app.data.local.OrigenDatos
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.Carrera
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Corral
import cl.stryda.app.domain.EstadoCaballo
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.Participacion
import cl.stryda.app.domain.Sexo
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.theme.StrydaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class MiCorralScreenTest {

    @get:Rule val regla = createComposeRule()

    private val hoy = LocalDate.of(2026, 9, 25)
    private val corral = Corral("c01", "Corral Los Aromos", "Jorge Valenzuela", "Club Hípico de Santiago")
    private val retamo = Caballo("k01", "c01", "Gran Retamo", Sexo.MACHO, 4, null, null, null, EstadoCaballo.ACTIVO, null)
    private val tronador = Caballo("k03", "c01", "Tronador del Sur", Sexo.MACHO, 5, null, null, null, EstadoCaballo.LESIONADO, null)

    private fun contenido(correnPronto: List<CarreraDelCaballo>) = UiState.Contenido(
        MiCorralContenido(
            corral = corral,
            filas = listOf(
                FilaCaballo(retamo, if (correnPronto.isEmpty()) IndicadorEstado.EntrenaNormal else IndicadorEstado.CorreEn(2)),
                FilaCaballo(tronador, IndicadorEstado.Lesionado),
            ),
            correnPronto = correnPronto,
            datos = EstadoDatos(Instant.now(), OrigenDatos.PRUEBA),
            hoy = hoy,
            ahora = Instant.now(),
        ),
    )

    private val carrera = CarreraDelCaballo(
        Carrera("car1", "Club Hípico de Santiago", hoy.plusDays(2), LocalTime.of(17, 0), 13, 1200, "arena", null, null, null),
        Participacion("p1", "car1", "k01", 4, "Joaquín Reyes", 56.0, null, null, null),
        retamo,
    )

    private fun mostrar(estado: UiState<MiCorralContenido>, onRegistrar: () -> Unit = {}, onCaballo: (String) -> Unit = {}) {
        regla.setContent {
            StrydaTheme(oscuro = false) {
                MiCorralContenidoUi(estado, { id, _ -> onCaballo(id) }, {}, onRegistrar, {})
            }
        }
    }

    @Test
    fun franjaNoExisteSiNadieCorre() {
        mostrar(contenido(emptyList()))
        regla.onNodeWithText("Corren esta semana").assertDoesNotExist()
        regla.onNodeWithContentDescription("Gran Retamo", substring = true).assertIsDisplayed()
    }

    @Test
    fun franjaApareceConLaCarrera() {
        mostrar(contenido(listOf(carrera)))
        regla.onNodeWithText("Corren esta semana").assertIsDisplayed()
        regla.onNodeWithContentDescription("Gran Retamo, Corre en 2 días", substring = true).assertIsDisplayed()
    }

    @Test
    fun filaLlevaUnSoloIndicadorConTextoLegibleParaTalkBack() {
        mostrar(contenido(emptyList()))
        regla.onNodeWithContentDescription("Tronador del Sur, 5 años · Macho, Lesionado").assertIsDisplayed()
    }

    @Test
    fun tocarFilaAbreElCaballo() {
        var abierto: String? = null
        mostrar(contenido(emptyList()), onCaballo = { abierto = it })
        regla.onNodeWithContentDescription("Gran Retamo, 4 años · Macho, Entrena normal").performClick()
        assertEquals("k01", abierto)
    }

    @Test
    fun botonRegistrarTrabajoVisible() {
        var tocado = false
        mostrar(contenido(emptyList()), onRegistrar = { tocado = true })
        regla.onNodeWithText("Registrar trabajo de hoy", useUnmergedTree = true).performClick()
        assertEquals(true, tocado)
    }

    @Test
    fun estadoVacioExplicaYOfreceAccion() {
        mostrar(UiState.Vacio)
        regla.onNodeWithText("Todavía no hay ejemplares").assertIsDisplayed()
        regla.onNodeWithText("Buscar datos nuevos").assertIsDisplayed()
    }

    @Test
    fun temaOscuroNoSeCae() {
        regla.setContent {
            StrydaTheme(oscuro = true) {
                MiCorralContenidoUi(contenido(listOf(carrera)), { _, _ -> }, {}, {}, {})
            }
        }
        regla.onNodeWithText("Corral Los Aromos").assertIsDisplayed()
    }
}
