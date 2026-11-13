package app.vibedeck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import app.vibedeck.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var store: RecentStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        store = RecentStore(this)

        setContent {
            HomeScreen(store = store, onPlay = ::play)
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val raw = intent?.data?.getQueryParameter("url") ?: return
        play(raw)
    }

    private fun play(input: String): Boolean {
        val url = UrlPolicy.normalize(input) ?: return false
        store.addRecent(url)
        startActivity(GameActivity.intent(this, url))
        return true
    }
}
