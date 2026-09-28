package cl.stryda.app.ui.screens.carreras

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.stryda.app.AppContainer
import cl.stryda.app.domain.CarreraDelCaballo
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.components.EstadoVacio
import cl.stryda.app.ui.components.ListaSkeleton
import cl.stryda.app.ui.components.fechaRelativaFutura
import cl.stryda.app.ui.components.hora
import cl.stryda.app.ui.contenedor
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CarrerasContenido(val carreras: List<CarreraDelCaballo>, val hoy: LocalDate)

class CarrerasViewModel(c: AppContainer) : ViewModel() {
    private val hoy = c.reloj.hoy()

    val estado: StateFlow<UiState<CarrerasContenido>> = c.carreras.observarProximasPropias(hoy)
        .map { if (it.isEmpty()) UiState.Vacio else UiState.Contenido(CarrerasContenido(it, hoy)) }
        .catch { emit(UiState.Error("No se pudieron leer las carreras guardadas en el teléfono.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Cargando)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCarreras(onAbrirCarrera: (CarreraDelCaballo) -> Unit) {
    val c = contenedor()
    val vm: CarrerasViewModel = viewModel { CarrerasViewModel(c) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Próximas carreras") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val e = estado) {
            UiState.Cargando -> ListaSkeleton(filas = 3, modifier = Modifier.padding(padding))
            UiState.Vacio -> EstadoVacio(
                icono = Icons.Rounded.Flag,
                titulo = "Sin carreras programadas",
                detalle = "Cuando un ejemplar del corral quede inscrito, la carrera aparece acá con su hora y número.",
                modifier = Modifier.padding(padding),
            )
            is UiState.Error -> EstadoVacio(
                icono = Icons.Rounded.ErrorOutline,
                titulo = "No se pudo abrir",
                detalle = e.mensaje,
                modifier = Modifier.padding(padding),
            )
            is UiState.Contenido -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Espacio.l),
                verticalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                items(e.datos.carreras, key = { it.participacion.id }) { carrera ->
                    TarjetaCarrera(carrera, e.datos.hoy, onClick = { onAbrirCarrera(carrera) })
                }
            }
        }
    }
}

@Composable
private fun TarjetaCarrera(c: CarreraDelCaballo, hoy: LocalDate, onClick: () -> Unit) {
    val cuando = listOfNotNull(fechaRelativaFutura(c.carrera.fecha, hoy), c.carrera.hora?.let(::hora)).joinToString(" · ")
    val detalle = listOfNotNull(
        c.carrera.numero?.let { "${it}ª carrera" },
        "${c.carrera.distanciaM} m",
        c.carrera.hipodromo,
    ).joinToString(" · ")
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, StrydaTema.colores.border),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "${c.caballo.nombre}, $cuando, $detalle" },
    ) {
        Row(Modifier.heightIn(min = 72.dp).padding(Espacio.l), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cuando.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = StrydaTema.colores.info)
                Text(c.caballo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = StrydaTema.colores.textMuted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StrydaTema.colores.textMuted)
        }
    }
}
