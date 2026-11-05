package app.henrik.water

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Uses inexact (but doze-friendly) alarms, so no "exact alarm" permission is needed.
 * Reminders may fire a few minutes late, which is fine for water.
 */
object Scheduler {
    const val ACTION_REMIND = "app.henrik.water.REMIND"
    const val ACTION_MIDNIGHT = "app.henrik.water.MIDNIGHT"

    fun scheduleAll(c: Context) {
        scheduleNextReminder(c)
        scheduleMidnightRefresh(c)
    }

    private fun pending(c: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, requestCode,
            Intent(c, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun millis(t: LocalDateTime): Long =
        t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun scheduleNextReminder(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pending(c, ACTION_REMIND, 10)
        val times = Store.reminders(c)
        if (times.isEmpty()) {
            am.cancel(pi)
            return
        }
        val now = LocalDateTime.now()
        val nowMs = millis(now)
        val today = LocalDate.now()
        val next = (0L..1L).flatMap { dayOffset ->
            times.map { m -> millis(today.plusDays(dayOffset).atTime(m / 60, m % 60)) }
        }.filter { it > nowMs }.min()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
    }

    /** Refreshes the persistent notification just after midnight so the total resets to 0. */
    fun scheduleMidnightRefresh(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        val at = millis(LocalDate.now().plusDays(1).atTime(0, 1))
        am.setAndAllowWhileIdle(AlarmManager.RTC, at, pending(c, ACTION_MIDNIGHT, 11))
    }
}
