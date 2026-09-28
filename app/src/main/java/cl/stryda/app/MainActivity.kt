package cl.stryda.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.stryda.app.data.local.ModoTema
import cl.stryda.app.ui.StrydaApp
import cl.stryda.app.ui.contenedor
import cl.stryda.app.ui.theme.StrydaTheme
import cl.stryda.app.ui.theme.temaEsOscuro

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ajustes = contenedor.ajustes
        setContent {
            val modo by ajustes.modoTema.collectAsStateWithLifecycle(initialValue = ModoTema.SISTEMA)
            val oscuro = temaEsOscuro(modo)
            // Íconos de la barra de estado según el tema elegido, no solo el del sistema
            DisposableEffect(oscuro) {
                val estilo = if (oscuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose {}
            }
            StrydaTheme(oscuro = oscuro) {
                StrydaApp()
            }
        }
    }
}
