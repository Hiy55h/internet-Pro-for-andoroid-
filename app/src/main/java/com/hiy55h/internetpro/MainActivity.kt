package com.hiy55h.internetpro

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private data class BrowserTab(val id: Int, var title: String, var url: String, val web: WebView)
    private val tabs = mutableListOf<BrowserTab>()
    private var activeId = 0
    private var nextId = 1
    private var desktopMode = false
    private var mobileUa: String? = null
    private lateinit var root: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var address: EditText
    private lateinit var pageHost: FrameLayout
    private lateinit var status: TextView

    private val blue = Color.rgb(26, 115, 232)
    private val surface = Color.rgb(245, 247, 250)
    private val ink = Color.rgb(32, 33, 36)
    private val desktopUa = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        desktopMode = getSharedPreferences("browser", MODE_PRIVATE).getBoolean("desktop", false)
        buildUi()
        addTab("https://www.google.com")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun textButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(ink)
            setPadding(dp(7), 0, dp(7), 0)
            minWidth = dp(42)
            setOnClickListener { action() }
        }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
            layoutParams = ViewGroup.LayoutParams(-1, -1)
        }
        tabStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(Color.rgb(232, 236, 242))
        }
        val tabsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabStrip)
        }
        root.addView(tabsScroll, LinearLayout.LayoutParams(-1, dp(48)))

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(8), dp(5))
            setBackgroundColor(Color.WHITE)
        }
        address = EditText(this).apply {
            singleLine = true
            textSize = 15f
            setTextColor(ink)
            setHintTextColor(Color.GRAY)
            hint = "検索またはURLを入力"
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(dp(14), 0, dp(10), 0)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(241, 243, 244))
                cornerRadius = dp(24).toFloat()
            }
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    navigate(address.text.toString())
                    true
                } else false
            }
        }
        addressRow.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        addressRow.addView(textButton("＋") { addTab("https://www.google.com") })
        addressRow.addView(textButton("⋮") { showMenu(addressRow) })
        root.addView(addressRow)

        pageHost = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(pageHost, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(3), dp(2), dp(3))
            setBackgroundColor(Color.WHITE)
        }
        bottom.addView(textButton("戻る") { current()?.web?.let { if (it.canGoBack()) it.goBack() } })
        bottom.addView(textButton("進む") { current()?.web?.let { if (it.canGoForward()) it.goForward() } })
        bottom.addView(textButton("再読込") { current()?.web?.reload() })
        bottom.addView(textButton("⌂") { navigate("https://www.google.com") })
        bottom.addView(textButton("−") { current()?.web?.zoomOut() })
        bottom.addView(textButton("＋") { current()?.web?.zoomIn() })
        bottom.addView(textButton("PC") { toggleDesktopMode() })
        root.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(bottom)
        }, LinearLayout.LayoutParams(-1, dp(48)))

        status = TextView(this).apply {
            textSize = 10f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(4))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(status)
        setContentView(root)
        updateStatus()
    }

    private fun updateStatus() {
        status.text = "Internet Pro  •  " + if (desktopMode) "デスクトップモード" else "モバイルモード"
    }

    private fun current(): BrowserTab? = tabs.firstOrNull { it.id == activeId }

    private fun addTab(url: String) {
        val web = WebView(this)
        mobileUa = mobileUa ?: web.settings.userAgentString
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.settings.setSupportMultipleWindows(false)
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = !desktopMode
        web.settings.setSupportZoom(true)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        if (desktopMode) web.settings.userAgentString = desktopUa
        web.addJavascriptInterface(ImageBridge(), "InternetProBridge")
        web.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                tabs.firstOrNull { it.web == view }?.let {
                    it.title = title?.take(22)?.ifBlank { "新しいタブ" } ?: "新しいタブ"
                    refreshTabs()
                }
            }
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                status.text = if (newProgress < 100) "読み込み中… $newProgress%" else
                    "Internet Pro  •  " + if (desktopMode) "デスクトップモード" else "モバイルモード"
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
            override fun onPageFinished(view: WebView, url: String) {
                val tab = tabs.firstOrNull { it.web == view }
                if (tab != null) {
                    tab.url = url
                    if (activeId == tab.id) address.setText(url)
                }
                installPageFeatures(view)
            }
        }
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            downloadFile(url, userAgent, contentDisposition, mimeType)
        }
        val tab = BrowserTab(nextId++, "新しいタブ", url, web)
        tabs.add(tab)
        activeId = tab.id
        pageHost.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.loadUrl(url)
        showActiveTab()
    }

    private fun installPageFeatures(web: WebView) {
        // A lightweight user-script feature, not the full Chrome Extensions API.
        val scripts = getSharedPreferences("extensions", MODE_PRIVATE)
            .getStringSet("scripts", emptySet())?.toList().orEmpty()
        scripts.forEach { script ->
            web.evaluateJavascript("(function(){try{\n$script\n}catch(e){console.error(e)}})();", null)
        }
        // Catch image right-clicks on pages. The native bridge exposes only a download prompt.
        val js = """
            (function() {
              if (window.__internetProContextMenuInstalled) return;
              window.__internetProContextMenuInstalled = true;
              document.addEventListener('contextmenu', function(e) {
                var img = e.target && e.target.closest ? e.target.closest('img') : null;
                if (img && img.src) {
                  e.preventDefault();
                  InternetProBridge.askImageDownload(img.currentSrc || img.src);
                }
              }, true);
            })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }

    private inner class ImageBridge {
        @JavascriptInterface
        fun askImageDownload(url: String) {
            runOnUiThread {
                val safeUrl = url.trim()
                if (!safeUrl.startsWith("https://") && !safeUrl.startsWith("http://")) {
                    Toast.makeText(this@MainActivity, "この形式の画像は保存できません", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("画像のダウンロード")
                    .setMessage("画像をダウンロードしますか？")
                    .setPositiveButton("はい") { _, _ ->
                        downloadFile(safeUrl, current()?.web?.settings?.userAgentString ?: "", null, "image/*")
                    }
                    .setNegativeButton("いいえ", null)
                    .show()
            }
        }
    }

    private fun downloadFile(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            if (!mimeType.isNullOrBlank()) request.setMimeType(mimeType)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("cookie", it) }
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            request.setTitle(filename)
            request.setDescription("Internet Pro ダウンロード")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "ダウンロードを開始しました", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "ダウンロードできません: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val url = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + Uri.encode(value)
        }
        current()?.let {
            it.url = url
            it.web.loadUrl(url)
        } ?: addTab(url)
    }

    private fun showActiveTab() {
        pageHost.removeAllViews()
        current()?.let {
            (it.web.parent as? ViewGroup)?.removeView(it.web)
            pageHost.addView(it.web, FrameLayout.LayoutParams(-1, -1))
            address.setText(it.url)
        }
        refreshTabs()
    }

    private fun refreshTabs() {
        tabStrip.removeAllViews()
        tabs.forEach { tab ->
            val b = Button(this).apply {
                text = (if (tab.id == activeId) "● " else "") + tab.title
                isAllCaps = false
                textSize = 11f
                minWidth = dp(90)
                setOnClickListener { activeId = tab.id; showActiveTab() }
                setOnLongClickListener { closeTab(tab.id); true }
            }
            tabStrip.addView(b, LinearLayout.LayoutParams(dp(132), dp(38)).apply { marginEnd = dp(5) })
        }
    }

    private fun closeTab(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = tabs.removeAt(index)
        pageHost.removeView(removed.web)
        removed.web.stopLoading()
        removed.web.removeJavascriptInterface("InternetProBridge")
        removed.web.destroy()
        if (tabs.isEmpty()) addTab("https://www.google.com")
        else {
            activeId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id ?: tabs.last().id
            showActiveTab()
        }
    }

    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        getSharedPreferences("browser", MODE_PRIVATE).edit().putBoolean("desktop", desktopMode).apply()
        tabs.forEach { tab ->
            tab.web.settings.userAgentString = if (desktopMode) desktopUa else (mobileUa ?: tab.web.settings.userAgentString)
            tab.web.settings.loadWithOverviewMode = !desktopMode
            tab.web.reload()
        }
        updateStatus()
        Toast.makeText(this, if (desktopMode) "デスクトップモード ON" else "モバイルモード ON", Toast.LENGTH_SHORT).show()
    }

    private fun showMenu(anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add("新しいタブ").setOnMenuItemClickListener { addTab("https://www.google.com"); true }
        menu.menu.add("タブを閉じる").setOnMenuItemClickListener { current()?.let { closeTab(it.id) }; true }
        menu.menu.add("ブックマーク").setOnMenuItemClickListener { showBookmarks(); true }
        menu.menu.add("ダウンロード").setOnMenuItemClickListener { showDownloads(); true }
        menu.menu.add("履歴").setOnMenuItemClickListener { showHistory(); true }
        menu.menu.add(if (desktopMode) "モバイルモードにする" else "デスクトップモードにする")
            .setOnMenuItemClickListener { toggleDesktopMode(); true }
        menu.menu.add("拡張機能（ユーザースクリプト）").setOnMenuItemClickListener { showExtensions(); true }
        menu.menu.add("設定").setOnMenuItemClickListener {
            AlertDialog.Builder(this).setTitle("Internet Pro")
                .setMessage("CPU: armeabi-v7a (32bit)\nAndroid 11 対応\n拡張機能はユーザースクリプト方式です。")
                .setPositiveButton("OK", null).show()
            true
        }
        menu.show()
    }

    private fun showExtensions() {
        val prefs = getSharedPreferences("extensions", MODE_PRIVATE)
        val scripts = prefs.getStringSet("scripts", emptySet())?.toMutableSet() ?: mutableSetOf()
        val names = scripts.mapIndexed { i, _ -> "登録スクリプト ${i + 1}" }.toMutableList()
        names.add("＋ スクリプトを追加")
        AlertDialog.Builder(this)
            .setTitle("拡張機能（ユーザースクリプト）")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == names.lastIndex) addUserScript()
                else {
                    val selected = scripts.elementAtOrNull(which) ?: return@setItems
                    AlertDialog.Builder(this).setTitle("スクリプト")
                        .setMessage(selected.take(1200))
                        .setPositiveButton("削除") { _, _ ->
                            scripts.remove(selected)
                            prefs.edit().putStringSet("scripts", scripts).apply()
                        }
                        .setNegativeButton("閉じる", null).show()
                }
            }
            .setNegativeButton("閉じる", null)
            .show()
    }

    private fun addUserScript() {
        val input = EditText(this).apply {
            hint = "JavaScriptを入力"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 5
            gravity = Gravity.TOP
        }
        AlertDialog.Builder(this).setTitle("ユーザースクリプトを追加")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val script = input.text.toString().trim()
                if (script.isNotEmpty()) {
                    val prefs = getSharedPreferences("extensions", MODE_PRIVATE)
                    val scripts = prefs.getStringSet("scripts", emptySet())?.toMutableSet() ?: mutableSetOf()
                    scripts.add(script)
                    prefs.edit().putStringSet("scripts", scripts).apply()
                    Toast.makeText(this, "保存しました。ページを再読み込みすると実行されます", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun bookmarkCurrent() {
        val tab = current() ?: return
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val old = prefs.getStringSet("urls", emptySet())?.toMutableSet() ?: mutableSetOf()
        old.add(tab.url)
        prefs.edit().putStringSet("urls", old).apply()
        Toast.makeText(this, "ブックマークに追加しました", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        val urls = getSharedPreferences("bookmarks", MODE_PRIVATE).getStringSet("urls", emptySet())?.toList().orEmpty()
        if (urls.isEmpty()) {
            Toast.makeText(this, "ブックマークはありません", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("ブックマーク")
            .setItems(urls.toTypedArray()) { _, which -> navigate(urls[which]) }
            .setNegativeButton("閉じる", null).show()
    }

    private fun showDownloads() {
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query())
        val rows = mutableListOf<String>()
        cursor?.use {
            val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            while (it.moveToNext()) {
                val title = if (titleCol >= 0) it.getString(titleCol) else "ファイル"
                val state = if (statusCol >= 0) it.getInt(statusCol) else -1
                rows.add("$title  •  " + if (state == DownloadManager.STATUS_SUCCESSFUL) "完了" else if (state == DownloadManager.STATUS_FAILED) "失敗" else "進行中")
            }
        }
        AlertDialog.Builder(this).setTitle("ダウンロード")
            .setMessage(if (rows.isEmpty()) "ダウンロードはありません\n保存先: Download" else rows.joinToString("\n"))
            .setPositiveButton("OK", null).show()
    }

    private fun showHistory() {
        val history = current()?.web?.copyBackForwardList()
        val lines = (0 until (history?.size ?: 0)).mapNotNull { i -> history?.getItemAtIndex(i)?.let { it.title + "\n" + it.url } }
        AlertDialog.Builder(this).setTitle("このタブの履歴")
            .setMessage(if (lines.isEmpty()) "履歴はありません" else lines.takeLast(15).reversed().joinToString("\n\n"))
            .setPositiveButton("OK", null).show()
    }

    override fun onBackPressed() {
        val web = current()?.web
        if (web != null && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { it.web.stopLoading(); it.web.removeJavascriptInterface("InternetProBridge"); it.web.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
