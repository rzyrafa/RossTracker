package pl.rosstracker.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import pl.rosstracker.data.db.PricePointEntity
import pl.rosstracker.formatDayMonth
import pl.rosstracker.formatPrice
import pl.rosstracker.ui.coffee
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Price history. X is the index of the check rather than the timestamp, so irregular
 * check times don't produce a huge number of empty x steps; labels show the real date.
 */
@Composable
fun PriceChart(points: List<PricePointEntity>, modifier: Modifier = Modifier) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        producer.runTransaction {
            lineModel { series(points.indices.toList(), points.map { it.price }) }
        }
    }
    val dateFormatter = remember(points) {
        object : CartesianValueFormatter {
            override fun format(context: CartesianMeasuringContext, value: Double, verticalAxisPosition: Axis.Position.Vertical?): CharSequence =
                points.getOrNull(value.toInt())?.let { formatDayMonth(it.timestamp) } ?: " "
        }
    }
    val priceFormatter = remember {
        object : CartesianValueFormatter {
            override fun format(context: CartesianMeasuringContext, value: Double, verticalAxisPosition: Axis.Position.Vertical?): CharSequence =
                formatPrice(value)
        }
    }
    val accent = coffee.accent
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(accent)),
        // Soft terracotta wash under the line, fading out towards the axis.
        areaFill = LineCartesianLayer.AreaFill.single(
            Fill(Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0f)))),
        ),
    )
    // Zoom the price axis around the data instead of starting at 0 zł, so changes are visible.
    val range = remember(points) {
        val min = points.minOf { it.price }
        val max = points.maxOf { it.price }
        CartesianLayerRangeProvider.fixed(minY = floor(min * 0.85), maxY = ceil(max * 1.08))
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(line), rangeProvider = range),
            startAxis = VerticalAxis.rememberStart(valueFormatter = priceFormatter),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dateFormatter),
        ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(200.dp),
    )
}
