package pl.rosstracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pl.rosstracker.ServiceLocator
import pl.rosstracker.notify.Notifier

class PriceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val scheduled get() = inputData.getBoolean(WorkScheduler.KEY_SCHEDULED, false)

    override suspend fun doWork(): Result {
        val result = try {
            ServiceLocator.repository.refreshAll()
        } catch (e: Exception) {
            // Never let an unexpected error break the 7:00 / 19:00 chain.
            if (scheduled) WorkScheduler.scheduleNext(applicationContext)
            return Result.failure()
        }
        result.events.forEach { (product, events) ->
            events.forEach { Notifier.notify(applicationContext, product, it) }
        }
        // Retry only when nothing could be reached at all (e.g. flaky connection), and not forever.
        val allFailed = result.checked > 0 && result.networkFailures == result.checked
        if (allFailed && runAttemptCount < 3) return Result.retry()

        if (scheduled) {
            if (!allFailed) ServiceLocator.autoChecks.markChecked()
            WorkScheduler.scheduleNext(applicationContext)
        }
        return Result.success()
    }
}
