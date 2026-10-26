package pl.rosstracker.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import pl.rosstracker.data.db.PricePointEntity
import pl.rosstracker.data.db.ProductDao
import pl.rosstracker.data.db.ProductEntity
import pl.rosstracker.data.net.ProductSnapshot
import pl.rosstracker.data.net.RossmannClient
import java.io.IOException
import kotlin.random.Random

data class ProductEvents(val product: ProductEntity, val events: List<PriceEvent>)

data class RefreshResult(val events: List<ProductEvents>, val networkFailures: Int, val checked: Int)

class ProductRepository(
    private val dao: ProductDao,
    private val client: RossmannClient,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeProducts(): Flow<List<ProductEntity>> = dao.observeProducts()
    fun observeProduct(id: Long): Flow<ProductEntity?> = dao.observeProduct(id)
    fun observePricePoints(id: Long): Flow<List<PricePointEntity>> = dao.observePricePoints(id)

    /** Downloads the product page without saving anything, for the "add product" preview. */
    suspend fun preview(url: String): ProductSnapshot = client.fetchProduct(url)

    suspend fun isTracked(id: Long): Boolean = dao.getProduct(id) != null

    suspend fun track(snapshot: ProductSnapshot) {
        val time = now()
        val existing = dao.getProduct(snapshot.id)
        save(snapshot, existing, time)
    }

    suspend fun delete(id: Long) = dao.deleteProduct(id)

    /** Checks one product, stores the new price and returns what changed. */
    suspend fun refresh(id: Long): ProductEvents? {
        val existing = dao.getProduct(id) ?: return null
        val time = now()
        val snapshot = try {
            client.fetchProduct(existing.url)
        } catch (e: Exception) {
            dao.setError(id, e.message ?: e.javaClass.simpleName, time)
            throw e
        }
        val saved = save(snapshot, existing, time)
        val events = detectEvents(
            previous = PriceState(existing.price, existing.oldPrice),
            current = PriceState(snapshot.price, snapshot.oldPrice),
            label = snapshot.promoLabel,
            promoTo = snapshot.promoTo,
        )
        return ProductEvents(saved, events)
    }

    /** Checks every tracked product one by one, with a short pause so we don't hammer the shop. */
    suspend fun refreshAll(pause: Boolean = true): RefreshResult {
        val products = dao.getProducts()
        val events = mutableListOf<ProductEvents>()
        var networkFailures = 0
        products.forEachIndexed { index, product ->
            if (pause && index > 0) delay(Random.nextLong(1_000, 3_000))
            try {
                refresh(product.id)?.takeIf { it.events.isNotEmpty() }?.let(events::add)
            } catch (_: IOException) {
                networkFailures++
            } catch (_: Exception) {
                // Parse errors are stored on the product and shown in the UI.
            }
        }
        return RefreshResult(events, networkFailures, products.size)
    }

    private suspend fun save(snapshot: ProductSnapshot, existing: ProductEntity?, time: Long): ProductEntity {
        val entity = ProductEntity(
            id = snapshot.id,
            url = snapshot.url,
            name = snapshot.name,
            imageUrl = snapshot.imageUrl,
            price = snapshot.price,
            oldPrice = snapshot.oldPrice,
            promoLabel = snapshot.promoLabel,
            promoFrom = snapshot.promoFrom,
            promoTo = snapshot.promoTo,
            lowestPrice30d = snapshot.lowestPrice30d,
            available = snapshot.available,
            addedAt = existing?.addedAt ?: time,
            lastChecked = time,
            lastError = null,
            brand = snapshot.brand,
            title = snapshot.title,
            caption = snapshot.caption,
            unit = snapshot.unit,
        )
        dao.upsertProduct(entity)
        dao.insertPricePoint(PricePointEntity(productId = entity.id, timestamp = time, price = entity.price, oldPrice = entity.oldPrice))
        return entity
    }
}
