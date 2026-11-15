package `in`.campusone.app.util

import android.content.Context
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import `in`.campusone.app.R`

/**
 * Debug-only startup probe.
 *
 * It deliberately does NOT change behaviour and never throws: every step is
 * isolated so the log always shows how far process start got. If the app dies
 * right after launch, the last line under this tag identifies the stage that
 * failed (font resources, typography, work scheduling).
 *
 * Logcat: `adb logcat -s CampusOneStartup`
 */
object StartupDiagnostics {

    const val TAG = "CampusOneStartup"

    fun run(context: Context) {
        runCatching { Log.i(TAG, "process start") }

        runCatching {
            val display = ResourcesCompat.getFont(context, R.font.manrope_family)
            Log.i(TAG, "font resource manrope_family loaded=${display != null}")
        }.onFailure {
            Log.e(TAG, "FONT FAILURE manrope_family: ${it.javaClass.name}: ${it.message}", it)
        }

        runCatching {
            val text = ResourcesCompat.getFont(context, R.font.inter_family)
            Log.i(TAG, "font resource inter_family loaded=${text != null}")
        }.onFailure {
            Log.e(TAG, "FONT FAILURE inter_family: ${it.javaClass.name}: ${it.message}", it)
        }

        runCatching { Log.i(TAG, "startup probe complete") }
    }
}
