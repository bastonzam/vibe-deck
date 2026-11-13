# Methods exposed to JS must keep their names
-keepclassmembers class app.vibedeck.GameBridge {
    @android.webkit.JavascriptInterface <methods>;
}
