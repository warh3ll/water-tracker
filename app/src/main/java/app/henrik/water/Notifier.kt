package app.henrik.water

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate

object Notifier {
    const val ACTION_ADD = "app.henrik.water.ADD_DRINK"
    const val ACTION_DISMISSED = "app.henrik.water.PERSISTENT_DISMISSED"

    private const val CH_PERSISTENT = "persistent"
    private const val CH_REMINDER = "reminder"
    private const val ID_PERSISTENT = 1
    private const val ID_REMINDER = 2

    fun createChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PERSISTENT, "Water tracker", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Always-visible notification with an Add drink button"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders to drink water"
            }
        )
    }

    fun canPost(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 0, Intent(c, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun broadcast(c: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, requestCode,
            Intent(c, ActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun summary(c: Context): Pair<Int, String> {
        val ml = Store.intake(c, LocalDate.now())
        val glass = Store.glassMl(c)
        return ml to "%.1f glasses (1 glass = %d ml)".format(ml / glass.toFloat(), glass)
    }

    /** (Re)posts the ongoing notification showing today's total. */
    @SuppressLint("MissingPermission")
    fun updatePersistent(c: Context) {
        if (!canPost(c)) return
        val (ml, text) = summary(c)
        val n = NotificationCompat.Builder(c, CH_PERSISTENT)
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle("$ml ml today")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openApp(c))
            .setDeleteIntent(broadcast(c, ACTION_DISMISSED, 2))
            .addAction(0, "Add drink", broadcast(c, ACTION_ADD, 1))
            .build()
        NotificationManagerCompat.from(c).notify(ID_PERSISTENT, n)
    }

    @SuppressLint("MissingPermission")
    fun showReminder(c: Context) {
        if (!canPost(c)) return
        val (ml, _) = summary(c)
        val n = NotificationCompat.Builder(c, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle("Time to drink water")
            .setContentText("$ml ml so far today")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(c))
            .addAction(0, "Add drink", broadcast(c, ACTION_ADD, 1))
            .build()
        NotificationManagerCompat.from(c).notify(ID_REMINDER, n)
    }

    fun cancelReminder(c: Context) {
        NotificationManagerCompat.from(c).cancel(ID_REMINDER)
    }
}
