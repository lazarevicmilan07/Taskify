package com.taskify.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.taskify.app.worker.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class — entry point for Hilt dependency graph.
 *
 * Implements Configuration.Provider to give WorkManager access to Hilt-injected
 * workers. Without this, WorkManager would use the default factory and @Inject
 * in Worker subclasses would fail silently.
 */
@HiltAndroidApp
class TaskifyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
