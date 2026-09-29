package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EmailItem
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

@Composable
fun EmailPoolScreen(
    emails: List<EmailItem>,
    onAddEmail: (String) -> Unit,
    onImportBulk: (String) -> Unit,
    onGenerateTestEmails: () -> Unit,
    onDeleteEmail: (EmailItem) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var singleInput by remember { mutableStateOf("") }
    var showBulkDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pool Header Metrics
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
                    Text("EMAIL POOL READY", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${emails.size} Available", color = CpaPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Auto-consumed on task execution", color = CpaTextDim, fontSize = 11.sp)
                }

                if (emails.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CpaCardElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Next: ${emails.first().email.take(18)}...",
                            color = CpaSuccess,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Quick Single Add
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = singleInput,
                    onValueChange = { singleInput = it },
                    placeholder = { Text("Add email (e.g. user@gmail.com)", color = CpaTextDim) },
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
                        if (singleInput.isNotBlank()) {
                            onAddEmail(singleInput)
                            singleInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Batch Tool Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showBulkDialog = true },
                    border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Bulk", tint = CpaAccent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bulk Import", color = CpaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onGenerateTestEmails,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Generate", tint = CpaPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gen 10 Test", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (emails.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAll,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CpaError.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = CpaError, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Email Items List
        itemsIndexed(emails, key = { _, item -> item.id }) { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${index + 1}.",
                            color = CpaTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(28.dp)
                        )
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = CpaTextDim, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.email,
                            color = CpaText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onDeleteEmail(item) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = CpaTextDim, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showBulkDialog) {
        var bulkText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showBulkDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CpaCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CpaBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("BULK IMPORT EMAILS", color = CpaText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paste emails separated by newlines, commas, or semicolons", color = CpaTextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        placeholder = { Text("user1@gmail.com\nuser2@outlook.com\nuser3@yahoo.com", color = CpaTextDim) },
                        minLines = 5,
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
                        OutlinedButton(onClick = { showBulkDialog = false }) {
                            Text("Cancel", color = CpaTextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onImportBulk(bulkText)
                                showBulkDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary, contentColor = Color.Black)
                        ) {
                            Text("Import Emails", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
