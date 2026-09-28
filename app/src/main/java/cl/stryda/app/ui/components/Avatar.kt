package cl.stryda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.stryda.app.ui.theme.StrydaTema
import coil3.compose.SubcomposeAsyncImage

/** Foto del ejemplar o, si no hay (o no carga sin señal), sus iniciales. */
@Composable
fun AvatarCaballo(
    nombre: String,
    fotoUrl: String?,
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
) {
    val base = modifier.size(tamano).clip(CircleShape)
    if (fotoUrl == null) {
        Iniciales(nombre, base)
    } else {
        SubcomposeAsyncImage(
            model = fotoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base,
            loading = { Iniciales(nombre, Modifier) },
            error = { Iniciales(nombre, Modifier) },
        )
    }
}

@Composable
private fun Iniciales(nombre: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(StrydaTema.colores.primarySoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = iniciales(nombre),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
