package pl.rosstracker.data

import pl.rosstracker.discountPercent

/** Something worth notifying the user about, found by comparing two consecutive checks. */
sealed interface PriceEvent {
    data class PromoStarted(
        val price: Double,
        val regularPrice: Double,
        val label: String?,
        val promoTo: String?,
    ) : PriceEvent {
        val discountPercent: Int get() = discountPercent(price, regularPrice)
    }

    data class PriceDropped(val from: Double, val to: Double) : PriceEvent
}

/** Minimal price state needed to compare two checks. */
data class PriceState(val price: Double, val oldPrice: Double?) {
    val isPromo: Boolean get() = oldPrice != null && oldPrice > price
}

private const val EPSILON = 0.005

/**
 * Compares the previous and the current check. A promo start wins over a plain
 * price drop so the user gets a single notification for the same change.
 */
fun detectEvents(
    previous: PriceState?,
    current: PriceState,
    label: String? = null,
    promoTo: String? = null,
): List<PriceEvent> {
    if (previous == null) return emptyList()
    if (current.isPromo && !previous.isPromo) {
        return listOf(PriceEvent.PromoStarted(current.price, current.oldPrice!!, label, promoTo))
    }
    if (current.price < previous.price - EPSILON) {
        return listOf(PriceEvent.PriceDropped(previous.price, current.price))
    }
    return emptyList()
}
