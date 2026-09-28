package cl.stryda.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.stryda.app.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Cifras tabulares en toda la app: tiempos y pesos quedan alineados en columna
private val base = TextStyle(fontFamily = Inter, fontFeatureSettings = "tnum")

private fun estilo(tamano: Int, peso: FontWeight, interlineado: Int) =
    base.copy(fontSize = tamano.sp, fontWeight = peso, lineHeight = interlineado.sp)

/**
 * Escala 14 · 16 · 20 · 24 · 32 · 48 sp. El 12 del documento no se usa:
 * la regla "nunca menos de 14sp" gana.
 */
val StrydaTypography = Typography(
    displayLarge = estilo(48, FontWeight.SemiBold, 56),
    displayMedium = estilo(32, FontWeight.SemiBold, 40),
    displaySmall = estilo(32, FontWeight.Normal, 40),
    headlineLarge = estilo(32, FontWeight.SemiBold, 40),
    headlineMedium = estilo(24, FontWeight.SemiBold, 32),
    headlineSmall = estilo(20, FontWeight.SemiBold, 28),
    titleLarge = estilo(20, FontWeight.SemiBold, 28),
    titleMedium = estilo(16, FontWeight.SemiBold, 24),
    titleSmall = estilo(14, FontWeight.SemiBold, 20),
    bodyLarge = estilo(16, FontWeight.Normal, 24),
    bodyMedium = estilo(16, FontWeight.Normal, 24),
    bodySmall = estilo(14, FontWeight.Normal, 20),
    labelLarge = estilo(16, FontWeight.Medium, 24),
    labelMedium = estilo(14, FontWeight.Medium, 20),
    labelSmall = estilo(14, FontWeight.Medium, 20),
)

/** Números protagonistas: peso, velocidad, tiempo récord. */
@Immutable
data class Cifras(
    val grande: TextStyle = estilo(48, FontWeight.SemiBold, 56),
    val media: TextStyle = estilo(32, FontWeight.SemiBold, 40),
)

val LocalCifras = staticCompositionLocalOf { Cifras() }
