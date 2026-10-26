package pl.rosstracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    /** Best deals first: biggest discount in percent, then everything without a promo by name. */
    @Query(
        """
        SELECT * FROM products
        ORDER BY CASE WHEN oldPrice > price THEN (oldPrice - price) / oldPrice ELSE 0 END DESC, name
        """,
    )
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeProduct(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM price_points WHERE productId = :productId ORDER BY timestamp")
    fun observePricePoints(productId: Long): Flow<List<PricePointEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProduct(id: Long): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getProducts(): List<ProductEntity>

    @Upsert
    suspend fun upsertProduct(product: ProductEntity)

    @Insert
    suspend fun insertPricePoint(point: PricePointEntity)

    @Query("UPDATE products SET lastError = :error, lastChecked = :checkedAt WHERE id = :id")
    suspend fun setError(id: Long, error: String?, checkedAt: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)
}
