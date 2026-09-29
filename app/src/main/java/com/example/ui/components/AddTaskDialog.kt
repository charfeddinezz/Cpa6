package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TaskEntity
import com.example.data.model.TaskMode
import com.example.service.IdentityService
import com.example.service.TaskCategoryPlanner
import com.example.ui.theme.CpaAccent
import com.example.ui.theme.CpaAccentDim
import com.example.ui.theme.CpaBg
import com.example.ui.theme.CpaBorder
import com.example.ui.theme.CpaCard
import com.example.ui.theme.CpaCardElevated
import com.example.ui.theme.CpaPrimary
import com.example.ui.theme.CpaPrimaryBorder
import com.example.ui.theme.CpaPrimaryDim
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    taskToEdit: TaskEntity?,
    workTemplates: List<com.example.data.model.WorkTemplateEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Basic, 1: AI Funnel, 2: Mode, 3: Advanced

    var name by remember { mutableStateOf(taskToEdit?.name ?: "") }
    var url by remember { mutableStateOf(taskToEdit?.url ?: "") }
    var referer by remember { mutableStateOf(taskToEdit?.referer ?: IdentityService.RANDOM_REFERRERS[0]) }
    var userAgent by remember { mutableStateOf(taskToEdit?.userAgent ?: "random") }
    var mode by remember { mutableStateOf(taskToEdit?.mode ?: TaskMode.MODE1.id) }
    var repeatCountStr by remember { mutableStateOf((taskToEdit?.repeatCount ?: 1).toString()) }
    var workTemplateId by remember { mutableStateOf(taskToEdit?.workTemplateId) }

    var categoriesList by remember {
        mutableStateOf(
            TaskCategoryPlanner.parseCategories(taskToEdit?.categories ?: "Email Submit, Survey / Quiz")
        )
    }
    var customCategoryInput by remember { mutableStateOf("") }

    // Mode 1 config
    var browserDurationStr by remember { mutableStateOf((taskToEdit?.browserDuration ?: 45).toString()) }
    var mode1RepeatStr by remember { mutableStateOf((taskToEdit?.mode1RepeatCount ?: 1).toString()) }

    // Mode 2 config
    var taskDurationStr by remember { mutableStateOf((taskToEdit?.taskDuration ?: 60).toString()) }
    var taskRepeatCountStr by remember { mutableStateOf((taskToEdit?.taskRepeatCount ?: 3).toString()) }
    var operationRepeatCountStr by remember { mutableStateOf((taskToEdit?.operationRepeatCount ?: 1).toString()) }

    // Mode 3 config
    var completionKeywords by remember {
        mutableStateOf(taskToEdit?.completionKeywords ?: "thank you, congratulations, success, completed, verified")
    }

    // Repetition & Clean Session Config
    var sequentialProxyRotation by remember { mutableStateOf(taskToEdit?.sequentialProxyRotation ?: true) }
    var clearCacheOnRepeat by remember { mutableStateOf(taskToEdit?.clearCacheOnRepeat ?: true) }
    var newIdentityOnRepeat by remember { mutableStateOf(taskToEdit?.newIdentityOnRepeat ?: true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CpaCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (taskToEdit == null) "NEW CPA TASK" else "EDIT TASK",
                        color = CpaText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CpaTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Basic / Mode / Advanced
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CpaCardElevated)
                        .padding(3.dp)
                ) {
                    listOf("Basic", "AI Funnel", "Mode", "Advanced").forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CpaPrimaryDim else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSelected) CpaPrimary else CpaTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // BASIC TAB
                        Text("TASK NAME *", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("e.g. Gift Card Survey", color = CpaTextDim) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CpaPrimary,
                                unfocusedBorderColor = CpaBorder,
                                focusedTextColor = CpaText,
                                unfocusedTextColor = CpaText
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("task_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("TARGET URL *", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = url,
                                onValueChange = { 
                                    url = it
                                    if (it.length > 8 && (it.contains("consumertestconnect") || it.contains("cpa") || it.contains("sweep"))) {
                                        val plan = TaskCategoryPlanner.extractFunnelPlanFromUrl(it)
                                        if (name.isBlank()) name = plan.detectedName
                                    }
                                },
                                placeholder = { Text("https://example.com/offer", color = CpaTextDim) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder,
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText
                                ),
                                modifier = Modifier.weight(1f).testTag("task_url_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (url.isNotBlank()) {
                                        url = IdentityService.generateUTM(url)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CpaPrimaryDim),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CpaPrimaryBorder)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "UTM", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UTM", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Auto-Extract & Optimize Plan Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaPrimaryDim)
                                .border(1.dp, CpaPrimaryBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val targetToAnalyze = url.ifBlank { "https://consumertestconnect.com/ctc-100gcsweep" }
                                    if (url.isBlank()) url = targetToAnalyze
                                    val plan = TaskCategoryPlanner.extractFunnelPlanFromUrl(targetToAnalyze)
                                    name = plan.detectedName
                                    categoriesList = plan.categories
                                    mode = plan.recommendedMode
                                    browserDurationStr = plan.recommendedDuration.toString()
                                    completionKeywords = plan.recommendedKeywords
                                    referer = plan.recommendedReferer
                                    errorMessage = null
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Auto-Extract", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "⚡ استخراج وتحسين الخطة تلقائياً (Auto-Extract Plan)",
                                    color = CpaPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "يحلل الرابط ويضبط الاسم والمسار والمدة وكلمات التأكيد تلقائياً ليعمل كل شيء ذاتياً",
                                    color = CpaTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset Offer Plans Chips
                        Text("قوالب عروض جاهزة (PRESET FUNNELS)", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TaskCategoryPlanner.PRESET_OFFER_PLANS.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CpaCardElevated)
                                        .border(1.dp, CpaBorder, RoundedCornerShape(6.dp))
                                        .clickable {
                                            url = preset.url
                                            name = preset.name
                                            categoriesList = TaskCategoryPlanner.parseCategories(preset.categories)
                                            mode = preset.mode
                                            browserDurationStr = preset.duration.toString()
                                            completionKeywords = preset.completionKeywords
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = CpaText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("REFERER", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = referer,
                                onValueChange = { referer = it },
                                placeholder = { Text("https://www.google.com", color = CpaTextDim) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder,
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { referer = IdentityService.RANDOM_REFERRERS.random() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CpaAccentDim)
                                    .border(1.dp, CpaAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Randomize", tint = CpaAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("REPEAT COUNT (0 = Unlimited)", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = repeatCountStr,
                            onValueChange = { repeatCountStr = it.filter { ch -> ch.isDigit() } },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CpaPrimary,
                                unfocusedBorderColor = CpaBorder,
                                focusedTextColor = CpaText,
                                unfocusedTextColor = CpaText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("AI INTENT & FUNNEL", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaCardElevated)
                                .border(1.dp, CpaBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 1 }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Funnel", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (categoriesList.isEmpty()) "No categories selected" else TaskCategoryPlanner.formatPlanSummary(categoriesList),
                                    color = CpaPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("Configure ➔", color = CpaTextDim, fontSize = 10.sp)
                            }
                        }
                    } else if (selectedTab == 1) {
                        // AI FUNNEL & CATEGORIES TAB
                        Text("AI INTENT CATEGORIES (تصنيفات فهم المهمة)", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "أضف تصنيفات للمهمة (مثل Sign Up، Survey، Email). يقوم الذكاء الاصطناعي بفهم المطلوب وترتيب خطوات المسار تلقائياً.",
                            color = CpaTextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset Category Selection Chips
                        Text("PRESET ACTIONS (تصنيفات سريعة)", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TaskCategoryPlanner.PRESET_CATEGORIES.forEach { def ->
                                val isSelected = categoriesList.any {
                                    it.equals(def.labelEn, ignoreCase = true) || it.equals(def.id, ignoreCase = true) || it.equals(def.labelAr, ignoreCase = true)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) CpaPrimaryDim else CpaCardElevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) CpaPrimary else CpaBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            categoriesList = if (isSelected) {
                                                categoriesList.filterNot {
                                                    it.equals(def.labelEn, ignoreCase = true) || it.equals(def.id, ignoreCase = true) || it.equals(def.labelAr, ignoreCase = true)
                                                }
                                            } else {
                                                categoriesList + def.labelEn
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(def.emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = def.labelEn,
                                            color = if (isSelected) CpaPrimary else CpaText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = CpaPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Add Custom Category
                        Text("ADD CUSTOM CATEGORY (إضافة تصنيف مخصص)", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customCategoryInput,
                                onValueChange = { customCategoryInput = it },
                                placeholder = { Text("e.g. sin up, mobile install, pin...", color = CpaTextDim, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder,
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val trimmed = customCategoryInput.trim()
                                    if (trimmed.isNotEmpty() && !categoriesList.any { it.equals(trimmed, ignoreCase = true) }) {
                                        categoriesList = categoriesList + trimmed
                                        customCategoryInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CpaPrimaryDim),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CpaPrimaryBorder)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Active Categories List
                        Text("ACTIVE SELECTED CATEGORIES (${categoriesList.size})", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (categoriesList.isEmpty()) {
                            Text("No categories selected yet. Tap a preset above or add a custom one.", color = CpaTextDim, fontSize = 11.sp)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                categoriesList.forEach { cat ->
                                    val def = TaskCategoryPlanner.findDefinition(cat)
                                    val emoji = def?.emoji ?: "🎯"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CpaCardElevated)
                                            .border(1.dp, CpaAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(emoji, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(cat, color = CpaText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = CpaTextMuted,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        categoriesList = categoriesList.filterNot { it == cat }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Real-time AI Sequence & Funnel Order Explanation
                        val orderedSteps = TaskCategoryPlanner.orderCategories(categoriesList)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaPrimaryDim)
                                .border(1.dp, CpaPrimaryBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Plan", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "AI EXECUTION PIPELINE (ترتيب الذكاء التلقائي):",
                                        color = CpaPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (orderedSteps.isEmpty()) {
                                    Text("Select categories to see how AI arranges them in the conversion pipeline.", color = CpaTextDim, fontSize = 11.sp)
                                } else {
                                    orderedSteps.forEach { step ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${step.order}.",
                                                color = CpaPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${step.emoji} ${step.labelEn} (${step.labelAr})",
                                                color = CpaText,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "   ↳ ${step.description}",
                                            color = CpaTextMuted,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 2) {
                        // MODE TAB
                        Text("SELECT AUTOMATION MODE", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))

                        val modes = listOf(
                            Triple(TaskMode.MODE1, Icons.Default.Timer, "Mode 1 – Timer (Duration countdown)"),
                            Triple(TaskMode.MODE2, Icons.Default.Repeat, "Mode 2 – Repeat in same session"),
                            Triple(TaskMode.MODE3, Icons.Default.Psychology, "Mode 3 – Smart Keyword Detection")
                        )

                        modes.forEach { (m, ic, title) ->
                            val isChosen = mode == m.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) CpaPrimaryDim else CpaCardElevated)
                                    .border(1.dp, if (isChosen) CpaPrimary else CpaBorder, RoundedCornerShape(8.dp))
                                    .clickable { mode = m.id }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(ic, contentDescription = title, tint = if (isChosen) CpaPrimary else CpaTextMuted, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.label, color = if (isChosen) CpaPrimary else CpaText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(m.desc, color = CpaTextMuted, fontSize = 11.sp)
                                }
                                if (isChosen) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = CpaPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dynamic mode config inputs
                        if (mode == TaskMode.MODE1.id) {
                            Text("MODE 1 SETTINGS", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Browser Open Duration (seconds)", color = CpaTextMuted, fontSize = 11.sp)
                            OutlinedTextField(
                                value = browserDurationStr,
                                onValueChange = { browserDurationStr = it.filter { c -> c.isDigit() } },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder,
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (mode == TaskMode.MODE2.id) {
                            Text("MODE 2 SETTINGS", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Task Repeats In Same Session", color = CpaTextMuted, fontSize = 11.sp)
                            OutlinedTextField(
                                value = taskRepeatCountStr,
                                onValueChange = { taskRepeatCountStr = it.filter { c -> c.isDigit() } },
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
                            Text("Total Task Duration (seconds)", color = CpaTextMuted, fontSize = 11.sp)
                            OutlinedTextField(
                                value = taskDurationStr,
                                onValueChange = { taskDurationStr = it.filter { c -> c.isDigit() } },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder,
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("MODE 3 SETTINGS", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Completion Keywords (comma-separated)", color = CpaTextMuted, fontSize = 11.sp)
                            OutlinedTextField(
                                value = completionKeywords,
                                onValueChange = { completionKeywords = it },
                                placeholder = { Text("thank you, congratulations, success", color = CpaTextDim) },
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder,
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        // ADVANCED TAB
                        // WORK TEMPLATE SELECTION
                        Text("ATTACH WORK TEMPLATE (ربط قالب عمل مخصص)", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))

                        val isSmartAutoTmpl = workTemplateId.isNullOrBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSmartAutoTmpl) CpaPrimaryDim else CpaCardElevated)
                                .border(1.dp, if (isSmartAutoTmpl) CpaPrimary else CpaBorder, RoundedCornerShape(8.dp))
                                .clickable { workTemplateId = null }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚡ قالب العمل التلقائي الذكي (Smart Auto)", color = if (isSmartAutoTmpl) CpaPrimary else CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("تحليل عناصر الصفحة تلقائياً بالذكاء الاصطناعي وبناء خريطة عمل ديناميكية", color = CpaTextDim, fontSize = 10.sp)
                            }
                            if (isSmartAutoTmpl) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CpaPrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        workTemplates.forEach { tmpl ->
                            val isChosen = workTemplateId == tmpl.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) CpaPrimaryDim else CpaCardElevated)
                                    .border(1.dp, if (isChosen) CpaPrimary else CpaBorder, RoundedCornerShape(8.dp))
                                    .clickable { workTemplateId = tmpl.id }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📋 ${tmpl.name}", color = if (isChosen) CpaPrimary else CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(tmpl.description.ifBlank { tmpl.targetCategory }, color = CpaTextDim, fontSize = 10.sp)
                                }
                                if (isChosen) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CpaPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // USER AGENT SECTION
                        Text("USER AGENT SPOOFING & RANDOMIZATION", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Random User Agent Master Card
                        val isRandomUA = userAgent.equals("random", ignoreCase = true) || userAgent.startsWith("random")
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isRandomUA) CpaPrimary else CpaBorder, RoundedCornerShape(8.dp)),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = if (isRandomUA) CpaPrimaryDim else CpaCardElevated)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { userAgent = "random" },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🎲 User-Agent عشوائي متجدد في كل دورة", color = if (isRandomUA) CpaPrimary else CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("توليد وتطبيق متصفح وبصمة مختلفة لكل دورة وتكرار لتقليل كشف الجلسات", color = CpaTextDim, fontSize = 10.sp)
                                    }
                                    if (isRandomUA) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CpaPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                if (isRandomUA) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            Pair("🎲 عشوائي شامل", "random"),
                                            Pair("📱 موبايل فقط", "random_mobile"),
                                            Pair("💻 كمبيوتر فقط", "random_desktop")
                                        ).forEach { (label, key) ->
                                            val isSub = userAgent == key
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSub) CpaPrimary else CpaCard)
                                                    .border(0.5.dp, if (isSub) CpaPrimary else CpaBorder, RoundedCornerShape(4.dp))
                                                    .clickable { userAgent = key }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(label, color = if (isSub) Color.Black else CpaText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("أو اختيار متصفح ثابت محدد (PRESET BROWSER):", color = CpaTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))

                        IdentityService.USER_AGENTS.forEach { ua ->
                            val isSelected = userAgent == ua.value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CpaPrimaryDim else CpaCardElevated)
                                    .border(1.dp, if (isSelected) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                                    .clickable { userAgent = ua.value }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ua.label, color = if (isSelected) CpaPrimary else CpaText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text(ua.value.take(45) + "...", color = CpaTextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("REPETITION & ISOLATION (تكرار المهمة والعزل)", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sequential Proxy Rotation Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaCardElevated)
                                .border(1.dp, CpaBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تدوير البروكسي بالتسلسل", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("تغيير البروكسي تلقائياً بالترتيب بين كل دورة تكرار", color = CpaTextDim, fontSize = 10.sp)
                            }
                            androidx.compose.material3.Switch(
                                checked = sequentialProxyRotation,
                                onCheckedChange = { sequentialProxyRotation = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Cache Wiping Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaCardElevated)
                                .border(1.dp, CpaBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("حذف كاش المتصفح بالكامل", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("مسح الكوكيز والتخزين والذاكرة المؤقتة 100% بين كل تكرار", color = CpaTextDim, fontSize = 10.sp)
                            }
                            androidx.compose.material3.Switch(
                                checked = clearCacheOnRepeat,
                                onCheckedChange = { clearCacheOnRepeat = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Fresh Identity Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CpaCardElevated)
                                .border(1.dp, CpaBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("معلومات وإيميل جديد لكل تكرار", color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("توليد هوية وشخصية وبريد جديد كلياً في كل دورة", color = CpaTextDim, fontSize = 10.sp)
                            }
                            androidx.compose.material3.Switch(
                                checked = newIdentityOnRepeat,
                                onCheckedChange = { newIdentityOnRepeat = it }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = CpaTextMuted)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || url.isBlank()) {
                                errorMessage = "Task Name and Target URL are required"
                                return@Button
                            }
                            val task = TaskEntity(
                                id = taskToEdit?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                url = url.trim(),
                                referer = referer.trim(),
                                userAgent = userAgent,
                                mode = mode,
                                repeatCount = repeatCountStr.toIntOrNull() ?: 1,
                                completedRuns = taskToEdit?.completedRuns ?: 0,
                                enabled = taskToEdit?.enabled ?: true,
                                browserDuration = browserDurationStr.toIntOrNull() ?: 45,
                                mode1RepeatCount = mode1RepeatStr.toIntOrNull() ?: 1,
                                taskDuration = taskDurationStr.toIntOrNull() ?: 60,
                                taskRepeatCount = taskRepeatCountStr.toIntOrNull() ?: 3,
                                operationRepeatCount = operationRepeatCountStr.toIntOrNull() ?: 1,
                                completionKeywords = completionKeywords.trim(),
                                categories = categoriesList.joinToString(", "),
                                workTemplateId = workTemplateId,
                                sequentialProxyRotation = sequentialProxyRotation,
                                clearCacheOnRepeat = clearCacheOnRepeat,
                                newIdentityOnRepeat = newIdentityOnRepeat,
                                createdAt = taskToEdit?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(task)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text(if (taskToEdit == null) "Add Task" else "Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
