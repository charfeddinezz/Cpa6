package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.AutomationState
import com.example.data.model.OfferClickItem
import androidx.compose.material.icons.filled.Lock
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClickScreen(
    clickItems: List<OfferClickItem>,
    automationState: AutomationState,
    settings: AppSettings = AppSettings(),
    onUpdateSettings: ((AppSettings) -> Unit)? = null,
    onAddItem: (text: String, tag: String) -> Unit,
    onUpdateItem: (OfferClickItem) -> Unit,
    onToggleItem: (id: Long, enabled: Boolean) -> Unit,
    onDeleteItem: (id: Long) -> Unit,
    onMoveItem: (id: Long, direction: Int) -> Unit,
    onResetDefaults: () -> Unit,
    onTestInBrowser: (text: String) -> Unit,
    onLaunchLandingBlogspot: () -> Unit,
    onRunAutomation: () -> Unit,
    onRunNikeDirect: () -> Unit = onRunAutomation,
    modifier: Modifier = Modifier
) {
    var newText by remember { mutableStateOf("") }
    var newTag by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<OfferClickItem?>(null) }
    var itemToDelete by remember { mutableStateOf<OfferClickItem?>(null) }

    val enabledCount = clickItems.count { it.enabled }
    val totalClicks = clickItems.sumOf { it.clickCount }
    val sortedItems = remember(clickItems) { clickItems.sortedBy { it.orderIndex } }

    val presetSuggestions = listOf(
        "Get \$1000 Walmart gift card" to "Walmart $1000",
        "Claim \$750 Cash App Reward" to "Cash App $750",
        "Get \$500 Amazon Gift Card" to "Amazon $500",
        "Win \$100 Target Gift Card" to "Target $100",
        "Claim \$500 Apple Store Card" to "Apple Card",
        "Claim Your Free Gas Card" to "Gas Card $250",
        "Continue to Survey Offer" to "General Survey"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. Header & Priority #1 Directive Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CpaCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CpaPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CpaPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = "Click Priority",
                                    tint = CpaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "شاشة النقرة على العرض",
                                        color = CpaText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CpaWarning.copy(alpha = 0.2f))
                                            .border(1.dp, CpaWarning, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "أولوية تنفيذ #1",
                                            color = CpaWarning,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "Offer Click Engine • First Priority Step",
                                    color = CpaTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onResetDefaults,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CpaCardElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Defaults",
                                tint = CpaTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "صنف «النقرة على العرض» دائماً يكون الخطوة رقم 1 في دورة التنفيذ: يبحث في صفحة الهبوط أو جسر العرض (مثل gdfqo.blogspot.com) عن النص المحدد، وينقر عليه فوراً لتحويل المتصفح إلى صفحة موقع العرض الأساسية لمتابعة إكمال الاستبيان أو الفورم.",
                        color = CpaTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBadge(
                            label = "إجمالي النصوص",
                            value = "${clickItems.size}",
                            color = CpaText,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "مفعلة بالتسلسل",
                            value = "$enabledCount",
                            color = if (enabledCount > 0) CpaSuccess else CpaError,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "النقرات الناجحة",
                            value = "$totalClicks",
                            color = CpaPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- 1.5. Locker Integration & New Tab Engine Card (ربط اللوكر والانتقال لتبويب جديد) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CpaCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (automationState.lockerDetectedOnPage) CpaSuccess else CpaPrimaryBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (automationState.lockerDetectedOnPage) CpaSuccess.copy(alpha = 0.2f) else CpaPrimaryDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (automationState.lockerDetectedOnPage) CpaSuccess else CpaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "محرك اللوكر والتبويب الجديد",
                                        color = CpaText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (automationState.lockerDetectedOnPage) CpaSuccess.copy(alpha = 0.15f) else CpaPrimaryDim)
                                            .border(0.5.dp, if (automationState.lockerDetectedOnPage) CpaSuccess else CpaPrimaryBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (automationState.lockerDetectedOnPage) "مكتشف بالصفحة ✅" else "رصد تلقائي نشط 🛡️",
                                            color = if (automationState.lockerDetectedOnPage) CpaSuccess else CpaPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "Auto Locker Detection • Click & Multi-Tab Isolation",
                                    color = CpaTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Automated Locker & Tab Status Chip (Fully Automated - No Manual Button)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (automationState.lockerOfferClicked) CpaSuccess.copy(alpha = 0.2f)
                                    else if (automationState.lockerDetectedOnPage) CpaPrimary.copy(alpha = 0.2f)
                                    else CpaCardElevated
                                )
                                .border(
                                    0.5.dp,
                                    if (automationState.lockerOfferClicked) CpaSuccess
                                    else if (automationState.lockerDetectedOnPage) CpaPrimary
                                    else CpaBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (automationState.lockerOfferClicked) CpaSuccess
                                            else if (automationState.lockerDetectedOnPage) CpaPrimary
                                            else CpaTextMuted
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (automationState.lockerOfferClicked) "تم النقر ➔ تبويب جديد 🚀"
                                           else if (automationState.lockerDetectedOnPage) "لوكر نشط ومجهز 🔒"
                                           else "مؤتمت بالكامل ⚡",
                                    color = if (automationState.lockerOfferClicked) CpaSuccess
                                            else if (automationState.lockerDetectedOnPage) CpaPrimary
                                            else CpaTextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🔗 الربط التلقائي بين Offer Click واللوكر:\nعند إضافة تصنيفي 'Offer Click' و 'Locker' في المهمة، يقوم المحرك تلقائياً بانتظار ظهور اللوكر على الصفحة (أو داخل iframe)، وفحص قائمة العروض ومطابقتها مع نصوص وعروض هذه الشاشة، ثم النقر على العرض المختار. بعد النقر يتم فتح موقع العرض تلقائياً في تبويب جديد منفصل، ونقل بيئة العمل والمحاكاة إليه فوراً لإكمال مهام التحويل والتأكيد!",
                        color = CpaTextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )

                    // Live Status Telemetry
                    if (automationState.lockerDetectedOnPage || automationState.lockerOfferClicked || automationState.newTabActionStatus.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaCardElevated)
                                .border(0.5.dp, CpaBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (automationState.lockerDetectedOnPage) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CpaSuccess, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "معرف اللوكر النشط: ${automationState.lockerId ?: "معروف"} | العروض المتوفرة: ${automationState.lockerOffersFoundCount}",
                                            color = CpaSuccess,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (automationState.newTabActionStatus.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = CpaPrimary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = automationState.newTabActionStatus,
                                            color = CpaPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (automationState.lastClickedOfferUrl != null) {
                                    Text(
                                        text = "الرابط الهدف: ${automationState.lastClickedOfferUrl}",
                                        color = CpaTextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Controls & Options
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Switch 1: Auto Trigger Locker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التعامل التلقائي مع اللوكر", color = CpaText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("تشغيل call_locker() تلقائياً وفك حظر السكربتات", color = CpaTextMuted, fontSize = 9.5.sp)
                            }
                            Switch(
                                checked = settings.cpaLockerAutoTrigger,
                                onCheckedChange = { onUpdateSettings?.invoke(settings.copy(cpaLockerAutoTrigger = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CpaPrimary)
                            )
                        }

                        // Switch 2: Open In New Tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("الانتقال التلقائي إلى تبويب جديد", color = CpaText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("فتح رابط العرض في توبيت جديد والتركيز عليه فوراً", color = CpaTextMuted, fontSize = 9.5.sp)
                            }
                            Switch(
                                checked = settings.offerClickOpenInNewTab,
                                onCheckedChange = { onUpdateSettings?.invoke(settings.copy(offerClickOpenInNewTab = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CpaPrimary)
                            )
                        }

                        // Switch 3: Simulate Human Interaction
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("محاكاة التصفح البشري في التبويب الجديد", color = CpaText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text("تمرير متدرج وقضاء مدة طبيعية (${settings.offerClickStayDurationSec} ثوانٍ) وفحص الاستبيان", color = CpaTextMuted, fontSize = 9.5.sp)
                            }
                            Switch(
                                checked = settings.offerClickAutoSimulateHuman,
                                onCheckedChange = { onUpdateSettings?.invoke(settings.copy(offerClickAutoSimulateHuman = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CpaPrimary)
                            )
                        }

                        // Strategy Selector Chips
                        Column {
                            Text("استراتيجية اختيار العرض من اللوكر:", color = CpaTextDim, fontSize = 10.5.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    "priority" to "حسب نصوص الأولوية 🎯",
                                    "first" to "أول عرض متاح 1️⃣",
                                    "random" to "عشوائي 🎲"
                                ).forEach { (stratKey, stratLabel) ->
                                    val isSelected = settings.offerSelectionStrategy == stratKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) CpaPrimary else CpaCardElevated)
                                            .border(1.dp, if (isSelected) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                                            .clickable { onUpdateSettings?.invoke(settings.copy(offerSelectionStrategy = stratKey)) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = stratLabel,
                                            color = if (isSelected) Color.Black else CpaText,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Stay Duration Chips
                        Column {
                            Text("مدة البقاء في صفحة العرض الجديد:", color = CpaTextDim, fontSize = 10.5.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(10, 15, 25, 40).forEach { sec ->
                                    val isSelected = settings.offerClickStayDurationSec == sec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) CpaPrimary else CpaCardElevated)
                                            .border(1.dp, if (isSelected) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                                            .clickable { onUpdateSettings?.invoke(settings.copy(offerClickStayDurationSec = sec)) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            color = if (isSelected) Color.Black else CpaText,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Target Landing Bridge Card (https://gdfqo.blogspot.com) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CpaCardElevated),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Target Website",
                            tint = CpaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "موقع جسر العروض المحدد",
                                color = CpaTextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "https://gdfqo.blogspot.com",
                                color = CpaText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onLaunchLandingBlogspot,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CpaPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فتح الموقع", fontSize = 10.sp)
                        }

                        Button(
                            onClick = onRunNikeDirect,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (automationState.isRunning) CpaError else CpaSuccess)
                        ) {
                            Icon(
                                imageVector = if (automationState.isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (automationState.isRunning) "إيقاف" else "تشغيل Nike Direct", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // --- 3. Active Sequential Indicator & Quick Actions ---
        item {
            val activeText = automationState.activeClickText ?: sortedItems.firstOrNull { it.enabled }?.text
            Card(
                colors = CardDefaults.cardColors(containerColor = CpaCard),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النص الفعال في التسلسل الحالي:",
                            color = CpaTextMuted,
                            fontSize = 11.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CpaSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "اختيار تسلسلي تلقائي",
                                color = CpaSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (!activeText.isNullOrBlank()) "« $activeText »" else "لا يوجد نص مفعل حالياً",
                        color = if (!activeText.isNullOrBlank()) CpaPrimary else CpaTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    if (!automationState.lastClickedOfferUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "التحول الأخير: ${automationState.lastClickedOfferUrl}",
                            color = CpaSuccess,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- 4. Manual Add Text Section Header & Button ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "نصوص النقرة اليدوية بالتسلسل",
                        color = CpaText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "يتم تنفيذ النصوص بالترتيب مع إمكانية إيقاف/تفعيل أي نص",
                        color = CpaTextMuted,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_click_text_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Text", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "إضافة نص", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 5. Quick Preset Chips ---
        item {
            Column {
                Text(
                    text = "اقتراحات سريعة لنصوص العروض الأكثر تحويلاً (اضغط للإضافة):",
                    color = CpaTextMuted,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetSuggestions.forEach { (text, tag) ->
                        val alreadyAdded = clickItems.any { it.text.equals(text, ignoreCase = true) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (alreadyAdded) CpaCardElevated else CpaPrimaryDim)
                                .border(
                                    1.dp,
                                    if (alreadyAdded) CpaBorder else CpaPrimary.copy(alpha = 0.5f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (!alreadyAdded) {
                                        onAddItem(text, tag)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (alreadyAdded) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = CpaSuccess,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = CpaPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = text,
                                    color = if (alreadyAdded) CpaTextMuted else CpaText,
                                    fontSize = 10.sp,
                                    fontWeight = if (alreadyAdded) FontWeight.Normal else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 6. Click Items List ---
        if (sortedItems.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CpaCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = CpaTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد نصوص نقرة مضافة حالياً",
                            color = CpaText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أضف نصوص العروض يدوياً أو اضغط على استعادة الافتراضيات",
                            color = CpaTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onResetDefaults,
                            colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary)
                        ) {
                            Text("استعادة النصوص الافتراضية", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(sortedItems, key = { _, item -> item.id }) { index, item ->
                ClickItemCard(
                    item = item,
                    sequenceNumber = index + 1,
                    isFirst = index == 0,
                    isLast = index == sortedItems.size - 1,
                    onToggle = { onToggleItem(item.id, it) },
                    onMoveUp = { onMoveItem(item.id, -1) },
                    onMoveDown = { onMoveItem(item.id, 1) },
                    onEdit = { itemToEdit = item },
                    onDelete = { itemToDelete = item },
                    onTestInBrowser = { onTestInBrowser(item.text) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- Dialog: Add New Click Text ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CpaCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = CpaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة نص نقرة جديد",
                        color = CpaText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أدخل النص الظاهر في صفحة الهبوط تماماً كما يظهر للزائر (مثال: Get $1000 Walmart gift card):",
                        color = CpaTextMuted,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        label = { Text("نص العرض المراد النقر عليه") },
                        placeholder = { Text("Get $1000 Walmart gift card") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("click_text_input")
                    )

                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("ملاحظة / تصنيف العرض (اختياري)") },
                        placeholder = { Text("Walmart Offer 1000") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newText.isNotBlank()) {
                            onAddItem(newText, newTag)
                            newText = ""
                            newTag = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                    enabled = newText.isNotBlank()
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = CpaTextMuted)
                }
            }
        )
    }

    // --- Dialog: Edit Click Text ---
    itemToEdit?.let { item ->
        var editText by remember(item) { mutableStateOf(item.text) }
        var editTag by remember(item) { mutableStateOf(item.tagOrNote) }
        var editEnabled by remember(item) { mutableStateOf(item.enabled) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            containerColor = CpaCard,
            title = {
                Text("تعديل نص النقرة", color = CpaText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("النص") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTag,
                        onValueChange = { editTag = it },
                        label = { Text("التصنيف / الملاحظة") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CpaText,
                            unfocusedTextColor = CpaText,
                            focusedBorderColor = CpaPrimary,
                            unfocusedBorderColor = CpaBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مفعل في التسلسل", color = CpaText, fontSize = 12.sp)
                        Switch(
                            checked = editEnabled,
                            onCheckedChange = { editEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CpaPrimary,
                                checkedTrackColor = CpaPrimaryDim
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editText.isNotBlank()) {
                            onUpdateItem(item.copy(text = editText.trim(), tagOrNote = editTag.trim(), enabled = editEnabled))
                            itemToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) {
                    Text("إلغاء", color = CpaTextMuted)
                }
            }
        )
    }

    // --- Dialog: Delete Confirmation ---
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = CpaCard,
            title = {
                Text("حذف نص النقرة", color = CpaError, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف النص «${item.text}»؟",
                    color = CpaText,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CpaError)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("إلغاء", color = CpaTextMuted)
                }
            }
        )
    }
}

@Composable
private fun ClickItemCard(
    item: OfferClickItem,
    sequenceNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestInBrowser: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.enabled) CpaCard else CpaCard.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.enabled) CpaPrimaryBorder else CpaBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("click_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Sequence Number + Tag + Priority Badge + Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sequence Badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (item.enabled) CpaPrimary else CpaTextMuted.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$sequenceNumber",
                            color = if (item.enabled) Color.Black else CpaTextMuted,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (item.tagOrNote.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CpaCardElevated)
                                .border(1.dp, CpaBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.tagOrNote,
                                color = CpaTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (item.clickCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CpaSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${item.clickCount} نقرة ناجحة",
                                color = CpaSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Enabled/Disabled Toggle Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.enabled) "مفعل" else "مغلق",
                        color = if (item.enabled) CpaSuccess else CpaTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CpaPrimary,
                            checkedTrackColor = CpaPrimaryDim,
                            uncheckedThumbColor = CpaTextMuted,
                            uncheckedTrackColor = CpaCardElevated
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Click Text
            Text(
                text = item.text,
                color = if (item.enabled) CpaText else CpaTextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions row: Move up, Move down, Edit, Test, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ordering arrows
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Up",
                            tint = if (!isFirst) CpaText else CpaTextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Move Down",
                            tint = if (!isLast) CpaText else CpaTextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Test in Browser button
                    OutlinedButton(
                        onClick = onTestInBrowser,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CpaPrimary),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "اختبار النقر", fontSize = 10.sp)
                    }

                    // Edit
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = CpaTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CpaError,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CpaCardElevated)
            .border(1.dp, CpaBorder, RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = label, color = CpaTextMuted, fontSize = 9.sp)
        }
    }
}
