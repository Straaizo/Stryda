package cl.stryda.app.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import cl.stryda.app.data.local.ModoTema

private val EsquemaClaro = lightColorScheme(
    primary = PaletaClara.primary,
    onPrimary = PaletaClara.surface,
    primaryContainer = PaletaClara.primarySoft,
    onPrimaryContainer = PaletaClara.primaryHover,
    secondary = PaletaClara.primary,
    onSecondary = PaletaClara.surface,
    secondaryContainer = PaletaClara.primarySoft,
    onSecondaryContainer = PaletaClara.text,
    background = PaletaClara.bg,
    onBackground = PaletaClara.text,
    surface = PaletaClara.surface,
    onSurface = PaletaClara.text,
    surfaceVariant = PaletaClara.surfaceContainer,
    onSurfaceVariant = PaletaClara.textMuted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = PaletaClara.surface,
    surfaceContainerLow = PaletaClara.surface,
    surfaceContainer = PaletaClara.surfaceContainer,
    surfaceContainerHigh = PaletaClara.surfaceContainerHigh,
    surfaceContainerHighest = PaletaClara.surfaceContainerHigh,
    outline = PaletaClara.outline,
    outlineVariant = PaletaClara.border,
    error = PaletaClara.danger,
    onError = PaletaClara.surface,
    errorContainer = PaletaClara.dangerSoft,
    onErrorContainer = PaletaClara.danger,
)

private val EsquemaOscuro = darkColorScheme(
    primary = PaletaOscura.primary,
    onPrimary = PaletaOscura.onPrimary,
    primaryContainer = PaletaOscura.primarySoft,
    onPrimaryContainer = PaletaOscura.primaryHover,
    secondary = PaletaOscura.primary,
    onSecondary = PaletaOscura.onPrimary,
    secondaryContainer = PaletaOscura.primarySoft,
    onSecondaryContainer = PaletaOscura.text,
    background = PaletaOscura.bg,
    onBackground = PaletaOscura.text,
    surface = PaletaOscura.surface,
    onSurface = PaletaOscura.text,
    surfaceVariant = PaletaOscura.surfaceContainer,
    onSurfaceVariant = PaletaOscura.textMuted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = PaletaOscura.bg,
    surfaceContainerLow = PaletaOscura.surface,
    surfaceContainer = PaletaOscura.surfaceContainer,
    surfaceContainerHigh = PaletaOscura.surfaceContainerHigh,
    surfaceContainerHighest = PaletaOscura.surfaceContainerHigh,
    outline = PaletaOscura.outline,
    outlineVariant = PaletaOscura.border,
    error = PaletaOscura.danger,
    onError = PaletaOscura.bg,
    errorContainer = PaletaOscura.dangerSoft,
    onErrorContainer = PaletaOscura.danger,
)

/** `true` si el sistema tiene las animaciones desactivadas (accesibilidad o ahorro). */
val LocalAnimacionReducida = staticCompositionLocalOf { false }

@Composable
fun temaEsOscuro(modo: ModoTema): Boolean = when (modo) {
    ModoTema.SISTEMA -> isSystemInDarkTheme()
    ModoTema.CLARO -> false
    ModoTema.OSCURO -> true
}

/** Sin dynamicColor: el fondo de pantalla del usuario no decide los colores de la marca. */
@Composable
fun StrydaTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val animacionReducida = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(
        LocalAppColors provides if (oscuro) AppColorsOscuro else AppColorsClaro,
        LocalCifras provides Cifras(),
        LocalAnimacionReducida provides animacionReducida,
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = StrydaTypography,
            shapes = StrydaShapes,
            content = content,
        )
    }
}

/** Acceso corto: `StrydaTema.colores.textMuted`. */
object StrydaTema {
    val colores: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val cifras: Cifras
        @Composable @ReadOnlyComposable get() = LocalCifras.current
}
