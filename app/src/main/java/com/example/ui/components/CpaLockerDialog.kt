package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.AutomationScriptBuilder
import com.example.ui.theme.CpaAccent
import com.example.ui.theme.CpaBg
import com.example.ui.theme.CpaBorder
import com.example.ui.theme.CpaCard
import com.example.ui.theme.CpaCardElevated
import com.example.ui.theme.CpaPrimary
import com.example.ui.theme.CpaPrimaryBorder
import com.example.ui.theme.CpaPrimaryDim
import com.example.ui.theme.CpaSuccess
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted

/**
 * Clean, modular dialog for controlling, inspecting, and injecting CPA Content Lockers.
 */
@Composable
fun CpaLockerDialog(
    detectedLockerUrl: String?,
    detectedLockerId: String?,
    isLockerTriggered: Boolean,
    lockerStatusMsg: String?,
    initialInput: String,
    onDismiss: () -> Unit,
    onExecuteLocker: (url: String, id: String, forceInject: Boolean) -> Unit
) {
    val context = LocalContext.current
    var lockerScriptInput by remember { mutableStateOf(initialInput) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CpaCard,
            border = BorderStroke(1.dp, CpaBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔓 تشغيل وتفعيل سكريبت اللوكر (CPA Locker)",
                        color = CpaPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CpaTextDim)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "فك حظر وتشغيل سكريبتات قفل الروابط والعروض (مثل alignmentfiles.com و cpagrip و script_include.php مع أي ID). يعالج تلقائياً قيود المتصفح ونقص مكتبة jQuery واستدعاء دالة call_locker().",
                    color = CpaTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detection Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (detectedLockerUrl != null) CpaPrimaryDim else CpaCardElevated)
                        .border(1.dp, if (detectedLockerUrl != null) CpaPrimaryBorder else CpaBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = if (detectedLockerUrl != null) "✅ تم اكتشاف السكريبت في الصفحة الحالية!" else "ℹ️ لم يتم اكتشاف سكريبت تلقائياً بالصفحة الحالية",
                            color = if (detectedLockerUrl != null) CpaPrimary else CpaTextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (detectedLockerUrl != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الرابط: $detectedLockerUrl",
                                color = CpaText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "معرّف اللوكر (ID): ${detectedLockerId ?: "غير محدد"}",
                                color = CpaSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (lockerStatusMsg != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الحالة: $lockerStatusMsg",
                                color = CpaAccent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input field for custom script tag or URL or ID
                Text(
                    text = "رابط السكريبت / وسم <script> / أو ID:",
                    color = CpaText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = lockerScriptInput,
                    onValueChange = { lockerScriptInput = it },
                    textStyle = TextStyle(fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = CpaText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CpaPrimary,
                        unfocusedBorderColor = CpaBorder,
                        focusedContainerColor = CpaBg,
                        unfocusedContainerColor = CpaBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CpaCardElevated)
                            .border(0.5.dp, CpaBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                lockerScriptInput = "<script type=\"text/javascript\" src=\"https://alignmentfiles.com/script_include.php?id=1783346\"></script>"
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("1783346 (افتراضي)", color = CpaPrimary, fontSize = 9.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CpaCardElevated)
                            .border(0.5.dp, CpaBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                val (_, currentId) = AutomationScriptBuilder.extractLockerUrlAndId(lockerScriptInput)
                                lockerScriptInput = "https://alignmentfiles.com/script_include.php?id=$currentId"
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("alignmentfiles", color = CpaText, fontSize = 9.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CpaCardElevated)
                            .border(0.5.dp, CpaBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                val (_, currentId) = AutomationScriptBuilder.extractLockerUrlAndId(lockerScriptInput)
                                lockerScriptInput = "https://www.cpagrip.com/script_include.php?id=$currentId"
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("cpagrip", color = CpaText, fontSize = 9.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val (parsedUrl, parsedId) = AutomationScriptBuilder.extractLockerUrlAndId(lockerScriptInput)
                            onExecuteLocker(parsedUrl, parsedId, false)
                            Toast.makeText(context, "جاري فك حظر وتشغيل سكريبت اللوكر...", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ تشغيل وتفعيل اللوكر الآن (Trigger Locker)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val (parsedUrl, parsedId) = AutomationScriptBuilder.extractLockerUrlAndId(lockerScriptInput)
                            onExecuteLocker(parsedUrl, parsedId, true)
                            Toast.makeText(context, "تم حقن السكريبت وتشغيله فوراً!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CpaCardElevated),
                        border = BorderStroke(1.dp, CpaPrimaryBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💉 حقن وتشغيل السكريبت بالصفحة (Inject & Run)", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
