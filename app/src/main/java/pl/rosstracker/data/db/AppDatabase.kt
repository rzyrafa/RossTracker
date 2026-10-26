package pl.rosstracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductEntity::class, PricePointEntity::class, LabelEntity::class, ProductLabel::class],
    version = 3,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun labelDao(): LabelDao

    companion object {
        /**
         * Every schema change gets a migration so updates never wipe anyone's price history.
         * Schemas are exported to `app/schemas/`.
         */
        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "rosstracker.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}

/** v1.3: product name split into brand / title / caption / unit for the new cards. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("brand", "title", "caption", "unit").forEach {
            db.execSQL("ALTER TABLE products ADD COLUMN $it TEXT")
        }
    }
}

/** v1.4: user labels. SQL copied from `schemas/.../3.json` so Room's schema check passes. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `labels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `color` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `product_labels` (`productId` INTEGER NOT NULL, `labelId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`productId`, `labelId`), " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`labelId`) REFERENCES `labels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_labels_labelId` ON `product_labels` (`labelId`)")
    }
}
