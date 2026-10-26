package pl.rosstracker

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * A sharing shortcut puts RossTracker in the top "direct share" row of share sheets
 * (Chrome, the Rossmann app), instead of somewhere down the full app list.
 * Shares through it arrive at MainActivity as a normal ACTION_SEND.
 */
object ShareShortcut {
    private const val ID = "track_product"
    private const val CATEGORY = "pl.rosstracker.category.SHARE_TARGET" // must match res/xml/shortcuts.xml

    fun publish(context: Context) {
        val shortcut = ShortcutInfoCompat.Builder(context, ID)
            .setShortLabel("Śledź cenę")
            .setLongLabel("Śledź cenę w RossTracker")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .setCategories(setOf(CATEGORY))
            .setLongLived(true)
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
    }
}
