package cl.stryda.app.ui.screens.corrales

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
import androidx.compose.material.icons.rounded.Grass
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.stryda.app.AppContainer
import cl.stryda.app.domain.CorralResumen
import cl.stryda.app.ui.UiState
import cl.stryda.app.ui.components.EstadoVacio
import cl.stryda.app.ui.components.ListaSkeleton
import cl.stryda.app.ui.contenedor
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MisCorralesViewModel(private val c: AppContainer) : ViewModel() {
    val estado: StateFlow<UiState<List<CorralResumen>>> = c.corrales.observarCorrales()
        .map { if (it.isEmpty()) UiState.Vacio else UiState.Contenido(it) }
        .catch { emit(UiState.Error("No se pudieron leer los datos guardados en el teléfono.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Cargando)

    fun buscarDatosNuevos() {
        viewModelScope.launch { c.datos.actualizarDesdeRemoto() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisCorrales(onAbrirCorral: (String) -> Unit) {
    val c = contenedor()
    val vm: MisCorralesViewModel = viewModel { MisCorralesViewModel(c) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis corrales") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when (val e = estado) {
            UiState.Cargando -> ListaSkeleton(filas = 3, modifier = Modifier.padding(padding))
            UiState.Vacio -> EstadoVacio(
                icono = Icons.Rounded.Grass,
                titulo = "Todavía no hay corrales",
                detalle = "Cuando se agregue un corral a la planilla, aparece acá con sus ejemplares.",
                accion = "Buscar datos nuevos",
                onAccion = vm::buscarDatosNuevos,
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
                items(e.datos, key = { it.corral.id }) { resumen ->
                    TarjetaCorral(resumen, onClick = { onAbrirCorral(resumen.corral.id) })
                }
            }
        }
    }
}

@Composable
private fun TarjetaCorral(resumen: CorralResumen, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, StrydaTema.colores.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 72.dp).padding(Espacio.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(resumen.corral.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${resumen.corral.hipodromo} · ${resumen.numeroEjemplares} ejemplares",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrydaTema.colores.textMuted,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = StrydaTema.colores.textMuted)
        }
    }
}
