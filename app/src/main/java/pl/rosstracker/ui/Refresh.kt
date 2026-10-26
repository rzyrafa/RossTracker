package pl.rosstracker.ui

import android.content.Context
import pl.rosstracker.ServiceLocator
import pl.rosstracker.data.RefreshResult
import pl.rosstracker.notify.Notifier

/** Manual refresh from the UI; behaves like the background worker, notifications included. */
suspend fun refreshAllAndNotify(context: Context): RefreshResult {
    val result = ServiceLocator.repository.refreshAll()
    result.events.forEach { (product, events) ->
        events.forEach { Notifier.notify(context, product, it) }
    }
    return result
}
