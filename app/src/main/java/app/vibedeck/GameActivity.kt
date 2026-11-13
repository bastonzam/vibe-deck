package app.vibedeck

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** หน้าเล่นเกม: WebView เต็มจอ + กันจอดับอัตโนมัติ (ไม่ต้องรอให้เกมเรียก bridge) */
class GameActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var container: FrameLayout? = null
    private lateinit var displaySettings: DisplaySettings
    private var safeAreaEnabled = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displaySettings = DisplaySettings(this)
        safeAreaEnabled = displaySettings.safeArea
        applyOrientation("auto")

        val url = intent.getStringExtra(EXTRA_URL)
        if (url == null || !UrlPolicy.isAllowedNavigation(url)) {
            finish()
            return
        }

        // 1) กันจอดับเป็นค่าเริ่มต้น เกมปิดเองได้ผ่าน bridge
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2) Immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val wv = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(Color.BLACK)
            overScrollMode = WebView.OVER_SCROLL_NEVER
            isLongClickable = false
            setOnLongClickListener { true } // กันเมนู long-press รบกวนเกม

            settings.apply {
                javaScriptEnabled = true
                // Keep the normal app-wide WebView profile: localStorage / IndexedDB and
                // cookies survive closing a game, for both accounts and guest saves.
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                setSupportZoom(false)
                builtInZoomControls = false
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            addJavascriptInterface(GameBridge(this@GameActivity, this), "VibeDeckNative")

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    applyOrientation("auto")
                    setSafeAreaEnabled(displaySettings.safeArea)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    CookieManager.getInstance().flush()
                    // Runs in the main document. Explicit SDK requests take precedence over metadata.
                    view.evaluateJavascript(assets.open("display.js").bufferedReader().use { it.readText() }, null)
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    !UrlPolicy.isAllowedNavigation(request.url.toString())

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    // renderer crash/ถูกระบบฆ่า: สร้าง Activity ใหม่ แทนที่จะให้แอพล้มทั้งตัว
                    recreate()
                    return true
                }
            }
            webChromeClient = WebChromeClient()
        }

        webView = wv
        val frame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(wv)
        }
        container = frame
        ViewCompat.setOnApplyWindowInsetsListener(frame) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.mandatorySystemGestures()
            )
            view.setPadding(
                if (safeAreaEnabled) safe.left else 0,
                if (safeAreaEnabled) safe.top else 0,
                if (safeAreaEnabled) safe.right else 0,
                if (safeAreaEnabled) safe.bottom else 0,
            )
            // The WebView viewport is already safe; don't apply the same insets a second time.
            WindowInsetsCompat.CONSUMED
        }
        setContentView(frame)
        ViewCompat.requestApplyInsets(frame)
        val restored = savedInstanceState?.getBundle(STATE_WEB_VIEW)?.let { wv.restoreState(it) }
        if (restored == null) wv.loadUrl(url)

        onBackPressedDispatcher.addCallback(this) {
            if (wv.canGoBack()) wv.goBack() else finish()
        }
    }

    fun setSafeAreaEnabled(enabled: Boolean) {
        safeAreaEnabled = enabled
        container?.let { ViewCompat.requestApplyInsets(it) }
    }

    fun applyOrientation(mode: String) {
        val selected = displaySettings.orientation.takeUnless { it == "auto" } ?: mode
        requestedOrientation = when (selected) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "landscape-primary" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "landscape-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "portrait-primary" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "portrait-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "natural" -> ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        container?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
    }

    override fun onPause() {
        webView?.onPause()
        // Persist login/guest cookies when leaving the game or backgrounding the app.
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView?.let { view ->
            val state = Bundle()
            view.saveState(state)
            outState.putBundle(STATE_WEB_VIEW, state)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView?.apply {
            removeJavascriptInterface("VibeDeckNative")
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val STATE_WEB_VIEW = "webViewState"
        fun intent(context: Context, url: String) =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_URL, url)
    }
}
