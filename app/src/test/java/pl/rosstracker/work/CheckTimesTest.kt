package pl.rosstracker.work

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class CheckTimesTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test
    fun picksMorningSlotAfterMidnight() = assertEquals(at(24, 7), nextCheckTime(at(24, 2)))

    @Test
    fun picksEveningSlotDuringTheDay() = assertEquals(at(24, 19), nextCheckTime(at(24, 7, 30)))

    @Test
    fun rollsOverToNextMorning() = assertEquals(at(25, 7), nextCheckTime(at(24, 21)))

    @Test
    fun skipsSlotThatIsAboutToStart() = assertEquals(at(24, 19), nextCheckTime(at(24, 6, 58)))

    @Test
    fun handlesDstChange() {
        // Poland leaves summer time on 2026-10-25; 7:00 local still means 7:00 local.
        assertEquals(at(25, 7), nextCheckTime(at(24, 20)))
    }
}
