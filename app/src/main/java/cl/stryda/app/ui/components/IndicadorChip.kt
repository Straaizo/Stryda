package cl.stryda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.stryda.app.domain.IndicadorEstado
import cl.stryda.app.ui.theme.FormaPill
import cl.stryda.app.ui.theme.StrydaTema
import cl.stryda.app.ui.theme.Espacio

data class AparienciaIndicador(
    val texto: String,
    val icono: ImageVector,
    val color: Color,
    val fondo: Color,
)

fun textoIndicador(indicador: IndicadorEstado): String = when (indicador) {
    is IndicadorEstado.CorreEn -> when (indicador.dias) {
        0L -> "Corre hoy"
        1L -> "Corre mañana"
        else -> "Corre en ${indicador.dias} días"
    }
    IndicadorEstado.Lesionado -> "Lesionado"
    is IndicadorEstado.EnTratamiento -> "En tratamiento"
    IndicadorEstado.CargaAlta -> "Carga alta"
    IndicadorEstado.EnDescanso -> "En descanso"
    IndicadorEstado.EntrenaNormal -> "Entrena normal"
}

@Composable
fun aparienciaIndicador(indicador: IndicadorEstado): AparienciaIndicador {
    val c = StrydaTema.colores
    val texto = textoIndicador(indicador)
    return when (indicador) {
        is IndicadorEstado.CorreEn -> AparienciaIndicador(texto, Icons.Rounded.Flag, c.info, c.infoSoft)
        IndicadorEstado.Lesionado -> AparienciaIndicador(texto, Icons.Rounded.Healing, c.danger, c.dangerSoft)
        is IndicadorEstado.EnTratamiento -> AparienciaIndicador(texto, Icons.Rounded.Medication, c.warning, c.warningSoft)
        IndicadorEstado.CargaAlta -> AparienciaIndicador(texto, Icons.Rounded.TrendingUp, c.warning, c.warningSoft)
        IndicadorEstado.EnDescanso -> AparienciaIndicador(
            texto, Icons.Rounded.Hotel, c.textMuted, MaterialTheme.colorScheme.surfaceContainer,
        )
        IndicadorEstado.EntrenaNormal -> AparienciaIndicador(texto, Icons.Rounded.CheckCircle, c.success, c.successSoft)
    }
}

/** Ícono + texto: el color nunca es el único portador del significado. */
@Composable
fun IndicadorChip(indicador: IndicadorEstado, modifier: Modifier = Modifier) {
    val a = aparienciaIndicador(indicador)
    Row(
        modifier = modifier
            .background(a.fondo, FormaPill)
            .padding(horizontal = Espacio.m, vertical = Espacio.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs + 2.dp),
    ) {
        Icon(a.icono, contentDescription = null, tint = a.color, modifier = Modifier.size(18.dp))
        Text(a.texto, style = MaterialTheme.typography.labelMedium, color = a.color, maxLines = 1)
    }
}
