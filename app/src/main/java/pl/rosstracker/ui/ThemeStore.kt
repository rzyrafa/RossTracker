package pl.rosstracker.ui

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("Systemowy"),
    LIGHT("Jasny"),
    DARK("Ciemny"),
}

/** The user's theme choice, persisted in SharedPreferences. */
class ThemeStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(
        prefs.getString(KEY, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
    )
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    init {
        applyToSystem(_mode.value)
    }

    fun set(mode: ThemeMode) {
        prefs.edit { putString(KEY, mode.name) }
        _mode.value = mode
        applyToSystem(mode)
    }

    /**
     * On Android 12+ tells the system our choice too, so the splash screen and the
     * window background follow the app setting instead of the system one.
     */
    private fun applyToSystem(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val night = when (mode) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(night)
    }

    private companion object {
        const val KEY = "theme_mode"
    }
}
