package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ActorAccountEntity
import com.example.data.ActorRole
import com.example.ui.theme.*

@Composable
fun GovtHeaderAndRoleBar(
    loggedInAccount: ActorAccountEntity,
    selectedRole: ActorRole,
    isOfflineHillMode: Boolean,
    onToggleOfflineMode: () -> Unit,
    pendingOfflineCount: Int,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onLogoutSwitchPortal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SovereignNavyDark, SovereignNavy)
                )
            )
    ) {
        // Top Tricolor Government Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFFF9933))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF138808))
            )
        }

        // Official Ministry Header Row (Marathi + English)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, EmblemSaffron.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "National Health Mission Emblem",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "जल सुरक्षा पूर्वोत्तर • JALSURAKSHA NE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = selectedRole.marathiName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${loggedInAccount.fullName} • ${loggedInAccount.mobileNumber}" +
                                (loggedInAccount.aadhaarNumberMasked?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Offline Hill-Tract Mode Toggle
                Surface(
                    onClick = onToggleOfflineMode,
                    shape = RoundedCornerShape(18.dp),
                    color = if (isOfflineHillMode) RiskyAmber else SafeGreen,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 34.dp)
                        .testTag("offline_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOfflineHillMode) Icons.Default.WifiOff else Icons.Default.CloudDone,
                            contentDescription = "Network & P2P Sync Status",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOfflineHillMode) "ऑफलाईन ($pendingOfflineCount)" else "ऑनलाईन",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Switch Portal / Logout Button (बाहेर पडा)
                Surface(
                    onClick = onLogoutSwitchPortal,
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 34.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .testTag("logout_switch_portal_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Switch Actor Portal / Logout",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "पोर्टल बदला / Logout",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Live Toast / Action Confirmation Banner
        AnimatedVisibility(visible = bannerMessage != null) {
            bannerMessage?.let { msg ->
                Surface(
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alert Info",
                                tint = RiskyAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF451A03),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = Color(0xFF451A03),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyBadgePill(safetyLevel: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (safetyLevel) {
        "CONTAMINATED" -> Triple(ContaminatedRedBg, ContaminatedRed, "दूषित • CONTAMINATED")
        "RISKY" -> Triple(RiskyAmberBg, RiskyAmber, "धोकादायक • RISKY")
        else -> Triple(SafeGreenBg, SafeGreen, "सुरक्षित • SAFE")
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, textColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
