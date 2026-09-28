package cl.stryda.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cl.stryda.app.R
import cl.stryda.app.ui.theme.LocalAnimacionReducida
import kotlinx.coroutines.delay

// Respaldo 3 del documento: la secuencia de Muybridge (1878, dominio público), trazada como vectores
// desde el escaneo en alta resolución (tools/vectorizar_caballo.py): nítida en cualquier pantalla.
// Cuando exista caballo.riv, se reemplaza el contenido de este archivo por Rive y nada más cambia.
private val CUADROS = intArrayOf(
    R.drawable.caballo_galope_00, R.drawable.caballo_galope_01, R.drawable.caballo_galope_02,
    R.drawable.caballo_galope_03, R.drawable.caballo_galope_04, R.drawable.caballo_galope_05,
    R.drawable.caballo_galope_06, R.drawable.caballo_galope_07, R.drawable.caballo_galope_08,
    R.drawable.caballo_galope_09, R.drawable.caballo_galope_10,
)

// Muybridge fotografió cada 1/25 s: 40 ms por cuadro es la velocidad real del galope
private const val MS_CUADRO_RAPIDO = 45L
private const val MS_CUADRO_LENTO = 90L
private const val CUADROS_FRENADO = 6

/**
 * Caballo al galope. Solo para el splash y cargas largas.
 * [cargando] = galopa; al pasar a `false` desacelera unos cuadros y se detiene, sin corte brusco.
 * [intensidad] 0..1 regula la velocidad. Con animación reducida muestra el primer cuadro fijo.
 */
@Composable
fun CaballoGalopando(
    color: Color,
    modifier: Modifier = Modifier,
    cargando: Boolean = true,
    intensidad: Float = 1f,
) {
    val reducida = LocalAnimacionReducida.current
    var cuadro by remember { mutableIntStateOf(0) }
    val cargandoActual by rememberUpdatedState(cargando)
    val intensidadActual by rememberUpdatedState(intensidad.coerceIn(0f, 1f))

    LaunchedEffect(reducida, cargando) {
        if (reducida) {
            cuadro = 0
            return@LaunchedEffect
        }
        while (cargandoActual) {
            val ms = MS_CUADRO_LENTO - ((MS_CUADRO_LENTO - MS_CUADRO_RAPIDO) * intensidadActual).toLong()
            delay(ms)
            cuadro = (cuadro + 1) % CUADROS.size
        }
        repeat(CUADROS_FRENADO) { i ->
            delay(MS_CUADRO_RAPIDO + i * 25L)
            cuadro = (cuadro + 1) % CUADROS.size
        }
    }

    // Los 11 vectores se cargan una vez: cambiar de cuadro no vuelve a leer recursos (sin saltos)
    val cuadros = CUADROS.map { painterResource(it) }
    Image(
        painter = cuadros[cuadro],
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .aspectRatio(900f / 488f)
            .semantics { contentDescription = "Caballo al galope" },
    )
}
