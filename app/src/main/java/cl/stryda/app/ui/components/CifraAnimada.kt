package cl.stryda.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import cl.stryda.app.ui.theme.Duracion
import cl.stryda.app.ui.theme.LocalAnimacionReducida

/** Número protagonista que cuenta hacia arriba una sola vez al aparecer. Con animación reducida, aparece fijo. */
@Composable
fun CifraAnimada(
    valor: Double,
    formato: (Double) -> String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    val reducida = LocalAnimacionReducida.current
    val animado = remember { Animatable(if (reducida) valor.toFloat() else 0f) }
    LaunchedEffect(valor, reducida) {
        if (reducida) {
            animado.snapTo(valor.toFloat())
        } else {
            animado.animateTo(valor.toFloat(), tween(Duracion.GRAFICO, easing = FastOutSlowInEasing))
        }
    }
    Text(formato(animado.value.toDouble()), style = estilo, color = color, modifier = modifier)
}
