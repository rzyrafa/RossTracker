package pl.rosstracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels ORDER BY createdAt")
    fun observeLabels(): Flow<List<LabelEntity>>

    /** Every product–label link; the lists are small, so counts and filtering happen in memory. */
    @Query("SELECT * FROM product_labels")
    fun observeProductLabels(): Flow<List<ProductLabel>>

    @Insert
    suspend fun insertLabel(label: LabelEntity): Long

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :id")
    suspend fun deleteLabel(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<ProductLabel>)

    @Query("DELETE FROM product_labels WHERE productId IN (:productIds) AND labelId IN (:labelIds)")
    suspend fun deleteLinks(productIds: Collection<Long>, labelIds: Collection<Long>)

    /** Adds [add] and removes [remove] for every product in [productIds], in one go. */
    @Transaction
    suspend fun setLabels(productIds: Collection<Long>, add: Collection<Long>, remove: Collection<Long>) {
        if (remove.isNotEmpty()) deleteLinks(productIds, remove)
        if (add.isNotEmpty()) insertLinks(productIds.flatMap { p -> add.map { ProductLabel(p, it) } })
    }
}
