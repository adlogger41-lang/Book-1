package com.example

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {

  private lateinit var webView: WebView
  private lateinit var insetsController: WindowInsetsControllerCompat

  private var pendingExportText: String? = null
  private var pendingBackupJson: String? = null
  private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

  // Activity Result Launchers for Storage Access Framework (SAF)
  private val createTextDocumentLauncher =
      registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && pendingExportText != null) {
          writeContentToUri(uri, pendingExportText!!.toByteArray(Charsets.UTF_8))
          Toast.makeText(this, "Text exported successfully", Toast.LENGTH_SHORT).show()
        }
        pendingExportText = null
      }

  private val createBackupDocumentLauncher =
      registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && pendingBackupJson != null) {
          writeContentToUri(uri, pendingBackupJson!!.toByteArray(Charsets.UTF_8))
          Toast.makeText(this, "Backup saved successfully", Toast.LENGTH_SHORT).show()
        }
        pendingBackupJson = null
      }

  private val openBackupDocumentLauncher =
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
          val content = readContentFromUri(uri)
          if (content != null) {
            // Send content to JS for restoration
            val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            runOnUiThread {
              val js = "try { window.onNativeRestore(decodeURIComponent(escape(atob('$encoded')))); } catch(e) { alert('Restore error: ' + e); }"
              webView.evaluateJavascript(js, null)
            }
          } else {
            Toast.makeText(this, "Could not read selected file", Toast.LENGTH_SHORT).show()
          }
        }
      }

  private val fileChooserLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris: Array<Uri>? =
            if (result.resultCode == RESULT_OK && result.data != null) {
              val uri = result.data?.data
              if (uri != null) arrayOf(uri) else null
            } else {
              null
            }
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
      }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Edge to edge window configuration
    WindowCompat.setDecorFitsSystemWindows(window, false)
    insetsController = WindowInsetsControllerCompat(window, window.decorView)

    val rootLayout = FrameLayout(this).apply {
      layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(0xFF241910.toInt()) // Antique leather dark background
    }

    webView = WebView(this).apply {
      layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(0xFF241910.toInt())
      isFocusable = true
      isFocusableInTouchMode = true
      isVerticalScrollBarEnabled = false
      isHorizontalScrollBarEnabled = false
    }

    configureWebSettings(webView.settings)

    // Add JavaScript Interface
    webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

    // Prevent empty or corrupted variations seed file from generating Chromium warnings
    try {
      val webViewDir = File(applicationInfo.dataDir, "app_webview")
      if (webViewDir.exists()) {
        val seed = File(webViewDir, "variations_seed")
        val seedNew = File(webViewDir, "variations_seed_new")
        if (seed.exists() && seed.length() == 0L) seed.delete()
        if (seedNew.exists() && seedNew.length() == 0L) seedNew.delete()
      }
    } catch (_: Exception) {}

    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(
          view: WebView?,
          request: WebResourceRequest?
      ): Boolean {
        val url = request?.url?.toString() ?: return false
        return if (url.startsWith("file:///android_asset/")) {
          false
        } else {
          try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          } catch (_: Exception) {}
          true
        }
      }

      @Suppress("DEPRECATION")
      override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return if (url.startsWith("file:///android_asset/")) {
          false
        } else {
          try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          } catch (_: Exception) {}
          true
        }
      }

      override fun onRenderProcessGone(
          view: WebView?,
          detail: android.webkit.RenderProcessGoneDetail?
      ): Boolean {
        // Recover gracefully if system reclaims WebView renderer under memory pressure
        return true
      }
    }

    webView.webChromeClient = object : WebChromeClient() {
      override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
        android.util.Log.d("TheOldBook", "[JS] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
        return true
      }
      override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?
      ): Boolean {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = filePathCallback

        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
          type = "*/*"
          addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
          fileChooserLauncher.launch(intent)
        } catch (e: Exception) {
          fileChooserCallback?.onReceiveValue(null)
          fileChooserCallback = null
          return false
        }
        return true
      }

      override fun onJsAlert(
          view: WebView?,
          url: String?,
          message: String?,
          result: JsResult?
      ): Boolean {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.app_name)
            .setMessage(message ?: "")
            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
      }

      override fun onJsConfirm(
          view: WebView?,
          url: String?,
          message: String?,
          result: JsResult?
      ): Boolean {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.app_name)
            .setMessage(message ?: "")
            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
      }

      override fun onJsPrompt(
          view: WebView?,
          url: String?,
          message: String?,
          defaultValue: String?,
          result: JsPromptResult?
      ): Boolean {
        val input = EditText(this@MainActivity).apply {
          setText(defaultValue ?: "")
        }
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.app_name)
            .setMessage(message ?: "")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
              result?.confirm(input.text.toString())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
              result?.cancel()
            }
            .setOnCancelListener { result?.cancel() }
            .show()
        return true
      }
    }

    rootLayout.addView(webView)
    setContentView(rootLayout)

    // Handle back button
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        webView.evaluateJavascript("window.onNativeBackPressed ? window.onNativeBackPressed() : false") { result ->
          if (result != "true") {
            // Back was not consumed by JavaScript (no fullscreen or dialog was active)
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            isEnabled = true
          }
        }
      }
    })

    // Load local asset
    webView.loadUrl("file:///android_asset/index.html")
  }

  @SuppressLint("SetJavaScriptEnabled")
  private fun configureWebSettings(settings: WebSettings) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.allowFileAccess = true
    settings.allowContentAccess = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.textZoom = 100
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.cacheMode = WebSettings.LOAD_DEFAULT
  }

  private fun writeContentToUri(uri: Uri, bytes: ByteArray) {
    try {
      contentResolver.openOutputStream(uri, "wt")?.use { os ->
        os.write(bytes)
        os.flush()
      }
    } catch (e: Exception) {
      Toast.makeText(this, "Failed to write file: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  private fun readContentFromUri(uri: Uri): String? {
    return try {
      contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.bufferedReader(Charsets.UTF_8).readText()
      }
    } catch (e: Exception) {
      null
    }
  }

  inner class AndroidBridge {
    @JavascriptInterface
    fun saveNotebook(json: String) {
      try {
        val file = File(filesDir, "the_old_book_state.json")
        val temp = File(filesDir, "the_old_book_state.tmp")
        temp.writeText(json, Charsets.UTF_8)
        if (temp.renameTo(file) || (file.delete() && temp.renameTo(file))) {
          // Success
        } else {
          file.writeText(json, Charsets.UTF_8)
        }
      } catch (_: Exception) {}
    }

    @JavascriptInterface
    fun loadNotebook(): String {
      return try {
        val file = File(filesDir, "the_old_book_state.json")
        if (file.exists()) file.readText(Charsets.UTF_8) else ""
      } catch (_: Exception) {
        ""
      }
    }

    @JavascriptInterface
    fun exportText(text: String) {
      pendingExportText = text
      runOnUiThread {
        try {
          createTextDocumentLauncher.launch("the-old-book.txt")
        } catch (e: Exception) {
          Toast.makeText(this@MainActivity, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }

    @JavascriptInterface
    fun backupJson(json: String) {
      pendingBackupJson = json
      runOnUiThread {
        try {
          createBackupDocumentLauncher.launch("the-old-book-backup.json")
        } catch (e: Exception) {
          Toast.makeText(this@MainActivity, "Backup error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }

    @JavascriptInterface
    fun requestRestore() {
      runOnUiThread {
        try {
          openBackupDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        } catch (e: Exception) {
          Toast.makeText(this@MainActivity, "Restore error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }

    @JavascriptInterface
    fun setFullscreen(fullscreen: Boolean) {
      runOnUiThread {
        if (fullscreen) {
          insetsController.hide(WindowInsetsCompat.Type.systemBars())
          insetsController.systemBarsBehavior =
              WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
          insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
      }
    }

    @JavascriptInterface
    fun showToast(message: String) {
      runOnUiThread {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onPause() {
    super.onPause()
    webView.evaluateJavascript("window.flushBeforeLeaving ? window.flushBeforeLeaving() : null", null)
    webView.onPause()
  }

  override fun onResume() {
    super.onResume()
    webView.onResume()
  }

  override fun onDestroy() {
    webView.evaluateJavascript("window.flushBeforeLeaving ? window.flushBeforeLeaving() : null", null)
    webView.destroy()
    super.onDestroy()
  }
}
