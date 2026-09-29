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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ScriptItem
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
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted
import java.util.UUID

@Composable
fun ScriptsScreen(
    scripts: List<ScriptItem>,
    onSaveScript: (ScriptItem) -> Unit,
    onToggleScript: (ScriptItem) -> Unit,
    onDeleteScript: (ScriptItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var scriptToEdit by remember { mutableStateOf<ScriptItem?>(null) }

    Box(modifier = modifier.fillMaxSize().background(CpaBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        Text("BROWSER INJECTION SCRIPTS", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${scripts.count { it.enabled }} Active Scripts", color = CpaPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Custom JS injected into targets", color = CpaTextDim, fontSize = 11.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaPrimaryDim)
                            .border(1.dp, CpaPrimaryBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                scriptToEdit = null
                                showDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("+ New Script", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(scripts, key = { it.id }) { script ->
                ScriptCard(
                    script = script,
                    onToggle = { onToggleScript(script) },
                    onEdit = {
                        scriptToEdit = script
                        showDialog = true
                    },
                    onDelete = { onDeleteScript(script) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                scriptToEdit = null
                showDialog = true
            },
            containerColor = CpaPrimary,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_script_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Script")
        }
    }

    if (showDialog) {
        AddScriptDialog(
            scriptToEdit = scriptToEdit,
            onDismiss = {
                showDialog = false
                scriptToEdit = null
            },
            onSave = {
                onSaveScript(it)
                showDialog = false
                scriptToEdit = null
            }
        )
    }
}

@Composable
fun ScriptCard(
    script: ScriptItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CpaCard)
            .border(1.dp, CpaBorder, RoundedCornerShape(10.dp))
            .then(if (!script.enabled) Modifier.alpha(0.5f) else Modifier)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Code, contentDescription = "Script", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(script.name, color = CpaText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Switch(
                    checked = script.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CpaPrimary,
                        checkedTrackColor = CpaPrimaryBorder,
                        uncheckedThumbColor = CpaTextDim,
                        uncheckedTrackColor = CpaCardElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timing & ExecMode badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CpaAccentDim)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (script.timing == "before") "Before Load" else "After Load",
                        color = CpaAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CpaCardElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = script.execMode.replaceFirstChar { it.uppercase() },
                        color = CpaTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Code snippet preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CpaBg)
                    .border(1.dp, CpaBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = script.code.lines().take(3).joinToString("\n"),
                    color = CpaTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions: Edit, Delete
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(
                    modifier = Modifier
                        .clickable { onEdit() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CpaPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (!script.isSystemPreset) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CpaError, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = CpaError, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddScriptDialog(
    scriptToEdit: ScriptItem?,
    onDismiss: () -> Unit,
    onSave: (ScriptItem) -> Unit
) {
    var name by remember { mutableStateOf(scriptToEdit?.name ?: "") }
    var timing by remember { mutableStateOf(scriptToEdit?.timing ?: "after") }
    var execMode by remember { mutableStateOf(scriptToEdit?.execMode ?: "sequential") }
    var code by remember {
        mutableStateOf(
            scriptToEdit?.code ?: """
(function() {
  console.log('[CPA] Custom script executed on: ' + window.location.href);
})();
true;
            """.trimIndent()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CpaCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (scriptToEdit == null) "NEW INJECTION SCRIPT" else "EDIT SCRIPT",
                    color = CpaText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("SCRIPT NAME", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Click Cookie Banner", color = CpaTextDim) },
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

                Text("INJECTION TIMING", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("before" to "Before Page Load", "after" to "After Page Load").forEach { (valKey, title) ->
                        val isChosen = timing == valKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChosen) CpaPrimaryDim else CpaCardElevated)
                                .border(1.dp, if (isChosen) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                                .clickable { timing = valKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, color = if (isChosen) CpaPrimary else CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("JAVASCRIPT CODE", color = CpaTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    minLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedTextColor = CpaText,
                        unfocusedTextColor = CpaText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel", color = CpaTextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    ScriptItem(
                                        id = scriptToEdit?.id ?: UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        timing = timing,
                                        execMode = execMode,
                                        code = code.trim(),
                                        enabled = scriptToEdit?.enabled ?: true,
                                        isSystemPreset = scriptToEdit?.isSystemPreset ?: false
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary, contentColor = Color.Black)
                    ) {
                        Text(if (scriptToEdit == null) "Add Script" else "Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
