package com.example.data.model

data class AppSettings(
    val waitBetweenTasks: Int = 5,
    val cpaUserId: String = "",
    val cpaApiKey: String = "",
    val captchaProvider: String = "none", // none, 2captcha, anticaptcha, capsolver, deathbycaptcha
    val captchaApiKey: String = "",
    val proxyType: String = "none", // none, http, https, socks4, socks5
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyUser: String = "",
    val proxyPass: String = "",
    val proxyAutoRotate: Boolean = false,
    val proxyListUrl: String = "",
    val webrtcMode: String = "spoof", // spoof, disabled, real
    val webrtcCustomIp: String = "",
    val forceProxyDns: Boolean = true, // Force DNS resolution through proxy & prevent DNS leaks
    val proxyEnabled: Boolean = true, // Master switch: when false, proxies are disabled & normal direct local connection is used
    val cpaLockerAutoTrigger: Boolean = true, // Auto-trigger call_locker() and polyfill document.write on locker detection
    val cpaLockerDefaultId: String = "1783346", // Default Locker ID if custom or missing
    val cpaLockerAutoInjectIfMissing: Boolean = false, // Automatically inject locker script if not found on page
    val offerClickOpenInNewTab: Boolean = true, // When offer clicked in locker, automatically open in new tab and switch
    val offerClickStayDurationSec: Int = 15, // Stay duration on the new offer tab simulating real interaction
    val offerClickAutoSimulateHuman: Boolean = true, // Auto scroll and simulate human activity on the newly opened offer tab
    val offerSelectionStrategy: String = "priority", // "priority", "first", "random"
    val cycleIntervalMinSec: Int = 15, // الحد الأدنى لفاصل دورة الحلقة التكرارية (بين 15 و 25 ثانية)
    val cycleIntervalMaxSec: Int = 25  // الحد الأقصى لفاصل دورة الحلقة التكرارية
)
