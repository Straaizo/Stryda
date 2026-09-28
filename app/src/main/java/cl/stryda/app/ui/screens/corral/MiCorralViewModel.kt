package cl.stryda.app.ui.screens.corral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.stryda.app.AppContainer
import cl.stryda.app.data.local.EstadoDatos
import cl.stryda.app.domain.Caballo
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.domain.Corral
import cl.stryda.app.domain.DIAS_CORRE_PRONTO
import cl.stryda.app.domain.DIAS_LINEA_BASE
import cl.stryda.app.domain.DIAS_VENTANA_RECIENTE
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.domain.indicadorEstado
import cl.stryda.app.domain.proximaCarrera
import cl.stryda.app.ui.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class FilaCaballo(val caballo: Caballo, val indicador: IndicadorEstado)

data class MiCorralContenido(
    val corral: Corral,
    val filas: List<FilaCaballo>,
    val correnPronto: List<CarreraDelCaballo>,
    val datos: EstadoDatos,
    val hoy: LocalDate,
    val ahora: Instant,
)

class MiCorralViewModel(corralId: String, private val c: AppContainer) : ViewModel() {

    private val hoy = c.reloj.hoy()

    private val base = combine(
        c.corrales.observarCorral(corralId),
        c.caballos.observarDelCorral(corralId),
        c.sesiones.observarDelCorralDesde(corralId, hoy.minusDays((DIAS_VENTANA_RECIENTE + DIAS_LINEA_BASE).toLong())),
        c.salud.observarTratamientosDelCorral(corralId),
        c.carreras.observarProximasDelCorral(corralId, hoy),
    ) { corral, caballos, sesiones, tratamientos, carreras ->
        Triple(corral, caballos, Triple(sesiones, tratamientos, carreras))
    }

    val estado: StateFlow<UiState<MiCorralContenido>> = combine(base, c.datos.estado) { (corral, caballos, resto), datos ->
        val (sesiones, tratamientos, carreras) = resto
        if (corral == null || caballos.isEmpty()) return@combine UiState.Vacio

        val sesionesPorCaballo = sesiones.groupBy { it.caballoId }
        val tratamientosPorCaballo = tratamientos.groupBy { it.caballoId }
        val carrerasPorCaballo = carreras.groupBy { it.caballo.id }

        val filas = caballos.map { caballo ->
            FilaCaballo(
                caballo = caballo,
                indicador = indicadorEstado(
                    caballo = caballo,
                    proximaCarrera = proximaCarrera(carrerasPorCaballo[caballo.id].orEmpty(), hoy)?.carrera?.fecha,
                    tratamientos = tratamientosPorCaballo[caballo.id].orEmpty(),
                    sesiones = sesionesPorCaballo[caballo.id].orEmpty(),
                    hoy = hoy,
                ),
            )
        }
        val correnPronto = carreras.filter { !it.carrera.fecha.isAfter(hoy.plusDays(DIAS_CORRE_PRONTO.toLong())) }
        UiState.Contenido(MiCorralContenido(corral, filas, correnPronto, datos, hoy, c.reloj.ahora()))
    }
        .catch { emit(UiState.Error("No se pudieron leer los datos guardados en el teléfono.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Cargando)

    fun buscarDatosNuevos() {
        c.scopeApp.launch { c.datos.actualizarDesdeRemoto() }
    }
}

