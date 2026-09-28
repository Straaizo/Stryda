package cl.stryda.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.stryda.app.domain.Chaquetilla
import cl.stryda.app.domain.Diseno
import cl.stryda.app.ui.theme.ColoresSeda

/** Dibujo de la chaquetilla con los colores y el diseño del stud. */
@Composable
fun ChaquetillaIcono(chaquetilla: Chaquetilla, descripcion: String, modifier: Modifier = Modifier, tamano: Dp = 48.dp) {
    val principal = ColoresSeda[chaquetilla.principal] ?: return
    val secundario = chaquetilla.secundario?.let { ColoresSeda[it] }
    val contorno = MaterialTheme.colorScheme.outline
    Canvas(
        modifier
            .size(tamano)
            .semantics { contentDescription = "Chaquetilla: $descripcion" },
    ) {
        val torso = poligono(TORSO)
        val mangaIzq = poligono(MANGA)
        val mangaDer = poligono(MANGA.map { (x, y) -> 1 - x to y })

        val colorMangas = if (chaquetilla.diseno == Diseno.MANGAS && secundario != null) secundario else principal
        drawPath(mangaIzq, colorMangas)
        drawPath(mangaDer, colorMangas)
        drawPath(torso, principal)

        if (secundario != null) {
            clipPath(torso) {
                when (chaquetilla.diseno) {
                    Diseno.FRANJA -> rect(secundario, 0.42f, 0f, 0.58f, 1f)
                    Diseno.CRUZ -> {
                        rect(secundario, 0.43f, 0f, 0.57f, 1f)
                        rect(secundario, 0f, 0.40f, 1f, 0.52f)
                    }
                    Diseno.LUNARES -> for (fila in 0..3) for (col in 0..2) {
                        val x = 0.32f + col * 0.18f + if (fila % 2 == 1) 0.09f else 0f
                        drawCircle(secundario, size.width * 0.04f, Offset(size.width * x, size.height * (0.30f + fila * 0.17f)))
                    }
                    Diseno.HOMBRERAS -> rect(secundario, 0f, 0f, 1f, 0.24f)
                    Diseno.MANGAS, Diseno.LISA -> Unit
                }
            }
            if (chaquetilla.punos) {
                drawPath(poligono(PUNO), secundario)
                drawPath(poligono(PUNO.map { (x, y) -> 1 - x to y }), secundario)
            }
        }
        // Contorno: la chaquetilla blanca no desaparece sobre fondo claro
        val trazo = Stroke(width = 1.5.dp.toPx())
        listOf(mangaIzq, mangaDer, torso).forEach { drawPath(it, contorno, style = trazo) }
    }
}

// Coordenadas relativas (0..1) de una chaquetilla vista de frente
private val TORSO = listOf(
    0.24f to 0.14f, 0.38f to 0.08f, 0.50f to 0.18f, 0.62f to 0.08f, 0.76f to 0.14f,
    0.76f to 0.95f, 0.24f to 0.95f,
)
private val MANGA = listOf(0.24f to 0.14f, 0.08f to 0.26f, 0.02f to 0.46f, 0.15f to 0.52f, 0.24f to 0.36f)
private val PUNO = listOf(0.02f to 0.46f, 0.15f to 0.52f, 0.18f to 0.46f, 0.05f to 0.39f)

private fun DrawScope.poligono(puntos: List<Pair<Float, Float>>): Path = Path().apply {
    puntos.forEachIndexed { i, (x, y) ->
        if (i == 0) moveTo(size.width * x, size.height * y) else lineTo(size.width * x, size.height * y)
    }
    close()
}

private fun DrawScope.rect(color: Color, x0: Float, y0: Float, x1: Float, y1: Float) {
    drawRect(color, topLeft = Offset(size.width * x0, size.height * y0), size = Rect(0f, 0f, size.width * (x1 - x0), size.height * (y1 - y0)).size)
}

/** Número de mandil, como el paño que lleva el caballo en la carrera. */
@Composable
fun NumeroMandil(numero: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp)
            .semantics { contentDescription = "Número $numero" },
        contentAlignment = Alignment.Center,
    ) {
        Text(numero.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
