package com.example.kotlinfrontend.ui.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlinfrontend.ui.theme.LexendFontFamily
import com.example.kotlinfrontend.ui.theme.PrimaryGreen

@Composable
fun LearningPathScreen(
    onNavigateToDictionary: () -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header Stats Area
            item {
                HeaderStatsBar()
            }

            // Quick Actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionButton(
                        title = "PSL Dictionary",
                        icon = Icons.Filled.Star,
                        color = Color(0xFF0EA5E9),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDictionary
                    )
                    QuickActionButton(
                        title = "Daily Practice",
                        icon = Icons.Filled.Star,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToQuiz
                    )
                }
            }

            // Path Header
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Unit 1: The Basics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LexendFontFamily,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Assalam-o-Alaikum! You're doing great. Only 3 more lessons to unlock the Family unit!",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF166534),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = LexendFontFamily
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // The Nodes
            item {
                PathNode(
                    icon = Icons.Filled.Star,
                    title = "Greetings",
                    isCompleted = true,
                    isCurrent = false,
                    isLocked = false,
                    onClick = onNavigateToQuiz
                )
                PathConnector(isCompleted = true)
                
                PathNode(
                    icon = Icons.Filled.Star,
                    title = "Alphabets",
                    isCompleted = false,
                    isCurrent = true,
                    isLocked = false,
                    onClick = onNavigateToQuiz,
                    offsetX = (-30).dp
                )
                PathConnector(isCompleted = false, offsetX = (-30).dp)

                PathNode(
                    icon = Icons.Filled.Lock,
                    title = "Family",
                    isCompleted = false,
                    isCurrent = false,
                    isLocked = true,
                    onClick = {},
                    offsetX = 30.dp
                )
                PathConnector(isCompleted = false, offsetX = 30.dp)

                PathNode(
                    icon = Icons.Filled.Lock,
                    title = "Food",
                    isCompleted = false,
                    isCurrent = false,
                    isLocked = true,
                    onClick = {},
                    offsetX = (-10).dp
                )
                PathConnector(isCompleted = false, offsetX = (-10).dp)

                PathNode(
                    icon = Icons.Filled.Lock,
                    title = "Home",
                    isCompleted = false,
                    isCurrent = false,
                    isLocked = true,
                    onClick = {}
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeaderStatsBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "PSL Journey",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LexendFontFamily,
                color = Color(0xFF0F172A)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatItem("🔥", "5")
            StatItem("💎", "525")
        }
    }
}

@Composable
fun StatItem(emoji: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontFamily = LexendFontFamily,
            color = Color(0xFF475569)
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LexendFontFamily,
                color = Color(0xFF0F172A),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PathConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    offsetX: androidx.compose.ui.unit.Dp = 0.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(40.dp)
                .padding(start = offsetX)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isCompleted) PrimaryGreen else Color(0xFFE2E8F0))
        )
    }
}

@Composable
fun PathNode(
    icon: ImageVector,
    title: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    offsetX: androidx.compose.ui.unit.Dp = 0.dp
) {
    val containerColor = when {
        isCurrent -> PrimaryGreen
        isCompleted -> PrimaryGreen.copy(alpha = 0.8f)
        else -> Color(0xFFE2E8F0)
    }
    
    val iconColor = when {
        isCurrent || isCompleted -> Color.White
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(start = offsetX)
        ) {
            Box(
                modifier = Modifier
                    .size( if (isCurrent) 80.dp else 64.dp )
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable(enabled = !isLocked, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, 
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(if (isCurrent) 40.dp else 32.dp)
                )
                
                // Active crown/ring indicator
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontFamily = LexendFontFamily,
                color = if (isLocked) Color(0xFF94A3B8) else Color(0xFF0F172A)
            )
        }
    }
}
