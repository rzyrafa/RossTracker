package pl.rosstracker.ui.list

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.rosstracker.data.db.ProductEntity
import pl.rosstracker.data.db.ProductLabel

class FilteringTest {
    private fun product(id: Long, price: Double = 10.0, oldPrice: Double? = null) = ProductEntity(
        id = id, url = "", name = "P$id", imageUrl = null, price = price, oldPrice = oldPrice,
        promoLabel = null, promoFrom = null, promoTo = null, lowestPrice30d = null,
        available = true, addedAt = 0, lastChecked = 0,
    )

    private val products = listOf(product(1), product(2, 8.0, oldPrice = 10.0), product(3))
    private val hair = 10L
    private val face = 20L
    private val links = listOf(
        ProductLabel(1, hair),
        ProductLabel(2, hair),
        ProductLabel(2, face),
        ProductLabel(99, face), // product already deleted
    )

    @Test
    fun noLabelKeepsEverythingInOrder() {
        assertEquals(listOf(1L, 2L, 3L), inLabel(products, links, null).map { it.id })
    }

    @Test
    fun labelTabShowsOnlyItsProducts() {
        assertEquals(listOf(1L, 2L), inLabel(products, links, hair).map { it.id })
        assertEquals(listOf(2L), inLabel(products, links, face).map { it.id })
        assertEquals(emptyList<Long>(), inLabel(products, links, 30L).map { it.id })
    }

    @Test
    fun promoFilterWorksInsideLabel() {
        val promos = inLabel(products, links, hair).filter(Filter.PROMO::matches)
        assertEquals(listOf(2L), promos.map { it.id })
    }

    @Test
    fun countsIgnoreLinksToMissingProducts() {
        assertEquals(mapOf(hair to 2, face to 1), labelCounts(products, links))
    }
}
