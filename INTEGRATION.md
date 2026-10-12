# Native Android WebView integration reference

`BrowserWebViewPlugin.kt` is a native WebView host reference, not a finished Tauri plugin. It demonstrates a browser toolbar and Android System WebView rendering with basic HTTP(S)-only top-level navigation.

It is **not wired into Tauri's generated Android Activity**. Tauri's mobile host owns its own WebView, so an actual in-app browser needs deliberate native integration: view layering/layout, lifecycle, Android back handling, Tauri v2 plugin registration, and frontend/Rust commands. Do not treat this reference file alone as an installable browser.

## Required integration and validation
1. Generate Android project with `npm run tauri android init`.
2. Add the host/plugin code in the generated Android module and wire it into the Activity layout/lifecycle.
3. Add Tauri v2 plugin command bindings for `loadUrl`, `goBack`, `goForward`, and `reload`.
4. Confirm the WebView view occupies the content area and is not hidden behind Tauri's frontend WebView.
5. Build with Android SDK/NDK and test on a physical device.
6. Test modern sites, legacy sites, file downloads, pop-ups, permissions, keyboard/IME, Android back, SSL errors, and external schemes.

Never bypass TLS certificate errors. A website may still refuse to load in WebView due to DRM, browser checks, unsupported APIs, or site policy.
