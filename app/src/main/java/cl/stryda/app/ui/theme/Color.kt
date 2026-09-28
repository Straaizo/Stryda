package cl.stryda.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Único archivo de la app con colores literales. Los Composables usan MaterialTheme o LocalAppColors.

internal object PaletaClara {
    val bg = Color(0xFFFBFCFB)
    val surface = Color(0xFFFFFFFF)
    val border = Color(0xFFE4E9E5)
    val text = Color(0xFF12201A)
    val textMuted = Color(0xFF5C6B63)
    val primary = Color(0xFF0E7A4F)
    val primaryHover = Color(0xFF0A6141)
    val primarySoft = Color(0xFFE6F4ED)
    val outline = Color(0xFF7D8D84)
    val surfaceContainer = Color(0xFFF3F6F4)
    val surfaceContainerHigh = Color(0xFFEDF1EE)

    val success = Color(0xFF0E7A4F)
    val successSoft = Color(0xFFE6F4ED)

    // #B7791F del documento da 3.6:1 sobre blanco; se oscurece para cumplir 4.5:1
    val warning = Color(0xFF8F5A0A)
    val warningSoft = Color(0xFFFBF1DE)
    val danger = Color(0xFFB42318)
    val dangerSoft = Color(0xFFFCE9E7)
    val info = Color(0xFF1E6FA8)
    val infoSoft = Color(0xFFE6F0F8)

    val skeletonBase = Color(0xFFEDF1EE)
    val skeletonBrillo = Color(0xFFF7F9F8)
}

internal object PaletaOscura {
    val bg = Color(0xFF0D1411)
    val surface = Color(0xFF161F1A)
    val border = Color(0xFF26332C)
    val text = Color(0xFFE8EFEA)
    val textMuted = Color(0xFF94A69B)
    val primary = Color(0xFF3DD68C)
    val primaryHover = Color(0xFF5FE3A3)
    val primarySoft = Color(0xFF152A20)
    val onPrimary = Color(0xFF0D1411)
    val outline = Color(0xFF5E7066)
    val surfaceContainer = Color(0xFF1B2620)
    val surfaceContainerHigh = Color(0xFF212D26)

    val success = Color(0xFF3DD68C)
    val successSoft = Color(0xFF152A20)
    val warning = Color(0xFFE8B04B)
    val warningSoft = Color(0xFF2D2413)
    val danger = Color(0xFFF97066)
    val dangerSoft = Color(0xFF3A1A17)
    val info = Color(0xFF6CB4EE)
    val infoSoft = Color(0xFF13263A)

    val skeletonBase = Color(0xFF1E2923)
    val skeletonBrillo = Color(0xFF28352D)
}

/** Tokens que Material 3 no cubre. */
@Immutable
data class AppColors(
    val border: Color,
    val textMuted: Color,
    val primaryHover: Color,
    val primarySoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val skeletonBase: Color,
    val skeletonBrillo: Color,
    val esOscuro: Boolean,
)

internal val AppColorsClaro = AppColors(
    border = PaletaClara.border,
    textMuted = PaletaClara.textMuted,
    primaryHover = PaletaClara.primaryHover,
    primarySoft = PaletaClara.primarySoft,
    success = PaletaClara.success,
    successSoft = PaletaClara.successSoft,
    warning = PaletaClara.warning,
    warningSoft = PaletaClara.warningSoft,
    danger = PaletaClara.danger,
    dangerSoft = PaletaClara.dangerSoft,
    info = PaletaClara.info,
    infoSoft = PaletaClara.infoSoft,
    skeletonBase = PaletaClara.skeletonBase,
    skeletonBrillo = PaletaClara.skeletonBrillo,
    esOscuro = false,
)

internal val AppColorsOscuro = AppColors(
    border = PaletaOscura.border,
    textMuted = PaletaOscura.textMuted,
    primaryHover = PaletaOscura.primaryHover,
    primarySoft = PaletaOscura.primarySoft,
    success = PaletaOscura.success,
    successSoft = PaletaOscura.successSoft,
    warning = PaletaOscura.warning,
    warningSoft = PaletaOscura.warningSoft,
    danger = PaletaOscura.danger,
    dangerSoft = PaletaOscura.dangerSoft,
    info = PaletaOscura.info,
    infoSoft = PaletaOscura.infoSoft,
    skeletonBase = PaletaOscura.skeletonBase,
    skeletonBrillo = PaletaOscura.skeletonBrillo,
    esOscuro = true,
)

val LocalAppColors = staticCompositionLocalOf { AppColorsClaro }

/** Colores de seda de las chaquetillas. Son los del stud, iguales en tema claro y oscuro. */
val ColoresSeda: Map<String, Color> = mapOf(
    "amarillo" to Color(0xFFF2C518),
    "azul" to Color(0xFF1E4FA3),
    "blanco" to Color(0xFFFFFFFF),
    "burdeo" to Color(0xFF6D1A2A),
    "cafe" to Color(0xFF6D4C41),
    "calipso" to Color(0xFF00A5B5),
    "celeste" to Color(0xFF6EC6F0),
    "dorado" to Color(0xFFC9A227),
    "fucsia" to Color(0xFFD81B7A),
    "granate" to Color(0xFF7B1E2B),
    "gris" to Color(0xFF9E9E9E),
    "lila" to Color(0xFFB39DDB),
    "morado" to Color(0xFF6A3FA0),
    "naranjo" to Color(0xFFF07D1A),
    "negro" to Color(0xFF1C1C1C),
    "plateado" to Color(0xFFBDBDBD),
    "rojo" to Color(0xFFD32F2F),
    "rosado" to Color(0xFFF08CB4),
    "turquesa" to Color(0xFF1BB5B0),
    "verde" to Color(0xFF2E7D32),
)
