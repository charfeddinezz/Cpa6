package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emails")
data class EmailItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "proxies")
data class ProxyItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int,
    val type: String = "socks5", // http, socks4, socks5
    val username: String = "",
    val password: String = "",
    val country: String = "US",
    val city: String = "",
    val isp: String = "",
    val status: String = "active", // active, working, failed
    val lastPingMs: Long = 0L,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val score: Int = 100,
    val lastCheckedAt: Long = 0L,
    val lastUsedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scripts")
data class ScriptItem(
    @PrimaryKey val id: String,
    val name: String,
    val timing: String = "after", // "before", "after"
    val execMode: String = "sequential", // "sequential", "parallel"
    val code: String,
    val enabled: Boolean = true,
    val isSystemPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lead_logs")
data class CampaignStat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val taskName: String,
    val ip: String,
    val country: String,
    val leadDetected: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

@Entity(tableName = "offer_click_items")
data class OfferClickItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String, // e.g. "Get $1000 Walmart gift card" / "Get a $100 Nike Gift Card!"
    val enabled: Boolean = true, // User can close/disable specific texts
    val orderIndex: Int = 0, // Sequential execution order
    val clickCount: Int = 0, // Recorded click count
    val showCount: Int = 0, // Impressions: times selected as active target (for CTR)
    val lastClickedAt: Long = 0L,
    val lastShownAt: Long = 0L,
    val tagOrNote: String = "", // e.g. "Walmart $1000", "CashApp"
    val targetUrlFilter: String = "", // Optional domain or URL match
    val createdAt: Long = System.currentTimeMillis()
) {
    val ctr: Double get() = if (showCount <= 0) 0.5 else clickCount.toDouble() / (showCount + 1)
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val level: String = "info", // "info", "success", "warning", "error"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val taskName: String? = null
)

data class AutomationState(
    val phase: String = "idle", // "idle", "preparing", "fetching_geo", "generating_identity", "browser", "executing", "checking_lead", "completed"
    val phaseDetail: String = "Ready to start automation",
    val currentTaskId: String? = null,
    val currentTaskName: String? = null,
    val currentUrl: String? = null,
    val activeTaskCategories: String = "",
    val activePlanSummary: String = "",
    val activeClickText: String? = null,
    val lastClickedOfferUrl: String? = null,
    val clickSequenceIndex: Int = 0,
    val activeIp: String = "Not Connected",
    val isRunning: Boolean = false,
    val loopCount: Int = 0,
    val completedThisSession: Int = 0,
    val leadsThisSession: Int = 0,
    val activeTabId: String = "tab_1",
    val detectedPageCategory: String = "",
    val detectedCategoryAr: String = "",
    val pageAnalysisSummary: String = "",
    val lastAnalysisTime: Long = 0L,
    val reanalysisCountdown: Int = 10,
    val currentRepeatIndex: Int = 1,
    val totalRepeats: Int = 1,
    val currentProxyIndex: Int = 0,
    val totalProxiesAvailable: Int = 0,
    val currentProxyInfo: String = "",
    val cacheWipeStatus: String = "Clean",
    val activeWorkTemplateName: String? = null,
    val pageActionMapSummary: String = "",
    val pageActionMapJson: String = "[]",
    val lastExtractedMidPageText: String = "",
    val lockerDetectedOnPage: Boolean = false,
    val lockerUrl: String? = null,
    val lockerId: String? = null,
    val lockerOffersFoundCount: Int = 0,
    val lockerOfferClicked: Boolean = false,
    val lastOpenedNewTabId: String? = null,
    val newTabActionStatus: String = "",
    val activeDirectiveTitle: String = "",
    val activeDirectiveReason: String = "",
    val activeDirectiveStep: String = "",
    val directiveArchetype: String = "",
    val analysisConfidence: Int = 0,
    val cycleCountdown: Int = 0,
    val cycleTotalDuration: Int = 20,
    // ── Smart Brain awareness fields ──
    val brainNextAction: String = "",
    val brainReasonAr: String = "",
    val brainConfidence: Int = 0,
    val isPageBlocked: Boolean = false,
    val isCaptchaPresent: Boolean = false,
    val isPageLoading: Boolean = false,
    val stuckCount: Int = 0,
    val sessionQualityScore: Int = 70,
    val smartDecisionTitle: String = "",
    val fingerprintSeed: Long = 0L
)
