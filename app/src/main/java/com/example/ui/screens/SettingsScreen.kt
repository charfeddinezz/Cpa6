package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.model.AppSettings
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
import com.example.ui.theme.CpaWarning
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted

@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onTestCpa: ((Boolean, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var waitBetweenTasksStr by remember { mutableStateOf(currentSettings.waitBetweenTasks.toString()) }
    var cycleIntervalMinStr by remember { mutableStateOf(currentSettings.cycleIntervalMinSec.toString()) }
    var cycleIntervalMaxStr by remember { mutableStateOf(currentSettings.cycleIntervalMaxSec.toString()) }
    var cpaUserId by remember { mutableStateOf(currentSettings.cpaUserId) }
    var cpaApiKey by remember { mutableStateOf(currentSettings.cpaApiKey) }

    var captchaProvider by remember { mutableStateOf(currentSettings.captchaProvider) }
    var captchaApiKey by remember { mutableStateOf(currentSettings.captchaApiKey) }

    var proxyAutoRotate by remember { mutableStateOf(currentSettings.proxyAutoRotate) }
    var webrtcMode by remember { mutableStateOf(currentSettings.webrtcMode) }
    var webrtcCustomIp by remember { mutableStateOf(currentSettings.webrtcCustomIp) }
    var forceProxyDns by remember { mutableStateOf(currentSettings.forceProxyDns) }

    var cpaLockerAutoTrigger by remember { mutableStateOf(currentSettings.cpaLockerAutoTrigger) }
    var cpaLockerDefaultId by remember { mutableStateOf(currentSettings.cpaLockerDefaultId) }
    var cpaLockerAutoInjectIfMissing by remember { mutableStateOf(currentSettings.cpaLockerAutoInjectIfMissing) }

    var isTestingCpa by remember { mutableStateOf(false) }
    var cpaTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Automation Cooldown & Loop Cycle Timing
        item {
            SettingsCard(title = "AUTOMATION LOOP & CYCLE TIMING", icon = Icons.Default.Timer) {
                Text("Loop Cycle Interval Range (Seconds): 15 - 25s", color = CpaPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("The automation loop pauses naturally between 15 and 25 seconds between actions.", color = CpaTextMuted, fontSize = 10.5.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Min Cycle (Sec)", color = CpaTextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = cycleIntervalMinStr,
                            onValueChange = { cycleIntervalMinStr = it.filter { c -> c.isDigit() } },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CpaPrimary,
                                unfocusedBorderColor = CpaBorder,
                                focusedTextColor = CpaText,
                                unfocusedTextColor = CpaText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Max Cycle (Sec)", color = CpaTextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = cycleIntervalMaxStr,
                            onValueChange = { cycleIntervalMaxStr = it.filter { c -> c.isDigit() } },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CpaPrimary,
                                unfocusedBorderColor = CpaBorder,
                                focusedTextColor = CpaText,
                                unfocusedTextColor = CpaText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Wait Time Between Campaign Tasks (seconds)", color = CpaTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = waitBetweenTasksStr,
                    onValueChange = { waitBetweenTasksStr = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // CPA Grip Postback & Verification
        item {
            SettingsCard(title = "CPA GRIP API INTEGRATION", icon = Icons.Default.Api) {
                Text("User ID", color = CpaTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cpaUserId,
                    onValueChange = { cpaUserId = it },
                    placeholder = { Text("e.g. 123456", color = CpaTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("API / RSS Key", color = CpaTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cpaApiKey,
                    onValueChange = { cpaApiKey = it },
                    placeholder = { Text("e.g. a1b2c3d4e5f6...", color = CpaTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            isTestingCpa = true
                            onTestCpa { success, msg ->
                                isTestingCpa = false
                                cpaTestResult = Pair(success, msg)
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CpaPrimaryBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTestingCpa) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CpaPrimary)
                        } else {
                            Text("Test CPA Connection", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    cpaTestResult?.let { (ok, msg) ->
                        Text(
                            text = if (ok) "Connected!" else "Check credentials",
                            color = if (ok) CpaSuccess else CpaError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Captcha Solver
        item {
            SettingsCard(title = "CAPTCHA SOLVER SERVICE", icon = Icons.Default.Security) {
                Text("Provider", color = CpaTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))

                val providers = listOf("none", "2captcha", "anticaptcha", "capsolver")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    providers.forEach { prov ->
                        val isSel = captchaProvider == prov
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                .border(1.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                                .clickable { captchaProvider = prov }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (prov == "none") "None" else prov.replaceFirstChar { it.uppercase() },
                                color = if (isSel) CpaPrimary else CpaTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (captchaProvider != "none") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("API Key", color = CpaTextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = captchaApiKey,
                        onValueChange = { captchaApiKey = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Proxy & Network Spoofing Configuration
        item {
            SettingsCard(title = "PROXY & NETWORK SPOOFING", icon = Icons.Default.Router) {
                // Active Proxy Display
                val hasProxy = currentSettings.proxyType != "none" && currentSettings.proxyHost.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasProxy) CpaPrimaryDim else CpaCardElevated)
                        .border(1.dp, if (hasProxy) CpaPrimaryBorder else CpaBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (hasProxy) CpaSuccess else CpaWarning)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasProxy) "Active Proxy: ${currentSettings.proxyType.uppercase()} · ${currentSettings.proxyHost}:${currentSettings.proxyPort}" else "No Active Proxy Configured",
                                color = if (hasProxy) CpaPrimary else CpaTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Proxies, credentials, and auto-fetching lists are configured and tested in the dedicated 'Proxies' screen.",
                            color = CpaTextDim,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Rotate IP Per Task", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rotates to next proxy in pool automatically every run", color = CpaTextDim, fontSize = 11.sp)
                    }
                    Switch(
                        checked = proxyAutoRotate,
                        onCheckedChange = { proxyAutoRotate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CpaPrimary,
                            checkedTrackColor = CpaPrimaryBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Force DNS Through Proxy", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CpaSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("Zero Leak", color = CpaSuccess, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Routes all DNS queries through remote proxy, disabling Google Safe Browsing and speculative prefetching", color = CpaTextDim, fontSize = 10.sp, lineHeight = 13.sp)
                    }
                    Switch(
                        checked = forceProxyDns,
                        onCheckedChange = { forceProxyDns = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CpaPrimary,
                            checkedTrackColor = CpaPrimaryBorder
                        )
                    )
                }
            }
        }

        // WebRTC Configuration
        item {
            SettingsCard(title = "WEBRTC IP & LEAK PROTECTION", icon = Icons.Default.Security) {
                Text("WebRTC IP Behavior", color = CpaTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))

                val webrtcOptions = listOf(
                    "spoof" to "Spoof Proxy IP",
                    "disabled" to "Kill WebRTC",
                    "real" to "Native WebRTC"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    webrtcOptions.forEach { (modeVal, label) ->
                        val isSel = webrtcMode == modeVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                .border(1.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                                .clickable { webrtcMode = modeVal }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) CpaPrimary else CpaTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (webrtcMode == "spoof") {
                    Text("Custom WebRTC IP (Optional Override)", color = CpaTextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = webrtcCustomIp,
                        onValueChange = { webrtcCustomIp = it },
                        placeholder = { Text("Auto-detect from extracted Proxy IP", color = CpaTextDim) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder,
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "When left empty, WebRTC STUN/ICE candidates automatically reflect the active extracted Proxy IP on BrowserLeaks and Whoer.",
                        color = CpaTextDim,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                } else if (webrtcMode == "disabled") {
                    Text(
                        text = "WebRTC APIs (RTCPeerConnection) are completely stripped and neutralized. Websites see WebRTC as unsupported.",
                        color = CpaWarning,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                } else {
                    Text(
                        text = "Native Android WebView WebRTC without modification. May bypass proxy over direct cellular/Wi-Fi UDP.",
                        color = CpaError,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // CPA Content Locker & Script Integration
        item {
            SettingsCard(title = "CPA CONTENT LOCKER & SCRIPT ENGINE", icon = Icons.Default.LockOpen) {
                Text("Default Locker ID", color = CpaTextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cpaLockerDefaultId,
                    onValueChange = { cpaLockerDefaultId = it.trim() },
                    placeholder = { Text("e.g. 1741238", color = CpaTextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controls default ID used for alignmentfiles.com / cpagrip script_include.php. Can be overridden per page.",
                    color = CpaTextDim,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Trigger Locker (call_locker)", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Unblocks document.write, ensures jQuery exists, and triggers call_locker() automatically when detected.", color = CpaTextDim, fontSize = 10.sp, lineHeight = 13.sp)
                    }
                    Switch(
                        checked = cpaLockerAutoTrigger,
                        onCheckedChange = { cpaLockerAutoTrigger = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CpaPrimary,
                            checkedTrackColor = CpaPrimaryBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Inject Script If Missing", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("If the webpage doesn't already contain the locker script, injects and executes it automatically.", color = CpaTextDim, fontSize = 10.sp, lineHeight = 13.sp)
                    }
                    Switch(
                        checked = cpaLockerAutoInjectIfMissing,
                        onCheckedChange = { cpaLockerAutoInjectIfMissing = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CpaPrimary,
                            checkedTrackColor = CpaPrimaryBorder
                        )
                    )
                }
            }
        }

        // Session Purge & Privacy Reset
        item {
            SettingsCard(title = "SESSION PURGE & PRIVACY RESET", icon = Icons.Default.DeleteSweep) {
                Text(
                    text = "Clear all cached assets, cookies, and HTML5 local storage on demand to ensure previous real-IP sessions never leak tracking tokens or cached scripts across proxy rotations.",
                    color = CpaTextDim,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        MainActivity.clearWebViewData(context) {
                            Toast.makeText(context, "WebView cache, cookies & storage successfully purged", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("purge_webview_data_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CpaError.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Purge Session", tint = CpaError)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Purge Cache, Cookies & Storage Now", color = CpaError, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val updated = currentSettings.copy(
                        waitBetweenTasks = waitBetweenTasksStr.toIntOrNull() ?: 5,
                        cycleIntervalMinSec = cycleIntervalMinStr.toIntOrNull() ?: 15,
                        cycleIntervalMaxSec = cycleIntervalMaxStr.toIntOrNull() ?: 25,
                        cpaUserId = cpaUserId.trim(),
                        cpaApiKey = cpaApiKey.trim(),
                        captchaProvider = captchaProvider,
                        captchaApiKey = captchaApiKey.trim(),
                        proxyAutoRotate = proxyAutoRotate,
                        webrtcMode = webrtcMode,
                        webrtcCustomIp = webrtcCustomIp.trim(),
                        forceProxyDns = forceProxyDns,
                        cpaLockerAutoTrigger = cpaLockerAutoTrigger,
                        cpaLockerDefaultId = cpaLockerDefaultId.trim().ifBlank { "1741238" },
                        cpaLockerAutoInjectIfMissing = cpaLockerAutoInjectIfMissing
                    )
                    onSaveSettings(updated)
                    Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CpaCard)
            .border(1.dp, CpaBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = CpaPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = CpaPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CpaBorder))
            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}
