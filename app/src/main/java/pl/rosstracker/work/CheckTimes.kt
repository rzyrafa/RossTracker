package pl.rosstracker.work

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/** Times of day for the automatic price check. Rossmann promos usually start at midnight. */
val CHECK_TIMES: List<LocalTime> = listOf(LocalTime.of(7, 0), LocalTime.of(19, 0))

/** Next check slot strictly after [now] (with a small margin so we never reschedule into the current slot). */
fun nextCheckTime(now: ZonedDateTime, times: List<LocalTime> = CHECK_TIMES): ZonedDateTime {
    val earliest = now.plusMinutes(5)
    return (0..1L).asSequence()
        .flatMap { day -> times.asSequence().map { now.toLocalDate().plusDays(day).atTime(it).atZone(now.zone) } }
        .filter { it.isAfter(earliest) }
        .min()
}

fun delayUntilNextCheck(now: ZonedDateTime): Duration = Duration.between(now, nextCheckTime(now))

/** Background checks count as stalled when none has finished for this long (two missed slots plus slack). */
val STALL_THRESHOLD: Duration = Duration.ofHours(36)
