# Still Browser — pre-build QA report

Date: 2026-10-12

## Checks completed in this environment

- `src/main.js` passes `node --check` (JavaScript syntax check).
- `package.json`, `src-tauri/tauri.conf.json`, and `src-tauri/capabilities/default.json` parse as valid JSON.
- The custom plugin `Cargo.toml` parses as valid TOML.
- Added a local SVG favicon and app icon assets; configured Tauri bundle icon paths.
- Connected the home-screen Back, Forward, Reload, and brand-home controls to the intended native plugin commands, with user-facing error toasts if the bridge is unavailable.
- Added site favicon images to the three launch cards, retaining letter fallbacks if remote icons fail to load.

## Known issues / not verified

- `npm install --ignore-scripts --no-audit --no-fund` did not complete before the environment command timed out, so a Vite production build could not be run here.
- No Android SDK/NDK, Gradle, Java toolchain, or Rust/Cargo compiler is available in this review environment; no APK was compiled.
- The Tauri Android plugin and its Kotlin bridge have not been compiled or run on a physical device. Their API/build integration remains the critical release blocker.
- Native WebView browsing, back/forward history, keyboard/IME, downloads, popups, permission prompts, Android system-back behavior, TLS failure behavior, memory usage, and old-device compatibility have not been device-tested.
- The settings dialog remains informational; it does not yet persist or change browser preferences.
- This is a single-view browser prototype, not a multi-tab browser. Website favicon cards are implemented, but current-page favicon display in the native browsing toolbar is not yet implemented.
- External favicon requests require a network connection. If a site icon fails, the card falls back to its letter.

## Release decision

**Not release-ready yet.** The front-end source received a limited static QA pass, but an APK build and real-device test are required before claiming the browser works end-to-end.
