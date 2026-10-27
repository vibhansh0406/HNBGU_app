package com.campusone.app

import android.app.Application
import com.campusone.app.di.AppContainer
import com.campusone.app.util.CrashLogManager
import com.campusone.app.worker.LowAttendanceAlertWorker

class CampusOneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLogManager.install(this)
        container = AppContainer(this)
        LowAttendanceAlertWorker.scheduleDailyCheck(this)
    }
}
