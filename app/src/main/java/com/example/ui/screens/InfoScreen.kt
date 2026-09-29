package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.AutomationState
import com.example.data.model.ExtractedInfo
import com.example.data.model.GeneratedIdentity
import com.example.ui.theme.CpaAccent
import com.example.ui.theme.CpaAccentDim
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

@Composable
fun InfoScreen(
    extractedInfo: ExtractedInfo,
    identity: GeneratedIdentity,
    automationState: AutomationState,
    onRegenerateIdentity: () -> Unit,
    onRefreshGeo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    var showCardNumber by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Controls Row: Regenerate Identity & Refresh Geo
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRegenerateIdentity,
                    colors = ButtonDefaults.buttonColors(containerColor = CpaPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("regenerate_identity_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Identity", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRefreshGeo,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CpaPrimaryBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Refresh Geo", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh IP", color = CpaPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Extracted Geo Info Card
        item {
            SectionCard(title = "EXTRACTED GEO & IP INFO", icon = Icons.Default.LocationOn) {
                InfoRow("IP Address", extractedInfo.ip, onCopy = { copyToClipboard("IP Address", extractedInfo.ip) }, isMonospace = true)
                InfoRow("Country", "${extractedInfo.country} (${extractedInfo.countryCode})")
                InfoRow("City / Region", "${extractedInfo.city}, ${extractedInfo.region}")
                InfoRow("Postal / Zip", extractedInfo.postalCode, onCopy = { copyToClipboard("Postal Code", extractedInfo.postalCode) })
                InfoRow("Timezone", extractedInfo.timezone, isMonospace = true)
                InfoRow("Language", extractedInfo.language)
                InfoRow("ISP / Org", extractedInfo.isp)
                InfoRow("Coordinates", "${extractedInfo.latitude}, ${extractedInfo.longitude}")
                InfoRow("Proxy Status", if (extractedInfo.isProxy) "Active Proxy" else "Direct Connection")
            }
        }

        // Generated Fake Identity Card
        item {
            SectionCard(title = "GENERATED PROFILE IDENTITY", icon = Icons.Default.Person) {
                InfoRow("Full Name", identity.fullName, onCopy = { copyToClipboard("Full Name", identity.fullName) })
                InfoRow("First / Last", "${identity.firstName} / ${identity.lastName}")
                InfoRow("Email", identity.email, onCopy = { copyToClipboard("Email", identity.email) }, isMonospace = true)
                InfoRow("Phone", identity.phone, onCopy = { copyToClipboard("Phone", identity.phone) }, isMonospace = true)
                InfoRow("Username", identity.username, onCopy = { copyToClipboard("Username", identity.username) })

                // Password row with show/hide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Password", color = CpaTextMuted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showPassword) identity.password else "••••••••••••",
                            color = CpaText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Password",
                                tint = CpaTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(onClick = { copyToClipboard("Password", identity.password) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CpaPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                InfoRow("Birth Date / Gender", "${identity.birthDate} (${identity.gender})")
                InfoRow("Street Address", identity.address, onCopy = { copyToClipboard("Address", identity.address) })
                InfoRow("City, State, Zip", "${identity.city}, ${identity.state} ${identity.postalCode}")
                InfoRow("Country", identity.country)
            }
        }

        // Generated Payment Card Information
        item {
            SectionCard(title = "PAYMENT & CARD INFORMATION", icon = Icons.Default.CreditCard) {
                InfoRow("Cardholder", identity.cardHolder, onCopy = { copyToClipboard("Cardholder", identity.cardHolder) })
                InfoRow("Card Type", identity.cardType)

                // Card number with toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Card Number", color = CpaTextMuted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (showCardNumber) identity.cardNumber else "•••• •••• •••• " + identity.cardNumber.takeLast(4),
                            color = CpaPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(onClick = { showCardNumber = !showCardNumber }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (showCardNumber) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Card Number",
                                tint = CpaTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(onClick = { copyToClipboard("Card Number", identity.cardNumber.replace(" ", "")) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CpaPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                InfoRow("Expiry Date", identity.cardExpiry, onCopy = { copyToClipboard("Expiry Date", identity.cardExpiry) }, isMonospace = true)
                InfoRow("CVV", identity.cardCvv, onCopy = { copyToClipboard("CVV", identity.cardCvv) }, isMonospace = true)
                InfoRow("Issuing Bank", identity.bankName)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SectionCard(
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
            Spacer(modifier = Modifier.height(6.dp))

            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = CpaTextMuted, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = CpaText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            )
            if (onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", tint = CpaPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
