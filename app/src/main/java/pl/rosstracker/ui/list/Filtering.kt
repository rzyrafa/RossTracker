package pl.rosstracker.ui.list

import pl.rosstracker.data.db.ProductEntity
import pl.rosstracker.data.db.ProductLabel
import java.time.Instant
import java.time.temporal.ChronoUnit

internal enum class Filter(val label: String) {
    ALL("Wszystkie"),
    PROMO("Promocje"),
    ENDING("Kończą się"),
}

/** Promo that ends within the next 3 days. */
internal fun ProductEntity.promoEndingSoon(now: Instant = Instant.now()): Boolean {
    val end = promoTo?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
    return isPromo && end.isBefore(now.plus(3, ChronoUnit.DAYS))
}

internal fun Filter.matches(p: ProductEntity) = when (this) {
    Filter.ALL -> true
    Filter.PROMO -> p.isPromo
    Filter.ENDING -> p.promoEndingSoon()
}

/** Products in the label tab [labelId] (null = every product), keeping the incoming order. */
internal fun inLabel(products: List<ProductEntity>, links: List<ProductLabel>, labelId: Long?): List<ProductEntity> {
    if (labelId == null) return products
    val ids = links.asSequence().filter { it.labelId == labelId }.map { it.productId }.toSet()
    return products.filter { it.id in ids }
}

/** How many tracked products carry each label. Links to products that no longer exist are ignored. */
internal fun labelCounts(products: List<ProductEntity>, links: List<ProductLabel>): Map<Long, Int> {
    val ids = products.mapTo(HashSet()) { it.id }
    return links.filter { it.productId in ids }.groupingBy { it.labelId }.eachCount()
}
