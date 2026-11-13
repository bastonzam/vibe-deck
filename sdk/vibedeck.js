/*!
 * vibedeck.js v0.1 — SDK ไฟล์เดียวสำหรับเกมเว็บ
 * ใช้ได้ทั้งในแอพ VibeDeck และบนเบราว์เซอร์ปกติ (fallback เป็น Screen Wake Lock API)
 *
 *   <script src="vibedeck.js"></script>
 *   VibeDeck.keepScreenOn(true);
 *   VibeDeck.haptic('light');              // light | medium | heavy
 *   VibeDeck.setOrientation('landscape');  // portrait | landscape | auto
 */
(function (w) {
  var native = w.VibeDeckNative || null;
  var wantAwake = false;
  var lock = null;
  var safeAreaPadding = null;

  async function acquire() {
    if (!wantAwake || lock || !('wakeLock' in navigator)) return;
    try {
      lock = await navigator.wakeLock.request('screen');
      lock.addEventListener('release', function () { lock = null; });
    } catch (e) { /* ต้องมี user gesture / โหมดประหยัดแบต อาจปฏิเสธ */ }
  }

  if (!native) {
    // Wake Lock หลุดทุกครั้งที่แท็บถูกซ่อน → ขอใหม่ตอนกลับมา
    document.addEventListener('visibilitychange', function () {
      if (document.visibilityState === 'visible') acquire();
    });
    // บางเบราว์เซอร์ต้องมี gesture ก่อน → ลองใหม่ตอนแตะครั้งแรก
    ['pointerdown', 'keydown'].forEach(function (ev) {
      w.addEventListener(ev, function () { acquire(); }, { passive: true });
    });
  }

  w.VibeDeck = {
    isNative: !!native,
    keepScreenOn: function (on) {
      on = on !== false;
      if (native) return native.keepScreenOn(on);
      wantAwake = on;
      if (on) acquire();
      else if (lock) { lock.release(); lock = null; }
    },
    haptic: function (kind) {
      if (native) return native.haptic(kind || 'medium');
      if (navigator.vibrate) navigator.vibrate(kind === 'heavy' ? 30 : 10);
    },
    setOrientation: function (mode) {
      mode = mode || 'auto';
      if (!/^(landscape|portrait)(-primary|-secondary)?$|^auto$/.test(mode)) return;
      w.__vibeDeckOrientationRequested = true;
      if (native) return native.setOrientation(mode);
      if (mode === 'auto' && screen.orientation && screen.orientation.unlock) {
        screen.orientation.unlock();
      } else if (screen.orientation && screen.orientation.lock && mode !== 'auto') {
        screen.orientation.lock(mode).catch(function () {});
      }
    },
    setSafeArea: function (enabled) {
      enabled = enabled !== false;
      if (native && native.setSafeArea) return native.setSafeArea(enabled);
      var root = document.documentElement;
      var sides = ['top', 'right', 'bottom', 'left'];
      if (enabled && !safeAreaPadding) {
        safeAreaPadding = sides.map(function (side) {
          return [root.style.getPropertyValue('padding-' + side),
            root.style.getPropertyPriority('padding-' + side)];
        });
        sides.forEach(function (side) {
          root.style.setProperty('padding-' + side, 'env(safe-area-inset-' + side + ', 0px)');
        });
      } else if (!enabled && safeAreaPadding) {
        sides.forEach(function (side, index) {
          root.style.setProperty('padding-' + side, safeAreaPadding[index][0], safeAreaPadding[index][1]);
        });
        safeAreaPadding = null;
      }
    }
  };
})(window);
