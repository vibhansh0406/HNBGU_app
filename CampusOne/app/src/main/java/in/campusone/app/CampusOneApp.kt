package `in`.campusone.app

import android.app.Application
import `in`.campusone.app.di.AppContainer
import `in`.campusone.app.util.CrashLogManager

class CampusOneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashLogManager.install(this)
        container = AppContainer(this)
    }
}
