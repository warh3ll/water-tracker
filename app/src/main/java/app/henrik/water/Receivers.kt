package app.henrik.water

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

/** Handles the "Add drink" button and re-posts the persistent notification if swiped away. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notifier.ACTION_ADD -> {
                Store.addIntake(context, LocalDate.now(), Store.glassMl(context))
                Notifier.cancelReminder(context)
                Notifier.updatePersistent(context)
            }
            Notifier.ACTION_DISMISSED -> Notifier.updatePersistent(context)
        }
    }
}

/** Fires for reminders and the midnight refresh. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Scheduler.ACTION_REMIND -> {
                Notifier.showReminder(context)
                Notifier.updatePersistent(context)
            }
            Scheduler.ACTION_MIDNIGHT -> Notifier.updatePersistent(context)
        }
        Scheduler.scheduleAll(context)
    }
}

/** Restores notification + alarms after reboot, app update, or clock/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifier.createChannels(context)
        Notifier.updatePersistent(context)
        Scheduler.scheduleAll(context)
    }
}
