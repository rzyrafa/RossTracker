package pl.rosstracker.data.net

import org.jsoup.Jsoup
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Parses a folder of real product pages downloaded from rossmann.pl. Skipped unless
 * ROSSTRACKER_BULK_DIR points at a folder with `index.tsv` (id<TAB>url) and `pages/<id>.html`.
 */
class BulkParseTest {

    @Test
    fun parsesEveryDownloadedPage() {
        val dir = System.getenv("ROSSTRACKER_BULK_DIR")?.let(::File)
        assumeTrue("ROSSTRACKER_BULK_DIR not set", dir != null && File(dir, "index.tsv").exists())

        val problems = mutableListOf<String>()
        var promos = 0
        var unavailable = 0
        val entries = File(dir, "index.tsv").readLines().filter { it.isNotBlank() }.map { it.split('\t') }
        for ((id, url) in entries) {
            val html = File(dir, "pages/$id.html").readText()
            val p = runCatching { RossmannParser.parse(html, url) }.getOrElse {
                problems += "$id: ${it.message}"
                continue
            }
            if (RossmannParser.parseNextData(Jsoup.parse(html), url) == null) problems += "$id: JSON-LD fallback only"
            if (p.id != id.toLong()) problems += "$id: wrong id ${p.id}"
            if (p.price <= 0) problems += "$id: price ${p.price}"
            if (p.name.isBlank()) problems += "$id: empty name"
            if (p.imageUrl == null) problems += "$id: no image"
            if (p.isPromo) promos++
            if (!p.available) unavailable++
            println("%-8s %8.2f %8s %-10s %s".format(id, p.price, p.oldPrice?.let { "%.2f".format(it) } ?: "-", p.promoLabel ?: "", p.name.take(60)))
        }
        println("parsed=${entries.size} promos=$promos unavailable=$unavailable problems=${problems.size}")
        problems.forEach(::println)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
