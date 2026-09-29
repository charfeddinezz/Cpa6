package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.AutomationState
import com.example.data.model.CampaignStat
import com.example.data.model.EmailItem
import com.example.data.model.ExtractedInfo
import com.example.data.model.GeneratedIdentity
import com.example.data.model.LogEntry
import com.example.data.model.ProxyItem
import com.example.data.model.ScriptItem
import com.example.data.model.TaskEntity
import com.example.service.AutomationScriptBuilder
import com.example.service.ExtractedPlanResult
import com.example.service.IdentityService
import com.example.service.TaskCategoryPlanner
import com.example.util.WebProxyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.UUID

enum class ScreenTab(val id: String, val title: String, val titleAr: String = "") {
    TASKS("tasks", "Tasks", "المهام"),
    BROWSER("browser", "Browser", "المتصفح"),
    TEMPLATES("templates", "Templates", "قوالب العمل"),
    CLICK("click", "Offer Click", "شاشة النقرة"),
    INFO("info", "Identity", "الهوية"),
    PROXIES("proxies", "Proxies", "البروكسي"),
    SCRIPTS("scripts", "Scripts", "السكربتات"),
    EMAILS("emails", "Emails", "الإيميلات"),
    STATS("stats", "Analytics", "الإحصائيات"),
    SETTINGS("settings", "Settings", "الإعدادات"),
    LOGS("logs", "Logs", "السجلات")
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val emailDao = db.emailDao()
    private val scriptDao = db.scriptDao()
    private val leadLogDao = db.leadLogDao()
    private val proxyDao = db.proxyDao()
    private val offerClickDao = db.offerClickDao()
    private val workTemplateDao = db.workTemplateDao()

    private var proxySequenceIndex: Int = 0

    private val prefs = application.getSharedPreferences("cpa_automator_prefs", Context.MODE_PRIVATE)

    // Navigation Tab
    private val _currentTab = MutableStateFlow(ScreenTab.TASKS)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Automation State
    private val _automationState = MutableStateFlow(AutomationState())
    val automationState: StateFlow<AutomationState> = _automationState.asStateFlow()

    // Logs
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Extracted Geo Info
    private val _extractedInfo = MutableStateFlow(ExtractedInfo())
    val extractedInfo: StateFlow<ExtractedInfo> = _extractedInfo.asStateFlow()

    // Generated Identity
    private val _identity = MutableStateFlow(IdentityService.generateIdentity())
    val identity: StateFlow<GeneratedIdentity> = _identity.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Flows from DB
    val tasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emails: StateFlow<List<EmailItem>> = emailDao.getAllEmails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emailCount: StateFlow<Int> = emailDao.getEmailCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val proxies: StateFlow<List<ProxyItem>> = proxyDao.getAllProxies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proxyCount: StateFlow<Int> = proxyDao.getProxyCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val scripts: StateFlow<List<ScriptItem>> = scriptDao.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<List<CampaignStat>> = leadLogDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offerClickItems: StateFlow<List<com.example.data.model.OfferClickItem>> = offerClickDao.getAllClickItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledClickItems: StateFlow<List<com.example.data.model.OfferClickItem>> = offerClickDao.getEnabledClickItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workTemplates: StateFlow<List<com.example.data.model.WorkTemplateEntity>> = workTemplateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Browser navigation event channel
    private val _browserCommand = MutableStateFlow<BrowserCommand?>(null)
    val browserCommand: StateFlow<BrowserCommand?> = _browserCommand.asStateFlow()

    // Completion callback flag from Smart Mode
    private var completionReceivedForCurrentTask: Boolean = false

    private var automationJob: Job? = null

    init {
        addLog("info", "CPA Automator initialized and ready.")
        val initSettings = _settings.value
        if (initSettings.proxyEnabled) {
            val initPort = initSettings.proxyPort.toIntOrNull()
            WebProxyManager.applyProxy(application, true, initSettings.proxyHost, initPort, initSettings.proxyType, initSettings.proxyUser, initSettings.proxyPass) { success, msg ->
                addLog(if (success) "info" else "warning", "[ProxyController] $msg")
            }
        } else {
            WebProxyManager.clearProxy(application) { _, _ ->
                addLog("info", "🌐 [الاتصال المحلي المباشر]: نظام البروكسي مغلق افتراضياً، الاعتماد على الاتصال المحلي العادي.")
            }
        }
        refreshGeoInfo()
        viewModelScope.launch(Dispatchers.IO) {
            val allTasks = taskDao.getEnabledTasks()
            val hasCtc = allTasks.any { it.url.contains("consumertestconnect") }
            if (!hasCtc) {
                val ctcTask = TaskEntity(
                    id = "task_ctc_100gc",
                    name = "ConsumerTestConnect ($100 GC)",
                    url = "https://consumertestconnect.com/ctc-100gcsweep",
                    referer = "https://www.google.com",
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    mode = "mode1",
                    repeatCount = 3,
                    browserDuration = 45,
                    categories = "Email Submit, Terms Agreement, Survey / Quiz, Lead Gen Form, Skip Upsells, Confirmation",
                    completionKeywords = "thank you, congratulations, success, confirmed, sweepstakes, completed",
                    enabled = true
                )
                taskDao.insertTask(ctcTask)
                addLog("info", "Loaded ConsumerTestConnect ($100 GC) optimized automation plan.")
            }

            // Ensure GDFQO Blogspot Offer Landing Bridge task exists (Priority #1)
            val fullTaskList = taskDao.getAllTasksList()
            if (fullTaskList.none { it.url.contains("gdfqo.blogspot.com") }) {
                val blogspotTask = TaskEntity(
                    id = "task_gdfqo_blogspot",
                    name = "GDFQO Offer Landing Bridge (Priority #1)",
                    url = "https://gdfqo.blogspot.com",
                    referer = "https://www.google.com",
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    mode = "mode1",
                    repeatCount = 5,
                    browserDuration = 60,
                    categories = "Offer Click, Survey / Quiz, Email Submit, Lead Gen Form, Skip Upsells, Confirmation",
                    completionKeywords = "thank you, congratulations, success, confirmed, reward, sweepstakes, completed",
                    enabled = true
                )
                taskDao.insertTask(blogspotTask)
                addLog("info", "Loaded GDFQO Landing Bridge task with Priority #1 Offer Click.")
            }

            // Ensure BrowserLeaks IP & WebRTC verification task exists
            if (fullTaskList.none { it.url.contains("browserleaks.com/ip") }) {
                val browserLeaksTask = TaskEntity(
                    id = "task_browserleaks_ip",
                    name = "BrowserLeaks IP & WebRTC Audit",
                    url = "https://browserleaks.com/ip",
                    referer = "https://www.google.com",
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    mode = "mode1",
                    repeatCount = 1,
                    browserDuration = 30,
                    categories = "Proxy Audit, WebRTC Leak Test, Fingerprint Validation",
                    completionKeywords = "WebRTC, IP Address, Leak Test",
                    enabled = true
                )
                taskDao.insertTask(browserLeaksTask)
                addLog("info", "Loaded BrowserLeaks IP & WebRTC audit task.")
            }

            // Ensure Canvas Fingerprint Noise script is active in scripts registry
            val activeScripts = scriptDao.getActiveScripts()
            if (activeScripts.none { it.id == "script_canvas_noise" }) {
                val canvasScript = ScriptItem(
                    id = "script_canvas_noise",
                    name = "Canvas Fingerprint Randomized Noise",
                    timing = "before",
                    execMode = "sequential",
                    code = AutomationScriptBuilder.buildCanvasNoiseScript(),
                    enabled = true,
                    isSystemPreset = true
                )
                scriptDao.insertScript(canvasScript)
                addLog("info", "Canvas API randomized noise anti-fingerprinting script registered.")
            }

            // Ensure Offer Click texts are seeded if table is currently empty
            val clickItems = offerClickDao.getAllClickItemsList()
            if (clickItems.isEmpty()) {
                val defaults = listOf(
                    com.example.data.model.OfferClickItem(text = "Get \$1000 Walmart gift card", enabled = true, orderIndex = 0, tagOrNote = "Walmart $1000 GC Offer"),
                    com.example.data.model.OfferClickItem(text = "Claim \$750 Cash App Reward", enabled = true, orderIndex = 1, tagOrNote = "Cash App Reward"),
                    com.example.data.model.OfferClickItem(text = "Get \$500 Amazon Gift Card", enabled = true, orderIndex = 2, tagOrNote = "Amazon $500 Sweep"),
                    com.example.data.model.OfferClickItem(text = "Win \$100 Target Gift Card", enabled = true, orderIndex = 3, tagOrNote = "Target Gift Card"),
                    com.example.data.model.OfferClickItem(text = "Claim \$500 Apple Store Card", enabled = false, orderIndex = 4, tagOrNote = "Apple Store Voucher")
                )
                offerClickDao.insertClickItems(defaults)
                addLog("info", "🎯 [شاشة النقرة]: تم تجهيز نصوص النقرة الافتراضية.")
            }

            // Automatically populate proxy pool: ensure user's default US proxy exists
            val currentProxyCount = proxyDao.getProxyCountOnce()
            if (currentProxyCount == 0) {
                val userProxy = com.example.data.model.ProxyItem(
                    host = "185.100.232.75",
                    port = 9999,
                    type = "socks5",
                    username = "bqjcykpcvw-a2c12283-3e44-4012-b6c0-7a476bef9351",
                    password = "DKiwjdv40s2tG3Jk",
                    country = "US",
                    status = "working",
                    lastPingMs = 120L
                )
                proxyDao.insertProxy(userProxy)
                val targetUrl = _settings.value.proxyListUrl.ifBlank { IdentityService.DEFAULT_ASOCKS_URL }
                val result = IdentityService.fetchProxiesFromUrl(targetUrl, "socks5")
                if (result.isSuccess) {
                    val list = result.getOrNull() ?: emptyList()
                    if (list.isNotEmpty()) {
                        proxyDao.insertProxies(list)
                        addLog("success", "Loaded ${list.size} Asocks US proxies into pool. Default active: 185.100.232.75:9999")
                    }
                }
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun addLog(level: String, message: String, taskName: String? = null) {
        val entry = LogEntry(level = level, message = message, taskName = taskName)
        _logs.update { listOf(entry) + it.take(250) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // --- Task Operations ---

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.insertTask(task)
            addLog("info", "Task saved: ${task.name}")
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(task)
            addLog("warning", "Task deleted: ${task.name}")
        }
    }

    fun toggleTaskEnabled(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(enabled = !task.enabled)
            taskDao.updateTask(updated)
        }
    }

    // --- Email Pool Operations ---

    fun addEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            emailDao.insertEmails(listOf(EmailItem(email = trimmed)))
            addLog("info", "Added email: $trimmed")
        }
    }

    fun importEmailsBulk(raw: String) {
        val lines = raw.split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.contains("@") && it.contains(".") }
        if (lines.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            emailDao.insertEmails(lines.map { EmailItem(email = it) })
            addLog("success", "Imported ${lines.size} emails to pool.")
        }
    }

    fun generateTestEmails(count: Int = 10) {
        val domains = listOf("gmail.com", "outlook.com", "yahoo.com", "icloud.com")
        val generated = (1..count).map {
            val ident = IdentityService.generateIdentity()
            val domain = domains.random()
            "${ident.firstName.lowercase()}.${ident.lastName.lowercase()}${System.currentTimeMillis() % 1000}@$domain"
        }
        viewModelScope.launch(Dispatchers.IO) {
            emailDao.insertEmails(generated.map { EmailItem(email = it) })
            addLog("info", "Generated $count test emails into pool.")
        }
    }

    fun deleteEmail(email: EmailItem) {
        viewModelScope.launch(Dispatchers.IO) {
            emailDao.deleteEmail(email)
        }
    }

    fun clearAllEmails() {
        viewModelScope.launch(Dispatchers.IO) {
            emailDao.clearAllEmails()
            addLog("warning", "Cleared email pool.")
        }
    }

    // --- Proxy Pool Operations ---

    fun fetchProxiesFromUrl(
        url: String,
        protocol: String = "socks5",
        onResult: (Boolean, String, Int) -> Unit
    ) {
        viewModelScope.launch {
            val cleanUrl = url.trim()
            addLog("info", "Fetching proxies from URL: $cleanUrl")
            val result = IdentityService.fetchProxiesFromUrl(cleanUrl, protocol)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                withContext(Dispatchers.IO) {
                    proxyDao.insertProxies(list)
                }
                if (list.isNotEmpty()) {
                    val first = list.first()
                    val s = _settings.value
                    updateSettings(
                        s.copy(
                            proxyListUrl = cleanUrl,
                            proxyHost = first.host,
                            proxyPort = first.port.toString(),
                            proxyType = first.type,
                            proxyUser = first.username,
                            proxyPass = first.password
                        )
                    )
                    addLog("success", "Active proxy set to #${1} ${first.host}:${first.port} [${first.type.uppercase()}]")
                    refreshGeoInfo()
                }
                addLog("success", "Successfully loaded ${list.size} proxies from URL into pool.")
                onResult(true, "Successfully imported ${list.size} proxies", list.size)
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error fetching proxies"
                addLog("error", "Failed to fetch proxies: $err")
                onResult(false, err, 0)
            }
        }
    }

    fun importProxiesBulk(raw: String, protocol: String = "socks5", onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = raw.trim()
            val list = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                val res = IdentityService.fetchProxiesFromUrl(trimmed, protocol)
                res.getOrNull() ?: IdentityService.parseBulkProxies(trimmed, protocol)
            } else {
                IdentityService.parseBulkProxies(trimmed, protocol)
            }
            if (list.isNotEmpty()) {
                proxyDao.insertProxies(list)
                addLog("success", "Imported ${list.size} proxies into pool.")
                onResult?.invoke(list.size)
            } else {
                onResult?.invoke(0)
            }
        }
    }

    fun addSingleProxy(proxy: ProxyItem, makeActive: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            proxyDao.insertProxy(proxy)
            addLog("info", "Added proxy: ${proxy.host}:${proxy.port} [${proxy.type.uppercase()}]")
            if (makeActive) {
                withContext(Dispatchers.Main) {
                    setActiveProxy(proxy)
                }
            }
        }
    }

    fun deleteProxy(proxy: ProxyItem) {
        viewModelScope.launch(Dispatchers.IO) {
            proxyDao.deleteProxy(proxy)
            addLog("info", "Deleted proxy: ${proxy.host}:${proxy.port}")
        }
    }

    fun clearAllProxies() {
        viewModelScope.launch(Dispatchers.IO) {
            proxyDao.clearAllProxies()
            addLog("warning", "Cleared all proxies from pool.")
        }
    }

    fun setActiveProxy(proxy: ProxyItem) {
        val s = _settings.value.copy(
            proxyType = proxy.type,
            proxyHost = proxy.host,
            proxyPort = proxy.port.toString(),
            proxyUser = proxy.username,
            proxyPass = proxy.password
        )
        updateSettings(s)
        addLog("success", "Switched active proxy to ${proxy.host}:${proxy.port} (${proxy.type.uppercase()})")
        refreshGeoInfo()
    }

    fun testProxy(proxy: ProxyItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val diag = withContext(Dispatchers.IO) {
                IdentityService.testAndDetectProxy(
                    host = proxy.host,
                    port = proxy.port,
                    preferredType = proxy.type,
                    user = proxy.username,
                    pass = proxy.password,
                    timeoutMs = 14000
                )
            }
            val isWorking = diag.isWorking
            val exitIp = diag.exitIp
            val ping = diag.pingMs
            withContext(Dispatchers.IO) {
                if (isWorking) {
                    if (diag.protocol != proxy.type) {
                        proxyDao.updateProxyType(proxy.id, diag.protocol)
                    }
                    proxyDao.updateProxyFullDetails(
                        id = proxy.id,
                        status = "working",
                        ping = ping,
                        country = diag.country,
                        city = diag.city,
                        isp = diag.isp,
                        score = diag.qualityScore
                    )
                } else {
                    proxyDao.recordProxyFailure(proxy.id)
                }
            }
            if (isWorking) {
                val protoMsg = if (diag.protocol != proxy.type) " [بروتوكول: ${diag.protocol.uppercase()}]" else ""
                val msg = "متصل: $exitIp (${ping}ms) - ${diag.city}, ${diag.country}$protoMsg (جودة: ${diag.qualityScore}/100)"
                addLog("success", "✅ بروكسي ${proxy.host}:${proxy.port} يعمل بنجاح! $msg")
                onResult(true, msg)
            } else {
                addLog("error", "❌ بروكسي ${proxy.host}:${proxy.port} فشل: ${diag.errorMessage}")
                onResult(false, "فشل: ${diag.errorMessage}")
            }
        }
    }

    fun testProxyDetails(
        host: String,
        port: Int,
        type: String,
        user: String = "",
        pass: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val diag = withContext(Dispatchers.IO) {
                IdentityService.testAndDetectProxy(host, port, type, user, pass, timeoutMs = 14000)
            }
            if (diag.isWorking) {
                onResult(true, "يعمل: ${diag.exitIp} - ${diag.pingMs}ms [${diag.protocol.uppercase()}]")
            } else {
                onResult(false, "فشل: ${diag.errorMessage}")
            }
        }
    }

    fun testAllProxies(
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: (Int, Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { proxyDao.getAllProxiesList() }
            if (list.isEmpty()) {
                onComplete(0, 0)
                return@launch
            }
            addLog("info", "بدء فحص مجموعة البروكسيات (${list.size} بروكسي) بسرعة متوازية وذكاء اكتشاف البروتوكول...")
            val total = list.size
            val progressCount = java.util.concurrent.atomic.AtomicInteger(0)
            val workingCount = java.util.concurrent.atomic.AtomicInteger(0)
            val failedCount = java.util.concurrent.atomic.AtomicInteger(0)

            val semaphore = Semaphore(4)

            withContext(Dispatchers.IO) {
                val jobs = list.map { proxy ->
                    async {
                        semaphore.withPermit {
                            val diag = IdentityService.testAndDetectProxy(
                                host = proxy.host,
                                port = proxy.port,
                                preferredType = proxy.type,
                                user = proxy.username,
                                pass = proxy.password,
                                timeoutMs = 15000
                            )
                            val isWorking = diag.isWorking
                            val ping = diag.pingMs

                            if (isWorking) {
                                if (diag.protocol != proxy.type) {
                                    proxyDao.updateProxyType(proxy.id, diag.protocol)
                                }
                                proxyDao.updateProxyFullDetails(
                                    id = proxy.id,
                                    status = "working",
                                    ping = ping,
                                    country = diag.country,
                                    city = diag.city,
                                    isp = diag.isp,
                                    score = diag.qualityScore
                                )
                                workingCount.incrementAndGet()
                                addLog("success", "[#${progressCount.incrementAndGet()}/$total] ✅ ${proxy.host}:${proxy.port} [${diag.protocol.uppercase()}] ONLINE (${diag.exitIp}) - ${ping}ms | ${diag.country} (جودة: ${diag.qualityScore}★)")
                            } else {
                                proxyDao.recordProxyFailure(proxy.id)
                                failedCount.incrementAndGet()
                                addLog("warning", "[#${progressCount.incrementAndGet()}/$total] ❌ ${proxy.host}:${proxy.port} OFFLINE (${diag.errorMessage})")
                            }

                            val done = progressCount.get()
                            withContext(Dispatchers.Main) {
                                onProgress(done, total)
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }

            val finalWorking = workingCount.get()
            val finalFailed = failedCount.get()
            addLog("info", "اكتمل فحص البروكسيات: $finalWorking شغالة ✅ | $finalFailed معطلة ❌")
            onComplete(finalWorking, finalFailed)
        }
    }

    fun deleteFailedProxies(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = proxyDao.deleteFailedProxies()
            addLog("info", "تم حذف $deleted بروكسي معطل من القائمة.")
            withContext(Dispatchers.Main) {
                onResult?.invoke(deleted)
            }
        }
    }

    fun resetFailedProxies(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = proxyDao.resetFailedProxies()
            addLog("info", "تمت إعادة تعيين $count بروكسي إلى الحالة النشطة لإعادة الفحص.")
            withContext(Dispatchers.Main) {
                onComplete?.invoke(count)
            }
        }
    }

    fun autoSelectFastestProxy(onResult: ((ProxyItem?) -> Unit)? = null) {
        viewModelScope.launch {
            val best = withContext(Dispatchers.IO) {
                proxyDao.getBestWorkingProxy() ?: proxyDao.getWorkingProxies().minByOrNull { if (it.lastPingMs > 0) it.lastPingMs else 999999 }
            }
            if (best != null) {
                setActiveProxy(best)
                addLog("success", "تم تفعيل أفضل بروكسي ذكياً: ${best.host}:${best.port} [${best.type.uppercase()}] (بنق: ${best.lastPingMs}ms | جودة: ${best.score}★)")
            } else {
                addLog("warning", "لا توجد بروكسيات صالحة في القائمة حالياً. يرجى فحص البروكسيات أولاً.")
            }
            onResult?.invoke(best)
        }
    }

    fun getWorkingProxiesFormatted(): String {
        val list = proxies.value
        val working = list.filter { it.status == "working" }
        val targetList = if (working.isNotEmpty()) working else list
        return targetList.joinToString("\n") { p ->
            if (p.username.isNotBlank() && p.password.isNotBlank()) {
                "${p.host}:${p.port}:${p.username}:${p.password}"
            } else {
                "${p.host}:${p.port}"
            }
        }
    }

    // --- Scripts Operations ---

    fun saveScript(script: ScriptItem) {
        viewModelScope.launch(Dispatchers.IO) {
            scriptDao.insertScript(script)
            addLog("info", "Saved script: ${script.name}")
        }
    }

    fun toggleScript(script: ScriptItem) {
        viewModelScope.launch(Dispatchers.IO) {
            scriptDao.toggleScript(script.id, !script.enabled)
        }
    }

    fun deleteScript(script: ScriptItem) {
        viewModelScope.launch(Dispatchers.IO) {
            scriptDao.deleteScript(script)
            addLog("warning", "Deleted script: ${script.name}")
        }
    }

    // --- Offer Click Operations (شاشة النقرة - Priority #1) ---

    fun addOfferClickItem(text: String, tagOrNote: String = "") {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = offerClickDao.getMaxOrderIndex() ?: -1
            val item = com.example.data.model.OfferClickItem(
                text = text.trim(),
                enabled = true,
                orderIndex = maxOrder + 1,
                tagOrNote = tagOrNote.trim()
            )
            offerClickDao.insertClickItem(item)
            addLog("info", "🎯 [شاشة النقرة]: تمت إضافة نص نقرة جديد: '${item.text}' (الترتيب: #${item.orderIndex + 1})")
        }
    }

    fun updateOfferClickItem(item: com.example.data.model.OfferClickItem) {
        viewModelScope.launch(Dispatchers.IO) {
            offerClickDao.updateClickItem(item)
            addLog("info", "🎯 [شاشة النقرة]: تم تعديل نص النقرة: '${item.text}'")
        }
    }

    fun toggleOfferClickItem(id: Long, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            offerClickDao.toggleClickItem(id, enabled)
            val action = if (enabled) "تفعيل" else "غلق / إيقاف"
            addLog("info", "🎯 [شاشة النقرة]: تم $action نص النقرة بنجاح")
        }
    }

    fun deleteOfferClickItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = offerClickDao.getClickItemById(id)
            if (item != null) {
                offerClickDao.deleteClickItem(item)
                addLog("warning", "🎯 [شاشة النقرة]: تم حذف نص النقرة: '${item.text}'")
            }
        }
    }

    fun moveOfferClickItem(id: Long, direction: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = offerClickDao.getAllClickItemsList().sortedBy { it.orderIndex }
            val index = all.indexOfFirst { it.id == id }
            if (index == -1) return@launch
            val targetIndex = index + direction
            if (targetIndex in all.indices) {
                val current = all[index]
                val other = all[targetIndex]
                offerClickDao.updateOrderIndex(current.id, other.orderIndex)
                offerClickDao.updateOrderIndex(other.id, current.orderIndex)
                addLog("info", "🎯 [شاشة النقرة]: تم تغيير تسلسل النص '${current.text}'")
            }
        }
    }

    fun resetDefaultOfferClickItems() {
        viewModelScope.launch(Dispatchers.IO) {
            offerClickDao.clearAll()
            val defaults = listOf(
                com.example.data.model.OfferClickItem(text = "Get \$1000 Walmart gift card", enabled = true, orderIndex = 0, tagOrNote = "Walmart $1000"),
                com.example.data.model.OfferClickItem(text = "Claim \$750 Cash App Reward", enabled = true, orderIndex = 1, tagOrNote = "Cash App $750"),
                com.example.data.model.OfferClickItem(text = "Get \$500 Amazon Gift Card", enabled = true, orderIndex = 2, tagOrNote = "Amazon $500"),
                com.example.data.model.OfferClickItem(text = "Win \$100 Target Gift Card", enabled = true, orderIndex = 3, tagOrNote = "Target $100"),
                com.example.data.model.OfferClickItem(text = "Claim \$500 Apple Store Card", enabled = false, orderIndex = 4, tagOrNote = "Apple Card")
            )
            offerClickDao.insertClickItems(defaults)
            addLog("success", "🎯 [شاشة النقرة]: تمت استعادة نصوص النقرة الافتراضية بنجاح.")
        }
    }

    fun onOfferClicked(text: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            offerClickDao.recordClickByText(text)
            _automationState.update {
                it.copy(
                    lastClickedOfferUrl = url,
                    activeClickText = text
                )
            }
            addLog("success", "🎯 [النقرة على العرض رقم 1]: تم النقر بنجاح على '$text' والتحول إلى: $url", _automationState.value.currentTaskName)
        }
    }

    fun onOfferClickedInNewTab(text: String, url: String, tabId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            offerClickDao.recordClickByText(text)
            _automationState.update {
                it.copy(
                    lastClickedOfferUrl = url,
                    activeClickText = text,
                    lockerOfferClicked = true,
                    lastOpenedNewTabId = tabId,
                    activeTabId = tabId,
                    currentUrl = url,
                    newTabActionStatus = "تم فتح موقع العرض في تبويب جديد (#$tabId) ونقل بيئة العمل إليه بنجاح 🚀"
                )
            }
            addLog("success", "🚀 [اللوكر ➔ التبويب الجديد]: تم النقر التلقائي على عرض اللوكر '$text' وفتح موقعه في تبويب جديد (#$tabId): $url. تم نقل بيئة العمل والتحكم التلقائي إليه فوراً!", _automationState.value.currentTaskName)
        }
    }

    fun onNewTabInteractionCompleted(tabId: String, currentUrl: String, details: String) {
        _automationState.update {
            it.copy(newTabActionStatus = "اكتملت المحاكاة في $tabId: $details")
        }
        addLog("success", "✅ [التعامل مع التبويب الجديد]: $details ($currentUrl)", _automationState.value.currentTaskName)
    }

    fun onLockerDetected(url: String, id: String, isTriggered: Boolean, offersCount: Int = 0) {
        _automationState.update {
            it.copy(
                lockerDetectedOnPage = true,
                lockerUrl = url,
                lockerId = id,
                lockerOffersFoundCount = offersCount
            )
        }
        addLog("info", "🛡️ [نظام اللوكر]: تم كشف لوكر في الصفحة (ID: $id) - العروض: $offersCount | الحالة: ${if (isTriggered) "نشط ومفعل" else "جاهز"}", _automationState.value.currentTaskName)
    }

    fun testLockerOfferClick(targetUrl: String = "https://gdfqo.blogspot.com", customLockerId: String? = null) {
        val lockerId = customLockerId ?: _settings.value.cpaLockerDefaultId.ifBlank { "1783346" }
        _automationState.update {
            it.copy(
                currentUrl = targetUrl,
                lockerId = lockerId,
                newTabActionStatus = "جاري فتح الصفحة واختبار اللوكر والنقر التلقائي إلى تبويب جديد..."
            )
        }
        _currentTab.value = ScreenTab.BROWSER
        _browserCommand.value = BrowserCommand.LoadUrl(targetUrl, "https://www.google.com", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        addLog("info", "🎯 [اختبار اللوكر والنقر]: جاري فحص الصفحة ($targetUrl) للتعامل التلقائي مع اللوكر والنقل إلى تبويب جديد...")
    }

    fun openOfferInNewTab(url: String, title: String = "Offer") {
        _browserCommand.value = BrowserCommand.OpenInNewTab(url, title)
        addLog("info", "🌐 [فتح تبويب جديد]: جاري فتح الرابط في تبويب منفصل: $url")
    }

    fun setActiveTabId(tabId: String) {
        _automationState.update { it.copy(activeTabId = tabId) }
    }

    fun onPageAnalyzed(reportJson: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val report = TaskCategoryPlanner.parseAnalysisReport(reportJson)
            val currentState = _automationState.value
            val currentCats = TaskCategoryPlanner.parseCategories(currentState.activeTaskCategories)
            val directive = TaskCategoryPlanner.deriveTacticalDirective(
                report = report,
                configuredCategories = currentCats,
                activeClickTarget = currentState.activeClickText
            )

            _automationState.update { state ->
                val updatedCats = if (report.detectedCategory.isNotBlank() && report.detectedCategory != "general") {
                    val det = report.detectedCategory
                    val isOfferClick = det == "offer_click"
                    val hadOfferClick = currentCats.any { it.equals("offer_click", ignoreCase = true) } ||
                        state.activeTaskCategories.contains("offer_click", ignoreCase = true)
                    if (isOfferClick && !hadOfferClick) {
                        state.activeTaskCategories
                    } else {
                        val reordered = (listOf(det) + currentCats.filter { !it.equals(det, ignoreCase = true) }).distinct()
                        reordered.joinToString(", ")
                    }
                } else {
                    state.activeTaskCategories
                }

                state.copy(
                    detectedPageCategory = report.detectedCategory,
                    detectedCategoryAr = report.detectedCategoryAr,
                    pageAnalysisSummary = report.summary,
                    activeTaskCategories = updatedCats,
                    lastAnalysisTime = System.currentTimeMillis(),
                    activeDirectiveTitle = directive.titleAr,
                    activeDirectiveReason = directive.reasonAr,
                    activeDirectiveStep = directive.actionPlanSteps.firstOrNull() ?: "",
                    directiveArchetype = directive.archetype,
                    analysisConfidence = report.confidence,
                    lockerDetectedOnPage = report.hasLocker || state.lockerDetectedOnPage
                )
            }

            addLog(
                "info",
                "🔍 [تحليل الصفحة]: رصد '${report.detectedCategoryAr}' (${report.detectedCategory}) - الثقة: ${report.confidence}% | الخطوة: ${report.recommendedNextAction}",
                _automationState.value.currentTaskName
            )
            addLog(
                "info",
                "🧭 [التوجيه التكتيكي الفوري]: ${directive.titleAr} [${directive.actionPlanSteps.size} خطوات]",
                _automationState.value.currentTaskName
            )

            // If confirmation/thank you detected, notify completion!
            if (report.isConfirmationPage && _automationState.value.isRunning) {
                completionReceivedForCurrentTask = true
                addLog("success", "🏆 [تأكيد التحويل]: تم رصد صفحة الشكر والإكمال بنجاح!", _automationState.value.currentTaskName)
            }
        }
    }

    fun testOfferClickInBrowser(text: String, landingUrl: String = "https://gdfqo.blogspot.com") {
        _automationState.update {
            it.copy(
                activeClickText = text,
                currentUrl = landingUrl
            )
        }
        _currentTab.value = ScreenTab.BROWSER
        _browserCommand.value = BrowserCommand.LoadUrl(landingUrl, "https://www.google.com", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
        addLog("info", "🎯 [شاشة النقرة]: جاري فتح صفحة الهبوط ($landingUrl) لاختبار النقر التلقائي على: '$text'")
    }

    // --- Identity Operations ---

    fun regenerateIdentity() {
        viewModelScope.launch(Dispatchers.Default) {
            val newIdent = IdentityService.generateIdentity(_extractedInfo.value.countryCode)
            _identity.value = newIdent
            addLog("info", "Generated new identity: ${newIdent.fullName} (${newIdent.email})")
        }
    }

    fun refreshGeoInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _settings.value
            if (s.proxyEnabled) {
                addLog("info", "Querying Geo location info via Proxy...")
                val port = s.proxyPort.toIntOrNull()
                WebProxyManager.applyProxy(getApplication(), true, s.proxyHost, port, s.proxyType, s.proxyUser, s.proxyPass) { success, msg ->
                    addLog(if (success) "info" else "warning", "[ProxyController] $msg")
                }
                val geo = IdentityService.fetchGeoInfo(s.proxyHost, port, s.proxyType, s.proxyUser, s.proxyPass)
                _extractedInfo.value = geo
                _automationState.update { it.copy(activeIp = geo.ip) }
                addLog(if (geo.isProxy) "success" else "warning", "Active IP: ${geo.ip} (${geo.city}, ${geo.country})")
            } else {
                addLog("info", "Querying Geo location info via Direct Local Network...")
                WebProxyManager.clearProxy(getApplication())
                val geo = IdentityService.fetchGeoInfo(null, null, "none", null, null)
                _extractedInfo.value = geo
                _automationState.update {
                    it.copy(
                        activeIp = geo.ip,
                        currentProxyInfo = "اتصال محلي مباشر (Direct)",
                        currentProxyIndex = 0
                    )
                }
                addLog("info", "🌐 [الاتصال المحلي المباشر]: عنوان IP المحلي الحالي: ${geo.ip} (${geo.country})")
            }
        }
    }

    // --- Settings Operations ---

    fun saveSettings(newSettings: AppSettings) = updateSettings(newSettings)

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        saveSettingsToPrefs(newSettings)
        if (newSettings.proxyEnabled) {
            val port = newSettings.proxyPort.toIntOrNull()
            WebProxyManager.applyProxy(getApplication(), true, newSettings.proxyHost, port, newSettings.proxyType, newSettings.proxyUser, newSettings.proxyPass) { success, msg ->
                addLog(if (success) "info" else "warning", "[ProxyController] $msg")
            }
        } else {
            WebProxyManager.clearProxy(getApplication()) { _, msg ->
                addLog("info", "[ProxyController] نظام البروكسي مغلق - $msg")
            }
        }
        addLog("info", "Settings updated.")
    }

    fun toggleProxyGlobalEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(proxyEnabled = enabled)
        _settings.value = updated
        saveSettingsToPrefs(updated)
        if (enabled) {
            val port = updated.proxyPort.toIntOrNull()
            WebProxyManager.applyProxy(getApplication(), true, updated.proxyHost, port, updated.proxyType, updated.proxyUser, updated.proxyPass) { success, msg ->
                addLog(if (success) "info" else "warning", "[ProxyController] $msg")
            }
            addLog("success", "🟢 [تفعيل نظام البروكسي]: تم تفعيل نظام البروكسي بنجاح وتوجيه حركة المرور.")
            refreshGeoInfo()
        } else {
            WebProxyManager.clearProxy(getApplication()) { _, msg ->
                addLog("info", "🚫 [غلق البروكسي بالكامل]: تم إيقاف وتعطيل نظام البروكسي كلياً، والاعتماد على الاتصال المحلي المباشر 100%.")
            }
            _automationState.update {
                it.copy(
                    currentProxyInfo = "اتصال محلي مباشر (Direct)",
                    currentProxyIndex = 0
                )
            }
            refreshGeoInfo()
        }
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            waitBetweenTasks = prefs.getInt("waitBetweenTasks", 5),
            cpaUserId = prefs.getString("cpaUserId", "") ?: "",
            cpaApiKey = prefs.getString("cpaApiKey", "") ?: "",
            captchaProvider = prefs.getString("captchaProvider", "none") ?: "none",
            captchaApiKey = prefs.getString("captchaApiKey", "") ?: "",
            proxyType = prefs.getString("proxyType", "socks5") ?: "socks5",
            proxyHost = prefs.getString("proxyHost", "185.100.232.75") ?: "185.100.232.75",
            proxyPort = prefs.getString("proxyPort", "9999") ?: "9999",
            proxyUser = prefs.getString("proxyUser", "bqjcykpcvw-a2c12283-3e44-4012-b6c0-7a476bef9351") ?: "bqjcykpcvw-a2c12283-3e44-4012-b6c0-7a476bef9351",
            proxyPass = prefs.getString("proxyPass", "DKiwjdv40s2tG3Jk") ?: "DKiwjdv40s2tG3Jk",
            proxyAutoRotate = prefs.getBoolean("proxyAutoRotate", false),
            proxyListUrl = prefs.getString("proxyListUrl", IdentityService.DEFAULT_ASOCKS_URL) ?: IdentityService.DEFAULT_ASOCKS_URL,
            webrtcMode = prefs.getString("webrtcMode", "spoof") ?: "spoof",
            webrtcCustomIp = prefs.getString("webrtcCustomIp", "") ?: "",
            forceProxyDns = prefs.getBoolean("forceProxyDns", true),
            proxyEnabled = prefs.getBoolean("proxyEnabled", true),
            cpaLockerAutoTrigger = prefs.getBoolean("cpaLockerAutoTrigger", true),
            cpaLockerDefaultId = prefs.getString("cpaLockerDefaultId", "1783346") ?: "1783346",
            cpaLockerAutoInjectIfMissing = prefs.getBoolean("cpaLockerAutoInjectIfMissing", false),
            offerClickOpenInNewTab = prefs.getBoolean("offerClickOpenInNewTab", true),
            offerClickStayDurationSec = prefs.getInt("offerClickStayDurationSec", 15),
            offerClickAutoSimulateHuman = prefs.getBoolean("offerClickAutoSimulateHuman", true),
            offerSelectionStrategy = prefs.getString("offerSelectionStrategy", "priority") ?: "priority",
            cycleIntervalMinSec = prefs.getInt("cycleIntervalMinSec", 15),
            cycleIntervalMaxSec = prefs.getInt("cycleIntervalMaxSec", 25)
        )
    }

    private fun saveSettingsToPrefs(s: AppSettings) {
        prefs.edit().apply {
            putInt("waitBetweenTasks", s.waitBetweenTasks)
            putString("cpaUserId", s.cpaUserId)
            putString("cpaApiKey", s.cpaApiKey)
            putString("captchaProvider", s.captchaProvider)
            putString("captchaApiKey", s.captchaApiKey)
            putString("proxyType", s.proxyType)
            putString("proxyHost", s.proxyHost)
            putString("proxyPort", s.proxyPort)
            putString("proxyUser", s.proxyUser)
            putString("proxyPass", s.proxyPass)
            putBoolean("proxyAutoRotate", s.proxyAutoRotate)
            putString("proxyListUrl", s.proxyListUrl)
            putString("webrtcMode", s.webrtcMode)
            putString("webrtcCustomIp", s.webrtcCustomIp)
            putBoolean("forceProxyDns", s.forceProxyDns)
            putBoolean("proxyEnabled", s.proxyEnabled)
            putBoolean("cpaLockerAutoTrigger", s.cpaLockerAutoTrigger)
            putString("cpaLockerDefaultId", s.cpaLockerDefaultId)
            putBoolean("cpaLockerAutoInjectIfMissing", s.cpaLockerAutoInjectIfMissing)
            putBoolean("offerClickOpenInNewTab", s.offerClickOpenInNewTab)
            putInt("offerClickStayDurationSec", s.offerClickStayDurationSec)
            putBoolean("offerClickAutoSimulateHuman", s.offerClickAutoSimulateHuman)
            putString("offerSelectionStrategy", s.offerSelectionStrategy)
            putInt("cycleIntervalMinSec", s.cycleIntervalMinSec)
            putInt("cycleIntervalMaxSec", s.cycleIntervalMaxSec)
            apply()
        }
    }

    fun updateCycleCountdown(countdown: Int, total: Int) {
        _automationState.update { it.copy(cycleCountdown = countdown, cycleTotalDuration = total) }
    }

    fun setForceProxyDns(enabled: Boolean) {
        val updated = _settings.value.copy(forceProxyDns = enabled)
        updateSettings(updated)
    }

    fun setWebRtcMode(mode: String) {
        val updated = _settings.value.copy(webrtcMode = mode)
        updateSettings(updated)
    }

    fun setWebRtcCustomIp(ip: String) {
        val updated = _settings.value.copy(webrtcCustomIp = ip)
        updateSettings(updated)
    }

    fun testCpaConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = _settings.value
            if (s.cpaUserId.isBlank() || s.cpaApiKey.isBlank()) {
                onResult(false, "Please provide CPA Grip User ID & API Key")
                return@launch
            }
            val ip = _extractedInfo.value.ip
            val res = IdentityService.checkLeadCPA(s.cpaUserId, s.cpaApiKey, ip)
            onResult(res.first, res.second)
            addLog(if (res.first) "success" else "info", "CPA Grip API Test: ${res.second}")
        }
    }

    fun clearStats() {
        viewModelScope.launch(Dispatchers.IO) {
            leadLogDao.clearAllLogs()
            _automationState.update { it.copy(completedThisSession = 0, leadsThisSession = 0) }
            addLog("info", "Campaign statistics cleared.")
        }
    }

    // --- Browser Navigation Controls ---

    fun navigateBrowser(url: String) {
        _currentTab.value = ScreenTab.BROWSER
        _browserCommand.value = BrowserCommand.LoadUrl(url, "https://www.google.com", "random")
    }

    fun reloadBrowser() {
        _browserCommand.value = BrowserCommand.Reload
    }

    fun goBackBrowser() {
        _browserCommand.value = BrowserCommand.GoBack
    }

    fun goForwardBrowser() {
        _browserCommand.value = BrowserCommand.GoForward
    }

    fun clearBrowserCommand() {
        _browserCommand.value = null
    }

    // Callback called from AndroidBridge when smart completion keyword detected
    fun notifyTaskCompleted(keyword: String, url: String) {
        completionReceivedForCurrentTask = true
        addLog("success", "Smart completion confirmed via keyword '$keyword' on $url")
    }

    // --- AUTOMATION ENGINE ---

    fun toggleAutomation() {
        if (_automationState.value.isRunning) {
            stopAutomation()
        } else {
            startAutomation()
        }
    }

    fun runTaskNow(task: TaskEntity) {
        if (automationJob?.isActive == true) {
            stopAutomation()
        }
        _currentTab.value = ScreenTab.BROWSER
        automationJob = viewModelScope.launch(Dispatchers.IO) {
            _automationState.update {
                it.copy(
                    isRunning = true,
                    phase = "preparing",
                    phaseDetail = "Preparing ${task.name}...",
                    loopCount = 1
                )
            }
            addLog("info", "Starting task directly: ${task.name}", task.name)
            try {
                runSingleTask(task)
            } catch (e: Exception) {
                addLog("error", "Task execution error: ${e.localizedMessage}", task.name)
            } finally {
                _automationState.update {
                    it.copy(
                        isRunning = false,
                        phase = "completed",
                        phaseDetail = "Task completed. Browser page preserved.",
                        currentTaskId = null,
                        currentTaskName = null,
                        currentUrl = null
                    )
                }
                // Maintain the browser page so the user can inspect the final conversion / thank you screen!
                addLog("info", "Task finished. Browser conversion screen preserved.")
            }
        }
    }

    /**
     * Extracts and optimizes a conversion funnel plan from ANY URL.
     */
    fun extractPlanForUrl(url: String): ExtractedPlanResult {
        return TaskCategoryPlanner.extractFunnelPlanFromUrl(url)
    }

    /**
     * Instantly creates a task from an extracted plan and immediately launches it in the browser.
     */
    fun createAndRunExtractedTask(rawUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val plan = TaskCategoryPlanner.extractFunnelPlanFromUrl(rawUrl)
            val newTask = TaskEntity(
                id = UUID.randomUUID().toString(),
                name = plan.detectedName,
                url = plan.targetUrl,
                referer = plan.recommendedReferer,
                userAgent = IdentityService.USER_AGENTS[0].value,
                mode = plan.recommendedMode,
                repeatCount = 1,
                browserDuration = plan.recommendedDuration,
                completionKeywords = plan.recommendedKeywords,
                categories = plan.categories.joinToString(", "),
                enabled = true
            )
            taskDao.insertTask(newTask)
            addLog("success", "Extracted & created task: ${newTask.name} [Funnel: ${plan.summary}]")
            withContext(Dispatchers.Main) {
                runTaskNow(newTask)
            }
        }
    }

    fun startAutomation() {
        if (automationJob?.isActive == true) return
        _currentTab.value = ScreenTab.BROWSER

        automationJob = viewModelScope.launch(Dispatchers.IO) {
            _automationState.update {
                it.copy(
                    isRunning = true,
                    phase = "preparing",
                    phaseDetail = "Smart Auto: Preparing intelligent campaign...",
                    loopCount = 0
                )
            }

            // 1. Intelligent Task Resolution: ensure we have optimized tasks ready
            var tasksToRun = taskDao.getEnabledTasks()
            if (tasksToRun.isEmpty()) {
                val allExisting = taskDao.getAllTasksList()
                if (allExisting.isNotEmpty()) {
                    addLog("info", "Smart Auto: Automatically enabling ${allExisting.size} existing tasks.")
                    allExisting.forEach { taskDao.updateTaskEnabled(it.id, true) }
                    tasksToRun = taskDao.getEnabledTasks()
                } else {
                    addLog("info", "Smart Auto: Initializing high-converting default CPA offer funnels...")
                    TaskCategoryPlanner.PRESET_OFFER_PLANS.take(2).forEach { preset ->
                        val newTask = TaskEntity(
                            id = UUID.randomUUID().toString(),
                            name = preset.name,
                            url = preset.url,
                            referer = "https://www.google.com",
                            userAgent = IdentityService.USER_AGENTS[0].value,
                            mode = preset.mode,
                            repeatCount = 1,
                            browserDuration = preset.duration,
                            completionKeywords = preset.completionKeywords,
                            categories = preset.categories,
                            enabled = true
                        )
                        taskDao.insertTask(newTask)
                    }
                    tasksToRun = taskDao.getEnabledTasks()
                    addLog("success", "Smart Auto: Initialized ${tasksToRun.size} intelligent CPA tasks.")
                }
            }

            // 2. Ensure Geo & Persona are ready
            if (_extractedInfo.value.ip.isBlank() || _extractedInfo.value.ip == "127.0.0.1") {
                _automationState.update { it.copy(phase = "fetching_geo", phaseDetail = "Detecting IP & Geo info...") }
                val s = _settings.value
                val port = s.proxyPort.toIntOrNull()
                val geo = IdentityService.fetchGeoInfo(s.proxyHost, port, s.proxyType, s.proxyUser, s.proxyPass)
                _extractedInfo.value = geo
                _automationState.update { it.copy(activeIp = geo.ip) }
                if (_identity.value.fullName.isBlank() || _identity.value.fullName == "John Doe") {
                    _identity.value = IdentityService.generateIdentity(geo.countryCode)
                }
            }

            addLog("info", "Smart Campaign running: ${tasksToRun.size} tasks queued.")

            try {
                var loop = 0
                while (_automationState.value.isRunning) {
                    loop++
                    _automationState.update { it.copy(loopCount = loop) }

                    val currentTasks = taskDao.getEnabledTasks()
                    if (currentTasks.isEmpty()) break

                    for (rawTask in currentTasks) {
                        if (!_automationState.value.isRunning) break

                        // Auto-optimize task if missing plan categories
                        val task = if (rawTask.categories.isBlank() || rawTask.completionKeywords.isBlank()) {
                            val plan = TaskCategoryPlanner.extractFunnelPlanFromUrl(rawTask.url)
                            val optimized = rawTask.copy(
                                name = if (rawTask.name.isBlank() || rawTask.name.startsWith("Task ")) plan.detectedName else rawTask.name,
                                categories = if (rawTask.categories.isBlank()) plan.categories.joinToString(", ") else rawTask.categories,
                                mode = if (rawTask.mode.isBlank()) plan.recommendedMode else rawTask.mode,
                                browserDuration = if (rawTask.browserDuration <= 0) plan.recommendedDuration else rawTask.browserDuration,
                                completionKeywords = if (rawTask.completionKeywords.isBlank()) plan.recommendedKeywords else rawTask.completionKeywords,
                                referer = if (rawTask.referer.isBlank()) plan.recommendedReferer else rawTask.referer
                            )
                            taskDao.updateTask(optimized)
                            optimized
                        } else {
                            rawTask
                        }

                        // Check repeat limit
                        if (task.repeatCount > 0 && task.completedRuns >= task.repeatCount) {
                            addLog("info", "Task ${task.name} reached repeat limit (${task.repeatCount}). Skipping.")
                            continue
                        }

                        runSingleTask(task)

                        // Wait between tasks: Cycle interval between 15 and 25 seconds
                        if (_automationState.value.isRunning) {
                            val minWait = _settings.value.cycleIntervalMinSec.coerceAtLeast(15)
                            val maxWait = _settings.value.cycleIntervalMaxSec.coerceAtLeast(minWait)
                            val waitSec = if (minWait < maxWait) kotlin.random.Random.nextInt(minWait, maxWait + 1) else minWait
                            for (w in waitSec downTo 1) {
                                if (!_automationState.value.isRunning) break
                                _automationState.update {
                                    it.copy(
                                        phase = "preparing",
                                        phaseDetail = "فاصل الدورة (${w} ث متبقية / 15-25 ث)...",
                                        cycleCountdown = w,
                                        cycleTotalDuration = waitSec
                                    )
                                }
                                delay(1000)
                            }
                        }
                    }

                    // Check if all tasks finished
                    val freshTasks = taskDao.getEnabledTasks()
                    val allDone = freshTasks.all { it.repeatCount > 0 && it.completedRuns >= it.repeatCount }
                    if (allDone) {
                        addLog("success", "All tasks finished their repeat quota!")
                        break
                    }
                }
            } catch (e: Exception) {
                addLog("error", "Automation error: ${e.localizedMessage}")
            } finally {
                _automationState.update {
                    it.copy(
                        isRunning = false,
                        phase = "completed",
                        phaseDetail = "Smart Automation finished. Conversion screen preserved.",
                        currentTaskId = null,
                        currentTaskName = null,
                        currentUrl = null,
                        activeTaskCategories = "",
                        activePlanSummary = ""
                    )
                }
                addLog("info", "Smart Automation campaign finished. Browser page preserved.")
            }
        }
    }

    private suspend fun prepareAndValidateProxy(taskName: String): ExtractedInfo = withContext(Dispatchers.IO) {
        var s = _settings.value
        if (!s.proxyEnabled) {
            WebProxyManager.clearProxy(getApplication())
            addLog("info", "🌐 [الاتصال المحلي المباشر]: نظام البروكسي مغلق بالكامل، الاعتماد على اتصال الإنترنت المحلي العادي.", taskName)
            val directGeo = IdentityService.fetchGeoInfo(null, null, "none", null, null)
            withContext(Dispatchers.Main) {
                _extractedInfo.value = directGeo
                _automationState.update {
                    it.copy(
                        activeIp = directGeo.ip,
                        currentProxyInfo = "اتصال محلي مباشر (Direct)",
                        currentProxyIndex = 0
                    )
                }
            }
            return@withContext directGeo
        }

        val hasConfiguredProxy = s.proxyHost.isNotBlank() && s.proxyPort.toIntOrNull() != null && s.proxyPort.toInt() > 0

        if (s.proxyAutoRotate || !hasConfiguredProxy) {
            val candidate = proxyDao.getBestWorkingProxy() ?: proxyDao.getNextWorkingProxy() ?: proxyDao.getNextProxy()
            if (candidate != null) {
                proxyDao.markProxyUsed(candidate.id)
                s = s.copy(
                    proxyType = candidate.type,
                    proxyHost = candidate.host,
                    proxyPort = candidate.port.toString(),
                    proxyUser = candidate.username,
                    proxyPass = candidate.password
                )
                withContext(Dispatchers.Main) { _settings.value = s }
                addLog("info", "🔄 [تدوير ذكي]: تم اختيار البروكسي: ${candidate.host}:${candidate.port} [${candidate.type.uppercase()}]", taskName)
            }
        }

        val port = s.proxyPort.toIntOrNull() ?: 0
        if (s.proxyHost.isBlank() || port <= 0) {
            WebProxyManager.clearProxy(getApplication())
            val directGeo = IdentityService.fetchGeoInfo()
            withContext(Dispatchers.Main) {
                _extractedInfo.value = directGeo
                _automationState.update { it.copy(activeIp = directGeo.ip) }
            }
            return@withContext directGeo
        }

        var activeHost = s.proxyHost
        var activePort = port
        var activeType = s.proxyType
        var activeUser = s.proxyUser
        var activePass = s.proxyPass

        addLog("info", "🔍 [ذكاء البروكسي]: جاري التحقق السريع من استجابة البروكسي $activeHost:$activePort...", taskName)
        var diag = IdentityService.testAndDetectProxy(activeHost, activePort, activeType, activeUser, activePass, timeoutMs = 5000)

        if (!diag.isWorking) {
            addLog("warning", "⚠️ [ذكاء البروكسي]: البروكسي ($activeHost:$activePort) لم يستجب (${diag.errorMessage}). جاري البحث والتحويل التلقائي لبديل موثوق...", taskName)

            val fallbacks = proxyDao.getWorkingProxies().filterNot { it.host == activeHost && it.port == activePort }
            var recovered = false

            for (fb in fallbacks) {
                addLog("info", "⚡ [محاولة بديل]: فحص البروكسي ${fb.host}:${fb.port}...", taskName)
                val fbDiag = IdentityService.testAndDetectProxy(fb.host, fb.port, fb.type, fb.username, fb.password, timeoutMs = 4500)
                if (fbDiag.isWorking) {
                    activeHost = fb.host
                    activePort = fb.port
                    activeType = fbDiag.protocol
                    activeUser = fb.username
                    activePass = fb.password
                    diag = fbDiag
                    recovered = true

                    proxyDao.recordProxySuccess(fb.id)
                    s = s.copy(
                        proxyType = activeType,
                        proxyHost = activeHost,
                        proxyPort = activePort.toString(),
                        proxyUser = activeUser,
                        proxyPass = activePass
                    )
                    withContext(Dispatchers.Main) { _settings.value = s }
                    addLog("success", "✅ [ذكاء البروكسي]: نجح التحويل التلقائي إلى البروكسي البديل: $activeHost:$activePort (${fbDiag.exitIp} | بنق: ${fbDiag.pingMs}ms)", taskName)
                    break
                } else {
                    proxyDao.recordProxyFailure(fb.id)
                }
            }

            if (!recovered) {
                addLog("error", "❌ [ذكاء البروكسي]: لم يتم العثور على بديل شغال. الاستمرار مع إعدادات البروكسي الحالية.", taskName)
            }
        }

        WebProxyManager.applyProxy(getApplication(), true, activeHost, activePort, activeType, activeUser, activePass) { success, msg ->
            addLog(if (success) "info" else "warning", "[ProxyController] $msg", taskName)
        }

        val geo = if (diag.isWorking && diag.exitIp.isNotBlank()) {
            ExtractedInfo(
                ip = diag.exitIp,
                country = if (diag.country == "US") "United States" else diag.country,
                countryCode = diag.country,
                city = diag.city.ifBlank { "New York" },
                region = "",
                street = "",
                postalCode = "10001",
                timezone = "America/New_York",
                language = "en-${diag.country}",
                currency = if (diag.country == "US") "USD" else "EUR",
                isp = diag.isp.ifBlank { "$activeHost:$activePort" },
                org = diag.isp,
                latitude = 40.7128,
                longitude = -74.0060,
                isProxy = true
            )
        } else {
            IdentityService.fetchGeoInfo(activeHost, activePort, activeType, activeUser, activePass)
        }

        withContext(Dispatchers.Main) {
            _extractedInfo.value = geo
            _automationState.update { it.copy(activeIp = geo.ip) }
        }
        addLog("info", "🌐 IP المتصل: ${geo.ip} (${geo.city}, ${geo.country}) [مزود الخدمة: ${geo.isp}]", taskName)
        return@withContext geo
    }

    private suspend fun runSingleTask(task: TaskEntity) {
        taskDao.updateTaskStatus(task.id, "running")
        val currentRunNumber = task.completedRuns + 1
        val parsedCats = TaskCategoryPlanner.parseCategories(task.categories)
        val planSummary = TaskCategoryPlanner.formatPlanSummary(parsedCats)
        
        // 0. Work Template resolution if attached
        var templateStepsJson: String? = null
        if (!task.workTemplateId.isNullOrBlank()) {
            val tmpl = workTemplateDao.getTemplateById(task.workTemplateId)
            if (tmpl != null) {
                templateStepsJson = tmpl.stepsJson
                _automationState.update { it.copy(activeWorkTemplateName = tmpl.name) }
                addLog("info", "📋 [قالب العمل]: تفعيل قالب '${tmpl.name}' للمهمة", task.name)
            }
        } else {
            _automationState.update { it.copy(activeWorkTemplateName = "قالب العمل التلقائي الذكي") }
        }

        _automationState.update {
            it.copy(
                currentTaskId = task.id,
                currentTaskName = task.name,
                currentUrl = task.url,
                activeTaskCategories = task.categories,
                activePlanSummary = planSummary,
                currentRepeatIndex = currentRunNumber,
                totalRepeats = task.repeatCount,
                phase = "preparing",
                phaseDetail = "تكرار $currentRunNumber/${task.repeatCount} | Funnel: $planSummary"
            )
        }
        addLog("info", "🔁 === بدء التكرار $currentRunNumber من ${task.repeatCount} لمهمة: ${task.name} ===", task.name)

        // 1. Complete Browser Cache & Storage Purge between repetitions
        if (task.clearCacheOnRepeat) {
            _automationState.update { it.copy(phase = "preparing", phaseDetail = "🧹 تنظيف وحذف كاش المتصفح والكوكيز بالكامل...") }
            _browserCommand.value = BrowserCommand.ClearCacheAndStorage
            delay(650)
            _automationState.update { it.copy(cacheWipeStatus = "تم مسح الكاش والتخزين بالكامل") }
            addLog("success", "🧹 [حذف الكاش بالكامل]: تم مسح الذاكرة المؤقتة، الكوكيز، والتخزين المحلي لضمان جلسة جديدة معزولة 100%.", task.name)
        }

        // 2. Sequential Proxy Rotation between repetitions (if proxy enabled globally)
        val allProxies = proxyDao.getAllProxiesList()
        if (_settings.value.proxyEnabled && task.sequentialProxyRotation && allProxies.isNotEmpty()) {
            val chosenProxy = allProxies[proxySequenceIndex % allProxies.size]
            proxySequenceIndex++
            var s = _settings.value.copy(
                proxyType = chosenProxy.type,
                proxyHost = chosenProxy.host,
                proxyPort = chosenProxy.port.toString(),
                proxyUser = chosenProxy.username,
                proxyPass = chosenProxy.password
            )
            _settings.value = s
            _automationState.update {
                it.copy(
                    currentProxyIndex = (proxySequenceIndex - 1) % allProxies.size + 1,
                    totalProxiesAvailable = allProxies.size,
                    currentProxyInfo = "${chosenProxy.host}:${chosenProxy.port} [${chosenProxy.type.uppercase()}]"
                )
            }
            addLog("info", "🔄 [تدوير البروكسي بالتسلسل]: تفعيل البروكسي (${(proxySequenceIndex - 1) % allProxies.size + 1}/${allProxies.size}) -> ${chosenProxy.host}:${chosenProxy.port}", task.name)
        } else if (!_settings.value.proxyEnabled) {
            _automationState.update {
                it.copy(
                    currentProxyIndex = 0,
                    totalProxiesAvailable = 0,
                    currentProxyInfo = "اتصال محلي مباشر (Direct)"
                )
            }
        }

        // 3. Proxy & Geo Info validation
        _automationState.update { it.copy(phase = "fetching_geo", phaseDetail = "فحص البروكسي و IP بالذكاء التلقائي...") }
        val geo = prepareAndValidateProxy(task.name)

        // 4. Brand New Identity & Email Generation
        _automationState.update { it.copy(phase = "generating_identity", phaseDetail = "توليد معلومات وهوية جديدة بالكامل...") }
        val poolEmail = emailDao.getNextEmail()
        if (poolEmail != null) {
            emailDao.deleteEmail(poolEmail)
            addLog("info", "Used email from pool: ${poolEmail.email}", task.name)
        }
        val identityData = IdentityService.generateIdentity(geo.countryCode, poolEmail?.email)
        _identity.value = identityData
        addLog("info", "👤 [معلومات وهوية جديدة]: ${identityData.fullName} | البريد: ${identityData.email} | الهاتف: ${identityData.phone}", task.name)

        // 5. Offer Click Priority #1 Sequential Selection
        val hasOfferClickCategory = parsedCats.any {
            it.equals("offer_click", ignoreCase = true) ||
            it.contains("offer", ignoreCase = true) ||
            it.contains("النقرة") ||
            it.contains("نقر")
        } ||
            task.categories.contains("offer_click", ignoreCase = true) ||
            task.categories.contains("النقرة", ignoreCase = true) ||
            task.categories.contains("نقر", ignoreCase = true)

        val nextClickTarget = if (hasOfferClickCategory) {
            val enabledClicks = offerClickDao.getEnabledClickItemsList()
            if (enabledClicks.isNotEmpty()) {
                val idx = _automationState.value.clickSequenceIndex % enabledClicks.size
                val chosen = enabledClicks[idx]
                _automationState.update {
                    it.copy(
                        activeClickText = chosen.text,
                        clickSequenceIndex = (idx + 1) % enabledClicks.size
                    )
                }
                addLog("info", "🎯 [النقرة على العرض]: تم تفعيلها لأنها مختارة في التصنيفات (#${idx + 1}): '${chosen.text}'", task.name)
                chosen.text
            } else {
                _automationState.update { it.copy(activeClickText = null) }
                null
            }
        } else {
            _automationState.update { it.copy(activeClickText = null) }
            addLog("info", "ℹ️ النقرة على العرض غير مفعلة (لم يتم اختيارها في تصنيفات هذه المهمة).", task.name)
            null
        }

        val isLockerCombo = TaskCategoryPlanner.isLockerOfferClickCombo(parsedCats)
        if (isLockerCombo) {
            addLog("info", "🔒🎯 [مسار اللوكر والنقر التلقائي]: تم رصد تصنيف Offer Click و Locker معاً! سيتولى المحرك تلقائياً انتظار ظهور اللوكر، النقر على العرض المناسب باستخدام نصوص شاشة Offer Click، ثم فتح موقعه تلقائياً في تبويب جديد والانتقال الفوري لإكمال العمل فيه.", task.name)
            _automationState.update {
                it.copy(newTabActionStatus = "في انتظار ظهور اللوكر والنقر التلقائي لفتح العرض في تبويب جديد...")
            }
        }

        // 6. Launch in Browser
        _automationState.update {
            it.copy(
                phase = "browser",
                phaseDetail = if (nextClickTarget != null) "Navigating to ${task.url} [Target: $nextClickTarget]..." else "Navigating to ${task.url}..."
            )
        }

        val ua = if (task.userAgent.equals("random", ignoreCase = true) ||
            task.userAgent.startsWith("random", ignoreCase = true) ||
            task.userAgent.isBlank()
        ) {
            val picked = when {
                task.userAgent.contains("mobile", ignoreCase = true) -> {
                    IdentityService.USER_AGENTS.filter { it.value.contains("Mobile") || it.value.contains("Android") || it.value.contains("iPhone") }.randomOrNull()?.value
                        ?: IdentityService.USER_AGENTS.random().value
                }
                task.userAgent.contains("desktop", ignoreCase = true) -> {
                    IdentityService.USER_AGENTS.filter { !it.value.contains("Mobile") && !it.value.contains("Android") && !it.value.contains("iPhone") }.randomOrNull()?.value
                        ?: IdentityService.USER_AGENTS.random().value
                }
                else -> IdentityService.USER_AGENTS.random().value
            }
            addLog("info", "🎲 [User-Agent عشوائي متجدد]: تم توليد وتطبيق UA جديد للجلسة: ${picked.take(45)}...", task.name)
            picked
        } else {
            task.userAgent
        }

        // Issue browser load command
        _browserCommand.value = BrowserCommand.LoadUrl(task.url, task.referer, ua)

        // If custom work template is attached, trigger template execution
        if (!templateStepsJson.isNullOrBlank()) {
            delay(1500)
            _browserCommand.value = BrowserCommand.ExecuteWorkTemplate(templateStepsJson)
        }

        // Execution based on Mode
        when (task.mode) {
            "mode1" -> runMode1(task)
            "mode2" -> runMode2(task, ua)
            "mode3" -> runMode3(task)
            else -> runMode1(task)
        }

        // 4. CPA Grip Lead Check
        val s = _settings.value
        if (s.cpaUserId.isNotBlank() && s.cpaApiKey.isNotBlank()) {
            _automationState.update { it.copy(phase = "checking_lead", phaseDetail = "Verifying conversion on CPA Grip...") }
            val leadResult = IdentityService.checkLeadCPA(s.cpaUserId, s.cpaApiKey, geo.ip)
            val isLead = leadResult.first
            leadLogDao.insertLog(
                CampaignStat(
                    taskId = task.id,
                    taskName = task.name,
                    ip = geo.ip,
                    country = geo.country,
                    leadDetected = isLead,
                    details = leadResult.second
                )
            )
            if (isLead) {
                _automationState.update { it.copy(leadsThisSession = it.leadsThisSession + 1) }
                addLog("success", "CONVERSION CONFIRMED: ${leadResult.second}", task.name)
            } else {
                addLog("info", "Conversion check: ${leadResult.second}", task.name)
            }
        } else {
            // Still record stat
            leadLogDao.insertLog(
                CampaignStat(
                    taskId = task.id,
                    taskName = task.name,
                    ip = geo.ip,
                    country = geo.country,
                    leadDetected = false,
                    details = "Task session completed."
                )
            )
        }

        // Increment task run count
        taskDao.incrementCompletedRuns(task.id)
        taskDao.updateTaskStatus(task.id, "completed")
        _automationState.update {
            it.copy(
                completedThisSession = it.completedThisSession + 1,
                phase = "completed",
                phaseDetail = "Completed run for ${task.name}. Page preserved."
            )
        }
        addLog("success", "Finished run for: ${task.name}. Conversion page preserved in Browser.", task.name)
    }

    private suspend fun runMode1(task: TaskEntity) {
        val duration = task.browserDuration.coerceAtLeast(5)
        for (sec in duration downTo 1) {
            if (!_automationState.value.isRunning) break
            _automationState.update {
                it.copy(
                    phase = "executing",
                    phaseDetail = "Browsing & filling forms... (${sec}s left)"
                )
            }
            delay(1000)
        }
    }

    private suspend fun runMode2(task: TaskEntity, ua: String) {
        val repeats = task.taskRepeatCount.coerceAtLeast(1)
        val durationPerRepeat = (task.taskDuration / repeats).coerceAtLeast(5)

        for (r in 1..repeats) {
            if (!_automationState.value.isRunning) break
            if (r > 1) {
                addLog("info", "Mode 2 repeat #$r: Reloading task in same session", task.name)
                _browserCommand.value = BrowserCommand.LoadUrl(task.url, task.referer, ua)
            }
            for (sec in durationPerRepeat downTo 1) {
                if (!_automationState.value.isRunning) break
                _automationState.update {
                    it.copy(
                        phase = "executing",
                        phaseDetail = "Mode 2 sub-run $r/$repeats (${sec}s left)"
                    )
                }
                delay(1000)
            }
        }
    }

    private suspend fun runMode3(task: TaskEntity) {
        completionReceivedForCurrentTask = false
        val maxWaitSec = 90
        val keywords = task.completionKeywords.split(",").map { it.trim() }
        addLog("info", "Mode 3 listening for completion keywords: ${keywords.take(4).joinToString(", ")}", task.name)

        for (sec in 1..maxWaitSec) {
            if (!_automationState.value.isRunning || completionReceivedForCurrentTask) break
            _automationState.update {
                it.copy(
                    phase = "executing",
                    phaseDetail = "Mode 3: Detecting completion keywords (${maxWaitSec - sec}s remaining)"
                )
            }
            delay(1000)
        }

        if (completionReceivedForCurrentTask) {
            addLog("success", "Smart Mode detected conversion page early!", task.name)
        } else {
            addLog("warning", "Smart Mode reached timeout waiting for keywords.", task.name)
        }
    }

    fun stopAutomation() {
        _automationState.update {
            it.copy(
                isRunning = false,
                phase = "idle",
                phaseDetail = "Stopping automation..."
            )
        }
        automationJob?.cancel()
        automationJob = null
        _browserCommand.value = BrowserCommand.ClearUrl
        addLog("warning", "Automation manually stopped by user.")
    }

    fun purgeSessionAndCache() {
        _browserCommand.value = BrowserCommand.ClearCacheAndStorage
        addLog("info", "Requested on-demand purge of WebView cache, cookies, and local storage.")
    }

    fun onPageActionMapGenerated(actionMapJson: String) {
        try {
            val count = org.json.JSONArray(actionMapJson).length()
            _automationState.update {
                it.copy(
                    pageActionMapJson = actionMapJson,
                    pageActionMapSummary = "$count خطوات مكتشفة لخريطة عمل الصفحة"
                )
            }
        } catch (_: Exception) {}
    }

    fun onPageActionStepExecuted(stepIndex: Int, stepName: String, status: String) {
        addLog("info", "⚡ [خريطة العمل]: خطوة ${stepIndex + 1}: $stepName ($status)")
    }

    fun onMidPageInfoExtracted(extractedKey: String, extractedValue: String) {
        _automationState.update { it.copy(lastExtractedMidPageText = "$extractedKey -> $extractedValue") }
        addLog("info", "🧠 [استخراج ذكي وتكيّف]: استخراج: '$extractedKey' | الإجابة: '$extractedValue'")
    }

    fun onTemplateGenerated(templateJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val obj = org.json.JSONObject(templateJson)
                val name = obj.optString("name", "قالب عمل تلقائي ذكي")
                val desc = obj.optString("description", "تم إنشاؤه تلقائياً بالذكاء الاصطناعي")
                val cat = obj.optString("targetCategory", "Smart Auto")
                val steps = obj.optJSONArray("steps")?.toString() ?: "[]"

                val template = com.example.data.model.WorkTemplateEntity(
                    id = "template_${System.currentTimeMillis()}",
                    name = name,
                    description = desc,
                    targetCategory = cat,
                    isAutoGenerated = true,
                    stepsJson = steps
                )
                workTemplateDao.insertTemplate(template)
                addLog("success", "✨ [صنع قالب تلقائي]: تم توليد وحفظ قالب العمل '${template.name}' بنجاح!")
            } catch (e: Exception) {
                addLog("error", "فشل تحليل وحفظ القالب التلقائي: ${e.message}")
            }
        }
    }

    fun requestGenerateTemplateFromPage() {
        _browserCommand.value = BrowserCommand.GenerateAutoTemplate
        addLog("info", "✨ جاري فحص عناصر المتصفح وتوليد قالب العمل التلقائي بالذكاء الاصطناعي...")
    }

    fun saveWorkTemplate(template: com.example.data.model.WorkTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            workTemplateDao.insertTemplate(template)
            addLog("success", "تم حفظ قالب العمل: ${template.name}")
        }
    }

    fun executeWorkTemplateInBrowser(template: com.example.data.model.WorkTemplateEntity) {
        _browserCommand.value = BrowserCommand.ExecuteWorkTemplate(template.stepsJson)
        addLog("info", "▶️ [تشغيل القالب في المتصفح]: تم إرسال أوامر القالب '${template.name}' للتنفيذ الفوري في المتصفح.")
    }

    fun deleteWorkTemplate(template: com.example.data.model.WorkTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            workTemplateDao.deleteTemplate(template)
            addLog("warning", "تم حذف قالب العمل: ${template.name}")
        }
    }
}

sealed class BrowserCommand {
    data class LoadUrl(val url: String, val referer: String?, val userAgent: String?) : BrowserCommand()
    object Reload : BrowserCommand()
    object GoBack : BrowserCommand()
    object GoForward : BrowserCommand()
    object ClearUrl : BrowserCommand()
    object ClearCacheAndStorage : BrowserCommand()
    object GenerateAutoTemplate : BrowserCommand()
    data class ExecuteWorkTemplate(val stepsJson: String) : BrowserCommand()
    data class OpenInNewTab(val url: String, val title: String = "Offer") : BrowserCommand()
}
