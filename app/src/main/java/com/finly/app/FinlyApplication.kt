package com.finly.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Custom Application class.
 *
 * Dark mode is applied HERE — in Application.onCreate() — before any Activity
 * is created. This is the only correct place to call setDefaultNightMode() on
 * app startup.
 *
 * Why not in MainActivity.onCreate()?
 *   If setDefaultNightMode() changes the current mode, Android immediately
 *   recreates the Activity. Because onSaveInstanceState was never called
 *   (Activity was never visible), savedInstanceState is null on recreation —
 *   which causes the splash screen to be skipped entirely.
 *
 * Migration rule (matches SettingsRepository.isSetupComplete()):
 *   • is_setup_complete key exists → use it
 *   • key missing (old install) + is_first_run = false → user did setup before key was
 *     introduced → treat as setup complete, respect saved dark mode pref
 *   • key missing + is_first_run = true (or missing) → fresh install → force light mode
 */
class FinlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("finly_prefs", Context.MODE_PRIVATE)

        val isSetupComplete = prefs.getBoolean("is_setup_complete", false)

        // Only apply the user's dark mode pref if they have actually completed setup.
        // On fresh installs and after "Clear All Data", always start in light mode.
        val isDark = isSetupComplete && prefs.getBoolean("dark_mode_enabled", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
