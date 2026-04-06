package com.finly.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("finly_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_MONTHLY_INCOME = "monthly_income"
        private const val KEY_CURRENCY = "selected_currency"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        
        private const val KEY_NOTIF_PUSH = "notif_push"
        private const val KEY_NOTIF_EMAIL = "notif_email"
        private const val KEY_NOTIF_BUDGET = "notif_budget"
        private const val KEY_NOTIF_GOAL = "notif_goal"
        private const val KEY_NOTIF_REPORTS = "notif_reports"
        private const val KEY_IS_FIRST_RUN = "is_first_run"
        private const val KEY_PROFILE_IMAGE = "profile_image_uri"
        // Dedicated flag: set to true only when user completes the setup wizard.
        // More reliable than KEY_IS_FIRST_RUN which was tied to seedData().
        private const val KEY_SETUP_COMPLETE = "is_setup_complete"
        private const val KEY_SAMPLE_DATA_ACTIVE = "sample_data_active"
    }

    fun getProfileImageUri(): String? = prefs.getString(KEY_PROFILE_IMAGE, null)
    fun saveProfileImageUri(uri: String) = prefs.edit().putString(KEY_PROFILE_IMAGE, uri).apply()

    // Setup completion: true only after user presses "Start tracking" in Setup Step 2.
    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }
    fun setSetupComplete(done: Boolean) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, done).apply()

    fun isFirstRun(): Boolean = prefs.getBoolean(KEY_IS_FIRST_RUN, true)
    fun setFirstRun(isFirst: Boolean) = prefs.edit().putBoolean(KEY_IS_FIRST_RUN, isFirst).apply()

    fun hasSampleData(): Boolean = prefs.getBoolean(KEY_SAMPLE_DATA_ACTIVE, false)
    fun setSampleData(active: Boolean) = prefs.edit().putBoolean(KEY_SAMPLE_DATA_ACTIVE, active).apply()

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "Kbalu") ?: "Kbalu"
    fun saveUserName(name: String) = prefs.edit().putString(KEY_USER_NAME, name).apply()

    fun getMonthlyIncome(): Double = prefs.getFloat(KEY_MONTHLY_INCOME, 544000f).toDouble()
    fun saveMonthlyIncome(income: Double) = prefs.edit().putFloat(KEY_MONTHLY_INCOME, income.toFloat()).apply()

    fun getCurrency(): String = prefs.getString(KEY_CURRENCY, "₹ INR") ?: "₹ INR"
    fun saveCurrency(currency: String) = prefs.edit().putString(KEY_CURRENCY, currency).apply()

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkModeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)
    fun setNotificationsEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)
    fun setBiometricEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()

    // Specific Notification Toggles (Image 5)
    fun getNotifPush(): Boolean = prefs.getBoolean(KEY_NOTIF_PUSH, true)
    fun setNotifPush(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIF_PUSH, enabled).apply()

    fun getNotifEmail(): Boolean = prefs.getBoolean(KEY_NOTIF_EMAIL, false)
    fun setNotifEmail(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIF_EMAIL, enabled).apply()

    fun getNotifBudget(): Boolean = prefs.getBoolean(KEY_NOTIF_BUDGET, true)
    fun setNotifBudget(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIF_BUDGET, enabled).apply()

    fun getNotifGoal(): Boolean = prefs.getBoolean(KEY_NOTIF_GOAL, true)
    fun setNotifGoal(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIF_GOAL, enabled).apply()

    fun getNotifReports(): Boolean = prefs.getBoolean(KEY_NOTIF_REPORTS, false)
    fun setNotifReports(enabled: Boolean) = prefs.edit().putBoolean(KEY_NOTIF_REPORTS, enabled).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
