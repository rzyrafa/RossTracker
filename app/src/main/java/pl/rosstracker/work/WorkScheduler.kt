package pl.rosstracker.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
    /** Chain of one-off checks, each scheduling the next slot (7:00 / 19:00). */
    private const val SCHEDULED = "price-check-scheduled"
    private const val ONE_OFF = "price-check-now"

    /** Name used by v1.0's 12-hour periodic work; cancelled so it doesn't run alongside the new schedule. */
    private const val LEGACY_PERIODIC = "price-check"

    const val KEY_SCHEDULED = "scheduled"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Called on every app start: KEEP leaves an already pending check alone and restarts a broken chain. */
    fun ensureScheduled(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_PERIODIC)
        wm.enqueueUniqueWork(SCHEDULED, ExistingWorkPolicy.KEEP, nextScheduledRequest())
    }

    /**
     * Called by the worker when it finishes. APPEND_OR_REPLACE queues the next run behind the
     * one still running (REPLACE would cancel it mid-flight).
     */
    fun scheduleNext(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SCHEDULED, ExistingWorkPolicy.APPEND_OR_REPLACE, nextScheduledRequest())
    }

    /** Runs a check right away, with notifications (used by the debug button). Doesn't touch the schedule. */
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PriceCheckWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.REPLACE, request)
    }

    private fun nextScheduledRequest() =
        OneTimeWorkRequestBuilder<PriceCheckWorker>()
            .setInitialDelay(delayUntilNextCheck(ZonedDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_SCHEDULED to true))
            .build()
}
