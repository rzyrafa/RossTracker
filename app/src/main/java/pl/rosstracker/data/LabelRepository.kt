package pl.rosstracker.data

import kotlinx.coroutines.flow.Flow
import pl.rosstracker.data.db.LabelDao
import pl.rosstracker.data.db.LabelEntity
import pl.rosstracker.data.db.ProductLabel

class LabelRepository(
    private val dao: LabelDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeLabels(): Flow<List<LabelEntity>> = dao.observeLabels()
    fun observeLinks(): Flow<List<ProductLabel>> = dao.observeProductLabels()

    /** Returns the new label's id. */
    suspend fun create(name: String, color: Int): Long =
        dao.insertLabel(LabelEntity(name = name.trim(), color = color, createdAt = now()))

    suspend fun update(label: LabelEntity) = dao.updateLabel(label.copy(name = label.name.trim()))

    suspend fun delete(id: Long) = dao.deleteLabel(id)

    suspend fun setLabels(productIds: Collection<Long>, add: Collection<Long>, remove: Collection<Long>) =
        dao.setLabels(productIds, add, remove)
}
