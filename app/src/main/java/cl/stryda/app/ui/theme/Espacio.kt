package cl.stryda.app.ui.theme

import androidx.compose.ui.unit.dp

/** Escala de 4dp. */
object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Objetivo táctil mínimo. */
    val tactil = 48.dp
}

/** Duraciones en ms. Nada supera 600. */
object Duracion {
    const val MICRO = 200
    const val PANTALLA = 300
    const val GRAFICO = 600
}
