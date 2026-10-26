package pl.rosstracker.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pl.rosstracker.MainActivity
import pl.rosstracker.R
import pl.rosstracker.data.PriceEvent
import pl.rosstracker.data.db.ProductEntity
import pl.rosstracker.formatIsoDayMonth
import pl.rosstracker.formatPrice

object Notifier {
    private const val CHANNEL_ID = "promos"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_promos),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, product: ProductEntity, event: PriceEvent) {
        if (!canNotify(context)) return
        val (title, text) = when (event) {
            is PriceEvent.PromoStarted -> {
                val until = formatIsoDayMonth(event.promoTo)?.let { " do $it" } ?: ""
                "${event.label ?: "Promocja"} −${event.discountPercent}%" to
                    "${product.name}: ${formatPrice(event.price)} (było ${formatPrice(event.regularPrice)})$until"
            }
            is PriceEvent.PriceDropped ->
                "Cena spadła" to "${product.name}: ${formatPrice(event.from)} → ${formatPrice(event.to)}"
        }
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_PRODUCT_ID, product.id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            product.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(product.id.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}
