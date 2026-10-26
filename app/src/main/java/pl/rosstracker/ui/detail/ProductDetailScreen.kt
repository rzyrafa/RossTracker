package pl.rosstracker.ui.detail

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pl.rosstracker.ServiceLocator
import pl.rosstracker.data.db.PricePointEntity
import pl.rosstracker.data.db.ProductEntity
import pl.rosstracker.discountPercent
import pl.rosstracker.formatDateTime
import pl.rosstracker.formatIsoDayMonth
import pl.rosstracker.formatPrice
import pl.rosstracker.notify.Notifier
import pl.rosstracker.ui.Eyebrow
import pl.rosstracker.ui.calm
import pl.rosstracker.ui.coffee
import pl.rosstracker.ui.common.Pill
import pl.rosstracker.ui.common.PillStyle
import pl.rosstracker.ui.common.PriceTag
import pl.rosstracker.ui.common.sharedProductImage

@Composable
fun ProductDetailScreen(productId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = ServiceLocator.repository
    val product by remember(productId) { repo.observeProduct(productId) }.collectAsStateWithLifecycle(initialValue = null)
    val points by remember(productId) { repo.observePricePoints(productId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val p = product
        if (p != null) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Hero(p)
                Sheet(p, points, Modifier.offset(y = (-32).dp))
            }
        }

        // Floating round buttons over the photo, like the reference design.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz", onClick = onBack)
            Spacer(Modifier.weight(1f))
            if (refreshing) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = coffee.accent)
                }
            } else {
                RoundButton(Icons.Default.Refresh, "Sprawdź cenę") {
                    refreshing = true
                    scope.launch {
                        runCatching { repo.refresh(productId) }.getOrNull()?.let { result ->
                            result.events.forEach { Notifier.notify(context, result.product, it) }
                        }
                        refreshing = false
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            RoundButton(Icons.Default.Delete, "Usuń", tint = coffee.accent) { confirmDelete = true }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Przestać śledzić?") },
            text = { Text("Produkt i jego historia cen zostaną usunięte.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        repo.delete(productId)
                        onBack()
                    }
                }) { Text("Usuń", color = coffee.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anuluj") } },
        )
    }
}

/** Cream circle with a soft shadow so it reads on white photos too. */
@Composable
private fun RoundButton(icon: ImageVector, description: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 3.dp,
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun Hero(p: ProductEntity) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(coffee.imageTile),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = p.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .sharedProductImage(p.id)
                .fillMaxWidth()
                .height(380.dp)
                .padding(start = 40.dp, end = 40.dp, top = 72.dp, bottom = 56.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Sheet(p: ProductEntity, points: List<PricePointEntity>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // The sheet drifts up into place a moment after the photo arrives.
    val rise = remember { Animatable(1f) }
    LaunchedEffect(Unit) { rise.animateTo(0f, calm(600, delayMillis = 120)) }
    val risePx = with(LocalDensity.current) { 40.dp.toPx() }

    Column(
        modifier
            .graphicsLayer {
                translationY = rise.value * risePx
                alpha = 1f - rise.value
            }
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val eyebrow = if (p.isPromo) {
            "${(p.promoLabel ?: "Promocja").uppercase()} · −${discountPercent(p.price, p.oldPrice!!)}%"
        } else {
            p.brand?.uppercase()
        }
        eyebrow?.let { Text(it, style = Eyebrow, color = coffee.accent) }
        Text(p.title ?: p.name, style = MaterialTheme.typography.headlineMedium)
        val subtitle = listOfNotNull(if (p.isPromo) p.brand else null, p.unit).joinToString(" · ")
        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.labelLarge, color = coffee.muted)
        p.caption?.let {
            Text(it.replaceFirstChar { c -> c.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = coffee.muted)
        }

        PriceTag(p.price, p.oldPrice, priceStyle = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (p.isPromo) {
                val from = formatIsoDayMonth(p.promoFrom)
                val to = formatIsoDayMonth(p.promoTo)
                if (from != null || to != null) Pill("promocja ${listOfNotNull(from, to).joinToString("–")}", style = PillStyle.Dark)
            }
            // Omnibus price: the lowest in the 30 days before the discount started.
            p.lowestPrice30d?.let { Pill("najniższa przed promocją ${formatPrice(it)}") }
            Pill("cena online", style = PillStyle.Outline)
            if (!p.available) Pill("niedostępny online", style = PillStyle.Accent)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Historia ceny", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("${points.size} SPRAWDZEŃ", style = Eyebrow, color = coffee.muted)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (points.size < 2) {
                Text(
                    "Wykres pojawi się po kolejnym sprawdzeniu (ok. 7:00 i 19:00).",
                    style = MaterialTheme.typography.bodySmall,
                    color = coffee.muted,
                )
            } else {
                PriceChart(points)
                val prices = points.map { it.price }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("min ${formatPrice(prices.min())}")
                    Pill("max ${formatPrice(prices.max())}")
                }
            }
        }

        Text(
            p.lastError?.let { "Ostatni błąd: $it" } ?: "Sprawdzono ${formatDateTime(p.lastChecked)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (p.lastError != null) MaterialTheme.colorScheme.error else coffee.muted,
        )
        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, p.url.toUri())) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Otwórz w sklepie", style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.height(24.dp))
    }
}
