package pl.rosstracker

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import pl.rosstracker.notify.Notifier
import pl.rosstracker.work.WorkScheduler

class RossTrackerApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Notifier.createChannel(this)
        WorkScheduler.ensureScheduled(this)
        ShareShortcut.publish(this)
    }

    /** Product photos fade in softly instead of popping. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).crossfade(400).build()
}
