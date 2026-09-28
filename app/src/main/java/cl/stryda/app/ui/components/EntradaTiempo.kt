package cl.stryda.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Tiempo escrito solo con números (teclado numérico, sin buscar ":" ni "."):
 * el último dígito son las décimas, los dos anteriores los segundos y el resto los minutos.
 * "486" → 48.6 · "1234" → 1:23.4
 */
fun digitosATexto(digitos: String): String {
    val d = digitos.filter(Char::isDigit).trimStart('0')
    return when {
        d.isEmpty() -> ""
        d.length == 1 -> "0.$d"
        d.length <= 3 -> "${d.dropLast(1)}.${d.last()}"
        else -> "${d.dropLast(3)}:${d.takeLast(3).take(2)}.${d.last()}"
    }
}

/** Segundos, o `null` si está vacío o los segundos pasan de 59 con minutos. */
fun digitosASegundos(digitos: String): Double? {
    val d = digitos.filter(Char::isDigit).trimStart('0')
    if (d.isEmpty()) return null
    val decimas = d.last().digitToInt()
    val resto = d.dropLast(1)
    val segundos = resto.takeLast(2).toIntOrNull() ?: 0
    val minutos = resto.dropLast(2).toIntOrNull() ?: 0
    if (minutos > 0 && segundos > 59) return null
    return minutos * 60 + segundos + decimas / 10.0
}

/** Muestra "1:23.4" mientras se escribe "1234". El cursor queda siempre al final. */
object TransformacionTiempo : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val visible = digitosATexto(text.text)
        return TransformedText(
            AnnotatedString(visible),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = visible.length
                override fun transformedToOriginal(offset: Int) = text.length
            },
        )
    }
}
