package pl.rosstracker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import pl.rosstracker.ServiceLocator
import java.io.File

private const val TAG = "RossImport"

private const val EXTRA_URLS = "urls"

/**
 * Stress-test hook (debug and staging builds only). Pass space-separated product links:
 * `adb shell am broadcast -a pl.rosstracker.DEBUG_IMPORT -p <package> --es urls "<url> <url> …"`
 * or, without the extra, put them one per line into `files/import.txt`.
 * Progress goes to logcat under the `RossImport` tag.
 */
class DebugImportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val urls = intent.getStringExtra(EXTRA_URLS).orEmpty()
        val request = OneTimeWorkRequestBuilder<DebugImportWorker>()
            .setInputData(workDataOf(EXTRA_URLS to urls))
            .build()
        WorkManager.getInstance(context).enqueue(request)
        Log.i(TAG, "import queued")
    }
}

class DebugImportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val inline = inputData.getString(EXTRA_URLS).orEmpty().split(' ').filter { it.isNotBlank() }
        val urls = inline.ifEmpty {
            File(applicationContext.filesDir, "import.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
        }
        val repo = ServiceLocator.repository
        val start = System.currentTimeMillis()
        var ok = 0
        urls.forEachIndexed { index, url ->
            if (index > 0) delay(1_000)
            val t = System.currentTimeMillis()
            runCatching { repo.track(repo.preview(url)) }
                .onSuccess { ok++; Log.i(TAG, "${index + 1}/${urls.size} ok ${System.currentTimeMillis() - t} ms") }
                .onFailure { Log.w(TAG, "${index + 1}/${urls.size} FAILED $url: $it") }
        }
        Log.i(TAG, "done ok=$ok/${urls.size} total=${System.currentTimeMillis() - start} ms")
        return Result.success()
    }
}
