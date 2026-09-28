package cl.stryda.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema

/** Estado vacío o de error: qué va a aparecer acá y la acción que lo llena. Texto alineado a la izquierda. */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Espacio.xl),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Icon(icono, contentDescription = null, tint = StrydaTema.colores.textMuted, modifier = Modifier.size(40.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(detalle, style = MaterialTheme.typography.bodyLarge, color = StrydaTema.colores.textMuted)
        if (accion != null) {
            Button(onClick = onAccion, modifier = Modifier.heightIn(min = Espacio.tactil)) {
                Text(accion)
            }
        }
    }
}
