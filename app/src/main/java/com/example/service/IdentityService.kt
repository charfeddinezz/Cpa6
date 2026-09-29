package com.example.service

import android.util.Log
import com.example.data.model.ExtractedInfo
import com.example.data.model.GeneratedIdentity
import com.example.data.model.ProxyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit

data class UserAgentOption(val label: String, val value: String)

data class ProxyDiagnosticResult(
    val isWorking: Boolean,
    val exitIp: String,
    val pingMs: Long,
    val protocol: String,
    val country: String = "US",
    val city: String = "",
    val isp: String = "",
    val qualityScore: Int = 0,
    val errorMessage: String = ""
)

object IdentityService {

    val USER_AGENTS = listOf(
        UserAgentOption(
            label = "Chrome 122 · Windows",
            value = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        ),
        UserAgentOption(
            label = "Chrome 121 · macOS",
            value = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
        ),
        UserAgentOption(
            label = "Safari 17 · iPhone",
            value = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1"
        ),
        UserAgentOption(
            label = "Safari 17 · macOS",
            value = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
        ),
        UserAgentOption(
            label = "Firefox 123 · Windows",
            value = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0"
        ),
        UserAgentOption(
            label = "Edge 122 · Windows",
            value = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"
        ),
        UserAgentOption(
            label = "Chrome 122 · Android",
            value = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36"
        )
    )

    val RANDOM_REFERRERS = listOf(
        "https://www.google.com/search?q=deals+and+rewards",
        "https://www.bing.com/search?q=online+surveys+and+offers",
        "https://www.facebook.com/",
        "https://twitter.com/promotions",
        "https://www.instagram.com/",
        "https://www.tiktok.com/",
        "https://www.reddit.com/r/freebies/",
        "https://www.youtube.com/"
    )

    private val FIRST_NAMES_MALE = listOf(
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
        "Thomas", "Charles", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven",
        "Paul", "Andrew", "Joshua", "Kenneth", "Kevin", "Brian", "George", "Timothy",
        "Ronald", "Jason", "Edward", "Jeffrey", "Ryan", "Jacob", "Gary", "Nicholas"
    )

    private val FIRST_NAMES_FEMALE = listOf(
        "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica",
        "Sarah", "Karen", "Lisa", "Nancy", "Betty", "Margaret", "Sandra", "Ashley",
        "Kimberly", "Emily", "Donna", "Michelle", "Carol", "Amanda", "Melissa", "Deborah",
        "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia", "Kathleen", "Amy", "Angela"
    )

    private val LAST_NAMES = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker"
    )

    private val US_CITIES = listOf(
        Triple("New York", "NY", "10001"),
        Triple("Los Angeles", "CA", "90001"),
        Triple("Chicago", "IL", "60601"),
        Triple("Houston", "TX", "77001"),
        Triple("Phoenix", "AZ", "85001"),
        Triple("Philadelphia", "PA", "19101"),
        Triple("San Antonio", "TX", "78201"),
        Triple("San Diego", "CA", "92101"),
        Triple("Dallas", "TX", "75201"),
        Triple("Austin", "TX", "78701"),
        Triple("Jacksonville", "FL", "32201"),
        Triple("Fort Worth", "TX", "76101"),
        Triple("Columbus", "OH", "43201"),
        Triple("Charlotte", "NC", "28201"),
        Triple("Indianapolis", "IN", "46201"),
        Triple("Seattle", "WA", "98101"),
        Triple("Denver", "CO", "80201"),
        Triple("Washington", "DC", "20001"),
        Triple("Boston", "MA", "02101"),
        Triple("Nashville", "TN", "37201")
    )

    private val STREET_NAMES = listOf(
        "Main St", "Oak Ave", "Maple St", "Cedar Ln", "Elm St", "Washington Blvd",
        "Park Ave", "Lakeview Dr", "Pine St", "Sunset Blvd", "Broadway", "Highland Ave",
        "Church Rd", "Lincoln Way", "Hillcrest Ave", "Valley Dr", "River Rd"
    )

    private val BANKS = listOf(
        "Chase Bank", "Bank of America", "Wells Fargo", "Citibank", "Capital One",
        "PNC Bank", "US Bank", "TD Bank", "Truist", "Discover Bank"
    )

    private val EMAIL_DOMAINS = listOf(
        "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com", "proton.me"
    )

    private val rnd = Random()

    fun generateUTM(baseUrl: String): String {
        val sources = listOf("google", "facebook", "newsletter", "twitter", "partner_direct")
        val mediums = listOf("cpc", "social", "email", "banner", "cpa_affiliate")
        val campaigns = listOf("spring_promo", "rewards_2026", "signup_bonus", "special_offer", "lead_gen")

        val source = sources[rnd.nextInt(sources.size)]
        val medium = mediums[rnd.nextInt(mediums.size)]
        val campaign = campaigns[rnd.nextInt(campaigns.size)]
        val content = "var_${rnd.nextInt(900) + 100}"

        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl${separator}utm_source=$source&utm_medium=$medium&utm_campaign=$campaign&utm_content=$content"
    }

    fun generateIdentity(countryCode: String = "US", providedEmail: String? = null): GeneratedIdentity {
        val isMale = rnd.nextBoolean()
        val gender = if (isMale) "Male" else "Female"
        val firstName = if (isMale) {
            FIRST_NAMES_MALE[rnd.nextInt(FIRST_NAMES_MALE.size)]
        } else {
            FIRST_NAMES_FEMALE[rnd.nextInt(FIRST_NAMES_FEMALE.size)]
        }
        val lastName = LAST_NAMES[rnd.nextInt(LAST_NAMES.size)]
        val fullName = "$firstName $lastName"

        val email = providedEmail?.trim()?.takeIf { it.isNotEmpty() } ?: run {
            val domain = EMAIL_DOMAINS[rnd.nextInt(EMAIL_DOMAINS.size)]
            val num = rnd.nextInt(900) + 100
            "${firstName.lowercase(Locale.US)}.${lastName.lowercase(Locale.US)}$num@$domain"
        }

        val username = "${firstName.lowercase(Locale.US)}_${lastName.lowercase(Locale.US)}${rnd.nextInt(90) + 10}"
        val password = "${firstName.take(3)}!${lastName.take(3)}#${rnd.nextInt(9000) + 1000}"

        val streetNum = rnd.nextInt(8999) + 100
        val streetName = STREET_NAMES[rnd.nextInt(STREET_NAMES.size)]
        val address = "$streetNum $streetName"

        val cityData = US_CITIES[rnd.nextInt(US_CITIES.size)]
        val city = cityData.first
        val state = cityData.second
        val postalCode = cityData.third

        // Phone: +1 (Area) XXX-XXXX
        val areaCode = rnd.nextInt(800) + 200
        val phoneMid = rnd.nextInt(900) + 100
        val phoneEnd = rnd.nextInt(9000) + 1000
        val phone = "+1 ($areaCode) $phoneMid-$phoneEnd"

        // Birth date (age 20-55)
        val cal = Calendar.getInstance()
        val age = rnd.nextInt(35) + 20
        cal.add(Calendar.YEAR, -age)
        cal.set(Calendar.MONTH, rnd.nextInt(12))
        cal.set(Calendar.DAY_OF_MONTH, rnd.nextInt(27) + 1)
        val birthDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        // Card generation with Luhn algorithm
        val cardType = when (rnd.nextInt(4)) {
            0 -> "Visa"
            1 -> "Mastercard"
            2 -> "Amex"
            else -> "Discover"
        }
        val cardNumber = generateValidLuhnCard(cardType)
        val expMonth = String.format(Locale.US, "%02d", rnd.nextInt(12) + 1)
        val expYear = (Calendar.getInstance().get(Calendar.YEAR) % 100) + rnd.nextInt(5) + 2
        val cardExpiry = "$expMonth/$expYear"
        val cardCvv = String.format(Locale.US, if (cardType == "Amex") "%04d" else "%03d", rnd.nextInt(if (cardType == "Amex") 9000 else 900) + 100)
        val bankName = BANKS[rnd.nextInt(BANKS.size)]

        return GeneratedIdentity(
            firstName = firstName,
            lastName = lastName,
            fullName = fullName,
            email = email,
            phone = phone,
            username = username,
            password = password,
            gender = gender,
            birthDate = birthDate,
            address = address,
            city = city,
            state = state,
            postalCode = postalCode,
            country = "United States",
            cardNumber = cardNumber,
            cardExpiry = cardExpiry,
            cardCvv = cardCvv,
            cardType = cardType,
            cardHolder = fullName.uppercase(Locale.US),
            bankName = bankName
        )
    }

    private fun generateValidLuhnCard(cardType: String): String {
        val (prefix, length) = when (cardType) {
            "Visa" -> Pair("4", 16)
            "Mastercard" -> Pair("5${rnd.nextInt(5) + 1}", 16)
            "Amex" -> Pair("37", 15)
            else -> Pair("6011", 16)
        }

        val digits = mutableListOf<Int>()
        for (ch in prefix) {
            digits.add(ch - '0')
        }
        while (digits.size < length - 1) {
            digits.add(rnd.nextInt(10))
        }

        // Calculate Luhn check digit
        var sum = 0
        var alt = true
        for (i in digits.size - 1 downTo 0) {
            var d = digits[i]
            if (alt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            alt = !alt
        }
        val checkDigit = (10 - (sum % 10)) % 10
        digits.add(checkDigit)

        // Format in groups of 4
        val raw = digits.joinToString("")
        return raw.chunked(4).joinToString(" ")
    }

    suspend fun fetchGeoInfo(
        proxyHost: String? = null,
        proxyPort: Int? = null,
        proxyType: String? = null,
        proxyUser: String? = null,
        proxyPass: String? = null
    ): ExtractedInfo = withContext(Dispatchers.IO) {
        val hasProxy = !proxyHost.isNullOrBlank() && proxyPort != null && proxyPort > 0 && proxyType != "none" && proxyType != "direct"

        var targetExitIp: String? = null
        if (hasProxy) {
            val cleanHost = proxyHost.trim()
            val cleanType = proxyType?.trim()?.lowercase() ?: "socks5"
            val cleanUser = proxyUser?.trim().orEmpty()
            val cleanPass = proxyPass?.trim().orEmpty()

            val testRes = testProxyConnection(cleanHost, proxyPort, cleanType, cleanUser, cleanPass, 15000)
            if (testRes.first) {
                targetExitIp = testRes.second
            } else {
                return@withContext ExtractedInfo(
                    ip = "Proxy Unreachable",
                    country = "Connection Error",
                    countryCode = "ERR",
                    city = "Offline",
                    region = testRes.second,
                    street = "",
                    postalCode = "",
                    timezone = "America/New_York",
                    language = "en-US",
                    currency = "USD",
                    isp = "$proxyHost:$proxyPort",
                    org = "Check host/port/auth",
                    latitude = 40.7128,
                    longitude = -74.0060,
                    isProxy = true
                )
            }
        }

        // Direct fetch for geo details using targetExitIp or current public IP (always via Proxy.NO_PROXY to avoid interference)
        val client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        val endpoints = if (!targetExitIp.isNullOrBlank()) {
            listOf(
                "https://ipwho.is/$targetExitIp",
                "https://ipapi.co/$targetExitIp/json/"
            )
        } else {
            listOf(
                "https://ipwho.is/",
                "https://api.ipify.org?format=json",
                "https://api.myip.com",
                "https://ipapi.co/json/"
            )
        }

        for (url in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()?.trim() ?: continue
                    if (body.isEmpty()) continue

                    val json = try { JSONObject(body) } catch (e: Exception) { null }

                    val rawIp = json?.optString("ip", json.optString("query", "")) 
                        ?: extractJsonString(body, "ip") 
                        ?: extractJsonString(body, "query")
                    val ip = rawIp?.trim().orEmpty().takeIf { it.isNotBlank() } ?: (targetExitIp ?: "")
                    if (ip.isBlank() || !ip.contains(".")) continue

                    val country = json?.optString("country_name", json.optString("country", "United States"))
                        ?: extractJsonString(body, "country_name")
                        ?: extractJsonString(body, "country")
                        ?: "United States"
                    val countryCode = json?.optString("country_code", json.optString("countryCode", "US"))
                        ?: extractJsonString(body, "country_code")
                        ?: extractJsonString(body, "countryCode")
                        ?: "US"
                    val city = json?.optString("city", "New York")
                        ?: extractJsonString(body, "city")
                        ?: "New York"
                    val region = json?.optString("region", json.optString("region_code", "NY"))
                        ?: extractJsonString(body, "region")
                        ?: extractJsonString(body, "region_code")
                        ?: "NY"
                    val postal = json?.optString("postal", json.optString("zip", "10001"))
                        ?: extractJsonString(body, "postal")
                        ?: extractJsonString(body, "zip")
                        ?: "10001"
                    val timezone = json?.optString("timezone", "America/New_York")
                        ?: extractJsonString(body, "timezone")
                        ?: "America/New_York"
                    val currency = json?.optString("currency", "USD")
                        ?: extractJsonString(body, "currency")
                        ?: "USD"
                    val isp = json?.optString("org", json.optString("isp", "Residential Cloud"))
                        ?: extractJsonString(body, "org")
                        ?: extractJsonString(body, "isp")
                        ?: "Residential Cloud"
                    val lat = json?.optDouble("latitude", 40.7128)
                        ?: extractJsonDouble(body, "latitude")
                        ?: 40.7128
                    val lon = json?.optDouble("longitude", -74.0060)
                        ?: extractJsonDouble(body, "longitude")
                        ?: -74.0060

                    return@withContext ExtractedInfo(
                        ip = ip,
                        country = country,
                        countryCode = countryCode,
                        city = city,
                        region = region,
                        street = "",
                        postalCode = postal,
                        timezone = timezone,
                        language = "en-$countryCode",
                        currency = currency,
                        isp = isp,
                        org = isp,
                        latitude = lat,
                        longitude = lon,
                        isProxy = hasProxy
                    )
                }
            } catch (e: Exception) {
                // Try next endpoint
            }
        }

        if (targetExitIp != null) {
            return@withContext ExtractedInfo(
                ip = targetExitIp,
                country = "United States",
                countryCode = "US",
                city = "New York",
                region = "NY",
                street = "",
                postalCode = "10001",
                timezone = "America/New_York",
                language = "en-US",
                currency = "USD",
                isp = "$proxyHost:$proxyPort",
                org = "Residential Network",
                latitude = 40.7128,
                longitude = -74.0060,
                isProxy = hasProxy
            )
        }

        if (hasProxy) {
            // When proxy is configured and all endpoints failed, report connection failure
            return@withContext ExtractedInfo(
                ip = "Proxy Unreachable",
                country = "Connection Error",
                countryCode = "ERR",
                city = "Offline",
                region = "",
                street = "",
                postalCode = "",
                timezone = "America/New_York",
                language = "en-US",
                currency = "USD",
                isp = "$proxyHost:$proxyPort",
                org = "Check host/port/auth",
                latitude = 40.7128,
                longitude = -74.0060,
                isProxy = true
            )
        }

        // Direct connection failed (no internet) - NO FAKE IP GENERATION!
        ExtractedInfo(
            ip = "Direct (No Internet)",
            country = "Offline",
            countryCode = "--",
            city = "Offline",
            region = "",
            street = "",
            postalCode = "",
            timezone = "America/New_York",
            language = "en-US",
            currency = "USD",
            isp = "Disconnected",
            org = "No Internet Connection",
            latitude = 40.7128,
            longitude = -74.0060,
            isProxy = false
        )
    }

    /**
     * Standalone direct proxy socket test for SOCKS4/5 and HTTP/HTTPS proxies.
     * Does NOT touch LocalSocks5HttpBridge or JVM system proxies.
     * Returns Triple(isWorking, exitIpOrError, pingMs)
     */
    private val fastGeoCache = java.util.concurrent.ConcurrentHashMap<String, Triple<String, String, String>>()

    fun testProxyConnection(
        host: String,
        port: Int,
        type: String,
        user: String = "",
        pass: String = "",
        timeoutMs: Int = 15000
    ): Triple<Boolean, String, Long> {
        val diag = testAndDetectProxy(host, port, type, user, pass, timeoutMs)
        return Triple(diag.isWorking, if (diag.isWorking) diag.exitIp else diag.errorMessage, diag.pingMs)
    }

    /**
     * Intelligent multi-protocol proxy tester and auto-detector.
     * If user specified SOCKS5 but server is HTTP (or vice versa), auto-detects and reports
     * the working protocol along with verified exit IP, ping, country, city, and Quality Score.
     */
    fun testAndDetectProxy(
        host: String,
        port: Int,
        preferredType: String,
        user: String = "",
        pass: String = "",
        timeoutMs: Int = 15000
    ): ProxyDiagnosticResult {
        val cleanHost = host.trim()
        val cleanUser = user.trim()
        val cleanPass = pass.trim()
        val pref = preferredType.trim().lowercase()

        val protocolsToTry = when {
            pref.startsWith("socks") -> listOf("socks5", "http")
            pref == "http" || pref == "https" -> listOf("http", "socks5")
            else -> listOf("socks5", "http")
        }

        var lastError = "تعذر الاتصال بالبروكسي"

        for (proto in protocolsToTry) {
            val startTime = System.currentTimeMillis()
            try {
                if (proto.startsWith("socks")) {
                    val sRes = testSocksSocket(cleanHost, port, cleanUser, cleanPass, timeoutMs)
                    val ping = (System.currentTimeMillis() - startTime).coerceAtLeast(10L)
                    if (sRes.first) {
                        val exitIp = sRes.second
                        val geo = lookupFastGeo(exitIp)
                        val score = calculateProxyScore(ping, geo.first, geo.third)
                        return ProxyDiagnosticResult(
                            isWorking = true,
                            exitIp = exitIp,
                            pingMs = ping,
                            protocol = "socks5",
                            country = geo.first,
                            city = geo.second,
                            isp = geo.third,
                            qualityScore = score
                        )
                    } else {
                        lastError = sRes.second
                    }
                } else {
                    val hRes = testHttpProxy(cleanHost, port, cleanUser, cleanPass, timeoutMs)
                    val ping = (System.currentTimeMillis() - startTime).coerceAtLeast(10L)
                    if (hRes.first) {
                        val exitIp = hRes.second
                        val geo = lookupFastGeo(exitIp)
                        val score = calculateProxyScore(ping, geo.first, geo.third)
                        return ProxyDiagnosticResult(
                            isWorking = true,
                            exitIp = exitIp,
                            pingMs = ping,
                            protocol = "http",
                            country = geo.first,
                            city = geo.second,
                            isp = geo.third,
                            qualityScore = score
                        )
                    } else {
                        lastError = hRes.second
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: e.message ?: "Connection error"
            }
        }

        return ProxyDiagnosticResult(
            isWorking = false,
            exitIp = "",
            pingMs = 0L,
            protocol = pref,
            errorMessage = lastError,
            qualityScore = 0
        )
    }

    private fun testSocksSocket(
        cleanHost: String,
        port: Int,
        cleanUser: String,
        cleanPass: String,
        timeoutMs: Int
    ): Pair<Boolean, String> {
        val socket = Socket()
        try {
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(cleanHost, port), timeoutMs)
            val sIn = socket.getInputStream()
            val sOut = socket.getOutputStream()

            // 1. SOCKS5 Greeting (RFC 1928)
            val hasAuth = cleanUser.isNotBlank() && cleanPass.isNotBlank()
            if (hasAuth) {
                sOut.write(byteArrayOf(0x05, 0x02, 0x00, 0x02))
            } else {
                sOut.write(byteArrayOf(0x05, 0x01, 0x00))
            }
            sOut.flush()

            val greeting = ByteArray(2)
            var read = 0
            while (read < 2) {
                val r = sIn.read(greeting, read, 2 - read)
                if (r == -1) return Pair(false, "SOCKS5 greeting EOF")
                read += r
            }

            if (greeting[0] != 0x05.toByte()) {
                return Pair(false, "Invalid SOCKS version: ${greeting[0]}")
            }

            val method = greeting[1].toInt() and 0xFF
            if (method == 0xFF) {
                return Pair(false, "البروكسي يتطلب اسم مستخدم وكلمة مرور (Auth Required) - تأكد من إدخال User و Pass")
            }

            // 2. Authentication subnegotiation (RFC 1929)
            if (method == 0x02) {
                val uBytes = cleanUser.toByteArray(Charsets.UTF_8)
                val pBytes = cleanPass.toByteArray(Charsets.UTF_8)
                val authReq = ByteArray(3 + uBytes.size + pBytes.size)
                authReq[0] = 0x01
                authReq[1] = uBytes.size.toByte()
                System.arraycopy(uBytes, 0, authReq, 2, uBytes.size)
                authReq[2 + uBytes.size] = pBytes.size.toByte()
                System.arraycopy(pBytes, 0, authReq, 3 + uBytes.size, pBytes.size)

                sOut.write(authReq)
                sOut.flush()

                val authResp = ByteArray(2)
                read = 0
                while (read < 2) {
                    val r = sIn.read(authResp, read, 2 - read)
                    if (r == -1) return Pair(false, "SOCKS auth EOF (انقطع الاتصال أثناء المصادقة)")
                    read += r
                }
                if (authResp[1] != 0x00.toByte()) {
                    return Pair(false, "فشل المصادقة: اسم المستخدم أو كلمة المرور غير صحيحة")
                }
            }

            // 3. Connect to IP echo target via SOCKS CONNECT (RFC 1928)
            val targetDomain = "api.ipify.org"
            val domainBytes = targetDomain.toByteArray(Charsets.US_ASCII)
            val connReq = ByteArray(4 + 1 + domainBytes.size + 2)
            connReq[0] = 0x05
            connReq[1] = 0x01 // CONNECT
            connReq[2] = 0x00
            connReq[3] = 0x03 // Domain name
            connReq[4] = domainBytes.size.toByte()
            System.arraycopy(domainBytes, 0, connReq, 5, domainBytes.size)
            connReq[5 + domainBytes.size] = 0x00
            connReq[6 + domainBytes.size] = 80.toByte()

            sOut.write(connReq)
            sOut.flush()

            val connHead = ByteArray(4)
            read = 0
            while (read < 4) {
                val r = sIn.read(connHead, read, 4 - read)
                if (r == -1) break
                read += r
            }

            if (read < 4 || connHead[1] != 0x00.toByte()) {
                val rep = if (read >= 2) connHead[1].toInt() and 0xFF else -1
                val repMsg = when (rep) {
                    0x01 -> "خادم البروكسي غير متاح (General SOCKS failure)"
                    0x02 -> "الاتصال غير مسموح بقواعد البروكسي (Connection not allowed)"
                    0x03 -> "الشبكة غير قابلة للوصول (Network unreachable)"
                    0x04 -> "الهدف غير متاح (Host unreachable)"
                    0x05 -> "تم رفض الاتصال من الهدف (Connection refused)"
                    else -> "فشل فتح قناة SOCKS (رمز الرد: $rep)"
                }
                return Pair(false, repMsg)
            }

            // Drain BND.ADDR and BND.PORT
            val atyp = connHead[3].toInt() and 0xFF
            when (atyp) {
                0x01 -> {
                    val b = ByteArray(6)
                    var off = 0
                    while (off < 6) { val r = sIn.read(b, off, 6 - off); if (r == -1) break; off += r }
                }
                0x03 -> {
                    val len = sIn.read()
                    if (len > 0) {
                        val b = ByteArray(len + 2)
                        var off = 0
                        while (off < b.size) { val r = sIn.read(b, off, b.size - off); if (r == -1) break; off += r }
                    }
                }
                0x04 -> {
                    val b = ByteArray(18)
                    var off = 0
                    while (off < 18) { val r = sIn.read(b, off, 18 - off); if (r == -1) break; off += r }
                }
            }

            // 4. Send HTTP GET to verify traffic and detect exit IP
            val httpReq = "GET /?format=json HTTP/1.1\r\nHost: api.ipify.org\r\nUser-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\r\nConnection: close\r\n\r\n"
            sOut.write(httpReq.toByteArray(Charsets.US_ASCII))
            sOut.flush()

            val responseBytes = java.io.ByteArrayOutputStream()
            val buf = ByteArray(1024)
            var n: Int
            while (sIn.read(buf).also { n = it } != -1) {
                responseBytes.write(buf, 0, n)
                val curr = responseBytes.toString("UTF-8")
                if (curr.contains("\r\n\r\n")) {
                    val bodyPart = curr.substringAfter("\r\n\r\n")
                    if (bodyPart.contains("}") || bodyPart.length >= 30) {
                        break
                    }
                }
                if (responseBytes.size() > 4096) break
            }

            val resStr = responseBytes.toString("UTF-8")
            val ipMatch = Regex("""\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b""").find(resStr)
            val detectedIp = ipMatch?.value?.trim()

            if (!detectedIp.isNullOrBlank() && detectedIp.contains(".")) {
                return Pair(true, detectedIp)
            } else {
                return Pair(true, cleanHost)
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "SOCKS connection error"
            val friendlyMsg = if (msg.contains("timed out", ignoreCase = true) || msg.contains("Timeout", ignoreCase = true)) {
                "انتهت مهلة الاتصال بالبروكسي (Timeout)"
            } else if (msg.contains("refused", ignoreCase = true)) {
                "تم رفض الاتصال بالبروكسي (Connection Refused)"
            } else {
                msg
            }
            return Pair(false, friendlyMsg)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun testHttpProxy(
        cleanHost: String,
        port: Int,
        cleanUser: String,
        cleanPass: String,
        timeoutMs: Int
    ): Pair<Boolean, String> {
        try {
            val clientBuilder = OkHttpClient.Builder()
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(cleanHost, port)))
                .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .followRedirects(true)

            if (cleanUser.isNotBlank() && cleanPass.isNotBlank()) {
                val credential = Credentials.basic(cleanUser, cleanPass)
                clientBuilder.proxyAuthenticator { _, response ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
                clientBuilder.addInterceptor { chain ->
                    val orig = chain.request()
                    val req = if (orig.header("Proxy-Authorization") == null) {
                        orig.newBuilder().header("Proxy-Authorization", credential).build()
                    } else orig
                    chain.proceed(req)
                }
            }

            val client = clientBuilder.build()
            val testUrls = listOf(
                "https://api.ipify.org?format=json",
                "http://icanhazip.com",
                "http://ifconfig.me/ip"
            )

            for (tUrl in testUrls) {
                try {
                    val request = Request.Builder()
                        .url(tUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .build()

                    val resp = client.newCall(request).execute()
                    if (resp.isSuccessful) {
                        val b = resp.body?.string().orEmpty()
                        val ipMatch = Regex("""\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b""").find(b)
                        val exitIp = ipMatch?.value?.trim() ?: b.trim()
                        if (exitIp.isNotBlank() && exitIp.contains(".")) {
                            return Pair(true, exitIp)
                        }
                    }
                } catch (_: Exception) {}
            }
            return Pair(false, "خادم HTTP لم يستجب لطلب الفحص")
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "HTTP proxy failed"
            val friendlyMsg = if (msg.contains("timed out", ignoreCase = true) || msg.contains("Timeout", ignoreCase = true)) {
                "انتهت مهلة الاتصال ببروكسي HTTP (Timeout)"
            } else if (msg.contains("407")) {
                "فشل مصادقة بروكسي HTTP: تأكد من بيانات الدخول"
            } else {
                msg
            }
            return Pair(false, friendlyMsg)
        }
    }

    fun calculateProxyScore(pingMs: Long, country: String, isp: String): Int {
        var score = 30
        if (pingMs in 1..400) score += 40
        else if (pingMs <= 1000) score += 30
        else if (pingMs <= 2500) score += 20
        else if (pingMs <= 5000) score += 10

        val isResidential = isp.contains("telecom", ignoreCase = true) ||
                isp.contains("cable", ignoreCase = true) ||
                isp.contains("wireless", ignoreCase = true) ||
                isp.contains("comcast", ignoreCase = true) ||
                isp.contains("att", ignoreCase = true) ||
                isp.contains("verizon", ignoreCase = true) ||
                isp.contains("charter", ignoreCase = true) ||
                isp.contains("residential", ignoreCase = true)
        if (isResidential) score += 20
        else score += 10

        if (country.isNotBlank() && country != "--") score += 10
        return score.coerceIn(10, 100)
    }

    fun lookupFastGeo(ip: String): Triple<String, String, String> {
        val cleanIp = ip.trim()
        if (cleanIp.isBlank() || !cleanIp.contains(".")) return Triple("US", "New York", "Residential Cloud")
        fastGeoCache[cleanIp]?.let { return it }

        val endpoints = listOf(
            "https://ipwhois.app/json/$cleanIp" to { body: String ->
                val j = JSONObject(body)
                Triple(j.optString("country_code", "US").uppercase(), j.optString("city", ""), j.optString("isp", j.optString("org", "")))
            },
            "http://ip-api.com/json/$cleanIp" to { body: String ->
                val j = JSONObject(body)
                Triple(j.optString("countryCode", "US").uppercase(), j.optString("city", ""), j.optString("isp", j.optString("org", "")))
            },
            "https://freeipapi.com/api/json/$cleanIp" to { body: String ->
                val j = JSONObject(body)
                Triple(j.optString("countryCode", "US").uppercase(), j.optString("cityName", ""), "Residential")
            }
        )

        for ((url, parser) in endpoints) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(1800, TimeUnit.MILLISECONDS)
                    .readTimeout(1800, TimeUnit.MILLISECONDS)
                    .build()
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    if (body.contains("{")) {
                        val parsed = parser(body)
                        if (parsed.first.isNotBlank() && parsed.first.length == 2) {
                            fastGeoCache[cleanIp] = parsed
                            return parsed
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        val defaultGeo = Triple("US", "United States", "Residential")
        fastGeoCache[cleanIp] = defaultGeo
        return defaultGeo
    }

    suspend fun checkLeadCPA(userId: String, apiKey: String, ip: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (userId.isBlank() || apiKey.isBlank()) {
            return@withContext Pair(false, "CPA Grip User ID and API Key required in Settings")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = "https://www.cpagrip.com/common/lead_check_rss.php?user_id=$userId&key=$apiKey&ip=$ip"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "CPAAutomator/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Pair(false, "API Error: HTTP ${response.code}")
            }

            val hasLead = body.contains("<lead>") ||
                    body.contains("<status>lead</status>") ||
                    (body.contains("<item>") && body.contains(ip))

            if (hasLead) {
                Pair(true, "Lead successfully verified for IP: $ip")
            } else {
                Pair(false, "No conversion detected yet for IP: $ip")
            }
        } catch (e: Exception) {
            Pair(false, "Check lead failed: ${e.localizedMessage}")
        }
    }

    const val DEFAULT_ASOCKS_URL = "https://asocks-list.org/Lb5lLZADymiWzwGhSVeZyGrcdJcc9m3g.txt?limit=10&type=res&template_id=2&country=US"

    private fun extractJsonString(json: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonDouble(json: String, key: String): Double? {
        val regex = Regex(""""$key"\s*:\s*(-?[0-9]+(?:\.[0-9]+)?)""")
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    data class ProxySourcePreset(
        val title: String,
        val description: String,
        val url: String,
        val protocol: String
    )

    val PRESET_SOURCES = listOf(
        ProxySourcePreset(
            title = "Asocks Whitelist Export (User Link)",
            description = "Asocks US Residential Export Link (Requires Active Token)",
            url = DEFAULT_ASOCKS_URL,
            protocol = "socks5"
        ),
        ProxySourcePreset(
            title = "Free SOCKS5 High-Speed Pool (Daily)",
            description = "Verified SOCKS5 residential & datacenter proxies",
            url = "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks5.txt",
            protocol = "socks5"
        ),
        ProxySourcePreset(
            title = "Free HTTP/HTTPS Web Proxy Pool",
            description = "Public high-anonymity HTTP web proxies",
            url = "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt",
            protocol = "http"
        )
    )

    fun parseProxyLine(line: String, defaultType: String = "socks5"): ProxyItem? {
        var clean = line.trim()
        if (clean.isEmpty() || clean.startsWith("#") || clean.startsWith("//")) return null

        var type = defaultType
        if (clean.startsWith("socks5://", ignoreCase = true)) {
            type = "socks5"
            clean = clean.substring("socks5://".length)
        } else if (clean.startsWith("socks4://", ignoreCase = true)) {
            type = "socks4"
            clean = clean.substring("socks4://".length)
        } else if (clean.startsWith("http://", ignoreCase = true)) {
            type = "http"
            clean = clean.substring("http://".length)
        } else if (clean.startsWith("https://", ignoreCase = true)) {
            type = "http"
            clean = clean.substring("https://".length)
        }

        // Format 1: user:pass@host:port OR host:port@user:pass
        if (clean.contains("@")) {
            val atParts = clean.split("@")
            val part1 = atParts[0].trim()
            val part2 = atParts.getOrNull(1)?.trim() ?: return null

            val hostTokens = part2.split(":")
            val portCandidate = hostTokens.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()
            if (hostTokens.size >= 2 && portCandidate != null && portCandidate in 1..65535) {
                // user:pass@host:port
                val authTokens = part1.split(":")
                val user = authTokens.getOrElse(0) { "" }.trim()
                val pass = authTokens.getOrElse(1) { "" }.trim()
                val host = hostTokens[0].trim()
                val country = if (hostTokens.size >= 3 && hostTokens[2].length in 2..4) hostTokens[2].uppercase() else "US"
                if (host.isNotEmpty()) {
                    return ProxyItem(host = host, port = portCandidate, type = type, username = user, password = pass, country = country)
                }
            } else {
                // host:port@user:pass
                val hostTokens1 = part1.split(":")
                val portCandidate1 = hostTokens1.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()
                if (hostTokens1.size >= 2 && portCandidate1 != null && portCandidate1 in 1..65535) {
                    val authTokens = part2.split(":")
                    val user = authTokens.getOrElse(0) { "" }.trim()
                    val pass = authTokens.getOrElse(1) { "" }.trim()
                    val host = hostTokens1[0].trim()
                    val country = if (authTokens.size >= 3 && authTokens[2].length in 2..4) authTokens[2].uppercase() else "US"
                    if (host.isNotEmpty()) {
                        return ProxyItem(host = host, port = portCandidate1, type = type, username = user, password = pass, country = country)
                    }
                }
            }
        }

        // Format 2: delimiters like colon, comma, tab, space, pipe
        val delimiters = if (clean.contains("\t")) arrayOf("\t")
        else if (clean.contains("|")) arrayOf("|")
        else if (clean.contains(",")) arrayOf(",")
        else if (clean.contains(" ")) arrayOf(" ")
        else arrayOf(":")

        val tokens = clean.split(*delimiters).map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size >= 2) {
            // Case A: host:port:user:pass or host:port
            val portCandidate = tokens[1].filter { it.isDigit() }.toIntOrNull()
            if (portCandidate != null && portCandidate in 1..65535) {
                val host = tokens[0]
                val user = tokens.getOrNull(2) ?: ""
                val pass = tokens.getOrNull(3) ?: ""
                val country = if (tokens.size >= 5 && tokens[4].length in 2..4) tokens[4].uppercase() else "US"
                if (host.isNotEmpty()) {
                    return ProxyItem(host = host, port = portCandidate, type = type, username = user, password = pass, country = country)
                }
            }

            // Case B: user:pass:host:port
            if (tokens.size >= 4) {
                val lastPort = tokens[3].filter { it.isDigit() }.toIntOrNull()
                if (lastPort != null && lastPort in 1..65535) {
                    val user = tokens[0]
                    val pass = tokens[1]
                    val host = tokens[2]
                    val country = if (tokens.size >= 5 && tokens[4].length in 2..4) tokens[4].uppercase() else "US"
                    if (host.isNotEmpty()) {
                        return ProxyItem(host = host, port = lastPort, type = type, username = user, password = pass, country = country)
                    }
                }
            }
        }

        return null
    }

    fun parseBulkProxies(text: String, defaultType: String = "socks5"): List<ProxyItem> {
        val trimmed = text.trim()
        val result = mutableListOf<ProxyItem>()

        // Check if response is JSON (array or object)
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                if (trimmed.startsWith("[")) {
                    val arr = JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val host = obj.optString("ip", obj.optString("host", obj.optString("server", obj.optString("address", "")))).trim()
                        val port = obj.optInt("port", obj.optInt("port_socks5", obj.optInt("port_http", 0)))
                        val type = obj.optString("type", obj.optString("protocol", defaultType)).lowercase()
                        val user = obj.optString("username", obj.optString("user", obj.optString("login", "")))
                        val pass = obj.optString("password", obj.optString("pass", obj.optString("pwd", "")))
                        val country = obj.optString("country", obj.optString("country_code", "US")).uppercase()
                        if (host.isNotEmpty() && port in 1..65535) {
                            result.add(ProxyItem(host = host, port = port, type = type, username = user, password = pass, country = country))
                        }
                    }
                } else {
                    val root = JSONObject(trimmed)
                    val arr = root.optJSONArray("proxies")
                        ?: root.optJSONArray("data")
                        ?: root.optJSONArray("list")
                        ?: root.optJSONArray("results")
                        ?: root.optJSONArray("ports")
                        ?: root.optJSONArray("items")
                        ?: root.optJSONObject("response")?.optJSONArray("ports")
                        ?: root.optJSONObject("data")?.optJSONArray("list")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val host = obj.optString("ip", obj.optString("host", obj.optString("server", obj.optString("address", "")))).trim()
                            val port = obj.optInt("port", obj.optInt("port_socks5", obj.optInt("port_http", 0)))
                            val type = obj.optString("type", obj.optString("protocol", defaultType)).lowercase()
                            val user = obj.optString("username", obj.optString("user", obj.optString("login", "")))
                            val pass = obj.optString("password", obj.optString("pass", obj.optString("pwd", "")))
                            val country = obj.optString("country", obj.optString("country_code", "US")).uppercase()
                            if (host.isNotEmpty() && port in 1..65535) {
                                result.add(ProxyItem(host = host, port = port, type = type, username = user, password = pass, country = country))
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall back to line-by-line text parsing
            }
        }

        if (result.isEmpty()) {
            val lines = trimmed.split(Regex("[\\r\\n;]+|(?=(?:socks[45]?|https?|http)://)"))
            for (line in lines) {
                val cleaned = line.trim()
                if (cleaned.isNotBlank()) {
                    val p = parseProxyLine(cleaned, defaultType)
                    if (p != null) {
                        result.add(p)
                    }
                }
            }
        }
        return result
    }

    suspend fun fetchProxiesFromUrl(url: String, defaultType: String = "socks5"): Result<List<ProxyItem>> = withContext(Dispatchers.IO) {
        try {
            var cleanInput = url.trim()
            if (cleanInput.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("URL أو قائمة البروكسيات فارغة (Input is empty)"))
            }

            // 1. Direct proxy list detection (User pasted proxy lines directly instead of an HTTP URL)
            if (!cleanInput.startsWith("http://", ignoreCase = true) &&
                !cleanInput.startsWith("https://", ignoreCase = true)) {
                val isDomainUrl = cleanInput.contains("/") &&
                        (cleanInput.contains(".org") || cleanInput.contains(".com") ||
                                cleanInput.contains(".net") || cleanInput.contains(".io") ||
                                cleanInput.contains(".co") || cleanInput.contains(".app") ||
                                cleanInput.contains(".xyz"))
                if (isDomainUrl) {
                    cleanInput = "https://$cleanInput"
                } else {
                    val directParsed = parseBulkProxies(cleanInput, defaultType)
                    if (directParsed.isNotEmpty()) {
                        return@withContext Result.success(directParsed)
                    }
                    cleanInput = "https://$cleanInput"
                }
            }

            val client = OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(cleanInput)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty().trim()

            if (!response.isSuccessful) {
                val code = response.code
                val isAsocks = cleanInput.contains("asocks", ignoreCase = true)
                val isWhitelist = cleanInput.contains("whitelist", ignoreCase = true)
                val errorMessage = when (code) {
                    401 -> if (isWhitelist) {
                        "خطأ مصادقة Asocks Whitelist (401 Unauthorized):\nرابط Whitelist يتطلب تفعيل الـ IP في لوحة Asocks، أو أن الرمز قد انتهت صلاحيته.\n💡 الحل الأسهل والأضمن: ادخل إلى asocks.com واختر وضع (User/Pass) بدلاً من Whitelist لتوليد رابط يعمل مباشرة على أي شبكة وبدون ربط بالـ IP."
                    } else if (isAsocks) {
                        "خطأ مصادقة Asocks (401 Unauthorized):\nانتهت صلاحية رمز قائمة Asocks أو أن الرابط غير صالح.\nيرجى فتح لوحة تحكم Asocks (asocks.com) وتجديد رابط التصدير (Export Link) أو استخدام اللصق اليدوي للبروكسيات."
                    } else {
                        "HTTP 401 Unauthorized: المفتاح أو الرابط غير مصرح به أو انتهت صلاحيته."
                    }
                    403 -> if (isAsocks) {
                        "رفض الوصول من Asocks (403 Forbidden):\nيرجى التأكد من إضافة عنوان الـ IP إلى Whitelist في حساب Asocks، أو تفعيل وضع User/Pass."
                    } else {
                        "HTTP 403 Forbidden: الوصول محظور من قبل الخادم."
                    }
                    404 -> "HTTP 404 Not Found: رابط تصدير البروكسيات غير موجود. تأكد من صحة الرابط."
                    else -> "خطأ HTTP $code: ${response.message}\n${rawBody.take(120)}"
                }
                return@withContext Result.failure(Exception(errorMessage))
            }

            // Inspect if response is JSON error object
            if (rawBody.startsWith("{")) {
                try {
                    val jsonObj = JSONObject(rawBody)
                    if (jsonObj.has("success") && !jsonObj.optBoolean("success")) {
                        val msg = jsonObj.optString("message", jsonObj.optString("error", "فشل طلب التصدير"))
                        return@withContext Result.failure(Exception("استجابة Asocks: $msg (يرجى تجديد التوكن من لوحة Asocks)"))
                    }
                    if (jsonObj.has("error") && !jsonObj.isNull("error")) {
                        val errVal = jsonObj.get("error")
                        if (errVal !is Boolean || errVal == true) {
                            val msg = jsonObj.optString("message", errVal.toString())
                            return@withContext Result.failure(Exception("خطأ من مزود البروكسيات: $msg"))
                        }
                    }
                } catch (_: Exception) {}
            }

            val proxies = parseBulkProxies(rawBody, defaultType)
            if (proxies.isEmpty()) {
                val snippet = if (rawBody.length > 100) rawBody.take(100) + "..." else rawBody
                return@withContext Result.failure(
                    Exception("لم يتم العثور على أي بروكسيات صالحة في الاستجابة.\nمحتوى الاستجابة: $snippet")
                )
            }

            Result.success(proxies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
