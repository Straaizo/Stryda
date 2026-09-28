package cl.stryda.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.stryda.app.domain.Medicion
import cl.stryda.app.ui.theme.StrydaTema
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.time.LocalDate

/**
 * Evolución del peso: una sola serie en el color primario, sin leyenda (el título la nombra).
 * Tocar un punto muestra su valor. La lista de pesajes debajo es la vista en tabla.
 */
@Composable
fun GraficoPeso(pesos: List<Medicion>, modifier: Modifier = Modifier) {
    val productor = remember { CartesianChartModelProducer() }
    val x = pesos.map { it.fecha.toEpochDay().toDouble() }
    val y = pesos.map { it.valor }
    LaunchedEffect(pesos) {
        productor.runTransaction { lineSeries { series(x, y) } }
    }
    val color = MaterialTheme.colorScheme.primary
    val tinta = StrydaTema.colores.textMuted
    val guia = StrydaTema.colores.border
    val etiqueta = rememberTextComponent(color = tinta, textSize = MaterialTheme.typography.bodySmall.fontSize)
    // Margen de 3 kg arriba y abajo: una variación de 2 kg no debe parecer un precipicio
    val minimo = (y.minOrNull() ?: 0.0) - 3
    val maximo = (y.maxOrNull() ?: 0.0) + 3

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = remember(color) { LineCartesianLayer.LineFill.single(fill(color)) },
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.point(rememberShapeComponent(fill(color), CorneredShape.Pill), 8.dp),
                        ),
                    ),
                ),
                rangeProvider = remember(minimo, maximo) { CartesianLayerRangeProvider.fixed(minY = minimo, maxY = maximo) },
            ),
            startAxis = VerticalAxis.rememberStart(
                label = etiqueta,
                guideline = rememberLineComponent(fill(guia)),
                valueFormatter = remember { CartesianValueFormatter { _, v, _ -> decimal(v, 0) } },
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                label = etiqueta,
                guideline = null,
                valueFormatter = remember { CartesianValueFormatter { _, v, _ -> fechaCorta(LocalDate.ofEpochDay(v.toLong())) } },
            ),
            marker = rememberDefaultCartesianMarker(
                label = rememberTextComponent(color = MaterialTheme.colorScheme.onSurface),
                valueFormatter = remember { com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker.ValueFormatter.default(java.text.DecimalFormat("#,##0.0 kg")) },
            ),
        ),
        modelProducer = productor,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = "Gráfico de peso con ${pesos.size} pesajes" },
    )
}
