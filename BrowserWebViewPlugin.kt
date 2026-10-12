/*
 * Reference native bridge for the requested in-app WebView.
 *
 * This is NOT wired into the generated Tauri Android project automatically.
 * Integrate it as a Tauri v2 Android plugin and register it in the Android host
 * before treating this project as a working in-app browser.
 */
package com.stillbrowser.app

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.EditText
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.text.InputType
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager

/**
 * Activity-side implementation reference.
 *
 * Security defaults:
 * - HTTPS/HTTP only for top-level navigation
 * - no certificate-error bypass
 * - JavaScript enabled for compatibility (standard browser behavior)
 * - mixed content blocked
 * - Safe Browsing enabled where supported
 * - external schemes are not loaded as web pages
 *
 * A production Tauri v2 plugin should expose load/back/forward/reload methods
 * to Rust/frontend, and must coordinate view sizing with Tauri's Android WebView.
 */
class BrowserWebViewHost(private val activity: Activity) {
    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private var created = false

    fun create(container: ViewGroup) {
        if (created) return
        created = true
        root = FrameLayout(activity)
        root.setBackgroundColor(Color.rgb(16, 19, 27))

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(16, 19, 27))
        }

        val toolbar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(25, 30, 41))
        }
        val back = button("‹")
        val forward = button("›")
        val reload = button("↻")
        address = EditText(activity).apply {
            hint = "Search or enter website"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(17, 21, 30))
            setPadding(dp(10), dp(4), dp(10), dp(4))
            textSize = 14f
        }
        toolbar.addView(back, LinearLayout.LayoutParams(dp(38), dp(42)))
        toolbar.addView(forward, LinearLayout.LayoutParams(dp(38), dp(42)))
        toolbar.addView(address, LinearLayout.LayoutParams(0, dp(42), 1f))
        toolbar.addView(reload, LinearLayout.LayoutParams(dp(42), dp(42)))

        progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(199, 242, 124))
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(48, 56, 73))
            visibility = View.GONE
        }
        web = WebView(activity).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) activity.title = title
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme?.lowercase()
                    return scheme != "http" && scheme != "https"
                }
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                    back.isEnabled = this@BrowserWebViewHost::web.isInitialized && web.canGoBack()
                    forward.isEnabled = this@BrowserWebViewHost::web.isInitialized && web.canGoForward()
                }
            }
        }
        column.addView(toolbar)
        column.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        column.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(column)
        container.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        back.setOnClickListener { if (web.canGoBack()) web.goBack() }
        forward.setOnClickListener { if (web.canGoForward()) web.goForward() }
        reload.setOnClickListener { web.reload() }
        address.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                loadInput(address.text.toString())
                true
            } else false
        }
        web.loadUrl("https://www.google.com/")
    }

    fun loadUrl(url: String) { if (::web.isInitialized) web.loadUrl(normalize(url)) }
    fun goBack() { if (::web.isInitialized && web.canGoBack()) web.goBack() }
    fun goForward() { if (::web.isInitialized && web.canGoForward()) web.goForward() }
    fun reload() { if (::web.isInitialized) web.reload() }
    fun destroy() { if (::web.isInitialized) { web.stopLoading(); web.destroy() } }

    private fun loadInput(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val target = if (value.startsWith("https://") || value.startsWith("http://")) value
        else if (value.matches(Regex("""^[\w.-]+\.[A-Za-z]{2,}(:\d+)?([/?#].*)?$""")) && !value.contains(" "))
            "https://$value"
        else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(value, "UTF-8")
        loadUrl(target)
    }

    private fun normalize(value: String): String {
        val uri = android.net.Uri.parse(value)
        require(uri.scheme == "http" || uri.scheme == "https") { "Only HTTP(S) URLs are supported" }
        return uri.toString()
    }

    private fun button(label: String) = TextView(activity).apply {
        text = label
        textSize = 24f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        isClickable = true
        isFocusable = true
        background = GradientDrawable().apply {
            setColor(Color.rgb(25, 30, 41))
            cornerRadius = dp(10).toFloat()
        }
    }
    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()
}
