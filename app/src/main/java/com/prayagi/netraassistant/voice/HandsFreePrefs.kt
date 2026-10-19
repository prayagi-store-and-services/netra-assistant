package com.prayagi.netraassistant.voice

import android.content.Context

/**
 * Whether hands-free listening should be on. Default ON: once mic permission is granted,
 * Netra One listens for the wake name by itself. The user's switch choice is remembered.
 */
class HandsFreePrefs(ctx: Context) {
    private val prefs = ctx.applicationContext.getSharedPreferences("netraone", Context.MODE_PRIVATE)

    fun enabled(): Boolean = prefs.getBoolean(KEY, true)

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
    }

    companion object {
        private const val KEY = "handsfree_enabled"
    }
}
