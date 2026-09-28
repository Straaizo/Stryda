package cl.stryda.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.stryda.app.data.local.EstadoDatos
import cl.stryda.app.data.local.OrigenDatos
import cl.stryda.app.ui.theme.Espacio
import cl.stryda.app.ui.theme.StrydaTema
import java.time.Instant

fun textoActualizacion(estado: EstadoDatos, ahora: Instant): String {
    val cuando = estado.ultimaActualizacion?.let { instanteRelativo(it, ahora) }
    return when (estado.origen) {
        OrigenDatos.PRUEBA -> if (cuando != null) "Datos de prueba · cargados $cuando" else "Datos de prueba"
        OrigenDatos.PLANILLA -> if (cuando != null) "Planilla al día $cuando" else "Datos guardados en el teléfono"
    }
}

/** Discreto: sin señal es el caso normal, no un error. */
@Composable
fun IndicadorActualizacion(estado: EstadoDatos, ahora: Instant, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs + 2.dp),
    ) {
        Icon(
            imageVector = if (estado.origen == OrigenDatos.PLANILLA) Icons.Rounded.CloudDone else Icons.Rounded.PhoneAndroid,
            contentDescription = null,
            tint = StrydaTema.colores.textMuted,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = textoActualizacion(estado, ahora),
            style = MaterialTheme.typography.bodySmall,
            color = StrydaTema.colores.textMuted,
        )
    }
}
