package app.henrik.water

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import java.time.LocalDate

/** All persistent data lives in one SharedPreferences file. */
object Store {
    /** 5 reminders spread between 09:00 and 22:00 (minutes since midnight). */
    val DEFAULT_REMINDERS = listOf(9 * 60, 12 * 60 + 15, 15 * 60 + 30, 18 * 60 + 45, 22 * 60)

    /** Read this in a composable to get recomposed whenever any data changes. */
    val version = mutableIntStateOf(0)

    /** Amounts are stored in ml internally but shown in liters, e.g. 250 -> "0.25", 3000 -> "3". */
    fun liters(ml: Int): String =
        "%.2f".format(java.util.Locale.US, ml / 1000.0).trimEnd('0').trimEnd('.')

    /** Parses "0.25" or "0,25" (liters) into ml, or null if not a number. */
    fun parseLiters(s: String): Int? =
        s.replace(',', '.').toDoubleOrNull()?.let { Math.round(it * 1000).toInt() }

    private fun p(c: Context) =
        c.applicationContext.getSharedPreferences("water", Context.MODE_PRIVATE)

    private fun bump() {
        version.intValue = version.intValue + 1
    }

    fun glassMl(c: Context): Int = p(c).getInt("glass_ml", 250)

    fun setGlassMl(c: Context, ml: Int) {
        p(c).edit().putInt("glass_ml", ml).apply()
        bump()
    }

    /** Daily goal in ml (default 3 liters). */
    fun goalMl(c: Context): Int = p(c).getInt("goal_ml", 3000)

    fun setGoalMl(c: Context, ml: Int) {
        p(c).edit().putInt("goal_ml", ml).apply()
        bump()
    }

    fun intake(c: Context, d: LocalDate): Int = p(c).getInt("ml_$d", 0)

    fun addIntake(c: Context, d: LocalDate, deltaMl: Int) {
        val next = maxOf(0, intake(c, d) + deltaMl)
        p(c).edit().putInt("ml_$d", next).apply()
        bump()
    }

    fun movicol(c: Context, d: LocalDate): Int = p(c).getInt("mov_$d", 0)

    fun addMovicol(c: Context, d: LocalDate, delta: Int) {
        val next = maxOf(0, movicol(c, d) + delta)
        p(c).edit().putInt("mov_$d", next).apply()
        bump()
    }

    fun reminders(c: Context): List<Int> {
        val s = p(c).getString("reminders", null) ?: return DEFAULT_REMINDERS
        return s.split(",").mapNotNull { it.toIntOrNull() }.sorted()
    }

    fun setReminders(c: Context, minutes: List<Int>) {
        val s = minutes.distinct().sorted().joinToString(",")
        p(c).edit().putString("reminders", s).apply()
        bump()
    }
}
