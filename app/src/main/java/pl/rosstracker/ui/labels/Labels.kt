package pl.rosstracker.ui.labels

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.rosstracker.ServiceLocator
import pl.rosstracker.data.db.LabelEntity
import pl.rosstracker.data.db.ProductLabel
import pl.rosstracker.ui.Eyebrow
import pl.rosstracker.ui.LabelColorNames
import pl.rosstracker.ui.calm
import pl.rosstracker.ui.coffee

/** Material's "label" tag shape; material-icons-core doesn't include it. */
val LabelIcon: ImageVector = materialIcon(name = "Filled.Label") {
    materialPath {
        moveTo(17.63f, 5.84f)
        curveTo(17.27f, 5.33f, 16.67f, 5f, 16f, 5f)
        lineTo(5f, 5.01f)
        curveTo(3.9f, 5.01f, 3f, 5.9f, 3f, 7f)
        verticalLineToRelative(10f)
        curveToRelative(0f, 1.1f, 0.9f, 1.99f, 2f, 1.99f)
        lineTo(16f, 19f)
        curveToRelative(0.67f, 0f, 1.27f, -0.33f, 1.63f, -0.84f)
        lineTo(22f, 12f)
        lineToRelative(-4.37f, -6.16f)
        close()
    }
}

/**
 * Rounded tab for a label (or "Wszystkie" when [color] is null). Unselected it shows a colour dot,
 * selected it fills with the label's colour.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LabelChip(
    name: String,
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val fill = color ?: MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(if (selected) fill else coffee.chip, calm(300), label = "labelBg")
    val fg by animateColorAsState(
        when {
            !selected -> MaterialTheme.colorScheme.onSurface
            color != null -> coffee.onLabel
            else -> MaterialTheme.colorScheme.onPrimary
        },
        calm(300),
        label = "labelFg",
    )
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (color != null && !selected) Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            if (count != null) "$name  $count" else name,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small "+ Nowa" chip that opens the label editor. */
@Composable
fun NewLabelChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Text("Nowa", style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Creates a label ([initial] null) or edits/deletes one. Saves straight to the database and
 * reports the label id through [onSaved].
 */
@Composable
fun LabelEditorDialog(
    initial: LabelEntity?,
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit = {},
    onDeleted: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val labels = ServiceLocator.labels
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var color by remember { mutableStateOf(initial?.color ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Usunąć etykietę „${initial.name}”?") },
            text = { Text("Produkty zostaną, zniknie tylko etykieta.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        labels.delete(initial.id)
                        onDeleted()
                    }
                }) { Text("Usuń", color = coffee.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anuluj") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nowa etykieta" else "Edytuj etykietę") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("KOLOR", style = Eyebrow, color = coffee.muted)
                ColorSwatches(selected = color, onSelect = { color = it })
                if (initial != null) {
                    TextButton(onClick = { confirmDelete = true }, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        Text("Usuń etykietę", color = coffee.accent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        val id = if (initial == null) {
                            labels.create(name, color)
                        } else {
                            labels.update(initial.copy(name = name, color = color))
                            initial.id
                        }
                        onSaved(id)
                    }
                },
            ) { Text(if (initial == null) "Utwórz" else "Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        coffee.labels.forEachIndexed { index, c ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(c)
                    .clickable { onSelect(index) }
                    .semantics { contentDescription = LabelColorNames[index] },
                contentAlignment = Alignment.Center,
            ) {
                if (index == selected) Icon(Icons.Default.Check, contentDescription = null, tint = coffee.onLabel, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Bottom sheet for the selected products: a ticked box means all of them have the label,
 * a dash means only some. Only labels the user actually changed are written on "Zastosuj".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelPickerSheet(
    productIds: Set<Long>,
    labels: List<LabelEntity>,
    links: List<ProductLabel>,
    onDismiss: () -> Unit,
    onApplied: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initial = remember(productIds, links) {
        labels.associate { label ->
            val count = links.count { it.labelId == label.id && it.productId in productIds }
            label.id to when (count) {
                0 -> ToggleableState.Off
                productIds.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
        }
    }
    val changed = remember { mutableStateMapOf<Long, ToggleableState>() }
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                "Etykiety · ${productIds.size} ${productsWord(productIds.size)}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            labels.forEach { label ->
                val state = changed[label.id] ?: initial[label.id] ?: ToggleableState.Off
                val toggle = { changed[label.id] = if (state == ToggleableState.On) ToggleableState.Off else ToggleableState.On }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = toggle)
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(12.dp).background(coffee.label(label.color), CircleShape))
                    Text(label.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TriStateCheckbox(
                        state = state,
                        onClick = toggle,
                        colors = CheckboxDefaults.colors(checkedColor = coffee.label(label.color), checkmarkColor = coffee.onLabel),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { creating = true }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Nowa etykieta", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    val add = changed.filterValues { it == ToggleableState.On }.keys
                    val remove = changed.filterValues { it == ToggleableState.Off }.keys
                    scope.launch {
                        ServiceLocator.labels.setLabels(productIds, add, remove)
                        onApplied()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            ) { Text("Zastosuj", style = MaterialTheme.typography.labelLarge) }
        }
    }

    if (creating) {
        LabelEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onSaved = { id ->
                // The new label shows up through the labels flow; tick it for the selection straight away.
                changed[id] = ToggleableState.On
                creating = false
            },
        )
    }
}

private fun productsWord(n: Int): String = when {
    n == 1 -> "produkt"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "produkty"
    else -> "produktów"
}
