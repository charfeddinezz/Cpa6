package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AutomationState
import com.example.data.model.WorkStep
import com.example.data.model.WorkStepType
import com.example.data.model.WorkTemplateEntity
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
import com.example.ui.theme.CpaWarning
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Composable
fun TemplatesScreen(
    templates: List<WorkTemplateEntity>,
    automationState: AutomationState,
    onSaveTemplate: (WorkTemplateEntity) -> Unit,
    onDeleteTemplate: (WorkTemplateEntity) -> Unit,
    onRequestAutoTemplate: () -> Unit,
    onExecuteTemplate: (WorkTemplateEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkTemplateEntity?>(null) }
    var showPresetLibraryDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var exportTemplateTarget by remember { mutableStateOf<WorkTemplateEntity?>(null) }
    var showLiveActionMap by remember { mutableStateOf(true) }

    val categories = listOf("الكل", "تلقائي ذكي", "CPA Lead", "Email Submit", "Survey / Quiz", "Human Warmup")

    val filteredTemplates = remember(templates, searchQuery, selectedCategory) {
        templates.filter { tmpl ->
            val matchesCategory = when (selectedCategory) {
                "الكل" -> true
                "تلقائي ذكي" -> tmpl.isAutoGenerated || tmpl.targetCategory.contains("Auto", ignoreCase = true)
                "CPA Lead" -> tmpl.targetCategory.contains("Lead", ignoreCase = true)
                "Email Submit" -> tmpl.targetCategory.contains("Email", ignoreCase = true)
                "Survey / Quiz" -> tmpl.targetCategory.contains("Survey", ignoreCase = true) || tmpl.targetCategory.contains("Quiz", ignoreCase = true)
                "Human Warmup" -> tmpl.targetCategory.contains("Warmup", ignoreCase = true) || tmpl.targetCategory.contains("Human", ignoreCase = true)
                else -> tmpl.targetCategory.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                tmpl.name.contains(searchQuery, ignoreCase = true) ||
                tmpl.description.contains(searchQuery, ignoreCase = true) ||
                tmpl.stepsJson.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg)
            .padding(12.dp)
    ) {
        // --- 1. Top Header & Primary Actions ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CpaCard),
            border = BorderStroke(1.dp, CpaPrimaryBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                                .background(CpaPrimaryDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = CpaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "قوالب العمل الذكية (Work Templates)",
                                color = CpaText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أتمتة الأزرار، التمرير، ملء الحقول بالهوية، وتجاوز العروض",
                                color = CpaTextDim,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Preset Library Button
                    Button(
                        onClick = { showPresetLibraryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaCardElevated),
                        border = BorderStroke(1.dp, CpaAccent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null, tint = CpaAccent, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مكتبة الجاهز", color = CpaAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            onRequestAutoTemplate()
                            Toast.makeText(context, "جاري فحص المتصفح وتوليد القالب تلقائياً بالذكاء...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaAccent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صنع قالب من الصفحة", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            editingTemplate = null
                            showCreateDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنشاء قالب جديد", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showImportJsonDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaCardElevated),
                        border = BorderStroke(0.5.dp, CpaBorder),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = CpaText, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("استيراد JSON", color = CpaText, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. Live Page Action Map (if active) ---
        if (automationState.pageActionMapJson.isNotBlank() && automationState.pageActionMapJson != "[]") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CpaCardElevated),
                border = BorderStroke(1.dp, CpaAccent.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = CpaAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "خريطة عمل الصفحة الحالية (${automationState.pageActionMapSummary.ifBlank { "ديناميكية" }})",
                                color = CpaAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { showLiveActionMap = !showLiveActionMap },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (showLiveActionMap) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = CpaTextDim
                            )
                        }
                    }

                    AnimatedVisibility(visible = showLiveActionMap) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            val steps = parseActionMapJson(automationState.pageActionMapJson)
                            steps.forEach { step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CpaCard)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${step.order}",
                                            color = CpaPrimary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = step.title,
                                            color = CpaText,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (step.status == "done") CpaSuccess.copy(alpha = 0.2f) else CpaWarning.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (step.status == "done") "مكتمل ✓" else "جاري التنفيذ...",
                                            color = if (step.status == "done") CpaSuccess else CpaWarning,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- 3. Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في أسماء القوالب أو الخطوات أو الحقول...", color = CpaTextDim, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CpaTextDim, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = CpaTextDim, modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CpaCard,
                unfocusedContainerColor = CpaCard,
                focusedTextColor = CpaText,
                unfocusedTextColor = CpaText,
                focusedBorderColor = CpaPrimary,
                unfocusedBorderColor = CpaBorder
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // --- 4. Category Filter Chips ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CpaPrimaryDim else CpaCard)
                        .border(1.dp, if (isSelected) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) CpaPrimary else CpaTextDim,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Count Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قوالب العمل (${filteredTemplates.size} من ${templates.size}):",
                color = CpaTextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (templates.isNotEmpty()) {
                Text(
                    text = "💡 نصيحة: يمكنك تشغيل القالب مباشرة في المتصفح أو ربطه بالمهمة",
                    color = CpaAccent,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 5. Templates List & Empty State ---
        if (templates.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                colors = CardDefaults.cardColors(containerColor = CpaCard),
                border = BorderStroke(1.dp, CpaBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = CpaPrimary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد قوالب عمل حتى الآن",
                        color = CpaText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قوالب العمل تقوم بفهم عناصر الصفحة، تمرير الشاشة، ملء بيانات الهوية تلقائياً، والضغط على أزرار التقديم والتخطي.",
                        color = CpaTextDim,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            WorkTemplatePresets.getAllPresets().forEach { onSaveTemplate(it) }
                            Toast.makeText(context, "تمت إضافة حزمة القوالب الموصى بها بنجاح (5 قوالب)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🚀 إضافة حزمة القوالب الجاهزة الموصى بها (5 قوالب)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        } else if (filteredTemplates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد قوالب تطابق البحث أو التصنيف المحدد.",
                    color = CpaTextDim,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTemplates, key = { it.id }) { template ->
                    TemplateItemCard(
                        template = template,
                        onEdit = {
                            editingTemplate = template
                            showCreateDialog = true
                        },
                        onDelete = { onDeleteTemplate(template) },
                        onExecute = {
                            onExecuteTemplate(template)
                            Toast.makeText(context, "▶️ تم إرسال القالب '${template.name}' للمتصفح", Toast.LENGTH_SHORT).show()
                        },
                        onClone = {
                            val cloned = template.copy(
                                id = UUID.randomUUID().toString(),
                                name = "${template.name} (نسخة)",
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSaveTemplate(cloned)
                            Toast.makeText(context, "تم استنساخ القالب بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        onExport = {
                            exportTemplateTarget = template
                        }
                    )
                }
            }
        }
    }

    // --- 6. Dialogs ---

    // Create / Edit Dialog
    if (showCreateDialog) {
        WorkTemplateEditorDialog(
            initialTemplate = editingTemplate,
            onDismiss = { showCreateDialog = false },
            onSave = { saved ->
                onSaveTemplate(saved)
                showCreateDialog = false
                Toast.makeText(context, "تم حفظ قالب العمل بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Preset Library Dialog
    if (showPresetLibraryDialog) {
        PresetLibraryDialog(
            onDismiss = { showPresetLibraryDialog = false },
            onImportPreset = { preset ->
                onSaveTemplate(preset)
                Toast.makeText(context, "تمت إضافة '${preset.name}' بنجاح", Toast.LENGTH_SHORT).show()
            },
            onImportAll = {
                WorkTemplatePresets.getAllPresets().forEach { onSaveTemplate(it) }
                showPresetLibraryDialog = false
                Toast.makeText(context, "تمت إضافة كافة القوالب الجاهزة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Import JSON Dialog
    if (showImportJsonDialog) {
        ImportJsonDialog(
            onDismiss = { showImportJsonDialog = false },
            onImport = { imported ->
                onSaveTemplate(imported)
                showImportJsonDialog = false
                Toast.makeText(context, "تم استيراد القالب '${imported.name}' بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Export Dialog
    exportTemplateTarget?.let { tmpl ->
        ExportJsonDialog(
            template = tmpl,
            onDismiss = { exportTemplateTarget = null }
        )
    }
}

// ==========================================
// TEMPLATE ITEM CARD WITH ACCORDION STEPS
// ==========================================
@Composable
fun TemplateItemCard(
    template: WorkTemplateEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExecute: () -> Unit,
    onClone: () -> Unit,
    onExport: () -> Unit
) {
    val stepsList = remember(template.stepsJson) { parseWorkSteps(template.stepsJson) }
    var expandedSteps by remember { mutableStateOf(false) }

    val estimatedSeconds = remember(stepsList) {
        var ms = 0L
        stepsList.forEach { s ->
            ms += s.delayMs + when (s.type) {
                WorkStepType.SCROLL -> 500L
                WorkStepType.FILL_FIELD -> 400L
                WorkStepType.CLICK_BUTTON -> 600L
                WorkStepType.CHECK_BOX -> 300L
                WorkStepType.SELECT_OPTION -> 400L
                WorkStepType.EXTRACT_ADAPT -> 1200L
                WorkStepType.DELAY -> 0L
            }
        }
        (ms / 1000.0).coerceAtLeast(1.0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CpaCard),
        border = BorderStroke(1.dp, if (template.isAutoGenerated) CpaAccent.copy(alpha = 0.5f) else CpaBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.name,
                            color = CpaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (template.isAutoGenerated) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CpaAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("تلقائي ذكي ✨", color = CpaAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Category Pill & Steps count
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(CpaPrimaryDim)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(template.targetCategory, color = CpaPrimary, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Text("•", color = CpaTextDim, fontSize = 9.sp)
                        Text("${stepsList.size} خطوات", color = CpaTextDim, fontSize = 9.sp)
                        Text("•", color = CpaTextDim, fontSize = 9.sp)
                        Text("⏱️ ~${String.format("%.1f", estimatedSeconds)}ث", color = CpaTextDim, fontSize = 9.sp)
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onExecute, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل واختبار فوري", tint = CpaSuccess, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClone, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ القالب", tint = CpaTextDim, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onExport, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير JSON", tint = CpaAccent, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = CpaPrimary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CpaError, modifier = Modifier.size(15.dp))
                    }
                }
            }

            if (template.description.isNotBlank()) {
                Text(
                    text = template.description,
                    color = CpaTextDim,
                    fontSize = 10.sp,
                    lineHeight = 13.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
            }

            // Steps chips preview & accordion trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CpaCardElevated)
                    .clickable { expandedSteps = !expandedSteps }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stepsList.take(3).forEach { step ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(CpaCard)
                                .border(0.5.dp, CpaBorder, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${step.type.icon} ${step.type.labelAr}",
                                color = CpaText,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                    if (stepsList.size > 3) {
                        Text("+${stepsList.size - 3}", color = CpaPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (expandedSteps) "إخفاء الخطوات" else "عرض التفاصيل",
                        color = CpaPrimary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (expandedSteps) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CpaPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Detailed Steps View
            AnimatedVisibility(visible = expandedSteps) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stepsList.forEachIndexed { idx, s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CpaCardElevated)
                                .border(0.5.dp, CpaBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                color = CpaPrimary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = s.type.icon, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.type.labelAr + (if (s.description.isNotBlank()) ": ${s.description}" else ""),
                                    color = CpaText,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val detail = when (s.type) {
                                    WorkStepType.CLICK_BUTTON -> "الهدف: '${s.targetValue.ifBlank { "افتراضي" }}'"
                                    WorkStepType.SCROLL -> "الاتجاه: ${s.scrollDirection} • مسافة: ${s.scrollAmount}px"
                                    WorkStepType.FILL_FIELD -> "المصدر: ${if (s.fieldSource == "identity") "بيانات الهوية [${s.fieldKey}]" else "مخصص: '${s.customValue}'"}"
                                    WorkStepType.CHECK_BOX -> "المربع: '${s.targetValue.ifBlank { "terms" }}'"
                                    WorkStepType.SELECT_OPTION -> "الخيار: '${s.targetValue}'"
                                    WorkStepType.DELAY -> "انتظار: ${s.delayMs}ms"
                                    WorkStepType.EXTRACT_ADAPT -> "استخراج وتكييف الإجابة الذكية من السياق"
                                }
                                Text(text = detail, color = CpaTextDim, fontSize = 8.5.sp)
                            }
                            if (s.delayMs > 0 && s.type != WorkStepType.DELAY) {
                                Text(text = "+${s.delayMs}ms", color = CpaAccent, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PRESET TEMPLATES CATALOG DIALOG
// ==========================================
@Composable
fun PresetLibraryDialog(
    onDismiss: () -> Unit,
    onImportPreset: (WorkTemplateEntity) -> Unit,
    onImportAll: () -> Unit
) {
    val presets = remember { WorkTemplatePresets.getAllPresets() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CpaBg,
            border = BorderStroke(1.dp, CpaAccent),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = CpaAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("مكتبة قوالب CPA الجاهزة", color = CpaText, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text("قوالب احترافية مجهزة ومختبرة لعروض CPA", color = CpaTextDim, fontSize = 9.5.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = CpaTextDim)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Import All Button
                Button(
                    onClick = onImportAll,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CpaAccent),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🚀 استيراد كافة القوالب الخمسة دفعة واحدة", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets list
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset ->
                        val steps = parseWorkSteps(preset.stepsJson)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CpaCard),
                            border = BorderStroke(1.dp, CpaBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(preset.name, color = CpaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(preset.targetCategory, color = CpaPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = { onImportPreset(preset) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimaryDim),
                                        border = BorderStroke(1.dp, CpaPrimary),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = CpaPrimary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("إضافة", color = CpaPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(preset.description, color = CpaTextDim, fontSize = 9.5.sp, modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    steps.forEach { st ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(CpaCardElevated)
                                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                        ) {
                                            Text("${st.type.icon} ${st.type.labelAr}", color = CpaTextDim, fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// WORK TEMPLATE EDITOR DIALOG
// ==========================================
@Composable
fun WorkTemplateEditorDialog(
    initialTemplate: WorkTemplateEntity?,
    onDismiss: () -> Unit,
    onSave: (WorkTemplateEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var description by remember { mutableStateOf(initialTemplate?.description ?: "") }
    var category by remember { mutableStateOf(initialTemplate?.targetCategory ?: "Smart Auto") }

    val categories = listOf("Smart Auto", "CPA Lead", "Email Submit", "Survey / Quiz", "Human Warmup", "Locker Bypass", "Custom")

    val steps = remember {
        mutableStateListOf<WorkStep>().apply {
            if (initialTemplate != null) {
                addAll(parseWorkSteps(initialTemplate.stepsJson))
            } else {
                add(WorkStep(type = WorkStepType.SCROLL, scrollAmount = 300, description = "تمرير الصفحة لأسفل نحو الاستمارة"))
                add(WorkStep(type = WorkStepType.FILL_FIELD, fieldKey = "email", description = "ملء البريد الإلكتروني من الهوية"))
                add(WorkStep(type = WorkStepType.CHECK_BOX, targetValue = "terms", description = "الموافقة على الشروط"))
                add(WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Continue, Submit, Next", description = "نقر زر المتابعة"))
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CpaBg,
            border = BorderStroke(1.dp, CpaBorder),
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTemplate == null) "إنشاء قالب عمل جديد" else "تعديل قالب العمل",
                        color = CpaText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = CpaTextDim)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم القالب", color = CpaTextDim, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText,
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف / الهدف من القالب", color = CpaTextDim, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText,
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category selector
                Text("تصنيف القالب:", color = CpaTextDim, fontSize = 10.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) CpaPrimaryDim else CpaCard)
                                .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(4.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cat, color = if (isSel) CpaPrimary else CpaText, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Steps Builder Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "خطوات القالب المتسلسلة (${steps.size}):",
                        color = CpaPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            steps.add(
                                WorkStep(
                                    type = WorkStepType.CLICK_BUTTON,
                                    targetValue = "Continue",
                                    description = "نقر زر"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimaryDim),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("+ إضافة خطوة", color = CpaPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                steps.forEachIndexed { index, step ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CpaCard),
                        border = BorderStroke(1.dp, CpaBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Step Title & Reordering
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        color = CpaPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${step.type.icon} ${step.type.labelAr}",
                                        color = CpaText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val prev = steps[index - 1]
                                                steps[index - 1] = step
                                                steps[index] = prev
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "تحريك لأعلى", tint = if (index > 0) CpaText else CpaTextDim)
                                    }
                                    // Move Down
                                    IconButton(
                                        onClick = {
                                            if (index < steps.size - 1) {
                                                val next = steps[index + 1]
                                                steps[index + 1] = step
                                                steps[index] = next
                                            }
                                        },
                                        enabled = index < steps.size - 1,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تحريك لأسفل", tint = if (index < steps.size - 1) CpaText else CpaTextDim)
                                    }
                                    // Delete
                                    IconButton(
                                        onClick = { steps.removeAt(index) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الخطوة", tint = CpaError)
                                    }
                                }
                            }

                            // Step Type Selector Pills
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                WorkStepType.values().forEach { st ->
                                    val isSel = step.type == st
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                            .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(4.dp))
                                            .clickable { steps[index] = step.copy(type = st) }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text("${st.icon} ${st.labelAr}", color = if (isSel) CpaPrimary else CpaText, fontSize = 8.5.sp)
                                    }
                                }
                            }

                            // Step Specific Parameter Inputs
                            when (step.type) {
                                WorkStepType.CLICK_BUTTON -> {
                                    OutlinedTextField(
                                        value = step.targetValue,
                                        onValueChange = { steps[index] = step.copy(targetValue = it) },
                                        label = { Text("نص الزر أو الكلاس/الآيدي", color = CpaTextDim, fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CpaText, unfocusedTextColor = CpaText, focusedBorderColor = CpaPrimary, unfocusedBorderColor = CpaBorder)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("Continue", "Submit", "Next", "Get Started", "Start", "Confirm", "I Agree").forEach { quickBtn ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(CpaCardElevated)
                                                    .clickable { steps[index] = step.copy(targetValue = quickBtn) }
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(quickBtn, color = CpaAccent, fontSize = 8.sp)
                                            }
                                        }
                                    }
                                }
                                WorkStepType.SCROLL -> {
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Direction
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("الاتجاه:", color = CpaTextDim, fontSize = 8.5.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("down" to "لأسفل ⬇️", "up" to "لأعلى ⬆️").forEach { (d, label) ->
                                                    val isSel = step.scrollDirection == d
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                                            .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(3.dp))
                                                            .clickable { steps[index] = step.copy(scrollDirection = d) }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(label, color = if (isSel) CpaPrimary else CpaText, fontSize = 8.5.sp)
                                                    }
                                                }
                                            }
                                        }
                                        // Distance chips
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("المسافة (بكسل):", color = CpaTextDim, fontSize = 8.5.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf(250, 450, 800).forEach { px ->
                                                    val isSel = step.scrollAmount == px
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                                            .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(3.dp))
                                                            .clickable { steps[index] = step.copy(scrollAmount = px) }
                                                            .padding(horizontal = 5.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("${px}px", color = if (isSel) CpaPrimary else CpaText, fontSize = 8.5.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                WorkStepType.FILL_FIELD -> {
                                    Text("مصدر البيانات:", color = CpaTextDim, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("identity" to "من بيانات الهوية (Identity)", "custom" to "قيمة مخصصة ثابته").forEach { (src, label) ->
                                            val isSel = step.fieldSource == src
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                                    .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(3.dp))
                                                    .clickable { steps[index] = step.copy(fieldSource = src) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(label, color = if (isSel) CpaPrimary else CpaText, fontSize = 8.5.sp)
                                            }
                                        }
                                    }

                                    if (step.fieldSource == "identity") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("email", "fullName", "firstName", "lastName", "phone", "postalCode", "address", "city", "state").forEach { fk ->
                                                val isSel = step.fieldKey == fk
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                                        .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(3.dp))
                                                        .clickable { steps[index] = step.copy(fieldKey = fk) }
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(fk, color = if (isSel) CpaPrimary else CpaText, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = step.customValue,
                                            onValueChange = { steps[index] = step.copy(customValue = it) },
                                            label = { Text("القيمة المخصصة", color = CpaTextDim, fontSize = 9.sp) },
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CpaText, unfocusedTextColor = CpaText, focusedBorderColor = CpaPrimary, unfocusedBorderColor = CpaBorder)
                                        )
                                    }
                                }
                                WorkStepType.CHECK_BOX -> {
                                    OutlinedTextField(
                                        value = step.targetValue,
                                        onValueChange = { steps[index] = step.copy(targetValue = it) },
                                        label = { Text("معرف المربع أو كلماته (مثال: terms, agree, privacy)", color = CpaTextDim, fontSize = 9.sp) },
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CpaText, unfocusedTextColor = CpaText, focusedBorderColor = CpaPrimary, unfocusedBorderColor = CpaBorder)
                                    )
                                }
                                WorkStepType.DELAY -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(500L, 1000L, 2000L, 3000L, 5000L).forEach { d ->
                                            val isSel = step.delayMs == d
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isSel) CpaPrimaryDim else CpaCardElevated)
                                                    .border(0.5.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(3.dp))
                                                    .clickable { steps[index] = step.copy(delayMs = d) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("${d}ms", color = if (isSel) CpaPrimary else CpaText, fontSize = 8.5.sp)
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }

                            // Step Description
                            OutlinedTextField(
                                value = step.description,
                                onValueChange = { steps[index] = step.copy(description = it) },
                                label = { Text("ملاحظة وتوضيح الخطوة", color = CpaTextDim, fontSize = 9.sp) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CpaText,
                                    unfocusedTextColor = CpaText,
                                    focusedBorderColor = CpaPrimary,
                                    unfocusedBorderColor = CpaBorder
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Button
                Button(
                    onClick = {
                        val template = WorkTemplateEntity(
                            id = initialTemplate?.id ?: UUID.randomUUID().toString(),
                            name = name.ifBlank { "قالب عمل جديد" },
                            description = description,
                            targetCategory = category,
                            isAutoGenerated = false,
                            stepsJson = serializeWorkSteps(steps),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(template)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ قالب العمل", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================================
// IMPORT JSON DIALOG
// ==========================================
@Composable
fun ImportJsonDialog(
    onDismiss: () -> Unit,
    onImport: (WorkTemplateEntity) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CpaBg,
            border = BorderStroke(1.dp, CpaBorder),
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("استيراد قالب عمل من JSON", color = CpaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = CpaTextDim)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("الصق كود JSON الخاص بقالب العمل هنا:", color = CpaTextDim, fontSize = 10.sp)

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("{\"name\": \"...\", \"steps\": [...]}", color = CpaTextDim, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = CpaText, unfocusedTextColor = CpaText, focusedBorderColor = CpaPrimary, unfocusedBorderColor = CpaBorder)
                )

                errorMessage?.let {
                    Text(it, color = CpaError, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
                }

                Button(
                    onClick = {
                        try {
                            val parsed = parseTemplateFromJson(jsonInput)
                            if (parsed != null) {
                                onImport(parsed)
                            } else {
                                errorMessage = "صيغة JSON غير صالحة، تأكد من صحة الكود."
                            }
                        } catch (e: Exception) {
                            errorMessage = "خطأ في التحليل: ${e.message}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("استيراد وحفظ القالب", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// EXPORT JSON DIALOG
// ==========================================
@Composable
fun ExportJsonDialog(
    template: WorkTemplateEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val jsonString = remember(template) {
        val root = JSONObject().apply {
            put("id", template.id)
            put("name", template.name)
            put("description", template.description)
            put("targetCategory", template.targetCategory)
            put("isAutoGenerated", template.isAutoGenerated)
            put("steps", JSONArray(template.stepsJson.ifBlank { "[]" }))
        }
        root.toString(2)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CpaBg,
            border = BorderStroke(1.dp, CpaAccent),
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تصدير كود JSON للقالب", color = CpaText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = CpaTextDim)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CpaCardElevated)
                        .border(0.5.dp, CpaBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonString,
                        color = CpaAccent,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("Work Template JSON", jsonString)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ كود JSON إلى الحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CpaAccent),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ كود JSON إلى الحافظة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// PRESETS CATALOG REPOSITORY
// ==========================================
object WorkTemplatePresets {
    fun getAllPresets(): List<WorkTemplateEntity> = listOf(
        // 1. Email Submit / Single Opt-in
        WorkTemplateEntity(
            id = "preset_email_submit",
            name = "🎯 قالب Email Submit / Opt-in الذكي",
            description = "تمرير سلس للاستمارة، ملء البريد الإلكتروني تلقائياً من بيانات الهوية، الموافقة على الشروط ونقر المتابعة.",
            targetCategory = "Email Submit",
            isAutoGenerated = false,
            stepsJson = serializeWorkSteps(
                listOf(
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "down", scrollAmount = 300, delayMs = 800L, description = "تمرير الصفحة نحو نموذج الإدخال"),
                    WorkStep(type = WorkStepType.DELAY, delayMs = 1200L, description = "انتظار طبيعي لمحاكاة المستخدم"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "email", delayMs = 600L, description = "ملء حقل البريد الإلكتروني"),
                    WorkStep(type = WorkStepType.CHECK_BOX, targetValue = "terms, agree, privacy", delayMs = 400L, description = "تحديد خيار الموافقة على الشروط"),
                    WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Continue, Submit, Next, Get Started", delayMs = 1500L, description = "نقر زر المتابعة والاستمرار"),
                    WorkStep(type = WorkStepType.DELAY, delayMs = 2500L, description = "انتظار اكتمال التحويل والتأكيد")
                )
            )
        ),
        // 2. Full Lead Generation
        WorkTemplateEntity(
            id = "preset_full_lead",
            name = "📋 قالب Sign-up Lead المتكامل (Full Info)",
            description = "تعبئة استمارة التسجيل الكاملة بالاسم، البريد، الهاتف، الرمز البريدي والعنوان ثم تقديم الطلب.",
            targetCategory = "CPA Lead",
            isAutoGenerated = false,
            stepsJson = serializeWorkSteps(
                listOf(
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "down", scrollAmount = 250, delayMs = 600L, description = "تمرير لقمة النموذج"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "firstName", delayMs = 400L, description = "ملء الاسم الأول"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "lastName", delayMs = 400L, description = "ملء اسم العائلة"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "email", delayMs = 500L, description = "ملء البريد الإلكتروني"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "phone", delayMs = 500L, description = "ملء رقم الهاتف"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "postalCode", delayMs = 400L, description = "ملء الرمز البريدي"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "address", delayMs = 400L, description = "ملء العنوان"),
                    WorkStep(type = WorkStepType.CHECK_BOX, targetValue = "terms, agree", delayMs = 300L, description = "الموافقة على الشروط"),
                    WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Submit, Register, Sign Up, Continue", delayMs = 2000L, description = "إرسال الاستمارة وتأكيد التسجيل")
                )
            )
        ),
        // 3. Survey & Quiz Flow
        WorkTemplateEntity(
            id = "preset_survey_quiz",
            name = "📊 قالب استبيان وتخطي الأسئلة (Survey Flow)",
            description = "استخراج أسئلة الاستبيان في وسط الصفحة والإجابة عليها، واختيار الخيارات ثم ملء الاستلام.",
            targetCategory = "Survey / Quiz",
            isAutoGenerated = false,
            stepsJson = serializeWorkSteps(
                listOf(
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "down", scrollAmount = 350, delayMs = 800L, description = "التمرير لمنطقة السؤال"),
                    WorkStep(type = WorkStepType.EXTRACT_ADAPT, delayMs = 1200L, description = "تحليل وفهم السؤال الذكي في وسط الصفحة"),
                    WorkStep(type = WorkStepType.SELECT_OPTION, targetValue = "Yes, Option 1, Agree", delayMs = 600L, description = "اختيار الإجابة الإيجابية المناسبة"),
                    WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Next, Continue, Proceed", delayMs = 1500L, description = "الانتقال للسؤال التالي"),
                    WorkStep(type = WorkStepType.FILL_FIELD, fieldSource = "identity", fieldKey = "email", delayMs = 800L, description = "ملء البريد لتأكيد الاستلام"),
                    WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Claim, Finish, Complete, Submit", delayMs = 2500L, description = "المطالبة بإنهاء الاستبيان")
                )
            )
        ),
        // 4. Human Warmup & Anti-Bot
        WorkTemplateEntity(
            id = "preset_human_warmup",
            name = "🛡️ قالب التصفح البشري الدافئ (Human Warmup)",
            description = "محاكاة سلوك التصفح البشري الطبيعي بالتمرير المتدرج والانتظار العشوائي لمنع الحظر.",
            targetCategory = "Human Warmup",
            isAutoGenerated = false,
            stepsJson = serializeWorkSteps(
                listOf(
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "down", scrollAmount = 450, delayMs = 1500L, description = "تمرير بطيء لأسفل الصفحة"),
                    WorkStep(type = WorkStepType.DELAY, delayMs = 2000L, description = "توقف طبيعي لقراءة المحتوى"),
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "up", scrollAmount = 200, delayMs = 1000L, description = "تمرير تصحيحي خفيف لأعلى"),
                    WorkStep(type = WorkStepType.DELAY, delayMs = 1500L, description = "فترة هدوء طبيعية"),
                    WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Read More, Explore, Learn More", delayMs = 1200L, description = "نقر رابط أو زر للاستكشاف والتفاعل"),
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "down", scrollAmount = 500, delayMs = 1800L, description = "متابعة تصفح مقال أو صفحة العرض")
                )
            )
        ),
        // 5. Content Locker Bypass
        WorkTemplateEntity(
            id = "preset_locker_bypass",
            name = "📲 قالب تجاوز Content Locker والتطبيقات",
            description = "التمرير إلى العرض المطلوب، الانتظار لتجاوز الحماية، ثم نقر زر التحميل والتأكيد.",
            targetCategory = "Locker Bypass",
            isAutoGenerated = false,
            stepsJson = serializeWorkSteps(
                listOf(
                    WorkStep(type = WorkStepType.SCROLL, scrollDirection = "down", scrollAmount = 400, delayMs = 1200L, description = "التمرير نحو صندوق العرض والقفل"),
                    WorkStep(type = WorkStepType.DELAY, delayMs = 2000L, description = "انتظار تجاوز فحص الروبوت"),
                    WorkStep(type = WorkStepType.CLICK_BUTTON, targetValue = "Download, Install, Free Download, Unlock", delayMs = 1500L, description = "نقر زر فتح العرض أو التحميل"),
                    WorkStep(type = WorkStepType.DELAY, delayMs = 3000L, description = "مراقبة انتقال المتصفح لصفحة العرض")
                )
            )
        )
    )
}

// ==========================================
// PARSING & SERIALIZATION UTILITIES
// ==========================================
fun parseWorkSteps(json: String): List<WorkStep> {
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<WorkStep>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val typeStr = obj.optString("type", "CLICK_BUTTON")
            val type = WorkStepType.values().firstOrNull { it.name == typeStr || it.id == typeStr } ?: WorkStepType.CLICK_BUTTON
            list.add(
                WorkStep(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    type = type,
                    targetType = obj.optString("targetType", "auto"),
                    targetValue = obj.optString("targetValue", ""),
                    fieldSource = obj.optString("fieldSource", "identity"),
                    fieldKey = obj.optString("fieldKey", "email"),
                    customValue = obj.optString("customValue", ""),
                    scrollDirection = obj.optString("scrollDirection", "down"),
                    scrollAmount = obj.optInt("scrollAmount", 300),
                    delayMs = obj.optLong("delayMs", 1000L),
                    description = obj.optString("description", "")
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

fun serializeWorkSteps(steps: List<WorkStep>): String {
    val arr = JSONArray()
    steps.forEach { s ->
        val obj = JSONObject().apply {
            put("id", s.id)
            put("type", s.type.name)
            put("targetType", s.targetType)
            put("targetValue", s.targetValue)
            put("fieldSource", s.fieldSource)
            put("fieldKey", s.fieldKey)
            put("customValue", s.customValue)
            put("scrollDirection", s.scrollDirection)
            put("scrollAmount", s.scrollAmount)
            put("delayMs", s.delayMs)
            put("description", s.description)
        }
        arr.put(obj)
    }
    return arr.toString()
}

fun parseTemplateFromJson(json: String): WorkTemplateEntity? {
    return try {
        val obj = JSONObject(json)
        val name = obj.optString("name", "قالب مستورد")
        val description = obj.optString("description", "")
        val category = obj.optString("targetCategory", "Smart Auto")
        val isAuto = obj.optBoolean("isAutoGenerated", false)
        val stepsArr = if (obj.has("steps")) {
            obj.getJSONArray("steps").toString()
        } else if (obj.has("stepsJson")) {
            obj.getString("stepsJson")
        } else {
            "[]"
        }
        WorkTemplateEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            targetCategory = category,
            isAutoGenerated = isAuto,
            stepsJson = stepsArr,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    } catch (_: Exception) {
        null
    }
}

data class ActionMapStepItem(
    val order: Int,
    val title: String,
    val type: String,
    val target: String,
    val status: String,
    val details: String
)

fun parseActionMapJson(json: String): List<ActionMapStepItem> {
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<ActionMapStepItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                ActionMapStepItem(
                    order = obj.optInt("order", i + 1),
                    title = obj.optString("title", "خطوة"),
                    type = obj.optString("type", "click"),
                    target = obj.optString("target", ""),
                    status = obj.optString("status", "pending"),
                    details = obj.optString("details", "")
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}
