package app.vibedeck

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface

/**
 * JS bridge: เกมเรียกผ่าน window.VibeDeckNative (แนะนำให้เรียกผ่าน sdk/vibedeck.js)
 *
 * เฟส 1: เปิดเฉพาะความสามารถความเสี่ยงต่ำ (จอ, สั่น, orientation)
 * เฟส 2: เมื่อมี "Verified developer" ให้ gate method ที่แรงกว่านี้ (storage, share ฯลฯ)
 *
 * หมายเหตุ: @JavascriptInterface ถูก inject ให้ทุก frame (รวม iframe) และเช็ก origin ไม่ได้
 * ถ้าจะเพิ่มสิทธิ์ที่เสี่ยงขึ้น ให้ย้ายไปใช้ WebViewCompat.addWebMessageListener + allowedOriginRules
 */
class GameBridge(
    private val activity: GameActivity,
    private val anchor: View,
) {
    @JavascriptInterface
    fun version(): Int = 2

    @JavascriptInterface
    fun keepScreenOn(on: Boolean) {
        activity.runOnUiThread {
            if (on) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @JavascriptInterface
    fun haptic(kind: String) {
        activity.runOnUiThread {
            val type = when (kind) {
                "heavy" -> HapticFeedbackConstants.LONG_PRESS
                "light" -> HapticFeedbackConstants.KEYBOARD_TAP
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
            anchor.performHapticFeedback(type)
        }
    }

    @JavascriptInterface
    fun setOrientation(mode: String) {
        activity.runOnUiThread {
            activity.applyOrientation(mode)
        }
    }

    @JavascriptInterface
    fun setSafeArea(enabled: Boolean) {
        activity.runOnUiThread { activity.setSafeAreaEnabled(enabled) }
    }
}
