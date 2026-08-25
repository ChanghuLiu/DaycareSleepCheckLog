package com.daycare.sleepcheck.log.data

import android.content.Context

class ReminderPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) { preferences.edit().putBoolean(KEY_ENABLED, value).apply() }

    companion object {
        private const val NAME = "sleep_check_reminders"
        private const val KEY_ENABLED = "enabled"
    }
}
