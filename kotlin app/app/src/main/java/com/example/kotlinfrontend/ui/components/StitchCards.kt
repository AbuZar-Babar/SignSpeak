package com.example.kotlinfrontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import com.example.kotlinfrontend.ui.theme.BrandBackground
import com.example.kotlinfrontend.ui.theme.BrandInk
import com.example.kotlinfrontend.ui.theme.BrandMuted
import com.example.kotlinfrontend.ui.theme.BrandPrimary
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLow
import com.example.kotlinfrontend.ui.theme.SurfaceContainerLowest

@Composable
fun TonalActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = BrandPrimary,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.14f), CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandInk,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun CloudSyncCard(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    TonalActionCard(
        modifier = modifier,
        title = "Cloud Sync",
        subtitle = "Synchronise across devices",
        icon = Icons.Rounded.Refresh,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = {},
                enabled = false,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceContainerLowest,
                    checkedTrackColor = BrandPrimary,
                    disabledCheckedThumbColor = SurfaceContainerLowest,
                    disabledCheckedTrackColor = BrandPrimary,
                    disabledUncheckedThumbColor = SurfaceContainerLowest,
                    disabledUncheckedTrackColor = BrandBackground
                )
            )
        }
    )
}
