# StillSpace Browser — spatial UI + native Android WebView

## What is in this archive
- Spatial-inspired responsive home UI (glass surfaces, soft ambient depth, orbit launch cards)
- Rust + Tauri 2 app shell
- A Tauri mobile plugin project for an Android native WebView overlay
- Native browser controls: URL/search field, home, back, forward, reload, progress
- Local app/favicon assets plus remote launch-card favicons with letter fallbacks
- Home-screen controls wired to the native plugin command names with visible error handling
- Pre-build audit: see `QA_REPORT.md` for verified checks and unresolved blockers
- HTTP/HTTPS-only URL handling, mixed content blocked, file/content access disabled, no TLS-error bypass
- Build instructions and test checklist

## Important build status
**No APK is included.** The front-end received a limited static QA pass, but the dependency install timed out and this environment lacks the Android SDK/NDK and Rust/Cargo toolchain. The native plugin packaging and Android integration still need to be compiled and tested in a real Tauri Android build. See `QA_REPORT.md`.

## How it is designed
Tauri's Android UI is itself rendered by the system Android WebView. To keep the spatial home UI while showing arbitrary websites, the native plugin adds a separate Android `WebView` overlay and keeps it hidden until a destination is opened. This avoids iframe embedding, which many sites prohibit.

Tauri's Android WebView uses the device's installed WebView provider, so modern/legacy compatibility depends on the Android System WebView/Chromium version on the phone.

## Build prerequisites
- Node.js 20+
- Rust stable + Cargo
- Java 17 or 21
- Tauri CLI v2
- Android SDK, platform tools, NDK
- `ANDROID_HOME` and `NDK_HOME` configured

## Build workflow
1. Extract this ZIP.
2. Install npm packages:
   ```sh
   npm install
   cargo install tauri-cli --version "^2"
   ```
3. Initialize the Tauri Android project:
   ```sh
   npm run tauri android init
   ```
4. The local plugin build script links its `android/` module into the generated Android project. Confirm that the plugin appears in Gradle configuration after the first build.
5. Build:
   ```sh
   npm run build
   npm run tauri android build -- --apk
   ```
6. Install on a physical Android device and test the checklist below. The CLI reports the APK output path, usually within `src-tauri/gen/android/app/build/outputs/apk/`.

## Test checklist
- Open HTTPS and HTTP pages
- Search for plain text
- Back / forward / reload / home
- Test a current web app and a legacy HTML page
- Keyboard and IME behavior
- SSL certificate errors must fail safely
- Non-HTTP(S) schemes must not load as web pages
- Memory and responsiveness on low-RAM devices
- Android system back button, download handling, popups, permissions, file uploads, and external schemes need additional integration/testing before calling this production-ready

## Security and compatibility notes
- JavaScript is enabled because modern websites require it.
- Mixed content is blocked by default.
- Local file/content access is disabled.
- TLS certificate validation is not bypassed.
- No browser engine can guarantee that every legacy or modern site works; site policy, DRM, API support and WebView version can affect behavior.
