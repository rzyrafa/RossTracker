package pl.rosstracker.ui.list

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pl.rosstracker.BuildConfig
import pl.rosstracker.ServiceLocator
import pl.rosstracker.data.db.LabelEntity
import pl.rosstracker.data.db.ProductEntity
import pl.rosstracker.data.db.ProductLabel
import pl.rosstracker.formatDateTime
import pl.rosstracker.formatIsoDayMonth
import pl.rosstracker.ui.Eyebrow
import pl.rosstracker.ui.ThemeMode
import pl.rosstracker.ui.calm
import pl.rosstracker.ui.coffee
import pl.rosstracker.ui.common.Pill
import pl.rosstracker.ui.common.PillStyle
import pl.rosstracker.ui.common.PriceTag
import pl.rosstracker.ui.common.PromoPill
import pl.rosstracker.ui.common.pressScale
import pl.rosstracker.ui.common.sharedProductImage
import pl.rosstracker.ui.labels.LabelChip
import pl.rosstracker.ui.labels.LabelEditorDialog
import pl.rosstracker.ui.labels.LabelIcon
import pl.rosstracker.ui.labels.LabelPickerSheet
import pl.rosstracker.ui.refreshAllAndNotify
import pl.rosstracker.work.STALL_THRESHOLD
import pl.rosstracker.work.WorkScheduler

/**
 * @param onAddClick opens the add dialog; gets the open label tab so the new product lands in it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onOpenProduct: (Long) -> Unit,
    onAddClick: (labelId: Long?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by ServiceLocator.repository.observeProducts().collectAsStateWithLifecycle(initialValue = null)
    val labels by ServiceLocator.labels.observeLabels().collectAsStateWithLifecycle(initialValue = null)
    val links by ServiceLocator.labels.observeLinks().collectAsStateWithLifecycle(initialValue = emptyList())
    val lastAutoCheck by ServiceLocator.autoChecks.lastChecked.collectAsStateWithLifecycle()
    var refreshing by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var selectedLabel by rememberSaveable { mutableStateOf<Long?>(null) }
    var selection by remember { mutableStateOf(emptySet<Long>()) }
    var editingLabel by remember { mutableStateOf<LabelEntity?>(null) }
    var creatingLabel by remember { mutableStateOf(false) }
    var pickingLabels by remember { mutableStateOf(false) }

    // A deleted label's tab falls back to "Wszystkie".
    LaunchedEffect(labels, selectedLabel) {
        val all = labels ?: return@LaunchedEffect
        if (selectedLabel != null && all.none { it.id == selectedLabel }) selectedLabel = null
    }
    BackHandler(enabled = selection.isNotEmpty()) { selection = emptySet() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selection.isEmpty()) {
                AddMenuButton(
                    onAddProduct = { onAddClick(selectedLabel) },
                    onCreateLabel = { creatingLabel = true },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    val result = refreshAllAndNotify(context)
                    refreshing = false
                    if (result.networkFailures > 0) {
                        Toast.makeText(context, "Nie udało się sprawdzić ${result.networkFailures} produktów", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            val list = products
            // Nothing until the first load: an almost empty grid would stay anchored to its footer.
            AnimatedVisibility(
                visible = list != null,
                enter = fadeIn(calm(600)) + slideInVertically(calm(600)) { it / 12 },
            ) {
                if (list.isNullOrEmpty()) {
                    EmptyState()
                } else {
                    ProductGrid(
                        products = list,
                        labels = labels.orEmpty(),
                        links = links,
                        selectedLabel = selectedLabel,
                        onSelectLabel = { selectedLabel = it },
                        onEditLabel = { editingLabel = it },
                        filter = filter,
                        onFilter = { filter = it },
                        selection = selection,
                        onSelectionChange = { selection = it },
                        onPickLabels = { pickingLabels = true },
                        lastAutoCheck = lastAutoCheck,
                        onOpenProduct = onOpenProduct,
                    )
                }
            }
        }
    }

    if (creatingLabel) {
        LabelEditorDialog(
            initial = null,
            onDismiss = { creatingLabel = false },
            onSaved = { id ->
                creatingLabel = false
                selectedLabel = id
            },
        )
    }
    editingLabel?.let { label ->
        LabelEditorDialog(
            initial = label,
            onDismiss = { editingLabel = null },
            onSaved = { editingLabel = null },
            onDeleted = { editingLabel = null },
        )
    }
    if (pickingLabels) {
        LabelPickerSheet(
            productIds = selection,
            labels = labels.orEmpty(),
            links = links,
            onDismiss = { pickingLabels = false },
            onApplied = {
                pickingLabels = false
                selection = emptySet()
            },
        )
    }
}

/** The + button opens a small menu: add a product or create a label. */
@Composable
private fun AddMenuButton(onAddProduct: () -> Unit, onCreateLabel: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(
            onClick = { open = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(Icons.Default.Add, contentDescription = "Dodaj") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            DropdownMenuItem(
                text = { Text("Dodaj produkt") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    open = false
                    onAddProduct()
                },
            )
            DropdownMenuItem(
                text = { Text("Utwórz etykietę") },
                leadingIcon = { Icon(LabelIcon, contentDescription = null) },
                onClick = {
                    open = false
                    onCreateLabel()
                },
            )
        }
    }
}

@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    labels: List<LabelEntity>,
    links: List<ProductLabel>,
    selectedLabel: Long?,
    onSelectLabel: (Long?) -> Unit,
    onEditLabel: (LabelEntity) -> Unit,
    filter: Filter,
    onFilter: (Filter) -> Unit,
    selection: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onPickLabels: () -> Unit,
    lastAutoCheck: Long?,
    onOpenProduct: (Long) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val labelled = remember(products, links, selectedLabel) { inLabel(products, links, selectedLabel) }
    val visible = remember(labelled, filter) { labelled.filter(filter::matches) }
    val counts = remember(products, links) { labelCounts(products, links) }
    val selecting = selection.isNotEmpty()
    // Masonry: cards keep their natural height, 2 columns on phones.
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) {
            if (selecting) {
                SelectionBar(count = selection.size, onClose = { onSelectionChange(emptySet()) }, onPickLabels = onPickLabels)
            } else {
                Header(lastAutoCheck, firstAddedAt = products.minOf { it.addedAt })
            }
        }
        if (labels.isNotEmpty()) {
            item(key = "labels", span = StaggeredGridItemSpan.FullLine) {
                LabelTabs(labels, counts, products.size, selectedLabel, onSelectLabel, onEditLabel)
            }
        }
        item(key = "filters", span = StaggeredGridItemSpan.FullLine) {
            FilterChips(labelled, filter, onFilter, secondary = labels.isNotEmpty())
        }
        items(visible, key = { it.id }) { product ->
            val isSelected = product.id in selection
            ProductCard(
                product,
                selected = isSelected,
                onClick = {
                    if (selecting) onSelectionChange(if (isSelected) selection - product.id else selection + product.id)
                    else onOpenProduct(product.id)
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectionChange(selection + product.id)
                },
                modifier = Modifier.animateItem(fadeInSpec = calm(400), placementSpec = calm(500), fadeOutSpec = calm(250)),
            )
        }
        if (visible.isEmpty()) {
            item(key = "no-match", span = StaggeredGridItemSpan.FullLine) {
                val label = labels.firstOrNull { it.id == selectedLabel }
                Text(
                    when {
                        label != null && labelled.isEmpty() ->
                            "Brak produktów z etykietą „${label.name}”.\nPrzytrzymaj produkt w zakładce Wszystkie, aby go oznaczyć."
                        filter == Filter.ENDING -> "Żadna promocja nie kończy się w ciągu 3 dni."
                        else -> "Nic tu teraz nie ma."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = coffee.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                )
            }
        }
        item(key = "version", span = StaggeredGridItemSpan.FullLine) {
            Text(
                "RossTracker ${BuildConfig.VERSION_NAME} · ceny ze sklepu online",
                style = MaterialTheme.typography.labelSmall,
                color = coffee.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun Header(lastAutoCheck: Long?, firstAddedAt: Long) {
    val context = LocalContext.current
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RossTracker", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (BuildConfig.DEBUG) {
                // Debug helper: runs the real background worker right now.
                IconButton(onClick = {
                    WorkScheduler.runNow(context)
                    Toast.makeText(context, "Uruchomiono sprawdzanie w tle", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.Refresh, contentDescription = "Sprawdź teraz w tle") }
            }
            ThemeMenu()
        }
        AutoCheckStatus(lastAutoCheck, firstAddedAt)
    }
}

/** Shown in place of the header while products are selected. */
@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, onPickLabels: () -> Unit) {
    Row(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Zakończ zaznaczanie") }
        Text("Zaznaczone: $count", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Button(
            onClick = onPickLabels,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = CircleShape,
        ) {
            Icon(LabelIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Etykiety")
        }
    }
}

/** Upper row: "Wszystkie" plus one tab per label. Long-press a label to edit or delete it. */
@Composable
private fun LabelTabs(
    labels: List<LabelEntity>,
    counts: Map<Long, Int>,
    total: Int,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    onEdit: (LabelEntity) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        item(key = "all") {
            LabelChip("Wszystkie", color = null, selected = selected == null, onClick = { onSelect(null) }, count = total)
        }
        items(labels.size, key = { labels[it].id }) { i ->
            val label = labels[i]
            LabelChip(
                label.name,
                color = coffee.label(label.color),
                selected = label.id == selected,
                onClick = { onSelect(label.id) },
                count = counts[label.id] ?: 0,
                onLongClick = { onEdit(label) },
            )
        }
    }
}

/**
 * Price filters. Under label tabs ([secondary]) they turn into smaller outlined chips, so the two
 * "Wszystkie" read as tab + sub-filter rather than two identical rows.
 */
@Composable
private fun FilterChips(products: List<ProductEntity>, selected: Filter, onSelect: (Filter) -> Unit, secondary: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        items(Filter.entries.size) { i ->
            val f = Filter.entries[i]
            val count = products.count(f::matches)
            val isSelected = f == selected
            val bg by animateColorAsState(
                when {
                    !isSelected -> if (secondary) Color.Transparent else coffee.chip
                    secondary -> coffee.accentSoft
                    else -> MaterialTheme.colorScheme.primary
                },
                calm(300),
                label = "chipBg",
            )
            val fg by animateColorAsState(
                when {
                    !isSelected -> MaterialTheme.colorScheme.onSurface
                    secondary -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onPrimary
                },
                calm(300),
                label = "chipFg",
            )
            val outline = if (secondary && !isSelected) MaterialTheme.colorScheme.outline else Color.Transparent
            Text(
                "${f.label}  $count",
                style = if (secondary) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = fg,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.dp, outline, CircleShape)
                    .clickable { onSelect(f) }
                    .padding(horizontal = if (secondary) 12.dp else 16.dp, vertical = if (secondary) 6.dp else 9.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductCard(
    product: ProductEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val border by animateColorAsState(if (selected) coffee.accent else Color.Transparent, calm(250), label = "selBorder")
    Column(
        modifier
            .pressScale(interaction)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(2.dp, border, MaterialTheme.shapes.large)
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
    ) {
        Box {
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(1f)
                        .padding(8.dp)
                        .size(26.dp)
                        .background(coffee.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Check, contentDescription = "Zaznaczony", tint = coffee.onAccent, modifier = Modifier.size(16.dp)) }
            }
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .sharedProductImage(product.id)
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(coffee.imageTile)
                    .padding(14.dp),
            )
            if (product.isPromo) {
                PromoPill(
                    product.promoLabel,
                    product.price,
                    product.oldPrice!!,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            product.brand?.let {
                Text(it.uppercase(), style = Eyebrow, color = coffee.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                product.title ?: product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            product.unit?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = coffee.muted) }
            Spacer(Modifier.height(2.dp))
            PriceTag(product.price, product.oldPrice)
            if (product.isPromo) {
                formatIsoDayMonth(product.promoTo)?.let {
                    Pill("do $it", style = if (product.promoEndingSoon()) PillStyle.Dark else PillStyle.Soft)
                }
            }
            if (product.lastError != null) {
                Text(
                    "Nie udało się sprawdzić",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ThemeMenu() {
    val themeStore = ServiceLocator.theme
    val current by themeStore.mode.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            Text(
                "MOTYW",
                style = Eyebrow,
                color = coffee.muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        themeStore.set(mode)
                        open = false
                    },
                    trailingIcon = {
                        if (mode == current) Icon(Icons.Default.Check, contentDescription = "Wybrany")
                    },
                )
            }
        }
    }
}

/**
 * Shows when the background check last ran. Some phone makers kill background work; after
 * [STALL_THRESHOLD] without a run we point the user at the app's battery settings.
 */
@Composable
private fun AutoCheckStatus(lastAutoCheck: Long?, firstAddedAt: Long) {
    val context = LocalContext.current
    val reference = lastAutoCheck ?: firstAddedAt
    val stalled = System.currentTimeMillis() - reference > STALL_THRESHOLD.toMillis()
    if (!stalled) {
        Text(
            lastAutoCheck?.let { "Ostatnie automatyczne sprawdzenie: ${formatDateTime(it)}" }
                ?: "Automatyczne sprawdzanie ok. 7:00 i 19:00",
            style = MaterialTheme.typography.bodySmall,
            color = coffee.muted,
        )
        return
    }
    Column(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            lastAutoCheck?.let { "Automatyczne sprawdzanie nie działa od ${formatDateTime(it)}." }
                ?: "Automatyczne sprawdzanie jeszcze ani razu się nie wykonało.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            "Telefon prawdopodobnie blokuje aplikację w tle. W ustawieniach aplikacji wejdź w „Bateria” " +
                "i wybierz „Bez ograniczeń” (na Xiaomi włącz też „Autostart”).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = coffee.accent, contentColor = coffee.onAccent),
        ) { Text("Otwórz ustawienia aplikacji") }
    }
}

@Composable
private fun EmptyState() {
    // Wrapped in a scrollable list so pull-to-refresh still works on an empty screen.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text("RossTracker", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(coffee.accentSoft),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Add, contentDescription = null, tint = coffee.accent) }
                Text("Twoja półka jest pusta", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Otwórz produkt na rossmann.pl, kliknij „Udostępnij” i wybierz RossTracker " +
                        "albo naciśnij + i wklej link.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = coffee.muted,
                )
            }
        }
    }
}
