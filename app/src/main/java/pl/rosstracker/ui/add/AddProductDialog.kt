package pl.rosstracker.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pl.rosstracker.ServiceLocator
import pl.rosstracker.data.net.ProductSnapshot
import pl.rosstracker.data.net.RossmannParser
import pl.rosstracker.ui.common.PriceTag
import pl.rosstracker.ui.Eyebrow
import pl.rosstracker.ui.coffee
import pl.rosstracker.ui.common.PromoPill
import pl.rosstracker.ui.labels.LabelChip
import pl.rosstracker.ui.labels.LabelEditorDialog
import pl.rosstracker.ui.labels.NewLabelChip

private sealed interface PreviewState {
    data object Idle : PreviewState
    data object Loading : PreviewState
    data class Loaded(val snapshot: ProductSnapshot, val alreadyTracked: Boolean) : PreviewState
    data class Failed(val message: String) : PreviewState
}

/**
 * Paste-a-link dialog. When opened from the share sheet, [initialText] holds the shared text
 * and the preview is fetched straight away.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProductDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onAdded: (ProductSnapshot) -> Unit,
    initialLabels: Set<Long> = emptySet(),
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialText) }
    var state by remember { mutableStateOf<PreviewState>(PreviewState.Idle) }
    val labels by ServiceLocator.labels.observeLabels().collectAsStateWithLifecycle(initialValue = emptyList())
    var chosenLabels by remember { mutableStateOf(initialLabels) }
    var creatingLabel by remember { mutableStateOf(false) }

    fun loadPreview() {
        val url = RossmannParser.extractProductUrl(text)
        if (url == null) {
            state = PreviewState.Failed("To nie wygląda na link do produktu z rossmann.pl")
            return
        }
        state = PreviewState.Loading
        scope.launch {
            state = try {
                val snapshot = ServiceLocator.repository.preview(url)
                PreviewState.Loaded(snapshot, ServiceLocator.repository.isTracked(snapshot.id))
            } catch (e: Exception) {
                PreviewState.Failed("Nie udało się pobrać produktu: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (RossmannParser.extractProductUrl(initialText) != null) loadPreview()
    }

    val loaded = state as? PreviewState.Loaded
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj produkt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; state = PreviewState.Idle },
                    label = { Text("Link do produktu") },
                    placeholder = { Text("https://www.rossmann.pl/Produkt/…") },
                    singleLine = true,
                    // URL keyboard without autocorrect: keyboards like to "fix" Produkt into Product.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                when (val s = state) {
                    PreviewState.Idle -> Text(
                        "Wklej link albo udostępnij stronę produktu z przeglądarki do RossTrackera.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    PreviewState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    is PreviewState.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error)
                    is PreviewState.Loaded -> Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = s.snapshot.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(coffee.imageTile)
                                .padding(8.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            s.snapshot.brand?.let { Text(it.uppercase(), style = Eyebrow, color = coffee.muted) }
                            Text(s.snapshot.title ?: s.snapshot.name, style = MaterialTheme.typography.titleMedium)
                            PriceTag(s.snapshot.price, s.snapshot.oldPrice)
                            if (s.snapshot.isPromo) PromoPill(s.snapshot.promoLabel, s.snapshot.price, s.snapshot.oldPrice!!)
                            if (s.alreadyTracked) Text("Już śledzisz ten produkt", style = MaterialTheme.typography.bodySmall, color = coffee.muted)
                        }
                    }
                }
                if (loaded != null && !loaded.alreadyTracked) {
                    Text("ETYKIETY", style = Eyebrow, color = coffee.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEach { label ->
                            val on = label.id in chosenLabels
                            LabelChip(
                                label.name,
                                color = coffee.label(label.color),
                                selected = on,
                                onClick = { chosenLabels = if (on) chosenLabels - label.id else chosenLabels + label.id },
                            )
                        }
                        NewLabelChip(onClick = { creatingLabel = true })
                    }
                }
            }
        },
        confirmButton = {
            if (loaded != null) {
                TextButton(
                    enabled = !loaded.alreadyTracked,
                    onClick = {
                        scope.launch {
                            ServiceLocator.repository.track(loaded.snapshot)
                            // Only labels that still exist; one could have been deleted meanwhile.
                            val existing = chosenLabels.filterTo(HashSet()) { id -> labels.any { it.id == id } }
                            ServiceLocator.labels.setLabels(listOf(loaded.snapshot.id), add = existing, remove = emptyList())
                            onAdded(loaded.snapshot)
                        }
                    },
                ) { Text("Śledź") }
            } else {
                TextButton(enabled = state != PreviewState.Loading && text.isNotBlank(), onClick = ::loadPreview) {
                    Text("Sprawdź")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )

    if (creatingLabel) {
        LabelEditorDialog(
            initial = null,
            onDismiss = { creatingLabel = false },
            onSaved = { id ->
                chosenLabels = chosenLabels + id
                creatingLabel = false
            },
        )
    }
}
