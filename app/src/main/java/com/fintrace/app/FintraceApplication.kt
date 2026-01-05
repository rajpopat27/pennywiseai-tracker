package com.fintrace.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fintrace.app.worker.PendingTransactionAutoSaveWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FintraceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private var activityReferences = 0
    private var isInForeground = false

    /**
     * Publicly accessible flag to check if the app is in the foreground.
     * Used by SmsBroadcastReceiver to determine whether to show notifications.
     */
    @Volatile
    var isAppInForeground: Boolean = false
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ForegroundLifecycleObserver())
        schedulePendingTransactionAutoSaveWork()
    }

    /**
     * Schedule periodic work to auto-save expired pending transactions.
     * Runs every hour to check for transactions that have been pending for more than 24 hours.
     */
    private fun schedulePendingTransactionAutoSaveWork() {
        val autoSaveRequest = PeriodicWorkRequestBuilder<PendingTransactionAutoSaveWorker>(
            1, TimeUnit.HOURS
        )
            .addTag("pending_transaction_auto_save")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PendingTransactionAutoSaveWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            autoSaveRequest
        )
    }

    /**
     * Lifecycle observer to track app foreground/background state.
     * Used by SmsBroadcastReceiver to determine whether to show notifications.
     */
    private inner class ForegroundLifecycleObserver : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
            activityReferences++
            if (!isInForeground) {
                // App came to foreground
                isInForeground = true
                isAppInForeground = true
            }
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            activityReferences--
            if (activityReferences == 0) {
                // App went to background
                isInForeground = false
                isAppInForeground = false
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }
}
