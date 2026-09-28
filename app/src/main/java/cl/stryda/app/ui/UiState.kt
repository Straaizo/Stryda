package cl.stryda.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cl.stryda.app.AppContainer
import cl.stryda.app.StrydaApplication

/** Estado único que expone cada ViewModel. Nada de booleanos sueltos. */
sealed interface UiState<out T> {
    data object Cargando : UiState<Nothing>
    data class Contenido<T>(val datos: T) : UiState<T>
    data object Vacio : UiState<Nothing>
    data class Error(val mensaje: String) : UiState<Nothing>
}

val Context.contenedor: AppContainer get() = (applicationContext as StrydaApplication).container

@Composable
fun contenedor(): AppContainer = LocalContext.current.contenedor
