package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.widget.Toast
import android.webkit.GeolocationPermissions
import android.webkit.HttpAuthHandler
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.WebAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.MainActivity
import com.example.data.model.AppSettings
import com.example.data.model.AutomationState
import com.example.data.model.ExtractedInfo
import com.example.data.model.GeneratedIdentity
import com.example.data.model.ScriptItem
import com.example.service.AutomationScriptBuilder
import com.example.service.TaskCategoryPlanner
import com.example.ui.BrowserCommand
import com.example.ui.theme.CpaAccent
import com.example.ui.theme.CpaBg
import com.example.ui.theme.CpaBorder
import com.example.ui.theme.CpaCard
import com.example.ui.theme.CpaCardElevated
import com.example.ui.theme.CpaError
import com.example.ui.theme.CpaPrimary
import com.example.ui.theme.CpaPrimaryBorder
import com.example.ui.theme.CpaPrimaryDim
import com.example.ui.theme.CpaSuccess
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted
import com.example.ui.theme.CpaWarning
import android.os.Message
import com.example.util.WebProxyManager
import com.example.ui.components.CpaLockerDialog
import kotlinx.coroutines.delay
import java.util.UUID

class WebAppInterface(
    private val onCompleted: (String, String) -> Unit,
    private val onOfferClicked: ((String, String) -> Unit)? = null,
    private val onOfferClickedInNewTab: ((String, String) -> Unit)? = null,
    private val onNewTabInteractionCompleted: ((String, String, String) -> Unit)? = null,
    private val onPageAnalyzed: ((String) -> Unit)? = null,
    private val onPageActionMapGenerated: ((String) -> Unit)? = null,
    private val onPageActionStepExecuted: ((Int, String, String) -> Unit)? = null,
    private val onMidPageInfoExtracted: ((String, String) -> Unit)? = null,
    private val onTemplateGenerated: ((String) -> Unit)? = null,
    private val onLockerDetected: ((String, String, Boolean, Int) -> Unit)? = null,
    private val onLockerStatus: ((String) -> Unit)? = null
) {
    @JavascriptInterface
    fun onTaskCompleted(keyword: String, url: String) {
        onCompleted(keyword, url)
    }

    @JavascriptInterface
    fun onOfferClicked(text: String, url: String) {
        onOfferClicked?.invoke(text, url)
    }

    @JavascriptInterface
    fun onOfferClickedInNewTab(text: String, url: String) {
        onOfferClickedInNewTab?.invoke(text, url)
    }

    @JavascriptInterface
    fun onNewTabInteractionCompleted(tabId: String, currentUrl: String, details: String) {
        onNewTabInteractionCompleted?.invoke(tabId, currentUrl, details)
    }

    @JavascriptInterface
    fun onPageAnalyzed(reportJson: String) {
        onPageAnalyzed?.invoke(reportJson)
    }

    @JavascriptInterface
    fun onPageActionMapGenerated(actionMapJson: String) {
        onPageActionMapGenerated?.invoke(actionMapJson)
    }

    @JavascriptInterface
    fun onPageActionStepExecuted(stepIndex: Int, stepName: String, status: String) {
        onPageActionStepExecuted?.invoke(stepIndex, stepName, status)
    }

    @JavascriptInterface
    fun onMidPageInfoExtracted(extractedKey: String, extractedValue: String) {
        onMidPageInfoExtracted?.invoke(extractedKey, extractedValue)
    }

    @JavascriptInterface
    fun onTemplateGenerated(templateJson: String) {
        onTemplateGenerated?.invoke(templateJson)
    }

    @JavascriptInterface
    fun onLockerDetected(url: String, id: String, isTriggered: Boolean) {
        onLockerDetected?.invoke(url, id, isTriggered, 0)
    }

    @JavascriptInterface
    fun onLockerDetectedWithCount(url: String, id: String, isTriggered: Boolean, offersCount: Int) {
        onLockerDetected?.invoke(url, id, isTriggered, offersCount)
    }

    @JavascriptInterface
    fun onLockerStatus(status: String) {
        onLockerStatus?.invoke(status)
    }
}

/**
 * Multi-Tab Model tracking tab identity, display title, URL, loading state and associated WebView.
 */
data class BrowserTabModel(
    val id: String,
    var title: String = "Tab 1",
    var url: String = "about:blank",
    var isLoading: Boolean = false,
    var progress: Float = 0f,
    var stageBadge: String = "",
    var webView: WebView? = null,
    var lastError: String? = null,
    var detectedLockerUrl: String? = null,
    var detectedLockerId: String? = null,
    var isLockerTriggered: Boolean = false,
    var lockerStatusMsg: String? = null,
    var isOfferTab: Boolean = false,
    var offerOpenedTime: Long = 0L,
    var hasSimulatedInteraction: Boolean = false
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    settings: AppSettings,
    automationState: AutomationState,
    extractedInfo: ExtractedInfo,
    identity: GeneratedIdentity,
    scripts: List<ScriptItem>,
    browserCommand: BrowserCommand?,
    clickTexts: List<String> = emptyList(),
    activeClickText: String? = null,
    onClearBrowserCommand: () -> Unit,
    onNotifyCompletion: (String, String) -> Unit,
    onOfferClicked: ((String, String) -> Unit)? = null,
    onOfferClickedInNewTab: ((String, String, String) -> Unit)? = null,
    onNewTabInteractionCompleted: ((String, String, String) -> Unit)? = null,
    onLockerDetectedWithCount: ((String, String, Boolean, Int) -> Unit)? = null,
    onPageAnalyzed: ((String) -> Unit)? = null,
    onPageActionMapGenerated: ((String) -> Unit)? = null,
    onPageActionStepExecuted: ((Int, String, String) -> Unit)? = null,
    onMidPageInfoExtracted: ((String, String) -> Unit)? = null,
    onTemplateGenerated: ((String) -> Unit)? = null,
    onRequestAutoTemplate: (() -> Unit)? = null,
    onActiveTabChanged: ((String) -> Unit)? = null,
    onUpdateWebRtcMode: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Multi-Tab browser tabs collection
    val tabs = remember {
        mutableStateListOf(
            BrowserTabModel(id = "tab_1", title = "Main Tab", url = "about:blank")
        )
    }
    var activeTabId by remember { mutableStateOf("tab_1") }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTabModel("tab_1")

    var urlInput by remember { mutableStateOf("") }
    var currentDisplayUrl by remember { mutableStateOf("about:blank") }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(false) }
    var showDirectiveDetails by remember { mutableStateOf(false) }

    val defaultMobileUa = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    val defaultDesktopUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    var isMobileUaMode by remember { mutableStateOf(true) }

    val openInNewTab: (String, String) -> Unit = { targetUrl, tabTitle ->
        val newId = "tab_${System.currentTimeMillis()}"
        val newTab = BrowserTabModel(
            id = newId,
            title = tabTitle.take(18).ifBlank { "Offer Tab" },
            url = targetUrl,
            stageBadge = "Offer",
            isOfferTab = true,
            offerOpenedTime = System.currentTimeMillis()
        )
        tabs.add(newTab)
        activeTabId = newId
        currentDisplayUrl = targetUrl
        urlInput = targetUrl
        onActiveTabChanged?.invoke(newId)
        onOfferClickedInNewTab?.invoke(tabTitle, targetUrl, newId)
    }

    val openPopupInNewTab: (String) -> Unit = { targetUrl ->
        val newId = "tab_${UUID.randomUUID()}"
        val newTab = BrowserTabModel(
            id = newId,
            title = "Offer Destination",
            url = targetUrl,
            stageBadge = "Offer",
            isOfferTab = true,
            offerOpenedTime = System.currentTimeMillis()
        )
        tabs.add(newTab)
        activeTabId = newId
        currentDisplayUrl = targetUrl
        urlInput = targetUrl
        onActiveTabChanged?.invoke(newId)
        onOfferClickedInNewTab?.invoke("Locker Offer", targetUrl, newId)
    }

    // Synchronize URL input with active tab
    LaunchedEffect(activeTabId, activeTab.url) {
        urlInput = if (activeTab.url == "about:blank") "" else activeTab.url
        currentDisplayUrl = activeTab.url
        isPageLoading = activeTab.isLoading
        webProgress = activeTab.progress
    }

    // Resolve the active proxy IP to be used for WebRTC ICE spoofing
    val effectiveWebRtcIp = remember(settings.webrtcCustomIp, automationState.activeIp, extractedInfo.ip, settings.proxyHost) {
        if (settings.webrtcCustomIp.isNotBlank()) {
            settings.webrtcCustomIp.trim()
        } else if (automationState.activeIp.isNotBlank() && automationState.activeIp != "Not Connected") {
            automationState.activeIp
        } else if (extractedInfo.ip.isNotBlank()) {
            extractedInfo.ip
        } else if (settings.proxyHost.isNotBlank()) {
            settings.proxyHost
        } else {
            "104.28.19.42"
        }
    }

    // Keep proxy configuration synchronized whenever proxy settings change
    var prevProxyEnabled by remember { mutableStateOf(settings.proxyEnabled) }
    LaunchedEffect(settings.proxyEnabled, settings.proxyHost, settings.proxyPort, settings.proxyType, settings.proxyUser, settings.proxyPass) {
        val proxyToggled = prevProxyEnabled != settings.proxyEnabled
        prevProxyEnabled = settings.proxyEnabled

        val port = settings.proxyPort.toIntOrNull()
        WebProxyManager.applyProxy(context, settings.proxyEnabled, settings.proxyHost, port, settings.proxyType, settings.proxyUser, settings.proxyPass) { _, _ ->
            if (proxyToggled && activeTab.url.isNotBlank() && activeTab.url != "about:blank") {
                activeTab.lastError = null
                activeTab.webView?.reload()
            }
        }
    }

    // Re-inject WebRTC protection dynamically ONLY into the ACTIVE displayed tab
    // Expert: stable fingerprintSeed per repetition — same session same fingerprint
    LaunchedEffect(activeTabId, activeTab.webView, effectiveWebRtcIp, settings.webrtcMode, extractedInfo.timezone, extractedInfo.language, automationState.fingerprintSeed) {
        activeTab.webView?.evaluateJavascript(
            AutomationScriptBuilder.buildAntiDetectionScript(
                proxyIp = effectiveWebRtcIp,
                webrtcMode = settings.webrtcMode,
                timezone = extractedInfo.timezone,
                language = extractedInfo.language,
                latitude = extractedInfo.latitude,
                longitude = extractedInfo.longitude,
                fingerprintSeed = automationState.fingerprintSeed
            ),
            null
        )
    }

    // Autonomous 10-Second / URL / Tab Re-Analysis Engine:
    // Only runs when automation is actively running!
    var reanalysisCountdown by remember { mutableIntStateOf(10) }
    LaunchedEffect(activeTabId, currentDisplayUrl, automationState.isRunning) {
        if (!automationState.isRunning) return@LaunchedEffect
        reanalysisCountdown = 10
        while (automationState.isRunning) {
            delay(1000)
            reanalysisCountdown--
            val activeDisplayed = tabs.find { it.id == activeTabId }
            if (activeDisplayed?.isOfferTab == true) {
                val realUrl = activeDisplayed.webView?.url.orEmpty()
                if (realUrl.isNotBlank() && realUrl != currentDisplayUrl && realUrl != "about:blank") {
                    currentDisplayUrl = realUrl
                    urlInput = realUrl
                    reanalysisCountdown = 10
                    activeDisplayed.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildPageAnalyzerScript(clickTexts, activeClickText ?: automationState.activeClickText),
                        null
                    )
                } else if (reanalysisCountdown <= 0) {
                    reanalysisCountdown = 10
                    activeDisplayed.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildPageAnalyzerScript(clickTexts, activeClickText ?: automationState.activeClickText),
                        null
                    )
                }
            }
        }
    }

    // Autonomous Smart Automation Loop:
    // Paced human cycle: Maintains the automation loop, running a cycle every 15 to 25 seconds!
    var cycleCountdownSeconds by remember { mutableIntStateOf(0) }
    var currentCycleTotal by remember { mutableIntStateOf(20) }

    LaunchedEffect(
        activeTabId, identity, extractedInfo, automationState.isRunning,
        automationState.activeTaskCategories, clickTexts, activeClickText,
        settings.cycleIntervalMinSec, settings.cycleIntervalMaxSec
    ) {
        while (automationState.isRunning) {
            val minSec = settings.cycleIntervalMinSec.coerceAtLeast(15)
            val maxSec = settings.cycleIntervalMaxSec.coerceAtLeast(minSec)
            val cycleSec = if (minSec < maxSec) kotlin.random.Random.nextInt(minSec, maxSec + 1) else minSec
            currentCycleTotal = cycleSec
            cycleCountdownSeconds = cycleSec

            val activeDisplayed = tabs.find { it.id == activeTabId }
            if (activeDisplayed != null) {
                val effClickText = activeClickText ?: automationState.activeClickText
                if (activeDisplayed.isOfferTab) {
                    // 1. Destination Offer Tab: Perform offer filling and interaction
                    activeDisplayed.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildSmartFormFillScript(
                            identity = identity,
                            categories = automationState.activeTaskCategories,
                            clickTexts = clickTexts,
                            activeClickText = effClickText,
                            extractedInfo = extractedInfo
                        ),
                        null
                    )
                } else {
                    // 2. Landing Page: Maintain loop, check for locker and auto-click matching offer (without touching the background site)
                    activeDisplayed.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildCpaLockerDetectorAndActivatorScript(
                            customId = settings.cpaLockerDefaultId.ifBlank { "1741238" },
                            forceTrigger = settings.cpaLockerAutoTrigger,
                            forceInjectIfMissing = settings.cpaLockerAutoInjectIfMissing
                        ),
                        null
                    )
                    activeDisplayed.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildLockerOfferAutoClickScript(
                            clickTexts = clickTexts,
                            activeTargetText = effClickText,
                            selectionStrategy = settings.offerSelectionStrategy,
                            openInNewTab = settings.offerClickOpenInNewTab
                        ),
                        null
                    )
                }
            }

            // Second-by-second countdown for the 15-25s interval
            for (sec in cycleSec downTo 1) {
                if (!automationState.isRunning) break
                cycleCountdownSeconds = sec
                delay(1000)
            }
        }
        cycleCountdownSeconds = 0
    }

    // Execute BrowserCommands from ViewModel on the ACTIVE tab
    LaunchedEffect(browserCommand) {
        browserCommand?.let { cmd ->
            when (cmd) {
                is BrowserCommand.LoadUrl -> {
                    urlInput = cmd.url
                    currentDisplayUrl = cmd.url
                    activeTab.url = cmd.url
                    activeTab.webView?.let { webView ->
                        if (!cmd.userAgent.isNullOrBlank()) {
                            webView.settings.userAgentString = cmd.userAgent
                        }
                        val headers = mutableMapOf<String, String>()
                        if (!cmd.referer.isNullOrBlank()) {
                            headers["Referer"] = cmd.referer
                        }
                        webView.loadUrl(cmd.url, headers)
                    }
                }
                is BrowserCommand.Reload -> activeTab.webView?.reload()
                is BrowserCommand.GoBack -> if (activeTab.webView?.canGoBack() == true) activeTab.webView?.goBack()
                is BrowserCommand.GoForward -> if (activeTab.webView?.canGoForward() == true) activeTab.webView?.goForward()
                is BrowserCommand.ClearUrl -> {
                    activeTab.webView?.let { webView ->
                        webView.clearCache(true)
                        webView.clearHistory()
                        webView.clearFormData()
                        webView.loadUrl("about:blank")
                        currentDisplayUrl = "about:blank"
                        activeTab.url = "about:blank"
                        activeTab.title = "New Tab"
                        urlInput = ""
                    }
                }
                is BrowserCommand.ClearCacheAndStorage -> {
                    MainActivity.clearWebViewData(context, activeTab.webView) {
                        activeTab.webView?.loadUrl("about:blank")
                        currentDisplayUrl = "about:blank"
                        activeTab.url = "about:blank"
                        activeTab.title = "New Tab"
                        urlInput = ""
                    }
                }
                is BrowserCommand.GenerateAutoTemplate -> {
                    activeTab.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildAutoTemplateGeneratorScript(),
                        null
                    )
                }
                is BrowserCommand.ExecuteWorkTemplate -> {
                    activeTab.webView?.evaluateJavascript(
                        AutomationScriptBuilder.buildWorkTemplateExecutionScript(cmd.stepsJson, identity),
                        null
                    )
                }
                is BrowserCommand.OpenInNewTab -> {
                    openInNewTab(cmd.url, cmd.title)
                }
            }
            onClearBrowserCommand()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(CpaBg)) {
        // 1. Ultra-Slim IP & Proxy Status Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(CpaCardElevated)
                .border(0.5.dp, CpaBorder.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // IP & WebRTC on the left
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = if (settings.proxyHost.isNotBlank()) CpaSuccess else CpaWarning,
                    modifier = Modifier.size(9.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "IP: $effectiveWebRtcIp",
                    color = CpaText,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                if (extractedInfo.countryCode.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[${extractedInfo.countryCode}]",
                        color = CpaPrimary,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WebRTC: ${settings.webrtcMode.uppercase()}",
                    color = if (settings.webrtcMode == "block") CpaError else CpaSuccess,
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Real-Time Page Perception & Countdown Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (automationState.isRunning) {
                    val catLabel = if (automationState.detectedCategoryAr.isNotBlank()) {
                        automationState.detectedCategoryAr
                    } else if (automationState.detectedPageCategory.isNotBlank()) {
                        automationState.detectedPageCategory
                    } else {
                        "جاري التحليل"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CpaPrimaryDim)
                            .border(0.5.dp, CpaPrimaryBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = "🧠 $catLabel (${reanalysisCountdown}s)",
                            color = CpaPrimary,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }

                val statusText = if (automationState.isRunning) {
                    automationState.phase.uppercase()
                } else {
                    "READY"
                }
                val statusColor = if (automationState.isRunning) CpaWarning else CpaPrimary

                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Multi-Tab Navigation Bar (Tabs Bar)
        val tabScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CpaCardElevated)
                .border(0.5.dp, CpaBorder)
                .horizontalScroll(tabScrollState)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = tab.id == activeTabId
                val tabBg = if (isActive) CpaCard else CpaBg.copy(alpha = 0.6f)
                val tabBorder = if (isActive) CpaPrimary else CpaBorder.copy(alpha = 0.4f)
                val tabTextColor = if (isActive) CpaPrimary else CpaTextMuted

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tabBg)
                        .border(1.dp, tabBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            activeTabId = tab.id
                            onActiveTabChanged?.invoke(tab.id)
                        }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (isActive) CpaPrimary else CpaTextDim,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val displayTitle = if (tab.title.isNotBlank()) tab.title else "Tab ${index + 1}"
                    Text(
                        text = displayTitle.take(15),
                        color = tabTextColor,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Funnel Stage Badge if available
                    if (tab.stageBadge.isNotBlank()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "[${tab.stageBadge}]",
                            color = CpaWarning,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Close Tab Action
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (tabs.size > 1) {
                                    val closingId = tab.id
                                    val closingIndex = tabs.indexOfFirst { it.id == closingId }
                                    tab.webView?.destroy()
                                    tabs.remove(tab)
                                    if (activeTabId == closingId) {
                                        val nextIndex = closingIndex.coerceAtMost(tabs.size - 1)
                                        activeTabId = tabs[nextIndex].id
                                        onActiveTabChanged?.invoke(activeTabId)
                                    }
                                } else {
                                    // Reset single tab
                                    tab.title = "Main Tab"
                                    tab.url = "about:blank"
                                    tab.webView?.loadUrl("about:blank")
                                    currentDisplayUrl = "about:blank"
                                    urlInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tab",
                            tint = if (isActive) CpaError else CpaTextDim,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // New Tab "+" Button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(4.dp))
                    .clickable {
                        val newId = "tab_${System.currentTimeMillis()}"
                        val newTab = BrowserTabModel(
                            id = newId,
                            title = "Tab ${tabs.size + 1}",
                            url = "about:blank"
                        )
                        tabs.add(newTab)
                        activeTabId = newId
                        onActiveTabChanged?.invoke(newId)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = CpaPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // 3. Navigation & URL Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CpaCard)
                .border(1.dp, CpaBorder)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { activeTab.webView?.let { if (it.canGoBack()) it.goBack() } },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CpaText, modifier = Modifier.size(14.dp))
            }

            IconButton(
                onClick = { activeTab.webView?.let { if (it.canGoForward()) it.goForward() } },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = CpaText, modifier = Modifier.size(14.dp))
            }

            IconButton(
                onClick = { activeTab.webView?.reload() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = CpaText, modifier = Modifier.size(14.dp))
            }

            if (currentDisplayUrl.isNotBlank() && currentDisplayUrl != "about:blank") {
                IconButton(
                    onClick = {
                        urlInput = ""
                        currentDisplayUrl = "about:blank"
                        activeTab.url = "about:blank"
                        activeTab.title = "New Tab"
                        activeTab.webView?.stopLoading()
                        activeTab.webView?.loadUrl("about:blank")
                        MainActivity.clearWebViewData(context, activeTab.webView)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Page & Clear", tint = CpaError, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(3.dp))

            // URL Input Container
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CpaBg)
                    .border(1.dp, CpaBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (currentDisplayUrl.startsWith("https")) Icons.Default.Lock else Icons.Default.Language,
                    contentDescription = "SSL",
                    tint = if (currentDisplayUrl.startsWith("https")) CpaSuccess else CpaTextMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = CpaText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Go
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onGo = {
                            var target = urlInput.trim()
                            if (target.isNotBlank()) {
                                if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                    target = "https://$target"
                                }
                                urlInput = target
                                currentDisplayUrl = target
                                activeTab.url = target
                                activeTab.webView?.loadUrl(target)
                            }
                        }
                    ),
                    cursorBrush = SolidColor(CpaPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // GO button
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CpaPrimary)
                    .clickable {
                        var target = urlInput.trim()
                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                            target = "https://$target"
                        }
                        urlInput = target
                        currentDisplayUrl = target
                        activeTab.url = target
                        activeTab.webView?.loadUrl(target)
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GO", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // 4. Quick Verification & Perception Bar
        val leakScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CpaCardElevated)
                .horizontalScroll(leakScrollState)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Automation Cycle Countdown Badge (15-25s)
            if (automationState.isRunning && cycleCountdownSeconds > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.2f))
                        .border(0.5.dp, Color(0xFF00E676), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔁 الدورة: بعد ${cycleCountdownSeconds}ث (15-25)",
                        color = Color(0xFF00E676),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // UA Mode Switcher (📱 Mobile / 💻 Desktop)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isMobileUaMode) CpaPrimary.copy(alpha = 0.2f) else CpaAccent.copy(alpha = 0.2f))
                    .border(0.5.dp, if (isMobileUaMode) CpaPrimary else CpaAccent, RoundedCornerShape(3.dp))
                    .clickable {
                        isMobileUaMode = !isMobileUaMode
                        val newUa = if (isMobileUaMode) defaultMobileUa else defaultDesktopUa
                        activeTab.webView?.settings?.userAgentString = newUa
                        activeTab.webView?.reload()
                        Toast.makeText(context, if (isMobileUaMode) "📱 تم تفعيل وضع الهاتف (Mobile UA) وإعادة التحميل" else "💻 تم تفعيل وضع الكمبيوتر (Desktop UA) وإعادة التحميل", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isMobileUaMode) "📱 وضع الهاتف" else "💻 وضع الكمبيوتر",
                    color = if (isMobileUaMode) CpaPrimary else CpaAccent,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Dedicated Locker Activator & Offer Scanner Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.25f))
                    .border(0.5.dp, Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                    .clickable {
                        activeTab.webView?.evaluateJavascript(
                            AutomationScriptBuilder.buildCpaLockerDetectorAndActivatorScript(
                                customId = settings.cpaLockerDefaultId.ifBlank { "1741238" },
                                forceTrigger = true,
                                forceInjectIfMissing = true
                            ),
                            null
                        )
                        val effClick = activeClickText ?: automationState.activeClickText
                        activeTab.webView?.evaluateJavascript(
                            AutomationScriptBuilder.buildLockerOfferAutoClickScript(
                                clickTexts = clickTexts,
                                activeTargetText = effClick,
                                selectionStrategy = settings.offerSelectionStrategy,
                                openInNewTab = settings.offerClickOpenInNewTab
                            ),
                            null
                        )
                        Toast.makeText(context, "⚡ جاري فك وإظهار لوكر العروض وتنشيطه...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("🔓 فك / تنشيط اللوكر", color = Color(0xFF81C784), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }

            // Passive Locker status badge (fully automated - no manual button)
            if (activeTab.detectedLockerUrl != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CpaPrimary.copy(alpha = 0.15f))
                        .border(0.5.dp, CpaPrimary.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔒 لوكر نشط (${activeTab.detectedLockerId ?: "مكتشف"})",
                        color = CpaPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Auto Template Generation from Active Page Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaPrimary)
                    .clickable {
                        activeTab.webView?.evaluateJavascript(
                            AutomationScriptBuilder.buildAutoTemplateGeneratorScript(),
                            null
                        )
                        Toast.makeText(context, "جاري استخراج عناصر الصفحة وتوليد قالب العمل...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("✨ صنع قالب من الصفحة", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Repetition & Cycle Status Badge
            if (automationState.totalRepeats > 1 || automationState.isRunning) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CpaPrimaryDim)
                        .border(0.5.dp, CpaPrimaryBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔁 تكرار ${automationState.currentRepeatIndex}/${automationState.totalRepeats}",
                        color = CpaPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Sequential Proxy Info Badge
            if (automationState.currentProxyInfo.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CpaCard)
                        .border(0.5.dp, CpaBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔄 بروكسي #${automationState.currentProxyIndex}: ${automationState.currentProxyInfo}",
                        color = CpaSuccess,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Map Summary Badge
            if (automationState.pageActionMapSummary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CpaAccent.copy(alpha = 0.15f))
                        .border(0.5.dp, CpaAccent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🗺️ ${automationState.pageActionMapSummary}",
                        color = CpaAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Mid-Page Extracted Intelligence Badge
            if (automationState.lastExtractedMidPageText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CpaWarning.copy(alpha = 0.15f))
                        .border(0.5.dp, CpaWarning.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🧠 ${automationState.lastExtractedMidPageText.take(28)}...",
                        color = CpaWarning,
                        fontSize = 8.sp
                    )
                }
            }

            // Immediate Perception Analysis Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaPrimaryDim)
                    .border(1.dp, CpaPrimaryBorder, RoundedCornerShape(3.dp))
                    .clickable {
                        activeTab.webView?.evaluateJavascript(
                            AutomationScriptBuilder.buildPageAnalyzerScript(clickTexts, activeClickText ?: automationState.activeClickText),
                            null
                        )
                        Toast.makeText(context, "جاري إعادة فحص وتحليل DOM الصفحة...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("⚡ إعادة تحليل الآن", color = CpaPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(3.dp))
                    .clickable {
                        val target = "https://browserleaks.com/webrtc"
                        urlInput = target
                        currentDisplayUrl = target
                        activeTab.url = target
                        activeTab.webView?.loadUrl(target)
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("Test WebRTC", color = CpaText, fontSize = 9.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(3.dp))
                    .clickable {
                        val target = "https://browserleaks.com/canvas"
                        urlInput = target
                        currentDisplayUrl = target
                        activeTab.url = target
                        activeTab.webView?.loadUrl(target)
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("Canvas Noise", color = CpaText, fontSize = 9.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(3.dp))
                    .clickable {
                        val target = "https://iphey.com"
                        urlInput = target
                        currentDisplayUrl = target
                        activeTab.url = target
                        activeTab.webView?.loadUrl(target)
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("IPHey Anonymity", color = CpaText, fontSize = 9.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(3.dp))
                    .clickable {
                        val target = "https://api.ipify.org"
                        urlInput = target
                        currentDisplayUrl = target
                        activeTab.url = target
                        activeTab.webView?.loadUrl(target)
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("api.ipify.org", color = CpaTextMuted, fontSize = 9.sp)
            }

            // On-demand Purge Cache, Cookies & Storage
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(CpaError.copy(alpha = 0.15f))
                    .border(1.dp, CpaError.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                    .clickable {
                        MainActivity.clearWebViewData(context, activeTab.webView) {
                            Toast.makeText(context, "Purged WebView Cache, Cookies & Local Storage", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Cache & Local Storage",
                        tint = CpaError,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Purge Cache & Storage", color = CpaError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Web Loading Progress Indicator
        if (isPageLoading && webProgress < 1f) {
            LinearProgressIndicator(
                progress = { webProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = CpaPrimary,
                trackColor = CpaBorder
            )
        }

        // 5. Intelligent Analysis & Directive Radar (التحليل أولاً ثم التوجيه والتنفيذ)
        if (automationState.isRunning || automationState.detectedPageCategory.isNotBlank() || automationState.activeDirectiveTitle.isNotBlank()) {
            val archetypeColor = when (automationState.directiveArchetype) {
                "CONFIRMATION" -> CpaSuccess
                "LOCKER" -> CpaPrimary
                "UPSELL" -> CpaWarning
                "SURVEY" -> CpaAccent
                "LEAD_FORM" -> Color(0xFF64B5F6)
                else -> CpaPrimary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CpaCardElevated)
                    .border(0.5.dp, archetypeColor.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Analysis Pill (التحليل المكتشف)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(archetypeColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🔍 التحليل: ",
                                color = CpaTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (automationState.detectedCategoryAr.isNotBlank()) automationState.detectedCategoryAr else "جاري استكشاف الصفحة",
                                color = archetypeColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (automationState.analysisConfidence > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${automationState.analysisConfidence}%)",
                                    color = CpaTextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Toggle directive details button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .clickable { showDirectiveDetails = !showDirectiveDetails }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (showDirectiveDetails) "إخفاء التفاصيل" else "تفاصيل التوجيه",
                                color = CpaPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (showDirectiveDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CpaPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Directive Pill (التوجيه التكتيكي)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧭 التوجيه: ",
                            color = CpaTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (automationState.activeDirectiveTitle.isNotBlank()) {
                                automationState.activeDirectiveTitle
                            } else {
                                "في انتظار مسح عناصر الصفحة وتحديد مسار العمل..."
                            },
                            color = CpaText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Expanded Details Panel (السبب وخطوات خطة العمل)
                    if (showDirectiveDetails) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CpaBg)
                                .border(0.5.dp, CpaBorder, RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Column {
                                if (automationState.activeDirectiveReason.isNotBlank()) {
                                    Text(
                                        text = "💡 مبرر التوجيه: ${automationState.activeDirectiveReason}",
                                        color = CpaTextDim,
                                        fontSize = 8.5.sp,
                                        lineHeight = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                                if (automationState.pageAnalysisSummary.isNotBlank()) {
                                    Text(
                                        text = "📊 ملخص عناصر الصفحة: ${automationState.pageAnalysisSummary}",
                                        color = CpaTextMuted,
                                        fontSize = 8.sp
                                    )
                                }
                                if (automationState.activePlanSummary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "🎯 المسار المستهدف: ${automationState.activePlanSummary}",
                                        color = CpaPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Connection Error Recovery Banner (Direct Local Fallback)
        if (activeTab.lastError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CpaError.copy(alpha = 0.12f))
                    .border(0.5.dp, CpaError.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!settings.proxyEnabled) "تعذر الاتصال (نظام البروكسي مغلق)" else "تعذر الاتصال بالصفحة",
                            color = CpaError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeTab.lastError} — انقر لإعادة التحميل بالاتصال المباشر",
                            color = CpaTextDim,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CpaPrimary)
                            .clickable {
                                activeTab.lastError = null
                                WebProxyManager.clearProxy(context) { _, _ ->
                                    activeTab.webView?.reload()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text("إعادة تحميل مباشر 🔄", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Multi-Tab WebViews Container
        // Each tab retains its own distinct WebView instance in memory.
        // Background tabs are kept hidden (View.GONE) to save GPU/rendering cycles.
        // Automated scripts and interactions ONLY target the active tab!
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            tabs.forEach { tabItem ->
                val isThisTabActive = tabItem.id == activeTabId

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isThisTabActive) 1f else 0f
                            translationY = if (isThisTabActive) 0f else 99999f
                        }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val port = settings.proxyPort.toIntOrNull()
                            WebProxyManager.applyProxy(ctx, settings.proxyEnabled, settings.proxyHost, port, settings.proxyType, settings.proxyUser, settings.proxyPass)

                            WebView(ctx).apply {
                                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                this.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    databaseEnabled = true
                                    setSupportMultipleWindows(true)
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = if (isMobileUaMode) defaultMobileUa else defaultDesktopUa

                                    safeBrowsingEnabled = false
                                    setGeolocationEnabled(false)
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    mediaPlaybackRequiresUserGesture = false
                                }

                                try {
                                    val cookieManager = android.webkit.CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)
                                } catch (e: Exception) {}

                                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                                    try {
                                        WebSettingsCompat.setSafeBrowsingEnabled(this.settings, false)
                                    } catch (e: Exception) {}
                                }

                                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                                    try {
                                        WebViewCompat.addDocumentStartJavaScript(
                                            this,
                                            AutomationScriptBuilder.buildAntiDetectionScript(
                                                proxyIp = effectiveWebRtcIp,
                                                webrtcMode = settings.webrtcMode,
                                                timezone = extractedInfo.timezone,
                                                language = extractedInfo.language,
                                                latitude = extractedInfo.latitude,
                                                longitude = extractedInfo.longitude,
                                                userAgent = this.settings.userAgentString,
                                                fingerprintSeed = automationState.fingerprintSeed
                                            ),
                                            setOf("*")
                                        )
                                        WebViewCompat.addDocumentStartJavaScript(
                                            this,
                                            AutomationScriptBuilder.buildTimezoneScript(extractedInfo.timezone, extractedInfo.language),
                                            setOf("*")
                                        )
                                        WebViewCompat.addDocumentStartJavaScript(
                                            this,
                                            AutomationScriptBuilder.buildCanvasNoiseScript(),
                                            setOf("*")
                                        )
                                    } catch (e: Exception) {}
                                }

                                addJavascriptInterface(
                                    WebAppInterface(
                                        onCompleted = { kw, pageUrl ->
                                            onNotifyCompletion(kw, pageUrl)
                                        },
                                        onOfferClicked = { txt, pageUrl ->
                                            onOfferClicked?.invoke(txt, pageUrl)
                                        },
                                        onOfferClickedInNewTab = { txt, pageUrl ->
                                            openInNewTab(pageUrl, txt)
                                        },
                                        onNewTabInteractionCompleted = { tid, pageUrl, details ->
                                            onNewTabInteractionCompleted?.invoke(tid, pageUrl, details)
                                        },
                                        onPageAnalyzed = { reportJson ->
                                            try {
                                                val report = TaskCategoryPlanner.parseAnalysisReport(reportJson)
                                                tabItem.stageBadge = report.detectedCategoryAr.take(8)
                                            } catch (e: Exception) {}
                                            onPageAnalyzed?.invoke(reportJson)
                                        },
                                        onPageActionMapGenerated = { mapJson ->
                                            onPageActionMapGenerated?.invoke(mapJson)
                                        },
                                        onPageActionStepExecuted = { idx, name, status ->
                                            onPageActionStepExecuted?.invoke(idx, name, status)
                                        },
                                        onMidPageInfoExtracted = { key, value ->
                                            onMidPageInfoExtracted?.invoke(key, value)
                                        },
                                        onTemplateGenerated = { tmplJson ->
                                            onTemplateGenerated?.invoke(tmplJson)
                                        },
                                        onLockerDetected = { lockerUrl, lockerId, isTriggered, count ->
                                            tabItem.detectedLockerUrl = lockerUrl
                                            tabItem.detectedLockerId = lockerId
                                            tabItem.isLockerTriggered = isTriggered
                                            onLockerDetectedWithCount?.invoke(lockerUrl, lockerId, isTriggered, count)
                                        },
                                        onLockerStatus = { status ->
                                            tabItem.lockerStatusMsg = status
                                        }
                                    ),
                                    "AndroidBridge"
                                )

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        val p = newProgress / 100f
                                        tabItem.progress = p
                                        tabItem.isLoading = newProgress < 100
                                        if (tabItem.id == activeTabId) {
                                            webProgress = p
                                            isPageLoading = newProgress < 100
                                        }
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                            tabItem.title = title
                                        }
                                    }

                                    override fun onPermissionRequest(request: PermissionRequest?) {
                                        try { request?.deny() } catch (e: Exception) {}
                                    }

                                    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                                        try { callback?.invoke(origin, true, false) } catch (e: Exception) {}
                                    }

                                    override fun onCreateWindow(
                                        view: WebView?,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message?
                                     ): Boolean {
                                         val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                         val context = view?.context ?: return false
                                         val popupRelay = WebView(context)
                                         var popupHandled = false

                                         fun openPopup(url: String?): Boolean {
                                             val uri = url?.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse) ?: return true
                                             if ((uri.scheme != "http" && uri.scheme != "https") || popupHandled) return true
                                             popupHandled = true
                                             openPopupInNewTab(uri.toString())
                                             popupRelay.post { popupRelay.destroy() }
                                             return true
                                         }

                                         popupRelay.settings.apply {
                                             javaScriptEnabled = true
                                             domStorageEnabled = true
                                             databaseEnabled = true
                                             userAgentString = view?.settings?.userAgentString ?: (if (isMobileUaMode) defaultMobileUa else defaultDesktopUa)
                                             mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                         }
                                         try {
                                             android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(popupRelay, true)
                                         } catch (e: Exception) {}

                                         popupRelay.webViewClient = object : WebViewClient() {
                                             override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                                                 openPopup(request?.url?.toString())

                                             @Deprecated("Deprecated in Java")
                                             override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                                                 openPopup(url)

                                             override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                 openPopup(url)
                                             }
                                         }
                                         transport.webView = popupRelay
                                         resultMsg.sendToTarget()
                                         popupRelay.postDelayed({
                                             if (!popupHandled) popupRelay.destroy()
                                         }, 15_000L)
                                         return true
                                     }
                                }

                                webViewClient = object : MainActivity.CustomWebViewClient(
                                    proxyUserProvider = { settings.proxyUser },
                                    proxyPassProvider = { settings.proxyPass },
                                    onPageStartedCallback = { view, url, _ ->
                                        tabItem.isLoading = true
                                        tabItem.lastError = null
                                        url?.let {
                                            tabItem.url = it
                                            if (tabItem.id == activeTabId) {
                                                currentDisplayUrl = it
                                                urlInput = it
                                                isPageLoading = true
                                            }
                                        }

                                        // WebRTC and anti-detection script injection
                                        val viewUa = view?.settings?.userAgentString.orEmpty()
                                        view?.evaluateJavascript(
                                            AutomationScriptBuilder.buildAntiDetectionScript(
                                                proxyIp = effectiveWebRtcIp,
                                                webrtcMode = settings.webrtcMode,
                                                timezone = extractedInfo.timezone,
                                                language = extractedInfo.language,
                                                latitude = extractedInfo.latitude,
                                                longitude = extractedInfo.longitude,
                                                userAgent = viewUa,
                                                fingerprintSeed = automationState.fingerprintSeed
                                            ),
                                            null
                                        )
                                        view?.evaluateJavascript(AutomationScriptBuilder.buildTimezoneScript(extractedInfo.timezone, extractedInfo.language), null)
                                        view?.evaluateJavascript(AutomationScriptBuilder.buildCanvasNoiseScript(), null)

                                        scripts.filter { it.enabled && it.timing == "before" }.forEach { s ->
                                            view?.evaluateJavascript(s.code, null)
                                        }
                                    },
                                    onPageFinishedCallback = { view, url ->
                                        tabItem.isLoading = false
                                        url?.let {
                                            tabItem.url = it
                                            if (tabItem.id == activeTabId) {
                                                currentDisplayUrl = it
                                                urlInput = it
                                                isPageLoading = false
                                            }
                                        }

                                        // Reinforced anti-detection script injection
                                        val finishUa = view?.settings?.userAgentString.orEmpty()
                                        view?.evaluateJavascript(
                                            AutomationScriptBuilder.buildAntiDetectionScript(
                                                proxyIp = effectiveWebRtcIp,
                                                webrtcMode = settings.webrtcMode,
                                                timezone = extractedInfo.timezone,
                                                language = extractedInfo.language,
                                                latitude = extractedInfo.latitude,
                                                longitude = extractedInfo.longitude,
                                                userAgent = finishUa,
                                                fingerprintSeed = automationState.fingerprintSeed
                                            ),
                                            null
                                        )

                                        // Automation only executes when automationState.isRunning == true!
                                        // When user is manually browsing/testing, ZERO automation scripts touch the webpage (Pure Chrome behavior)
                                        if (automationState.isRunning && tabItem.id == activeTabId) {
                                            val effClickText = activeClickText ?: automationState.activeClickText

                                            if (tabItem.isOfferTab) {
                                                // 1. Destination Offer Tab: This is where the CPA offer is filled and completed
                                                view?.evaluateJavascript(
                                                    AutomationScriptBuilder.buildSmartFormFillScript(
                                                        identity = identity,
                                                        categories = automationState.activeTaskCategories,
                                                        clickTexts = clickTexts,
                                                        activeClickText = effClickText,
                                                        extractedInfo = extractedInfo
                                                    ),
                                                    null
                                                )
                                                view?.evaluateJavascript(AutomationScriptBuilder.buildHumanBehaviorScript(), null)

                                                val keywords = listOf("thank you", "congratulations", "success", "completed", "verified", "confirmed", "survey", "reward")
                                                view?.evaluateJavascript(AutomationScriptBuilder.buildCompletionDetectorScript(keywords), null)

                                                // If this tab is an opened offer tab, simulate human browsing and interaction
                                                if (!tabItem.hasSimulatedInteraction && settings.offerClickAutoSimulateHuman) {
                                                    tabItem.hasSimulatedInteraction = true
                                                    view?.evaluateJavascript(
                                                        AutomationScriptBuilder.buildHumanInteractionOnOfferPageScript(
                                                            tabId = tabItem.id,
                                                            stayDurationSec = settings.offerClickStayDurationSec
                                                        ),
                                                        null
                                                    )
                                                }
                                            } else {
                                                // 2. Landing Page / Publisher's Website:
                                                // STRICT PROTECTION: The publisher's website is NEVER touched, filled, or modified!
                                                // Only activate the content locker and click the offer so it opens in a new tab:
                                                view?.evaluateJavascript(
                                                    AutomationScriptBuilder.buildCpaLockerDetectorAndActivatorScript(
                                                        customId = settings.cpaLockerDefaultId.ifBlank { "1741238" },
                                                        forceTrigger = settings.cpaLockerAutoTrigger,
                                                        forceInjectIfMissing = settings.cpaLockerAutoInjectIfMissing
                                                    ),
                                                    null
                                                )
                                                view?.evaluateJavascript(
                                                    AutomationScriptBuilder.buildLockerOfferAutoClickScript(
                                                        clickTexts = clickTexts,
                                                        activeTargetText = effClickText,
                                                        selectionStrategy = settings.offerSelectionStrategy,
                                                        openInNewTab = settings.offerClickOpenInNewTab
                                                    ),
                                                    null
                                                )
                                            }
                                        }

                                        scripts.filter { it.enabled && it.timing == "after" }.forEach { s ->
                                            view?.evaluateJavascript(s.code, null)
                                        }
                                    },
                                    onErrorCallback = { _, request, error ->
                                        if (request?.isForMainFrame == true) {
                                            tabItem.isLoading = false
                                            if (tabItem.id == activeTabId) isPageLoading = false
                                            tabItem.lastError = error?.description?.toString() ?: "ERR_CONNECTION_REFUSED"
                                        }
                                    }
                                ) {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val uri = request?.url ?: return false
                                        val scheme = uri.scheme?.lowercase() ?: ""

                                        if (MainActivity.CustomWebViewClient.isWebRtcStunTurnTraffic(uri, request.isForMainFrame)) {
                                            return true
                                        }

                                        if (scheme == "http" || scheme == "https") {
                                            return false
                                        }

                                        return try {
                                            if (scheme == "tel" || scheme == "mailto" || scheme == "sms") {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                view?.context?.startActivity(intent)
                                                true
                                            } else {
                                                true
                                            }
                                        } catch (e: Exception) {
                                            true
                                        }
                                    }
                                }

                                if (tabItem.url.isNotBlank() && tabItem.url != "about:blank") {
                                    loadUrl(tabItem.url)
                                } else {
                                    loadUrl("about:blank")
                                }
                                tabItem.webView = this
                            }
                        },
                        update = { webView ->
                            tabItem.webView = webView
                            if (isThisTabActive) {
                                webView.visibility = View.VISIBLE
                                webView.onResume()
                            } else {
                                webView.visibility = View.GONE
                                webView.onPause()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Standby Overlay when browser is idle and no page is loaded in active tab
            if (!automationState.isRunning && (currentDisplayUrl.isBlank() || currentDisplayUrl == "about:blank")) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CpaBg.copy(alpha = 0.96f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CpaCardElevated)
                                .border(1.dp, CpaBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WebAsset,
                                contentDescription = null,
                                tint = CpaPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "SMART MULTI-TAB BROWSER",
                            color = CpaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Multi-Tab isolation active. All automated scripts and 10s perception engine interact exclusively with the currently opened tab.",
                            color = CpaTextDim,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val target = "https://browserleaks.com/ip"
                                    urlInput = target
                                    currentDisplayUrl = target
                                    activeTab.url = target
                                    activeTab.webView?.loadUrl(target)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Check BrowserLeaks", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val target = "https://api.ipify.org"
                                    urlInput = target
                                    currentDisplayUrl = target
                                    activeTab.url = target
                                    activeTab.webView?.loadUrl(target)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CpaCardElevated),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Check Ipify", color = CpaText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
