package app.vibedeck

import android.content.Context

/** Shared by normal launches, recent games and deep links. */
class DisplaySettings(context: Context) {
    private val preferences = context.getSharedPreferences("display", Context.MODE_PRIVATE)

    var safeArea: Boolean
        get() = preferences.getBoolean("safeArea", true)
        set(value) { preferences.edit().putBoolean("safeArea", value).apply() }

    var orientation: String
        get() = preferences.getString("orientation", "auto") ?: "auto"
        set(value) { preferences.edit().putString("orientation", value).apply() }
}
