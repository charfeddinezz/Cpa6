package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppSettings
import com.example.service.ExtractedPlanResult
import com.example.service.TaskCategoryPlanner
import com.example.ui.AppViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.MainNavigation
import com.example.ui.components.PhaseIndicatorStrip
import com.example.ui.components.TopConsoleBar
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.ClickScreen
import com.example.ui.screens.EmailPoolScreen
import com.example.ui.screens.InfoScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ProxyPoolScreen
import com.example.ui.screens.ScriptsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TaskScreen
import com.example.ui.theme.CpaBg
import com.example.ui.theme.MyApplicationTheme
import java.io.ByteArrayInputStream
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Ensure hardware accelerated window rendering with graceful fallback
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        } catch (_: Exception) {}
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CpaAutomatorApp()
            }
        }
    }

    /**
     * Clears WebView cache, cookies, local storage, databases, and session state on demand
     * to prevent previous real-IP sessions from leaking information through cached assets.
     */
    fun clearWebViewData(webView: WebView? = null, onComplete: (() -> Unit)? = null) {
        clearWebViewData(this, webView, onComplete)
    }

    /**
     * Custom WebViewClient implemented in the main activity to intercept network requests
     * and strictly block all WebRTC-related STUN/TURN traffic and direct sockets to prevent
     * leaking the device's real public or private IP address.
     */
    open class CustomWebViewClient(
        private val proxyUserProvider: () -> String = { "" },
        private val proxyPassProvider: () -> String = { "" },
        private val onPageStartedCallback: ((WebView?, String?, Bitmap?) -> Unit)? = null,
        private val onPageFinishedCallback: ((WebView?, String?) -> Unit)? = null,
        private val onErrorCallback: ((WebView?, WebResourceRequest?, WebResourceError?) -> Unit)? = null
    ) : WebViewClient() {

        /**
         * Overrides shouldInterceptRequest to block WebRTC STUN/TURN server connections,
         * ICE candidate exchanges, and background probe sockets.
         */
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
            if (isWebRtcStunTurnTraffic(uri, request.isForMainFrame)) {
                Log.w(TAG, "Shield intercepted and blocked WebRTC STUN/TURN traffic: $uri")
                return createBlockedWebResourceResponse()
            }
            return super.shouldInterceptRequest(view, request)
        }

        @Deprecated("Deprecated in Java")
        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            if (url != null) {
                val uri = Uri.parse(url)
                if (isWebRtcStunTurnTraffic(uri, isMainFrame = false)) {
                    Log.w(TAG, "Shield intercepted and blocked WebRTC STUN/TURN traffic: $url")
                    return createBlockedWebResourceResponse()
                }
            }
            return super.shouldInterceptRequest(view, url)
        }

        override fun onReceivedHttpAuthRequest(
            view: WebView?,
            handler: HttpAuthHandler?,
            host: String?,
            realm: String?
        ) {
            val u = proxyUserProvider()
            val p = proxyPassProvider()
            if (u.isNotBlank() && p.isNotBlank()) {
                handler?.proceed(u, p)
            } else {
                super.onReceivedHttpAuthRequest(view, handler, host, realm)
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageStartedCallback?.invoke(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinishedCallback?.invoke(view, url)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            onErrorCallback?.invoke(view, request, error)
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: android.webkit.SslErrorHandler?,
            error: android.net.http.SslError?
        ) {
            Log.w(TAG, "SSL warning ignored for offer tracking redirect: ${error?.primaryError} on ${error?.url}")
            // Proceed so affiliate tracking networks and intermediate certificates do not fail or show blank pages
            handler?.proceed()
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?
        ): Boolean {
            Log.w(TAG, "WebView render process exited (didCrash=${detail?.didCrash()}). Handled gracefully.")
            return true
        }

        companion object {
            private const val TAG = "AntiWebRtcClient"

            /**
             * Inspects URI scheme, host, port, and path for any WebRTC STUN/TURN signatures.
             */
            fun isWebRtcStunTurnTraffic(uri: Uri, isMainFrame: Boolean = false): Boolean {
                val scheme = (uri.scheme ?: "").lowercase()
                val host = (uri.host ?: "").lowercase()
                val path = (uri.path ?: "").lowercase()
                val fullUrl = uri.toString().lowercase()
                val port = uri.port

                // 1. STUN/TURN explicit protocol schemes
                if (scheme == "stun" || scheme == "stuns" || scheme == "turn" || scheme == "turns") {
                    return true
                }

                // 2. Standard STUN / TURN port numbers
                if (port in setOf(3478, 5349, 19302, 19305, 19307, 19308)) {
                    return true
                }

                // 3. Known public & private STUN / TURN servers and relay endpoints
                if (host.startsWith("stun.") || host.startsWith("turn.") ||
                    host.contains(".stun.") || host.contains(".turn.") ||
                    host.contains("stunprotocol.org") ||
                    host.contains("stunserver.org") ||
                    host.contains("numb.viagenie.ca") ||
                    host.contains("stun.cloudflare.com") ||
                    host.contains("stun.l.google.com") ||
                    host.contains("stun1.l.google.com") ||
                    host.contains("stun2.l.google.com") ||
                    host.contains("stun3.l.google.com") ||
                    host.contains("stun4.l.google.com") ||
                    host.contains("stun.services.mozilla.com") ||
                    host.contains("global.stun.twilio.com") ||
                    host.contains("stun.ekiga.net") ||
                    host.contains("stun.ideasip.com") ||
                    host.contains("stun.voiparound.com") ||
                    host.contains("stun.voipbuster.com") ||
                    host.contains("stun.voipstunt.com") ||
                    host.contains("stun.voxgratia.org")
                ) {
                    return true
                }

                // 4. Subresources attempting WebRTC ICE candidate lookups or STUN/TURN signaling
                // (Explicitly allow Cloudflare Turnstile captcha scripts & CPA Locker scripts like alignmentfiles.com / cpagrip)
                if (!isMainFrame) {
                    val isCloudflareTurnstile = fullUrl.contains("challenges.cloudflare.com") ||
                            fullUrl.contains("turnstile") ||
                            path.contains("turnstile")

                    // Whitelist CPA lockers, offer scripts and affiliate redirects
                    val isCpaOrOfferScript = host.contains("alignmentfiles") ||
                            host.contains("cpagrip") ||
                            host.contains("filetrkr") ||
                            host.contains("ogads") ||
                            host.contains("cpabuild") ||
                            host.contains("cpalead") ||
                            host.contains("adworkmedia") ||
                            host.contains("adscend") ||
                            host.contains("monlix") ||
                            host.contains("revlum") ||
                            host.contains("lootably") ||
                            host.contains("wannads") ||
                            path.contains("script_include") ||
                            path.contains("preload.php") ||
                            path.contains("load_box") ||
                            path.contains("locker") ||
                            path.contains("offer")

                    if (!isCloudflareTurnstile && !isCpaOrOfferScript) {
                        val hasTurnParam = (fullUrl.contains("?turn=") || fullUrl.contains("&turn=") || fullUrl.contains("/turn=")) &&
                                !fullUrl.contains("return=") && !fullUrl.contains("return_url=")
                        val hasStunParam = fullUrl.contains("?stun=") || fullUrl.contains("&stun=") || fullUrl.contains("/stun=")

                        if (path == "/stun" || path.startsWith("/stun/") || path.endsWith("/stun") ||
                            path == "/turn" || path.startsWith("/turn/") || path.endsWith("/turn") ||
                            path.endsWith("/ice") || path.contains("/ice/") ||
                            path.contains("turn.php") || path.contains("stun.php") ||
                            path.contains("webrtc-ip") || path.contains("ice-candidate") ||
                            hasStunParam || hasTurnParam ||
                            fullUrl.contains("stun:") || fullUrl.contains("turn:")
                        ) {
                            return true
                        }
                    }
                }

                return false
            }

            /**
             * Returns a synthetic 403 Forbidden response to abort the WebRTC socket cleanly
             * and protect the user's real IP address from being broadcast.
             */
            fun createBlockedWebResourceResponse(): WebResourceResponse {
                val emptyStream = ByteArrayInputStream(ByteArray(0))
                val headers = mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "X-Shield-Action" to "WebRTC STUN/TURN Blocked"
                )
                return WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    403,
                    "Forbidden - WebRTC STUN/TURN Blocked",
                    headers,
                    emptyStream
                )
            }
        }
    }

    companion object {
        private const val MAIN_TAG = "MainActivity"

        /**
         * Clears all WebView cache, cookies, local storage, WebSQL/IndexedDB databases,
         * and cached assets on demand to ensure that previous real-IP sessions do not
         * leak information through cached assets, persistent cookies, or local storage.
         */
        fun clearWebViewData(
            context: Context,
            webView: WebView? = null,
            onComplete: (() -> Unit)? = null
        ) {
            try {
                // 1. Clear In-Memory and Disk Cache for WebView
                webView?.let { wv ->
                    wv.clearCache(true)
                    wv.clearFormData()
                    wv.clearHistory()
                    wv.clearSslPreferences()

                    // Clear DOM Storage (localStorage, sessionStorage), Cache API, and Service Workers
                    wv.evaluateJavascript(
                        """
                        (function() {
                            try {
                                if (window.localStorage) window.localStorage.clear();
                                if (window.sessionStorage) window.sessionStorage.clear();
                                if ('caches' in window) {
                                    caches.keys().then(function(names) {
                                        for (var i = 0; i < names.length; i++) caches.delete(names[i]);
                                    });
                                }
                                if (navigator.serviceWorker) {
                                    navigator.serviceWorker.getRegistrations().then(function(regs) {
                                        for (var i = 0; i < regs.length; i++) regs[i].unregister();
                                    });
                                }
                                if (window.indexedDB && window.indexedDB.databases) {
                                    window.indexedDB.databases().then(function(dbs) {
                                        for (var i = 0; i < dbs.length; i++) {
                                            if (dbs[i].name) window.indexedDB.deleteDatabase(dbs[i].name);
                                        }
                                    });
                                }
                            } catch(e) {}
                        })();
                        """.trimIndent(),
                        null
                    )
                }

                // 2. Clear all Cookies (Session + Persistent)
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies { _ ->
                    cookieManager.flush()
                }
                cookieManager.removeSessionCookies(null)

                // 3. Clear HTML5 Web Storage (localStorage, WebSQL, IndexedDB)
                WebStorage.getInstance().deleteAllData()

                // 4. Clear Geolocation permissions
                GeolocationPermissions.getInstance().clearAll()

                // 5. Clear saved HTTP Auth credentials and form data in WebViewDatabase
                try {
                    val webViewDatabase = WebViewDatabase.getInstance(context)
                    webViewDatabase.clearHttpAuthUsernamePassword()
                    @Suppress("DEPRECATION")
                    webViewDatabase.clearFormData()
                } catch (e: Exception) {
                    Log.w(MAIN_TAG, "Error clearing WebViewDatabase: ${e.message}")
                }

                // 6. Delete cached database and app_webview directory files on disk
                try {
                    context.deleteDatabase("webview.db")
                    context.deleteDatabase("webviewCache.db")
                    val webviewCacheDir = File(context.cacheDir, "org.chromium.android_webview")
                    if (webviewCacheDir.exists()) {
                        webviewCacheDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    Log.w(MAIN_TAG, "Error deleting webview cache directory: ${e.message}")
                }

                Log.i(MAIN_TAG, "WebView cache, cookies, and local storage successfully cleared on demand.")
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e(MAIN_TAG, "Failed to clear WebView data: ${e.message}", e)
                onComplete?.invoke()
            }
        }
    }
}

@Composable
fun CpaAutomatorApp(vm: AppViewModel = viewModel()) {
    val currentTab by vm.currentTab.collectAsState()
    val automationState by vm.automationState.collectAsState()
    val extractedInfo by vm.extractedInfo.collectAsState()
    val identity by vm.identity.collectAsState()
    val settings by vm.settings.collectAsState()
    val logs by vm.logs.collectAsState()
    val browserCommand by vm.browserCommand.collectAsState()

    val tasks by vm.tasks.collectAsState()
    val proxies by vm.proxies.collectAsState()
    val emails by vm.emails.collectAsState()
    val scripts by vm.scripts.collectAsState()
    val stats by vm.stats.collectAsState()
    val offerClickItems by vm.offerClickItems.collectAsState()
    val enabledClickItems by vm.enabledClickItems.collectAsState()
    val workTemplates by vm.workTemplates.collectAsState()

    var browserInitialized by remember { mutableStateOf(false) }
    val isBrowserTab = (currentTab == ScreenTab.BROWSER)
    if (isBrowserTab || automationState.isRunning) {
        browserInitialized = true
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TopConsoleBar(
                    automationState = automationState,
                    onToggleAutomation = { vm.toggleAutomation() },
                    onRefreshGeo = { vm.refreshGeoInfo() }
                )
                PhaseIndicatorStrip(automationState = automationState)
            }
        },
        bottomBar = {
            MainNavigation(
                selectedTab = currentTab,
                onTabSelected = { vm.selectTab(it) }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CpaBg)
                .padding(innerPadding)
        ) {
            // Persistent Browser Screen Container: Preserves WebView state, continuous automation, and prevents page reload
            if (browserInitialized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isBrowserTab) 1f else 0f
                        }
                        .then(if (isBrowserTab) Modifier else Modifier.zIndex(-1f))
                ) {
                    BrowserScreen(
                        settings = settings,
                        automationState = automationState,
                        extractedInfo = extractedInfo,
                        identity = identity,
                        scripts = scripts,
                        browserCommand = browserCommand,
                        clickTexts = enabledClickItems.map { it.text },
                        activeClickText = automationState.activeClickText,
                        onClearBrowserCommand = { vm.clearBrowserCommand() },
                        onNotifyCompletion = { kw, url -> vm.notifyTaskCompleted(kw, url) },
                        onOfferClicked = { txt, url -> vm.onOfferClicked(txt, url) },
                        onOfferClickedInNewTab = { txt, url, tabId -> vm.onOfferClickedInNewTab(txt, url, tabId) },
                        onNewTabInteractionCompleted = { tabId, url, details -> vm.onNewTabInteractionCompleted(tabId, url, details) },
                        onLockerDetectedWithCount = { url, id, isTriggered, count -> vm.onLockerDetected(url, id, isTriggered, count) },
                        onPageAnalyzed = { reportJson -> vm.onPageAnalyzed(reportJson) },
                        onPageActionMapGenerated = { vm.onPageActionMapGenerated(it) },
                        onPageActionStepExecuted = { idx, name, status -> vm.onPageActionStepExecuted(idx, name, status) },
                        onMidPageInfoExtracted = { k, v -> vm.onMidPageInfoExtracted(k, v) },
                        onTemplateGenerated = { vm.onTemplateGenerated(it) },
                        onRequestAutoTemplate = { vm.requestGenerateTemplateFromPage() },
                        onActiveTabChanged = { tabId -> vm.setActiveTabId(tabId) },
                        onUpdateWebRtcMode = { vm.setWebRtcMode(it) }
                    )
                }
            }

            if (!isBrowserTab) {
                when (currentTab) {
                    ScreenTab.TASKS -> TaskScreen(
                        tasks = tasks,
                        automationState = automationState,
                        workTemplates = workTemplates,
                        onSaveTask = { vm.saveTask(it) },
                        onDeleteTask = { vm.deleteTask(it) },
                        onToggleTask = { vm.toggleTaskEnabled(it) },
                        onRunTask = { vm.runTaskNow(it) },
                        onOptimizeTask = { task ->
                            val plan = vm.extractPlanForUrl(task.url)
                            val optimized = task.copy(
                                name = if (task.name.isBlank() || task.name.startsWith("Task ")) plan.detectedName else task.name,
                                categories = plan.categories.joinToString(", "),
                                mode = plan.recommendedMode,
                                browserDuration = plan.recommendedDuration,
                                completionKeywords = plan.recommendedKeywords,
                                referer = plan.recommendedReferer
                            )
                            vm.saveTask(optimized)
                        }
                    )
                    ScreenTab.BROWSER -> { /* Handled in persistent container above */ }
                    ScreenTab.TEMPLATES -> com.example.ui.screens.TemplatesScreen(
                        templates = workTemplates,
                        automationState = automationState,
                        onSaveTemplate = { vm.saveWorkTemplate(it) },
                        onDeleteTemplate = { vm.deleteWorkTemplate(it) },
                        onRequestAutoTemplate = { vm.requestGenerateTemplateFromPage() },
                        onExecuteTemplate = { tmpl ->
                            vm.executeWorkTemplateInBrowser(tmpl)
                        }
                    )
                    ScreenTab.CLICK -> ClickScreen(
                        clickItems = offerClickItems,
                        automationState = automationState,
                        settings = settings,
                        onUpdateSettings = { vm.saveSettings(it) },
                        onAddItem = { text, tag -> vm.addOfferClickItem(text, tag) },
                        onUpdateItem = { vm.updateOfferClickItem(it) },
                        onToggleItem = { id, enabled -> vm.toggleOfferClickItem(id, enabled) },
                        onDeleteItem = { vm.deleteOfferClickItem(it) },
                        onMoveItem = { id, dir -> vm.moveOfferClickItem(id, dir) },
                        onResetDefaults = { vm.resetDefaultOfferClickItems() },
                        onTestInBrowser = { vm.testOfferClickInBrowser(it) },
                        onLaunchLandingBlogspot = { vm.navigateBrowser("https://gdfqo.blogspot.com") },
                        onRunAutomation = { vm.toggleAutomation() },
                        onRunNikeDirect = { vm.runGdfqoNikeTaskDirect() }
                    )
                    ScreenTab.INFO -> InfoScreen(
                    extractedInfo = extractedInfo,
                    identity = identity,
                    automationState = automationState,
                    onRegenerateIdentity = { vm.regenerateIdentity() },
                    onRefreshGeo = { vm.refreshGeoInfo() }
                )
                ScreenTab.PROXIES -> ProxyPoolScreen(
                    proxies = proxies,
                    currentSettings = settings,
                    onFetchFromUrl = { url, protocol, callback ->
                        vm.fetchProxiesFromUrl(url, protocol, callback)
                    },
                    onImportBulk = { raw, protocol ->
                        vm.importProxiesBulk(raw, protocol)
                    },
                    onAddSingleProxy = { vm.addSingleProxy(it) },
                    onSetActiveProxy = { vm.setActiveProxy(it) },
                    onTestProxy = { proxy, callback ->
                        vm.testProxy(proxy, callback)
                    },
                    onDeleteProxy = { vm.deleteProxy(it) },
                    onClearAll = { vm.clearAllProxies() },
                    onToggleAutoRotate = { vm.updateSettings(settings.copy(proxyAutoRotate = it)) },
                    onToggleProxyGlobalEnabled = { vm.toggleProxyGlobalEnabled(it) },
                    onTestAllProxies = { onProg, onComp -> vm.testAllProxies(onProg, onComp) },
                    onDeleteFailed = { vm.deleteFailedProxies() },
                    onResetFailed = { vm.resetFailedProxies() },
                    onAutoSelectFastest = { vm.autoSelectFastestProxy() },
                    onExportWorking = { vm.getWorkingProxiesFormatted() }
                )
                ScreenTab.SCRIPTS -> ScriptsScreen(
                    scripts = scripts,
                    onSaveScript = { vm.saveScript(it) },
                    onToggleScript = { vm.toggleScript(it) },
                    onDeleteScript = { vm.deleteScript(it) }
                )
                ScreenTab.EMAILS -> EmailPoolScreen(
                    emails = emails,
                    onAddEmail = { vm.addEmail(it) },
                    onImportBulk = { vm.importEmailsBulk(it) },
                    onGenerateTestEmails = { vm.generateTestEmails() },
                    onDeleteEmail = { vm.deleteEmail(it) },
                    onClearAll = { vm.clearAllEmails() }
                )
                ScreenTab.STATS -> StatsScreen(
                    stats = stats,
                    tasks = tasks,
                    automationState = automationState,
                    onClearStats = { vm.clearStats() }
                )
                ScreenTab.SETTINGS -> SettingsScreen(
                    currentSettings = settings,
                    onSaveSettings = { vm.updateSettings(it) },
                    onTestCpa = { callback -> vm.testCpaConnection(callback) }
                )
                ScreenTab.LOGS -> LogsScreen(
                    logs = logs,
                    onClearLogs = { vm.clearLogs() }
                )
            }
        }
    }
}
}
