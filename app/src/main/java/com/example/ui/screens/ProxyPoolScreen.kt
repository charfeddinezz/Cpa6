package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppSettings
import com.example.data.model.ProxyItem
import com.example.service.IdentityService
import com.example.ui.theme.CpaAccent
import com.example.ui.theme.CpaAccentDim
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

@Composable
fun ProxyPoolScreen(
    proxies: List<ProxyItem>,
    currentSettings: AppSettings,
    onFetchFromUrl: (url: String, protocol: String, callback: (Boolean, String, Int) -> Unit) -> Unit,
    onImportBulk: (String, String) -> Unit,
    onAddSingleProxy: (ProxyItem) -> Unit,
    onSetActiveProxy: (ProxyItem) -> Unit,
    onTestProxy: (ProxyItem, callback: (Boolean, String) -> Unit) -> Unit,
    onDeleteProxy: (ProxyItem) -> Unit,
    onClearAll: () -> Unit,
    onToggleAutoRotate: (Boolean) -> Unit,
    onToggleProxyGlobalEnabled: ((Boolean) -> Unit)? = null,
    onTestAllProxies: (((Int, Int) -> Unit, (Int, Int) -> Unit) -> Unit)? = null,
    onDeleteFailed: (() -> Unit)? = null,
    onResetFailed: (() -> Unit)? = null,
    onAutoSelectFastest: (() -> Unit)? = null,
    onExportWorking: (() -> String)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val defaultAsocksUrl = IdentityService.DEFAULT_ASOCKS_URL

    var urlInput by remember {
        mutableStateOf(
            currentSettings.proxyListUrl.ifBlank { defaultAsocksUrl }
        )
    }
    var selectedProtocol by remember { mutableStateOf("socks5") }
    var isFetching by remember { mutableStateOf(false) }
    var fetchFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var showBulkDialog by remember { mutableStateOf(false) }
    var showAddSingleDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var testingProxyId by remember { mutableStateOf<Long?>(null) }
    var testResultMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var isTestingAll by remember { mutableStateOf(false) }
    var testingAllProgress by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 0. Master Switch: Completely Disable Proxy & Revert to Direct Local Connection ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (currentSettings.proxyEnabled) CpaPrimary else CpaWarning,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentSettings.proxyEnabled) CpaCardElevated else CpaWarning.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (currentSettings.proxyEnabled) CpaPrimaryDim else CpaWarning.copy(alpha = 0.2f))
                                    .border(1.dp, if (currentSettings.proxyEnabled) CpaPrimaryBorder else CpaWarning, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentSettings.proxyEnabled) Icons.Default.Router else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (currentSettings.proxyEnabled) CpaPrimary else CpaWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "مفتاح التحكم العام في البروكسي",
                                    color = CpaText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentSettings.proxyEnabled) "🟢 نظام البروكسي مفعّل (Proxy Active)" else "🚫 البروكسي مغلق — الاتصال المحلي المباشر نشط",
                                    color = if (currentSettings.proxyEnabled) CpaSuccess else CpaWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Switch(
                            checked = currentSettings.proxyEnabled,
                            onCheckedChange = { onToggleProxyGlobalEnabled?.invoke(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CpaPrimary,
                                checkedTrackColor = CpaPrimaryDim,
                                uncheckedThumbColor = CpaWarning,
                                uncheckedTrackColor = CpaCardElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (currentSettings.proxyEnabled) CpaCard else CpaWarning.copy(alpha = 0.15f))
                            .border(0.5.dp, if (currentSettings.proxyEnabled) CpaBorder else CpaWarning.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentSettings.proxyEnabled) {
                                "🛡️ التوجيه عبر البروكسي نشط: يتم إرسال كافة طلبات المتصفح وتدوير البروكسيات وفق القائمة والإعدادات."
                            } else {
                                "⚡ تم إيقاف وتعطيل البروكسي بالكامل: المتصفح والمهام تعتمد الآن 100% على اتصال الإنترنت المحلي العادي (Direct Local Network) بدون وسيط."
                            },
                            color = if (currentSettings.proxyEnabled) CpaTextDim else CpaWarning,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // --- 1. Pool Header Metrics Card ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PROXY ROTATION POOL",
                        color = CpaPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${proxies.size}",
                            color = CpaText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "proxies loaded",
                            color = CpaTextDim,
                            fontSize = 12.sp
                        )
                    }
                    if (!currentSettings.proxyEnabled) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Active: 🚫 مغلق (الاتصال المحلي المباشر نشط)",
                            color = CpaWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (currentSettings.proxyHost.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Active: ${currentSettings.proxyType.uppercase()}://${currentSettings.proxyHost}:${currentSettings.proxyPort}",
                            color = CpaSuccess,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Active: Direct Connection (No Proxy)",
                            color = CpaTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Auto-Rotate",
                            color = if (currentSettings.proxyAutoRotate) CpaPrimary else CpaTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = currentSettings.proxyAutoRotate,
                            onCheckedChange = { onToggleAutoRotate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CpaPrimary,
                                checkedTrackColor = CpaPrimaryDim,
                                uncheckedThumbColor = CpaTextMuted,
                                uncheckedTrackColor = CpaCardElevated
                            ),
                            modifier = Modifier.testTag("proxy_auto_rotate_switch")
                        )
                    }
                    if (proxies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clear All",
                            color = CpaError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showClearConfirmDialog = true }
                                .padding(2.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Fetch From Asocks / Whitelist URL Card ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CpaCardElevated)
                    .border(1.dp, CpaPrimaryBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = CpaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FETCH FROM URL / ASOCKS",
                            color = CpaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            urlInput = defaultAsocksUrl
                            selectedProtocol = "socks5"
                            fetchFeedback = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CpaPrimaryDim.copy(alpha = 0.5f),
                            contentColor = CpaPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaPrimaryBorder)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Asocks (رابطك)", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            urlInput = "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/socks5.txt"
                            selectedProtocol = "socks5"
                            fetchFeedback = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CpaPrimaryDim.copy(alpha = 0.3f),
                            contentColor = CpaPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaPrimaryBorder)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Free SOCKS5", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            urlInput = "https://raw.githubusercontent.com/TheSpeedX/SOCKS-List/master/http.txt"
                            selectedProtocol = "http"
                            fetchFeedback = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CpaPrimaryDim.copy(alpha = 0.3f),
                            contentColor = CpaPrimary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaPrimaryBorder)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Free HTTP", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (urlInput.contains("whitelist", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CpaWarning.copy(alpha = 0.12f))
                            .border(1.dp, CpaWarning.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = CpaWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تنبيه: أنت تستخدم رابط IP Whitelist",
                                    color = CpaWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "روابط Whitelist من Asocks لا تعمل إلا إذا أضفت IP جهازك الحالي في لوحة تحكم Asocks (قسم Whitelist). إذا لم يعمل معك الرابط أو ظهر 401، نوصي بشدة بالدخول إلى asocks.com واختيار وضع (User/Pass) بدلاً من Whitelist، ليتم توليد بروكسيات ببيانات تسجيل دخول تعمل في أي مكان بدون قيود IP.",
                                color = CpaText,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaCard)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 تنبيه Asocks: إذا ظهر 401 Unauthorized، قم بتسجيل الدخول إلى asocks.com وتجديد رابط التصدير (Export Link) أو انسخ البروكسيات والصقها مباشرة هنا.",
                            color = CpaAccent,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "أدخل رابط التصدير من Asocks أو Webshare، أو الصق قائمة البروكسيات مباشرة (IP:Port أو user:pass@host:port):",
                    color = CpaTextDim,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // URL Input Field with Paste & Clear
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        fetchFeedback = null
                    },
                    placeholder = {
                        Text(
                            "https://asocks-list.org/whitelist/....txt?limit=10&country=US",
                            color = CpaTextMuted,
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proxy_fetch_url_input"),
                    singleLine = false,
                    maxLines = 3,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        urlInput = clip.trim()
                                        fetchFeedback = null
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CpaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (urlInput.isNotBlank()) {
                                IconButton(
                                    onClick = { urlInput = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = CpaTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CpaBg,
                        unfocusedContainerColor = CpaBg,
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Protocol selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Protocol:", color = CpaTextMuted, fontSize = 11.sp)
                    listOf("socks5", "http", "socks4").forEach { proto ->
                        FilterChip(
                            selected = selectedProtocol == proto,
                            onClick = { selectedProtocol = proto },
                            label = {
                                Text(
                                    proto.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedProtocol == proto) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CpaPrimaryDim,
                                selectedLabelColor = CpaPrimary,
                                containerColor = CpaCard,
                                labelColor = CpaTextDim
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedProtocol == proto,
                                borderColor = if (selectedProtocol == proto) CpaPrimary else CpaBorder
                            ),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominent Full-Width Fetch Button
                Button(
                    onClick = {
                        if (urlInput.isBlank() || isFetching) return@Button
                        isFetching = true
                        fetchFeedback = null
                        onFetchFromUrl(urlInput.trim(), selectedProtocol) { success, message, count ->
                            isFetching = false
                            fetchFeedback = Pair(success, if (success) "تم بنجاح استيراد $count بروكسي وتعيين الأول كبروكسي نشط" else message)
                        }
                    },
                    enabled = !isFetching && urlInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CpaPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("fetch_proxies_button")
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري جلب وفحص البروكسيات...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جلب البروكسيات من الرابط (Fetch Proxies Now)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Feedback banner
                fetchFeedback?.let { (success, msg) ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (success) CpaSuccess.copy(alpha = 0.15f) else CpaError.copy(alpha = 0.15f))
                            .border(
                                1.dp,
                                if (success) CpaSuccess.copy(alpha = 0.5f) else CpaError.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (success) CpaSuccess else CpaError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = if (success) CpaSuccess else CpaError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 3. Secondary Actions: Manual Bulk Import & Single Add ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showBulkDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("bulk_import_proxies_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CpaCard,
                        contentColor = CpaAccent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CpaBorder)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual Bulk Paste", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showAddSingleDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("add_single_proxy_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CpaCard,
                        contentColor = CpaPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CpaBorder)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Single Proxy", fontSize = 11.sp)
                }
            }
        }

        // --- 4. Proxies List Section Header & Quick Action Toolbar ---
        item {
            val workingCount = proxies.count { it.status == "working" }
            val failedCount = proxies.count { it.status == "failed" }
            val untestedCount = proxies.size - workingCount - failedCount

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE PROXIES (${proxies.size})",
                            color = CpaTextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (proxies.isNotEmpty()) {
                            Text(
                                text = "اضغط على أي بروكسي لتعيينه كبروكسي نشط",
                                color = CpaTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Status Badges
                    if (proxies.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CpaSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("$workingCount شغال", color = CpaSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (failedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CpaError.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("$failedCount معطل", color = CpaError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Quick Action Bar
                if (proxies.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Test All
                        if (onTestAllProxies != null) {
                            Button(
                                onClick = {
                                    isTestingAll = true
                                    testingAllProgress = "0/${proxies.size}"
                                    onTestAllProxies(
                                        { current, total ->
                                            testingAllProgress = "$current/$total"
                                        },
                                        { _, _ ->
                                            isTestingAll = false
                                            testingAllProgress = null
                                        }
                                    )
                                },
                                enabled = !isTestingAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CpaPrimary,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).weight(1f).testTag("test_all_proxies_button")
                            ) {
                                if (isTestingAll) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(testingAllProgress ?: "...", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("فحص الكل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Auto Fastest
                        if (onAutoSelectFastest != null) {
                            OutlinedButton(
                                onClick = { onAutoSelectFastest() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CpaCard,
                                    contentColor = CpaPrimary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaPrimaryBorder)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).weight(1f).testTag("auto_select_fastest_button")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("الأسرع", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Delete Failed
                        if (onDeleteFailed != null && failedCount > 0) {
                            OutlinedButton(
                                onClick = { onDeleteFailed() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CpaCard,
                                    contentColor = CpaError
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaError.copy(alpha = 0.5f))),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).weight(1f).testTag("delete_failed_proxies_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("حذف المعطلة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reset Failed
                        if (onResetFailed != null && failedCount > 0) {
                            OutlinedButton(
                                onClick = { onResetFailed() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CpaCard,
                                    contentColor = CpaAccent
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaAccent.copy(alpha = 0.5f))),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).weight(1f).testTag("reset_failed_proxies_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("إعادة تفعيل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Export Working
                        if (onExportWorking != null && workingCount > 0) {
                            OutlinedButton(
                                onClick = {
                                    val exported = onExportWorking()
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exported))
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CpaCard,
                                    contentColor = CpaSuccess
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaSuccess.copy(alpha = 0.5f))),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).weight(1f).testTag("export_working_proxies_button")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("نسخ الشغالة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Empty state
        if (proxies.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CpaCard)
                        .border(1.dp, CpaBorder, RoundedCornerShape(10.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = CpaTextMuted,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Proxies In Pool",
                        color = CpaTextDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click 'Fetch Proxies' above to load residential IPs from your Asocks whitelist URL.",
                        color = CpaTextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            urlInput = IdentityService.DEFAULT_ASOCKS_URL
                            selectedProtocol = "socks5"
                            isFetching = true
                            fetchFeedback = null
                            onFetchFromUrl(IdentityService.DEFAULT_ASOCKS_URL, "socks5") { success, message, count ->
                                isFetching = false
                                fetchFeedback = Pair(success, if (success) "Loaded $count proxies from Asocks" else message)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CpaPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("quick_load_asocks_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-Tap Load 100 Asocks Proxies", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            itemsIndexed(proxies, key = { _, item -> item.id }) { index, proxy ->
                val isActive = currentSettings.proxyHost == proxy.host &&
                        currentSettings.proxyPort == proxy.port.toString()
                val isTesting = testingProxyId == proxy.id
                val testResult = testResultMap[proxy.id]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) CpaPrimaryDim.copy(alpha = 0.25f) else CpaCard)
                        .border(
                            1.dp,
                            if (isActive) CpaPrimary else CpaBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSetActiveProxy(proxy) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            color = CpaTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(34.dp)
                        )

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${proxy.host}:${proxy.port}",
                                    color = if (isActive) CpaPrimary else CpaText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isActive) CpaPrimary else CpaCardElevated)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = proxy.type.uppercase(),
                                        color = if (isActive) Color.Black else CpaTextDim,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (proxy.score > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val scoreColor = when {
                                        proxy.score >= 70 -> CpaSuccess
                                        proxy.score >= 40 -> CpaWarning
                                        else -> CpaError
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(scoreColor.copy(alpha = 0.2f))
                                            .border(1.dp, scoreColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "★ ${proxy.score}",
                                            color = scoreColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CpaSuccess)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (proxy.city.isNotBlank() || proxy.country.isNotBlank() || proxy.isp.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                val locStr = listOf(proxy.city, proxy.country).filter { it.isNotBlank() }.joinToString(", ")
                                val ispStr = if (proxy.isp.isNotBlank()) " · ${proxy.isp}" else ""
                                Text(
                                    text = "📍 $locStr$ispStr",
                                    color = CpaAccent,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (proxy.username.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Auth: ${proxy.username}:****",
                                    color = CpaTextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            testResult?.let { res ->
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = res,
                                    color = if (res.startsWith("Working") || res.startsWith("متصل") || res.startsWith("يعمل")) CpaSuccess else CpaError,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } ?: run {
                                if (proxy.lastPingMs > 0 || proxy.status != "untested") {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val statsStr = if (proxy.successCount > 0 || proxy.failCount > 0) " (نجاح: ${proxy.successCount} | فشل: ${proxy.failCount})" else ""
                                    val statusArabic = when (proxy.status) {
                                        "working" -> "يعمل بنجاح ✅"
                                        "active" -> "جاهز للاستخدام ⚡"
                                        "failed" -> "معطل ❌"
                                        else -> "غير مفحوص ⏳"
                                    }
                                    val pingStr = if (proxy.lastPingMs > 0) "Ping: ${proxy.lastPingMs}ms · " else ""
                                    Text(
                                        text = "$pingStr$statusArabic$statsStr",
                                        color = when (proxy.status) {
                                            "working" -> CpaSuccess
                                            "failed" -> CpaError
                                            "active" -> CpaAccent
                                            else -> CpaTextMuted
                                        },
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Test Button
                        IconButton(
                            onClick = {
                                testingProxyId = proxy.id
                                onTestProxy(proxy) { success, msg ->
                                    testingProxyId = null
                                    testResultMap = testResultMap + (proxy.id to msg)
                                }
                            },
                            enabled = !isTesting,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = CpaPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = "Test",
                                    tint = CpaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Delete button
                        IconButton(
                            onClick = { onDeleteProxy(proxy) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = CpaTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Bulk Import Dialog ---
    if (showBulkDialog) {
        var bulkText by remember { mutableStateOf("") }
        var bulkProtocol by remember { mutableStateOf("socks5") }

        Dialog(onDismissRequest = { showBulkDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = CpaCard
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .border(1.dp, CpaBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Text(
                        text = "MANUAL BULK PROXY IMPORT",
                        color = CpaPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste proxy lines in any of the following formats:\n• IP:PORT (e.g. 185.185.51.69:4220)\n• IP:PORT:USER:PASS\n• USER:PASS@IP:PORT",
                        color = CpaTextDim,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default:", color = CpaTextMuted, fontSize = 11.sp)
                        listOf("socks5", "http", "socks4").forEach { proto ->
                            FilterChip(
                                selected = bulkProtocol == proto,
                                onClick = { bulkProtocol = proto },
                                label = { Text(proto.uppercase(), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CpaPrimaryDim,
                                    selectedLabelColor = CpaPrimary,
                                    containerColor = CpaCardElevated,
                                    labelColor = CpaTextDim
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        placeholder = {
                            Text(
                                "185.185.51.69:4220\n175.110.115.153:19956\n...",
                                color = CpaTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CpaBg,
                            unfocusedContainerColor = CpaBg,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showBulkDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CpaTextMuted),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (bulkText.isNotBlank()) {
                                    onImportBulk(bulkText, bulkProtocol)
                                }
                                showBulkDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CpaPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Import Proxies", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- Add Single Proxy Dialog ---
    if (showAddSingleDialog) {
        var quickPaste by remember { mutableStateOf("") }
        var host by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("socks5") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var setActive by remember { mutableStateOf(true) }
        var isTestingDialog by remember { mutableStateOf(false) }
        var dialogTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

        fun parseAndFill(input: String) {
            val raw = input.trim()
            if (raw.isBlank()) return

            if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
                if (raw.contains("asocks") || raw.contains(".txt") || raw.contains("?") || raw.contains("/whitelist/")) {
                    dialogTestResult = Pair(false, "هذا رابط تصدير قائمة بروكسيات وليس بروكسي فردي.\nيرجى استيراده من خانة (جلب البروكسيات من الرابط) في الشاشة الرئيسية أو عبر الاستيراد الجماعي.")
                    return
                }
            }

            var working = raw
            if (working.startsWith("socks5://", ignoreCase = true)) {
                type = "socks5"
                working = working.substring("socks5://".length)
            } else if (working.startsWith("http://", ignoreCase = true)) {
                type = "http"
                working = working.substring("http://".length)
            } else if (working.startsWith("socks4://", ignoreCase = true)) {
                type = "socks4"
                working = working.substring("socks4://".length)
            }

            if (working.contains("@")) {
                val parts = working.split("@")
                if (parts.size >= 2) {
                    val creds = parts[0].split(":")
                    if (creds.isNotEmpty()) user = creds[0]
                    if (creds.size > 1) pass = creds[1]
                    val hp = parts[1].split(":")
                    if (hp.isNotEmpty()) host = hp[0].trim()
                    if (hp.size > 1) port = hp[1].filter { it.isDigit() }
                }
            } else if (working.contains(":")) {
                val parts = working.split(":")
                if (parts.size == 2) {
                    host = parts[0].trim()
                    port = parts[1].filter { it.isDigit() }
                } else if (parts.size >= 4) {
                    host = parts[0].trim()
                    port = parts[1].filter { it.isDigit() }
                    user = parts[2].trim()
                    pass = parts[3].trim()
                }
            } else {
                host = working.trim()
            }
        }

        Dialog(onDismissRequest = { showAddSingleDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = CpaCard
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .border(1.dp, CpaBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إضافة بروكسي منفرد / ADD PROXY",
                            color = CpaPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        IconButton(
                            onClick = { showAddSingleDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = CpaTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Paste & Auto-parse field
                    OutlinedTextField(
                        value = quickPaste,
                        onValueChange = {
                            quickPaste = it
                            dialogTestResult = null
                            parseAndFill(it)
                        },
                        label = { Text("لصق سريع (أي صيغة: host:port:user:pass)", color = CpaTextDim, fontSize = 10.sp) },
                        placeholder = { Text("185.185.51.69:4220 أو socks5://u:p@host:port", color = CpaTextMuted, fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        quickPaste = clip.trim()
                                        parseAndFill(clip.trim())
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CpaPrimary, modifier = Modifier.size(15.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CpaBg,
                            unfocusedContainerColor = CpaBg,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = host,
                        onValueChange = {
                            if (it.contains(":") || it.contains("@")) {
                                parseAndFill(it)
                            } else {
                                host = it
                            }
                            dialogTestResult = null
                        },
                        label = { Text("Proxy Host / IP *", color = CpaTextDim, fontSize = 10.sp) },
                        placeholder = { Text("185.185.51.69", color = CpaTextMuted, fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CpaBg,
                            unfocusedContainerColor = CpaBg,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = port,
                            onValueChange = {
                                port = it.filter { ch -> ch.isDigit() }
                                dialogTestResult = null
                            },
                            label = { Text("Port *", color = CpaTextDim, fontSize = 10.sp) },
                            placeholder = { Text("4220", color = CpaTextMuted, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CpaBg,
                                unfocusedContainerColor = CpaBg,
                                focusedBorderColor = CpaPrimary,
                                unfocusedBorderColor = CpaBorder,
                                focusedTextColor = CpaText,
                                unfocusedTextColor = CpaText
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Protocol", color = CpaTextMuted, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("socks5", "http").forEach { p ->
                                    FilterChip(
                                        selected = type == p,
                                        onClick = { type = p },
                                        label = { Text(p.uppercase(), fontSize = 9.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CpaPrimaryDim,
                                            selectedLabelColor = CpaPrimary
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = user,
                        onValueChange = {
                            user = it
                            dialogTestResult = null
                        },
                        label = { Text("Username (Optional)", color = CpaTextDim, fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CpaBg,
                            unfocusedContainerColor = CpaBg,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = pass,
                        onValueChange = {
                            pass = it
                            dialogTestResult = null
                        },
                        label = { Text("Password (Optional)", color = CpaTextDim, fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CpaBg,
                            unfocusedContainerColor = CpaBg,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Activate Immediately Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { setActive = !setActive },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = setActive,
                            onCheckedChange = { setActive = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CpaPrimary,
                                checkmarkColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعيين كبروكسي نشط فوراً (Set Active)",
                            color = CpaText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Test Proxy Result Banner
                    dialogTestResult?.let { (ok, msg) ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (ok) CpaSuccess.copy(alpha = 0.15f) else CpaError.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (ok) CpaSuccess else CpaError,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = msg,
                                color = if (ok) CpaSuccess else CpaError,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Test Button
                        OutlinedButton(
                            onClick = {
                                val p = port.toIntOrNull()
                                if (host.isNotBlank() && p != null && p > 0) {
                                    isTestingDialog = true
                                    dialogTestResult = null
                                    val testItem = ProxyItem(
                                        host = host.trim(),
                                        port = p,
                                        type = type,
                                        username = user.trim(),
                                        password = pass.trim()
                                    )
                                    onTestProxy(testItem) { ok, msg ->
                                        isTestingDialog = false
                                        dialogTestResult = Pair(ok, msg)
                                    }
                                }
                            },
                            enabled = host.isNotBlank() && port.toIntOrNull() != null && !isTestingDialog,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CpaPrimary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CpaPrimary)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isTestingDialog) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = CpaPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص الاتصال", fontSize = 11.sp)
                            }
                        }

                        Row {
                            OutlinedButton(
                                onClick = { showAddSingleDialog = false },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CpaTextMuted),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("إلغاء", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val p = port.toIntOrNull()
                                    if (host.isNotBlank() && p != null && p > 0) {
                                        val newProxy = ProxyItem(
                                            host = host.trim(),
                                            port = p,
                                            type = type,
                                            username = user.trim(),
                                            password = pass.trim()
                                        )
                                        onAddSingleProxy(newProxy)
                                        if (setActive) {
                                            onSetActiveProxy(newProxy)
                                        }
                                        showAddSingleDialog = false
                                    }
                                },
                                enabled = host.isNotBlank() && port.toIntOrNull() != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CpaPrimary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("حفظ البروكسي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Clear All Confirmation Dialog ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Clear All Proxies?", color = CpaText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(
                    "This will remove all ${proxies.size} proxies from the local rotation pool. You can re-fetch them from your Asocks URL at any time.",
                    color = CpaTextDim,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CpaError, contentColor = Color.White)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirmDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CpaTextDim)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = CpaCard
        )
    }
}
