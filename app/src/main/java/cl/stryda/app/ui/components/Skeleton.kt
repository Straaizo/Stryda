package cl.stryda.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.FormaPill
import cl.stryda.app.ui.theme.LocalAnimacionReducida
import cl.stryda.app.ui.theme.StrydaTema

/** Bloque gris con brillo que recorre. Con animación reducida queda quieto. */
fun Modifier.shimmer(forma: Shape): Modifier = composed {
    val c = StrydaTema.colores
    if (LocalAnimacionReducida.current) {
        background(c.skeletonBase, forma)
    } else {
        val transicion = rememberInfiniteTransition(label = "shimmer")
        val x by transicion.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmerX",
        )
        background(
            Brush.linearGradient(
                colors = listOf(c.skeletonBase, c.skeletonBrillo, c.skeletonBase),
                start = Offset(x * 600f, 0f),
                end = Offset(x * 600f + 600f, 0f),
            ),
            forma,
        )
    }
}

/** Mantiene el alto y la forma de una fila de ejemplar mientras carga. */
@Composable
fun FilaSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).shimmer(CircleShape))
        Spacer(Modifier.width(Espacio.m))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            Box(Modifier.fillMaxWidth(0.55f).height(16.dp).shimmer(MaterialTheme.shapes.small))
            Box(Modifier.fillMaxWidth(0.35f).height(14.dp).shimmer(MaterialTheme.shapes.small))
        }
        Box(Modifier.width(110.dp).height(30.dp).shimmer(FormaPill))
    }
}

@Composable
fun ListaSkeleton(modifier: Modifier = Modifier, filas: Int = 8) {
    Column(modifier.semantics { contentDescription = "Cargando ejemplares" }) {
        repeat(filas) { FilaSkeleton() }
    }
}
