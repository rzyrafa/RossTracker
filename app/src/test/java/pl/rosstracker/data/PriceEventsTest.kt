package pl.rosstracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceEventsTest {

    @Test
    fun firstCheckProducesNothing() {
        assertTrue(detectEvents(null, PriceState(10.0, 15.0)).isEmpty())
    }

    @Test
    fun promoStartIsReportedOnce() {
        val events = detectEvents(PriceState(22.99, null), PriceState(14.99, 22.99), "MEGA!", "2026-11-04T00:00:00Z")
        assertEquals(1, events.size)
        val promo = events.single() as PriceEvent.PromoStarted
        assertEquals(35, promo.discountPercent)
        assertEquals("MEGA!", promo.label)
    }

    @Test
    fun ongoingPromoIsNotReportedAgain() {
        assertTrue(detectEvents(PriceState(14.99, 22.99), PriceState(14.99, 22.99)).isEmpty())
    }

    @Test
    fun deeperDiscountDuringPromoIsAPriceDrop() {
        val events = detectEvents(PriceState(14.99, 22.99), PriceState(12.99, 22.99))
        assertEquals(listOf(PriceEvent.PriceDropped(14.99, 12.99)), events)
    }

    @Test
    fun regularPriceDropIsReported() {
        val events = detectEvents(PriceState(7.29, null), PriceState(6.99, null))
        assertEquals(listOf(PriceEvent.PriceDropped(7.29, 6.99)), events)
    }

    @Test
    fun priceIncreaseOrPromoEndIsSilent() {
        assertTrue(detectEvents(PriceState(6.99, null), PriceState(7.29, null)).isEmpty())
        assertTrue(detectEvents(PriceState(14.99, 22.99), PriceState(22.99, null)).isEmpty())
    }
}
