package app.vibedeck

import android.content.Context

/** เก็บ Recent / Favorites ในเครื่อง (SharedPreferences) เฟส 2 ค่อยย้ายไป Room/Supabase */
class RecentStore(context: Context) {
    private val prefs = context.getSharedPreferences("games", Context.MODE_PRIVATE)

    fun recent(): List<String> = read(KEY_RECENT)
    fun favorites(): List<String> = read(KEY_FAV)

    fun addRecent(url: String) {
        val list = read(KEY_RECENT).toMutableList()
        list.remove(url)
        list.add(0, url)
        write(KEY_RECENT, list.take(MAX_RECENT))
    }

    fun toggleFavorite(url: String) {
        val list = read(KEY_FAV).toMutableList()
        if (!list.remove(url)) list.add(0, url)
        write(KEY_FAV, list)
    }

    private fun read(key: String): List<String> =
        prefs.getString(key, "")!!.split('\n').filter { it.isNotBlank() }

    private fun write(key: String, list: List<String>) =
        prefs.edit().putString(key, list.joinToString("\n")).apply()

    private companion object {
        const val KEY_RECENT = "recent"
        const val KEY_FAV = "favorites"
        const val MAX_RECENT = 30
    }
}
