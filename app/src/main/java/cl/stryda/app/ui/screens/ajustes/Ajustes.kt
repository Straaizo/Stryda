package cl.stryda.app.ui.screens.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.stryda.app.AppContainer
import cl.stryda.app.BuildConfig
import cl.stryda.app.data.local.EstadoDatos
import cl.stryda.app.data.local.ModoTema
import cl.stryda.app.data.local.OrigenDatos
import cl.stryda.app.data.repository.ResultadoActualizacion
import cl.stryda.app.ui.components.textoActualizacion
import cl.stryda.app.ui.contenedor
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AjustesViewModel(private val c: AppContainer) : ViewModel() {
    val modoTema: StateFlow<ModoTema> = c.ajustes.modoTema
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModoTema.SISTEMA)
    val estadoDatos: StateFlow<EstadoDatos> = c.datos.estado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDatos(null, OrigenDatos.PRUEBA))

    private val _buscando = MutableStateFlow(false)
    val buscando: StateFlow<Boolean> = _buscando.asStateFlow()

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes = _mensajes.receiveAsFlow()

    fun cambiarTema(modo: ModoTema) {
        viewModelScope.launch { c.ajustes.guardarModoTema(modo) }
    }

    fun buscarDatosNuevos() {
        if (_buscando.value) return
        _buscando.value = true
        viewModelScope.launch {
            val mensaje = when (c.datos.actualizarDesdeRemoto()) {
                ResultadoActualizacion.Actualizado -> "Listo, los datos están al día."
                ResultadoActualizacion.SinPlanilla -> "Todavía no hay planilla conectada. La app usa los datos de prueba."
                ResultadoActualizacion.SinSenal -> "No hay señal. Seguimos con los datos guardados en el teléfono."
            }
            _buscando.value = false
            _mensajes.send(mensaje)
        }
    }

    fun ahora() = c.reloj.ahora()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes() {
    val c = contenedor()
    val vm: AjustesViewModel = viewModel { AjustesViewModel(c) }
    val modo by vm.modoTema.collectAsStateWithLifecycle()
    val datos by vm.estadoDatos.collectAsStateWithLifecycle()
    val buscando by vm.buscando.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Espacio.l),
            verticalArrangement = Arrangement.spacedBy(Espacio.xl),
        ) {
            Seccion("Apariencia") {
                Column(Modifier.selectableGroup()) {
                    listOf(
                        ModoTema.SISTEMA to "Igual que el teléfono",
                        ModoTema.CLARO to "Claro",
                        ModoTema.OSCURO to "Oscuro",
                    ).forEachIndexed { i, (opcion, etiqueta) ->
                        if (i > 0) HorizontalDivider(color = StrydaTema.colores.border)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(selected = modo == opcion, role = Role.RadioButton) { vm.cambiarTema(opcion) }
                                .padding(horizontal = Espacio.l),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = modo == opcion, onClick = null)
                            Spacer(Modifier.width(Espacio.m))
                            Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Seccion("Datos") {
                Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
                    Text(textoActualizacion(datos, vm.ahora()), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Todo lo que ves queda guardado en el teléfono y funciona sin señal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrydaTema.colores.textMuted,
                    )
                    OutlinedButton(
                        onClick = vm::buscarDatosNuevos,
                        enabled = !buscando,
                        modifier = Modifier.heightIn(min = Espacio.tactil),
                    ) {
                        Text(if (buscando) "Buscando…" else "Buscar datos nuevos")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                Text("Stryda", style = MaterialTheme.typography.titleMedium)
                Text(
                    "gestión y seguimiento de corrales de carrera",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrydaTema.colores.textMuted,
                )
                Text(
                    "Versión ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrydaTema.colores.textMuted,
                )
            }
            Spacer(Modifier.height(Espacio.l))
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleSmall,
            color = StrydaTema.colores.textMuted,
            modifier = Modifier.semantics { heading() },
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, StrydaTema.colores.border),
            modifier = Modifier.fillMaxWidth(),
        ) { contenido() }
    }
}
