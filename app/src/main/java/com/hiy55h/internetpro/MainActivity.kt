package com.hiy55h.internetpro

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
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
import android.app.Activity

class MainActivity : Activity() {
    private data class BrowserTab(val id: Int, var title: String, var url: String, val web: WebView)
    private val tabs = mutableListOf<BrowserTab>()
    private var activeId = 0
    private var nextId = 1
    private lateinit var root: LinearLayout
    private lateinit var tabStrip: LinearLayout
    private lateinit var address: EditText
    private lateinit var pageHost: FrameLayout
    private lateinit var status: TextView

    private val blue = Color.rgb(26, 115, 232)
    private val surface = Color.rgb(245, 247, 250)
    private val ink = Color.rgb(32, 33, 36)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
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
            setPadding(dp(8), 0, dp(8), 0)
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
        addressRow.addView(textButton("⋮") { showMenu(itAnchor = addressRow) })
        root.addView(addressRow)

        pageHost = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(pageHost, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(3), dp(4), dp(3))
            setBackgroundColor(Color.WHITE)
        }
        bottom.addView(textButton("‹") { current()?.web?.let { if (it.canGoBack()) it.goBack() } })
        bottom.addView(textButton("›") { current()?.web?.let { if (it.canGoForward()) it.goForward() } })
        bottom.addView(textButton("↻") { current()?.web?.reload() })
        bottom.addView(textButton("⌂") { navigate("https://www.google.com") })
        bottom.addView(textButton("☆") { bookmarkCurrent() })
        bottom.addView(textButton("▣") { showDownloads() })
        root.addView(bottom, LinearLayout.LayoutParams(-1, dp(48)))

        status = TextView(this).apply {
            text = "Internet Pro  •  WebView"
            textSize = 10f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(4))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(status)
        setContentView(root)
    }

    private fun current(): BrowserTab? = tabs.firstOrNull { it.id == activeId }

    private fun addTab(url: String) {
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.settings.setSupportMultipleWindows(false)
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                tabs.firstOrNull { it.web == view }?.let {
                    it.title = title?.take(22) ?: "新しいタブ"
                    refreshTabs()
                }
            }
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                status.text = if (newProgress < 100) "読み込み中… $newProgress%" else "Internet Pro"
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
            override fun onPageFinished(view: WebView, url: String) {
                tabs.firstOrNull { it.web == view }?.url = url
                if (activeId == tabs.firstOrNull { it.web == view }?.id) address.setText(url)
            }
        }
        web.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("cookie", it) }
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.setDescription("Internet Pro ダウンロード")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "ダウンロードを開始しました", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "ダウンロードできません: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        val tab = BrowserTab(nextId++, "新しいタブ", url, web)
        tabs.add(tab)
        activeId = tab.id
        pageHost.addView(web, FrameLayout.LayoutParams(-1, -1))
        web.loadUrl(url)
        showActiveTab()
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
                setOnLongClickListener {
                    closeTab(tab.id)
                    true
                }
            }
            tabStrip.addView(b, LinearLayout.LayoutParams(dp(132), dp(38)).apply {
                marginEnd = dp(5)
            })
        }
    }

    private fun closeTab(id: Int) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = tabs.removeAt(index)
        pageHost.removeView(removed.web)
        removed.web.stopLoading()
        removed.web.destroy()
        if (tabs.isEmpty()) addTab("https://www.google.com")
        else {
            activeId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id ?: tabs.last().id
            showActiveTab()
        }
    }

    private fun bookmarkCurrent() {
        val tab = current() ?: return
        val prefs = getSharedPreferences("bookmarks", MODE_PRIVATE)
        val old = prefs.getStringSet("urls", emptySet())?.toMutableSet() ?: mutableSetOf()
        old.add(tab.url)
        prefs.edit().putStringSet("urls", old).apply()
        Toast.makeText(this, "ブックマークに追加しました", Toast.LENGTH_SHORT).show()
    }

    private fun showMenu(itAnchor: View) {
        val menu = PopupMenu(this, itAnchor)
        menu.menu.add("新しいタブ").setOnMenuItemClickListener { addTab("https://www.google.com"); true }
        menu.menu.add("ブックマーク").setOnMenuItemClickListener { showBookmarks(); true }
        menu.menu.add("ダウンロード").setOnMenuItemClickListener { showDownloads(); true }
        menu.menu.add("履歴").setOnMenuItemClickListener { showHistory(); true }
        menu.menu.add("設定").setOnMenuItemClickListener {
            Toast.makeText(this, "設定は次のアップデートで追加予定です", Toast.LENGTH_SHORT).show(); true
        }
        menu.show()
    }

    private fun showBookmarks() {
        val urls = getSharedPreferences("bookmarks", MODE_PRIVATE).getStringSet("urls", emptySet())?.toList().orEmpty()
        if (urls.isEmpty()) {
            Toast.makeText(this, "ブックマークはありません", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this).setTitle("ブックマーク")
            .setItems(urls.toTypedArray()) { _, which -> navigate(urls[which]) }.setNegativeButton("閉じる", null).show()
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
        android.app.AlertDialog.Builder(this).setTitle("ダウンロード")
            .setMessage(if (rows.isEmpty()) "ダウンロードはありません\n保存先: Download" else rows.joinToString("\n"))
            .setPositiveButton("OK", null).show()
    }

    private fun showHistory() {
        val history = current()?.web?.copyBackForwardList()
        val lines = (0 until (history?.size ?: 0)).mapNotNull { i -> history?.getItemAtIndex(i)?.let { it.title + "\n" + it.url } }
        android.app.AlertDialog.Builder(this).setTitle("このタブの履歴")
            .setMessage(if (lines.isEmpty()) "履歴はありません" else lines.takeLast(15).reversed().joinToString("\n\n"))
            .setPositiveButton("OK", null).show()
    }

    override fun onBackPressed() {
        val web = current()?.web
        if (web != null && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { it.web.stopLoading(); it.web.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
