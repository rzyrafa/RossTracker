package pl.rosstracker.data.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** A single snapshot of a product as shown on rossmann.pl. */
data class ProductSnapshot(
    val id: Long,
    val url: String,
    /** Full name for notifications: brand, product line, caption and size in one string. */
    val name: String,
    /** Parts of [name] for the UI; null when the page didn't provide them (JSON-LD fallback). */
    val brand: String?,
    val title: String?,
    val caption: String?,
    val unit: String?,
    val imageUrl: String?,
    val price: Double,
    /** Regular price; set only while the product is discounted. */
    val oldPrice: Double?,
    val promoLabel: String?,
    val promoFrom: String?,
    val promoTo: String?,
    val lowestPrice30d: Double?,
    val available: Boolean,
) {
    val isPromo: Boolean get() = oldPrice != null && oldPrice > price
}

class ParseException(message: String) : Exception(message)

object RossmannParser {

    private const val BASE = "https://www.rossmann.pl"
    private val json = Json { ignoreUnknownKeys = true }
    private val whitespace = Regex("""\s+""")

    /** Matches product links such as `/Produkt/Kategoria/Slug,248554,13062`. */
    private val productUrlRegex =
        Regex("""https?://(?:www\.)?rossmann\.pl/Produkt/[^\s,]+,(\d+),(\d+)[^\s]*""", RegexOption.IGNORE_CASE)

    private val originRegex = Regex("""^https?://(?:www\.)?rossmann\.pl""", RegexOption.IGNORE_CASE)

    /**
     * Finds the first Rossmann product link in arbitrary shared text, normalised to
     * `https://www.rossmann.pl` (Android blocks cleartext `http://` requests).
     */
    fun extractProductUrl(text: String): String? =
        productUrlRegex.find(text)?.value?.substringBefore('?')?.substringBefore('#')
            ?.replace(originRegex, BASE)

    fun productIdFromUrl(url: String): Long? = productUrlRegex.find(url)?.groupValues?.get(1)?.toLongOrNull()

    fun parse(html: String, url: String): ProductSnapshot {
        val doc = Jsoup.parse(html)
        return parseNextData(doc, url)
            ?: parseJsonLd(doc, url)
            ?: throw ParseException("Nie znaleziono danych produktu na stronie")
    }

    /** The full source (with old price and promo dates); null when the page has no usable `__NEXT_DATA__`. */
    internal fun parseNextData(doc: Document, url: String): ProductSnapshot? {
        val raw = doc.getElementById("__NEXT_DATA__")?.data() ?: return null
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return null
        return findProduct(root, productIdFromUrl(url))?.let { fromNextData(it, url) }
    }

    /** Fallback with the current price only; it can't tell whether the product is on promo. */
    private fun parseJsonLd(doc: Document, url: String): ProductSnapshot? =
        doc.select("script[type=application/ld+json]").firstNotNullOfOrNull { script ->
            val el = runCatching { json.parseToJsonElement(script.data()) }.getOrNull() as? JsonObject
            if (el?.str("@type") == "Product") fromJsonLd(el, url) else null
        }

    /**
     * Depth-first search for the product object. The one whose `id` matches the URL wins;
     * otherwise the first object with both `price` and `oldPrice`. Products without a promo
     * may omit `oldPrice` entirely, so it can't be required for the id match.
     */
    private fun findProduct(el: JsonElement, wantedId: Long?): JsonObject? {
        var fallback: JsonObject? = null
        fun walk(e: JsonElement): JsonObject? {
            when (e) {
                is JsonObject -> {
                    if (e.num("price") != null) {
                        if (wantedId != null && e.long("id") == wantedId) return e
                        if (fallback == null && "oldPrice" in e) fallback = e
                    }
                    e.values.forEach { child -> walk(child)?.let { return it } }
                }
                is JsonArray -> e.forEach { child -> walk(child)?.let { return it } }
                else -> Unit
            }
            return null
        }
        return walk(el) ?: fallback
    }

    private fun fromNextData(p: JsonObject, url: String): ProductSnapshot {
        val price = p.num("price") ?: throw ParseException("Brak ceny")
        val oldPrice = p.num("oldPrice")?.takeIf { it > price }
        val brand = p.str("brand").clean()
        val unit = p.str("unit").clean()
        // Rossmann's `name` is the product line ("Glossy Hair") and is often empty; then
        // `fallbackName` ("Płyn micelarny") is the title and `fallbackCaption` the rest without repeating it.
        val productLine = p.str("name").clean()
        val title = productLine ?: p.str("fallbackName").clean()
        val caption = if (productLine != null) p.str("caption").clean() else p.str("fallbackCaption").clean() ?: p.str("caption").clean()
        val name = listOfNotNull(brand, productLine, p.str("caption").clean() ?: title, unit)
            .joinToString(" ").ifBlank { "Produkt" }
        val image = (p["pictures"] as? JsonArray)?.firstOrNull()?.let { it as? JsonObject }
            ?.let { it.str("medium") ?: it.str("large") }
        val badge = (p["badges"] as? JsonArray)?.firstOrNull()?.let { it as? JsonObject }?.str("name")
        val navigateUrl = p.str("navigateUrl")?.let { if (it.startsWith("/")) BASE + it else it }
        return ProductSnapshot(
            id = p.long("id") ?: productIdFromUrl(url) ?: throw ParseException("Brak ID produktu"),
            url = navigateUrl ?: url,
            name = name,
            brand = brand,
            title = title?.replaceFirstChar { it.uppercase() },
            caption = caption,
            unit = unit,
            imageUrl = image,
            price = price,
            oldPrice = oldPrice,
            promoLabel = if (oldPrice != null) badge ?: "Promocja" else null,
            promoFrom = if (oldPrice != null) p.str("promotionFrom") else null,
            promoTo = if (oldPrice != null) p.str("promotionTo") else null,
            lowestPrice30d = p.num("lastLowestPriceV2"),
            available = p.str("availability")?.let { it == "available" } ?: true,
        )
    }

    private fun fromJsonLd(p: JsonObject, url: String): ProductSnapshot? {
        val offers = when (val o = p["offers"]) {
            is JsonArray -> o.firstOrNull() as? JsonObject
            is JsonObject -> o
            else -> null
        } ?: return null
        val price = offers.num("price") ?: return null
        val image = when (val img = p["image"]) {
            is JsonPrimitive -> img.content
            is JsonArray -> (img.firstOrNull() as? JsonPrimitive)?.content
            else -> null
        }
        return ProductSnapshot(
            id = productIdFromUrl(url) ?: return null,
            url = url,
            name = p.str("name").clean() ?: "Produkt",
            brand = null,
            title = null,
            caption = null,
            unit = null,
            imageUrl = image,
            price = price,
            oldPrice = null,
            promoLabel = null,
            promoFrom = null,
            promoTo = null,
            lowestPrice30d = null,
            available = true,
        )
    }

    /** Collapses whitespace; blank becomes null. */
    private fun String?.clean(): String? = this?.replace(whitespace, " ")?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.prim(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }
    private fun JsonObject.str(key: String): String? = prim(key)?.takeIf { it.isString }?.content
    private fun JsonObject.num(key: String): Double? = prim(key)?.let { it.doubleOrNull ?: it.content.replace(',', '.').toDoubleOrNull() }
    private fun JsonObject.long(key: String): Long? = prim(key)?.longOrNull
}
