package com.stillbrowser.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.text.InputType
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.os.Build
import android.net.Uri
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin

@InvokeArg
internal class OpenArgs {
    lateinit var url: String
}

/**
 * Still Browser's native browsing surface.
 *
 * The Tauri webview renders the spatial home UI. This plugin adds a native
 * Android WebView overlay when a destination is opened, preserving the spatial
 * home beneath it. The overlay has its own minimal glass-inspired controls.
 */
@TauriPlugin
class BrowserPlugin(private val activity: Activity) : Plugin(activity) {
    private lateinit var overlay: FrameLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var backButton: TextView
    private lateinit var forwardButton: TextView
    private var initialized = false

    override fun load(webView: WebView) {
        if (initialized) return
        initialized = true
        activity.runOnUiThread { createBrowserSurface() }
    }

    private fun createBrowserSurface() {
        val bg = Color.rgb(8, 12, 20)
        val panel = Color.rgb(20, 27, 42)
        val line = Color.rgb(54, 66, 88)
        val text = Color.rgb(242, 245, 255)
        val muted = Color.rgb(158, 173, 198)
        val mint = Color.rgb(200, 255, 224)

        overlay = FrameLayout(activity).apply {
            setBackgroundColor(bg)
            visibility = View.GONE
            elevation = dp(18).toFloat()
        }
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val toolbar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(panel)
            elevation = dp(8).toFloat()
        }

        val home = makeButton("⌂", text, panel)
        backButton = makeButton("‹", text, panel)
        forwardButton = makeButton("›", text, panel)
        val reload = makeButton("↻", text, panel)
        address = EditText(activity).apply {
            hint = "Search or enter a website"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(text)
            setHintTextColor(muted)
            textSize = 13f
            setPadding(dp(10), 0, dp(10), 0)
            background = rounded(panel, dp(13).toFloat(), line)
        }
        listOf(home, backButton, forwardButton).forEach {
            toolbar.addView(it, LinearLayout.LayoutParams(dp(37), dp(42)).apply { marginEnd = dp(5) })
        }
        toolbar.addView(address, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(5) })
        toolbar.addView(reload, LinearLayout.LayoutParams(dp(40), dp(42)))

        progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(mint)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(line)
            visibility = View.GONE
        }
        web = WebView(activity).apply {
            setBackgroundColor(bg)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                }
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) activity.title = "$title · Still"
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme?.lowercase()
                    // Only navigate web content through this surface. Never bypass TLS errors.
                    return scheme != "http" && scheme != "https"
                }
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                    updateHistoryButtons()
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!url.isNullOrBlank()) address.setText(url)
                    updateHistoryButtons()
                }
            }
        }
        val status = TextView(activity).apply {
            text = "STILLSPACE  ·  SECURE WEB DESTINATION"
            textSize = 9f
            letterSpacing = .12f
            setTextColor(muted)
            setPadding(dp(15), dp(8), dp(10), dp(8))
        }

        column.addView(toolbar)
        column.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        column.addView(status)
        column.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        overlay.addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        activity.addContentView(overlay, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        home.setOnClickListener { overlay.visibility = View.GONE }
        backButton.setOnClickListener { if (web.canGoBack()) web.goBack(); updateHistoryButtons() }
        forwardButton.setOnClickListener { if (web.canGoForward()) web.goForward(); updateHistoryButtons() }
        reload.setOnClickListener { web.reload() }
        address.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                loadInput(address.text.toString())
                true
            } else false
        }
    }

    @Command
    fun open(invoke: Invoke) {
        val args = invoke.parseArgs(OpenArgs::class.java)
        activity.runOnUiThread {
            ensureReady()
            try {
                val target = normalize(args.url)
                address.setText(target)
                overlay.visibility = View.VISIBLE
                web.loadUrl(target)
                updateHistoryButtons()
                invoke.resolve(JSObject())
            } catch (e: Exception) {
                invoke.reject(e.message ?: "Invalid URL")
            }
        }
    }

    @Command
    fun home(invoke: Invoke) {
        activity.runOnUiThread {
            if (initialized) overlay.visibility = View.GONE
            invoke.resolve(JSObject())
        }
    }

    @Command
    fun back(invoke: Invoke) {
        activity.runOnUiThread {
            if (initialized && web.canGoBack()) web.goBack()
            updateHistoryButtons()
            invoke.resolve(JSObject())
        }
    }

    @Command
    fun forward(invoke: Invoke) {
        activity.runOnUiThread {
            if (initialized && web.canGoForward()) web.goForward()
            updateHistoryButtons()
            invoke.resolve(JSObject())
        }
    }

    @Command
    fun reload(invoke: Invoke) {
        activity.runOnUiThread {
            if (initialized) web.reload()
            invoke.resolve(JSObject())
        }
    }

    private fun ensureReady() {
        if (!initialized) createBrowserSurface()
    }

    private fun loadInput(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val target = when {
            value.startsWith("https://", true) || value.startsWith("http://", true) -> value
            value.matches(Regex("""^[\w.-]+\.[A-Za-z]{2,}(:\d+)?([/?#].*)?$""")) && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + java.net.URLEncoder.encode(value, "UTF-8")
        }
        try {
            web.loadUrl(normalize(target))
        } catch (_: Exception) {
            address.error = "Only HTTP and HTTPS addresses are supported"
        }
    }

    private fun normalize(value: String): String {
        val uri = Uri.parse(value.trim())
        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") { "Only HTTP and HTTPS URLs are supported." }
        require(!uri.host.isNullOrBlank()) { "Enter a valid website address." }
        return uri.toString()
    }

    private fun updateHistoryButtons() {
        if (!initialized) return
        backButton.isEnabled = web.canGoBack()
        forwardButton.isEnabled = web.canGoForward()
        backButton.alpha = if (web.canGoBack()) 1f else .35f
        forwardButton.alpha = if (web.canGoForward()) 1f else .35f
    }

    private fun makeButton(label: String, fg: Int, bg: Int) = TextView(activity).apply {
        text = label
        textSize = 23f
        gravity = Gravity.CENTER
        setTextColor(fg)
        isClickable = true
        isFocusable = true
        background = rounded(bg, dp(12).toFloat(), Color.rgb(49, 61, 81))
    }

    private fun rounded(color: Int, radius: Float, stroke: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        setStroke(dp(1), stroke)
    }

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
