package com.kaasu.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KaasuApp : Application(), Configuration.Provider {

    // Lets WorkManager create SmsBackfillWorker (a @HiltWorker) with its Hilt-injected
    // dependencies. Requires the default WorkManagerInitializer to be removed from the manifest
    // (see AndroidManifest.xml) since a custom Configuration.Provider disables manifest auto-init.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
