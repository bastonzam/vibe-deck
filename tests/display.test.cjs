const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { test } = require('node:test');

const sdk = fs.readFileSync('sdk/vibedeck.js', 'utf8');
const detection = fs.readFileSync('app/src/main/assets/display.js', 'utf8');

function page({ meta, manifest, native = true, fetch } = {}) {
  const orientations = [];
  const safeAreas = [];
  const styles = new Map();
  const context = vm.createContext({
    navigator: {}, screen: {}, AbortController, setTimeout, clearTimeout,
    fetch: fetch || (() => Promise.resolve({ ok: true, json: async () => manifest })),
    document: {
      addEventListener() {},
      querySelector(selector) {
        if (selector.startsWith('meta')) return meta ? { content: meta } : null;
        return manifest ? { href: 'https://game.example/manifest.json' } : null;
      },
      documentElement: { style: {
        getPropertyValue: key => styles.get(key)?.[0] || '',
        getPropertyPriority: key => styles.get(key)?.[1] || '',
        setProperty: (key, value, priority = '') => styles.set(key, [value, priority]),
      } },
    },
    addEventListener() {},
    VibeDeckNative: native ? {
      setOrientation: value => orientations.push(value),
      setSafeArea: value => safeAreas.push(value),
    } : undefined,
  });
  context.window = context;
  return { context, orientations, safeAreas, styles };
}

const flush = () => new Promise(resolve => setImmediate(resolve));

test('landscape metadata wins over manifest and detection runs once', async () => {
  const p = page({ meta: 'landscape', manifest: { orientation: 'portrait' } });
  vm.runInContext(detection, p.context);
  vm.runInContext(detection, p.context);
  await flush();
  assert.deepEqual(p.orientations, ['landscape']);
});

test('manifest orientation is applied, including primary/secondary', async () => {
  const p = page({ manifest: { orientation: 'landscape-secondary' } });
  vm.runInContext(detection, p.context);
  await flush();
  assert.deepEqual(p.orientations, ['landscape-secondary']);
});

test('missing, invalid or unreachable metadata keeps sensor fallback', async () => {
  for (const options of [ {}, { meta: 'invalid' },
    { manifest: { orientation: 'invalid' } },
    { manifest: {}, fetch: () => Promise.reject(new Error('offline')) } ]) {
    const p = page(options);
    vm.runInContext(detection, p.context);
    await flush();
    assert.deepEqual(p.orientations, []);
  }
});

test('SDK request before page finish takes priority over metadata', () => {
  const p = page({ meta: 'portrait' });
  vm.runInContext(sdk, p.context);
  p.context.VibeDeck.setOrientation('landscape');
  vm.runInContext(detection, p.context);
  assert.deepEqual(p.orientations, ['landscape']);
});

test('delayed manifest cannot override a newer SDK request', async () => {
  let resolve;
  const pending = new Promise(done => { resolve = done; });
  const p = page({ manifest: {}, fetch: () => pending });
  vm.runInContext(sdk, p.context);
  vm.runInContext(detection, p.context);
  p.context.VibeDeck.setOrientation('portrait');
  resolve({ ok: true, json: async () => ({ orientation: 'landscape' }) });
  await flush();
  assert.deepEqual(p.orientations, ['portrait']);
});

test('native safe area delegates without adding browser padding', () => {
  const p = page();
  vm.runInContext(sdk, p.context);
  p.context.VibeDeck.setSafeArea(true);
  p.context.VibeDeck.setSafeArea(false);
  assert.deepEqual(p.safeAreas, [true, false]);
  assert.equal(p.styles.size, 0);
});

test('browser safe area restores existing padding and auto unlocks orientation', () => {
  const p = page({ native: false });
  let unlocks = 0;
  p.context.screen.orientation = { unlock: () => unlocks++ };
  p.styles.set('padding-top', ['12px', 'important']);
  vm.runInContext(sdk, p.context);
  p.context.VibeDeck.setSafeArea(true);
  p.context.VibeDeck.setSafeArea(true);
  assert.equal(p.styles.get('padding-top')[0], 'env(safe-area-inset-top, 0px)');
  p.context.VibeDeck.setSafeArea(false);
  assert.deepEqual(p.styles.get('padding-top'), ['12px', 'important']);
  p.context.VibeDeck.setOrientation('auto');
  assert.equal(unlocks, 1);
});
