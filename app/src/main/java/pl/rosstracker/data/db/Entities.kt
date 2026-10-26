package pl.rosstracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    /** Rossmann product id taken from the product URL. */
    @PrimaryKey val id: Long,
    val url: String,
    val name: String,
    val imageUrl: String?,
    val price: Double,
    val oldPrice: Double?,
    val promoLabel: String?,
    val promoFrom: String?,
    val promoTo: String?,
    val lowestPrice30d: Double?,
    val available: Boolean,
    val addedAt: Long,
    val lastChecked: Long,
    val lastError: String? = null,
    // Added in DB version 2; empty for old rows until their next check.
    val brand: String? = null,
    val title: String? = null,
    val caption: String? = null,
    val unit: String? = null,
) {
    val isPromo: Boolean get() = oldPrice != null && oldPrice > price
}

@Entity(
    tableName = "price_points",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId")],
)
data class PricePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val timestamp: Long,
    val price: Double,
    val oldPrice: Double?,
)

/** User-made, Gmail-style label. [color] is an index into `LabelColors`, so night mode can use its own shade. */
@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val createdAt: Long,
)

/** A product can carry many labels; deleting either side removes the link. */
@Entity(
    tableName = "product_labels",
    primaryKeys = ["productId", "labelId"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("labelId")],
)
data class ProductLabel(
    val productId: Long,
    val labelId: Long,
)
