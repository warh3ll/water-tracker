package app.henrik.water

import android.app.Application

class WaterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        Notifier.updatePersistent(this)
        Scheduler.scheduleAll(this)
    }
}
