package pl.rosstracker

import android.content.Context
import pl.rosstracker.data.LabelRepository
import pl.rosstracker.data.ProductRepository
import pl.rosstracker.data.db.AppDatabase
import pl.rosstracker.data.net.RossmannClient
import pl.rosstracker.ui.ThemeStore
import pl.rosstracker.work.AutoCheckStore

/** Hand-rolled dependency container; the app is small enough not to need Hilt. */
object ServiceLocator {
    lateinit var repository: ProductRepository
        private set
    lateinit var labels: LabelRepository
        private set
    lateinit var autoChecks: AutoCheckStore
        private set
    lateinit var theme: ThemeStore
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        val db = AppDatabase.create(app)
        repository = ProductRepository(db.productDao(), RossmannClient())
        labels = LabelRepository(db.labelDao())
        autoChecks = AutoCheckStore(app)
        theme = ThemeStore(app)
    }
}
