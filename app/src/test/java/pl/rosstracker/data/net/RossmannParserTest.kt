package pl.rosstracker.data.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RossmannParserTest {

    private fun fixture(name: String) =
        javaClass.classLoader!!.getResource(name)!!.readText(Charsets.UTF_8)

    private val promoUrl =
        "https://www.rossmann.pl/Produkt/Plyny-do-demakijazu/NIVEA-plyn-micelarny-kojacy-do-cery-wrazliwej-i-nadwrazliwej-400-ml,248554,13062"
    private val regularUrl =
        "https://www.rossmann.pl/Produkt/Kremy-do-twarzy/NIVEA-Creme-krem-uniwersalny-do-twarzy-ciala-i-dloni-50-ml,9399,13049"

    @Test
    fun parsesPromoProduct() {
        val p = RossmannParser.parse(fixture("product_promo.html"), promoUrl)
        assertEquals(248554L, p.id)
        assertEquals(14.99, p.price, 0.001)
        assertEquals(22.99, p.oldPrice!!, 0.001)
        assertTrue(p.isPromo)
        assertEquals("MEGA!", p.promoLabel)
        assertEquals("2026-11-04T00:00:00Z", p.promoTo)
        assertEquals(18.99, p.lowestPrice30d!!, 0.001)
        assertTrue(p.name.startsWith("NIVEA"))
        assertTrue(p.imageUrl!!.startsWith("https://"))
        // Empty product line: fallbackName becomes the title, caption doesn't repeat it.
        assertEquals("NIVEA", p.brand)
        assertEquals("Płyn micelarny", p.title)
        assertEquals("kojący, do cery wrażliwej i nadwrażliwej", p.caption)
        assertEquals("400 ml", p.unit)
    }

    @Test
    fun parsesRegularProduct() {
        val p = RossmannParser.parse(fixture("product_regular.html"), regularUrl)
        assertEquals(9399L, p.id)
        assertEquals(7.29, p.price, 0.001)
        assertNull(p.oldPrice)
        assertFalse(p.isPromo)
        assertNull(p.promoLabel)
    }

    @Test
    fun parsesProductWithoutOldPriceField() {
        val url = "https://www.rossmann.pl/Produkt/Wygladzanie-wlosow/" +
            "Neboa-Glossy-Hair-spray-do-wlosow-kazdego-rodzaju-wygladzajacy-efekt-tafli-wody-175-ml,2033039,19837"
        val p = RossmannParser.parse(fixture("product_no_oldprice.html"), url)
        assertEquals(2033039L, p.id)
        assertEquals(32.99, p.price, 0.001)
        assertNull(p.oldPrice)
        // Brand comes from __NEXT_DATA__, not the JSON-LD fallback.
        assertTrue(p.name, p.name.startsWith("Neboa Glossy Hair"))
        assertEquals("Neboa", p.brand)
        assertEquals("Glossy Hair", p.title)
        assertEquals("spray do włosów każdego rodzaju, wygładzający, efekt tafli wody", p.caption)
        assertEquals("175 ml", p.unit)
    }

    @Test
    fun extractsUrlFromSharedText() {
        val shared = "Zobacz to: $promoUrl?utm_source=share fajne"
        assertEquals(promoUrl, RossmannParser.extractProductUrl(shared))
        assertEquals(248554L, RossmannParser.productIdFromUrl(promoUrl))
        assertNull(RossmannParser.extractProductUrl("https://www.rossmann.pl/promocje"))
    }

    @Test
    fun upgradesHttpUrlToHttps() {
        val path = "/Produkt/Szampony-w-plynie/Green-Pharmacy-szampon-z-Nagietkiem-350-ml,404665,13177"
        val expected = "https://www.rossmann.pl$path"
        assertEquals(expected, RossmannParser.extractProductUrl("http://www.rossmann.pl$path"))
        assertEquals(expected, RossmannParser.extractProductUrl("http://rossmann.pl$path"))
        assertEquals(expected, RossmannParser.extractProductUrl("HTTP://WWW.Rossmann.pl$path"))
    }
}
