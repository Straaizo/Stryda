package cl.stryda.app.ui.screens.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.stryda.app.AppContainer
import cl.stryda.app.ui.components.CaballoGalopando
import cl.stryda.app.ui.navigation.Ruta
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MS_SPLASH_MINIMO = 900L

/** Prepara los datos locales y decide la primera pantalla: con un solo corral se entra directo a él. */
class InicioViewModel(private val c: AppContainer) : ViewModel() {

    private val _destino = MutableStateFlow<Ruta?>(null)
    val destino: StateFlow<Ruta?> = _destino.asStateFlow()

    init {
        viewModelScope.launch {
            val t0 = System.currentTimeMillis()
            val corrales = try {
                c.datos.prepararDatosLocales()
                c.corrales.corrales()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            delay((MS_SPLASH_MINIMO - (System.currentTimeMillis() - t0)).coerceAtLeast(0))
            _destino.value = corrales.singleOrNull()?.let { Ruta.Corral(it.id) } ?: Ruta.Corrales
            c.scopeApp.launch { c.datos.actualizarDesdeRemoto() }
        }
    }
}

/** Único lugar, junto a Ajustes, donde aparece la marca. */
@Composable
fun PantallaInicio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Espacio.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CaballoGalopando(color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(220.dp))
        Spacer(Modifier.height(Espacio.xl))
        Text("Stryda", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Espacio.xs))
        Text(
            "gestión y seguimiento de corrales de carrera",
            style = MaterialTheme.typography.bodyLarge,
            color = StrydaTema.colores.textMuted,
        )
    }
}
