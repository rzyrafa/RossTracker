package pl.rosstracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pl.rosstracker.discountPercent
import pl.rosstracker.formatPrice
import pl.rosstracker.ui.coffee

@Composable
fun PriceTag(
    price: Double,
    oldPrice: Double?,
    modifier: Modifier = Modifier,
    priceStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val promo = oldPrice != null && oldPrice > price
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            formatPrice(price),
            style = priceStyle,
            fontWeight = FontWeight.SemiBold,
            color = if (promo) coffee.accent else MaterialTheme.colorScheme.onSurface,
        )
        if (promo) {
            Text(
                formatPrice(oldPrice),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = coffee.muted,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        }
    }
}

enum class PillStyle { Accent, Soft, Outline, Dark }

/** Rounded "bubble" for short facts: promo, dates, lowest price, errors. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, style: PillStyle = PillStyle.Soft) {
    val (bg, fg) = when (style) {
        PillStyle.Accent -> coffee.accent to coffee.onAccent
        PillStyle.Soft -> coffee.chip to MaterialTheme.colorScheme.onSurfaceVariant
        PillStyle.Outline -> Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
        PillStyle.Dark -> Color(0xCC2B2420) to Color(0xFFF7F2EA)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .background(bg, CircleShape)
            .then(if (style == PillStyle.Outline) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** "MEGA! −35%" */
@Composable
fun PromoPill(label: String?, price: Double, oldPrice: Double, modifier: Modifier = Modifier) {
    Pill("${label ?: "Promocja"} −${discountPercent(price, oldPrice)}%", modifier, PillStyle.Accent)
}
