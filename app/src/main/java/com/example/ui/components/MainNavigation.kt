package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.theme.CpaBorder
import com.example.ui.theme.CpaCard
import com.example.ui.theme.CpaCardElevated
import com.example.ui.theme.CpaPrimary
import com.example.ui.theme.CpaPrimaryBorder
import com.example.ui.theme.CpaPrimaryDim
import com.example.ui.theme.CpaTextMuted

@Composable
fun MainNavigation(
    selectedTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs: List<Pair<ScreenTab, ImageVector>> = listOf(
        Pair(ScreenTab.TASKS, Icons.Default.ListAlt),
        Pair(ScreenTab.BROWSER, Icons.Default.Language),
        Pair(ScreenTab.TEMPLATES, Icons.Default.Build),
        Pair(ScreenTab.CLICK, Icons.Default.TouchApp),
        Pair(ScreenTab.INFO, Icons.Default.Badge),
        Pair(ScreenTab.PROXIES, Icons.Default.Router),
        Pair(ScreenTab.EMAILS, Icons.Default.Email),
        Pair(ScreenTab.SCRIPTS, Icons.Default.Code),
        Pair(ScreenTab.STATS, Icons.Default.Assessment),
        Pair(ScreenTab.SETTINGS, Icons.Default.Settings),
        Pair(ScreenTab.LOGS, Icons.Default.Terminal)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CpaCard)
            .border(1.dp, CpaBorder)
            .navigationBarsPadding() // Ensures navigation bar sits safely above device navigation bar
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tab, icon) ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CpaPrimaryDim else CpaCardElevated)
                        .border(
                            1.dp,
                            if (isSelected) CpaPrimary else CpaBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("nav_tab_${tab.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) CpaPrimary else CpaTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) CpaPrimary else CpaTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
