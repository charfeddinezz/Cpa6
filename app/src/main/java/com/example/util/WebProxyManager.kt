package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.util.concurrent.Executor

object WebProxyManager {

    private const val TAG = "WebProxyManager"

    @Volatile
    var isProxyActive: Boolean = false
        private set

    @Volatile
    var activeProxySummary: String = "Direct (No Proxy)"
        private set

    /**
     * Applies proxy settings to the Android System WebView globally using AndroidX WebKit ProxyController.
     * Supports SOCKS5, SOCKS4, HTTP, and HTTPS proxies with full authentication support.
     * 
     * CRITICAL SECURITY DIRECTIVE:
     * We NEVER call .addDirect() when a proxy is configured. Calling .addDirect() creates a fallback
     * chain that silently connects through the real network if the proxy takes a moment or challenges auth,
     * which leaks the real IP address. By omitting .addDirect(), WebView traffic is strictly locked
     * through the proxy.
     */
    /**
     * Applies proxy settings to the Android System WebView globally using AndroidX WebKit ProxyController.
     * Supports SOCKS5, SOCKS4, HTTP, and HTTPS proxies with full authentication support.
     * 
     * When enabled is false, proxy settings are completely purged and direct local connections are restored.
     */
    fun applyProxy(
        context: Context,
        enabled: Boolean,
        host: String?,
        port: Int?,
        type: String?,
        username: String? = null,
        password: String? = null,
        onApplied: ((Boolean, String) -> Unit)? = null
    ) {
        if (!enabled) {
            clearProxy(context, onApplied)
            return
        }

        val cleanHost = host?.trim().orEmpty()
        val cleanType = type?.trim()?.lowercase() ?: "none"
        val cleanUser = username?.trim().orEmpty()
        val cleanPass = password?.trim().orEmpty()

        if (cleanHost.isBlank() || port == null || port <= 0 || cleanType == "none" || cleanType == "direct") {
            clearProxy(context, onApplied)
            return
        }

        val isSocks = cleanType == "socks" || cleanType == "socks5"
        var localBridgePort = 0
        if (isSocks && cleanUser.isNotBlank() && cleanPass.isNotBlank()) {
            // Android WebView does NOT support SOCKS5 username/password authentication natively.
            // Start the local bridge on 127.0.0.1 to handle RFC 1928/1929 auth and remote DNS.
            localBridgePort = LocalSocks5HttpBridge.start(cleanHost, port, cleanUser, cleanPass)
            if (localBridgePort > 0) {
                Log.i(TAG, "Local SOCKS5 bridge active on port $localBridgePort for $cleanHost:$port")
            } else {
                Log.w(TAG, "Local SOCKS5 bridge failed to start, falling back to direct socks")
            }
        } else {
            LocalSocks5HttpBridge.stop()
        }

        activeProxySummary = if (localBridgePort > 0) {
            "SOCKS5://$cleanHost:$port (via local bridge :$localBridgePort)"
        } else {
            "${cleanType.uppercase()}://$cleanHost:$port"
        }
        isProxyActive = true

        // 1. Set global Java Authenticator for HTTP & SOCKS proxy authentication
        if (cleanUser.isNotBlank() && cleanPass.isNotBlank()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(cleanUser, cleanPass.toCharArray())
                }
            })
        }

        // 2. Set JVM System Networking Properties with Remote DNS Enforcement
        try {
            when (cleanType) {
                "socks", "socks5", "socks4" -> {
                    System.setProperty("socksProxyHost", cleanHost)
                    System.setProperty("socksProxyPort", port.toString())
                    // Force SOCKS 5 protocol to delegate DNS resolution directly to the proxy server
                    System.setProperty("socksProxyVersion", "5")
                    System.setProperty("socksNonProxyHosts", "localhost|127.0.0.1|[::1]")
                    if (cleanUser.isNotBlank() && cleanPass.isNotBlank()) {
                        System.setProperty("java.net.socks.username", cleanUser)
                        System.setProperty("java.net.socks.password", cleanPass)
                    }
                    System.clearProperty("http.proxyHost")
                    System.clearProperty("http.proxyPort")
                    System.clearProperty("https.proxyHost")
                    System.clearProperty("https.proxyPort")
                }
                "http", "https" -> {
                    System.setProperty("http.proxyHost", cleanHost)
                    System.setProperty("http.proxyPort", port.toString())
                    System.setProperty("https.proxyHost", cleanHost)
                    System.setProperty("https.proxyPort", port.toString())
                    if (cleanUser.isNotBlank() && cleanPass.isNotBlank()) {
                        System.setProperty("http.proxyUser", cleanUser)
                        System.setProperty("http.proxyPassword", cleanPass)
                        System.setProperty("https.proxyUser", cleanUser)
                        System.setProperty("https.proxyPassword", cleanPass)
                    }
                    System.clearProperty("socksProxyHost")
                    System.clearProperty("socksProxyPort")
                    System.clearProperty("socksProxyVersion")
                    System.clearProperty("socksNonProxyHosts")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting system proxy properties: ${e.message}")
        }

        // 3. Set WebView Proxy via AndroidX ProxyController (API 29+ / Chromium WebView)
        // Forces all DNS queries and HTTP/HTTPS/WebSocket traffic through the proxy endpoint
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val builder = ProxyConfig.Builder()

            if (localBridgePort > 0) {
                // Route WebView through local authenticated SOCKS5 HTTP bridge
                builder.addProxyRule("http://127.0.0.1:$localBridgePort")
                builder.addProxyRule("127.0.0.1:$localBridgePort")
                // Do NOT bypass 127.0.0.1 since the proxy is on localhost
                builder.addBypassRule("<-loopback>")
            } else {
                when (cleanType) {
                    "socks5", "socks" -> {
                        builder.addProxyRule("socks://$cleanHost:$port")
                    }
                    "socks4" -> {
                        builder.addProxyRule("socks4://$cleanHost:$port")
                    }
                    "http", "https" -> {
                        builder.addProxyRule("http://$cleanHost:$port")
                        builder.addProxyRule("$cleanHost:$port")
                    }
                    else -> {
                        builder.addProxyRule("http://$cleanHost:$port")
                        builder.addProxyRule("$cleanHost:$port")
                    }
                }
                builder.addBypassRule("localhost")
                builder.addBypassRule("127.0.0.1")
            }

            try {
                val proxyConfig = builder.build()
                val executor = Executor { command ->
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        command.run()
                    } else {
                        Handler(Looper.getMainLooper()).post(command)
                    }
                }
                ProxyController.getInstance().setProxyOverride(
                    proxyConfig,
                    executor,
                    Runnable {
                        Log.i(TAG, "WebView ProxyOverride successfully locked to: $cleanHost:$port [$cleanType] (bridge=$localBridgePort)")
                        onApplied?.invoke(true, "Proxy locked: $cleanHost:$port ($cleanType)")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setProxyOverride: ${e.message}", e)
                onApplied?.invoke(false, "Failed to apply WebView proxy: ${e.message}")
            }
        } else {
            Log.w(TAG, "PROXY_OVERRIDE is not supported on this WebView version")
            onApplied?.invoke(false, "PROXY_OVERRIDE not supported by current Android WebView")
        }
    }

    /**
     * Overload for applyProxy defaulting enabled to true.
     */
    fun applyProxy(
        context: Context,
        host: String?,
        port: Int?,
        type: String?,
        username: String? = null,
        password: String? = null,
        onApplied: ((Boolean, String) -> Unit)? = null
    ) {
        applyProxy(context, true, host, port, type, username, password, onApplied)
    }

    /**
     * Clears all proxy configurations and returns WebView to direct internet connection.
     * Prevents ERR_CONNECTION_REFUSED by explicitly applying a direct proxy configuration first,
     * clearing proxy overrides in Chromium, resetting the Authenticator, and safely stopping
     * the local SOCKS bridge only after the direct route is firmly established.
     */
    fun clearProxy(context: Context, onCleared: ((Boolean, String) -> Unit)? = null) {
        activeProxySummary = "Direct (No Proxy)"
        isProxyActive = false

        try {
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")
            System.clearProperty("socksProxyVersion")
            System.clearProperty("socksNonProxyHosts")
            System.clearProperty("java.net.socks.username")
            System.clearProperty("java.net.socks.password")
            System.clearProperty("http.proxyHost")
            System.clearProperty("http.proxyPort")
            System.clearProperty("http.proxyUser")
            System.clearProperty("http.proxyPassword")
            System.clearProperty("https.proxyHost")
            System.clearProperty("https.proxyPort")
            System.clearProperty("https.proxyUser")
            System.clearProperty("https.proxyPassword")
            Authenticator.setDefault(null)
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing system properties: ${e.message}")
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            val executor = Executor { command ->
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    command.run()
                } else {
                    Handler(Looper.getMainLooper()).post(command)
                }
            }
            try {
                // Step 1: Explicitly force direct connections in Chromium first
                val directConfig = ProxyConfig.Builder()
                    .addDirect()
                    .build()

                ProxyController.getInstance().setProxyOverride(
                    directConfig,
                    executor,
                    Runnable {
                        // Step 2: Now clear the proxy override so WebView cleanly uses the system default network
                        try {
                            ProxyController.getInstance().clearProxyOverride(
                                executor,
                                Runnable {
                                    Log.i(TAG, "WebView ProxyOverride successfully cleared, direct connection active.")
                                    LocalSocks5HttpBridge.stop()
                                    onCleared?.invoke(true, "Direct connection restored")
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed in clearProxyOverride: ${e.message}", e)
                            LocalSocks5HttpBridge.stop()
                            onCleared?.invoke(true, "Direct connection restored")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setDirectProxy: ${e.message}", e)
                try {
                    ProxyController.getInstance().clearProxyOverride(
                        executor,
                        Runnable {
                            LocalSocks5HttpBridge.stop()
                            onCleared?.invoke(true, "Direct connection restored")
                        }
                    )
                } catch (ex: Exception) {
                    LocalSocks5HttpBridge.stop()
                    onCleared?.invoke(false, "Failed to clear proxy override")
                }
            }
        } else {
            LocalSocks5HttpBridge.stop()
            onCleared?.invoke(true, "Direct connection restored")
        }
    }

    /**
     * Clears all WebView cache, cookies, local storage, databases, and session state on demand
     * to ensure that previous real-IP sessions do not leak information through cached assets.
     */
    fun clearAllSessionData(
        context: Context,
        webView: android.webkit.WebView? = null,
        onComplete: (() -> Unit)? = null
    ) {
        try {
            // 1. Clear WebView instance cache, history, and storage
            webView?.let { wv ->
                wv.clearCache(true)
                wv.clearFormData()
                wv.clearHistory()
                wv.clearSslPreferences()
                wv.evaluateJavascript(
                    """
                    try {
                        if (window.localStorage) window.localStorage.clear();
                        if (window.sessionStorage) window.sessionStorage.clear();
                        if ('caches' in window) {
                            caches.keys().then(function(names) {
                                for (var i = 0; i < names.length; i++) caches.delete(names[i]);
                            });
                        }
                    } catch(e) {}
                    """.trimIndent(),
                    null
                )
            }

            // 2. Clear Cookies
            val cookieManager = android.webkit.CookieManager.getInstance()
            cookieManager.removeAllCookies {
                cookieManager.flush()
            }
            cookieManager.removeSessionCookies(null)

            // 3. Clear HTML5 Web Storage (localStorage, WebSQL, IndexedDB)
            android.webkit.WebStorage.getInstance().deleteAllData()

            // 4. Clear Geolocation permissions
            android.webkit.GeolocationPermissions.getInstance().clearAll()

            // 5. Clear saved HTTP auth credentials in WebViewDatabase
            try {
                val db = android.webkit.WebViewDatabase.getInstance(context)
                db.clearHttpAuthUsernamePassword()
                @Suppress("DEPRECATION")
                db.clearFormData()
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing WebViewDatabase: ${e.message}")
            }

            Log.i(TAG, "WebProxyManager purged WebView cache, cookies, and local storage on demand.")
            onComplete?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Error in clearAllSessionData: ${e.message}", e)
            onComplete?.invoke()
        }
    }
}
