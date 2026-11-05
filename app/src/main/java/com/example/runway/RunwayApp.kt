package com.example.runway

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RunwayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug builds only (NFR-10). AGP 9 has BuildConfig off by default, so check the flag.
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) Timber.plant(Timber.DebugTree())
    }
}