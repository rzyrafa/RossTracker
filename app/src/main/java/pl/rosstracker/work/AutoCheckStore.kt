package pl.rosstracker.work

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Remembers when the last scheduled background check succeeded, so the UI can spot a killed worker. */
class AutoCheckStore(context: Context) {
    private val prefs = context.getSharedPreferences("auto_checks", Context.MODE_PRIVATE)
    private val _lastChecked = MutableStateFlow(prefs.getLong(KEY, 0L).takeIf { it > 0 })
    val lastChecked: StateFlow<Long?> = _lastChecked.asStateFlow()

    fun markChecked(time: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY, time) }
        _lastChecked.value = time
    }

    private companion object {
        const val KEY = "last_scheduled_check"
    }
}
