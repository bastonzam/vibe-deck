(function () {
  if (window.__vibeDeckDisplayInstalled) return;
  window.__vibeDeckDisplayInstalled = true;
  var bridge = window.VibeDeckNative;
  if (!bridge || window.__vibeDeckOrientationRequested) return;
  var valid = /^(landscape|portrait)(-primary|-secondary)?$|^(auto|any|natural)$/;
  function apply(mode) {
    if (typeof mode !== 'string' || !valid.test(mode)) return false;
    if (!window.__vibeDeckOrientationRequested) bridge.setOrientation(mode);
    return true;
  }
  var meta = document.querySelector('meta[name="vibedeck-orientation"]');
  if (meta && apply(meta.content)) return;
  var manifest = document.querySelector('link[rel~="manifest"]');
  if (!manifest) return;
  var controller = new AbortController();
  var timeout = setTimeout(function () { controller.abort(); }, 5000);
  fetch(manifest.href, {
    credentials: manifest.crossOrigin === 'use-credentials' ? 'include' : 'same-origin',
    signal: controller.signal
  }).then(function (response) {
    if (!response.ok) throw new Error('Manifest unavailable');
    return response.json();
  }).then(function (data) { apply(data.orientation); })
    .catch(function () { /* Keep sensor rotation when metadata is unavailable. */ })
    .finally(function () { clearTimeout(timeout); });
})();
