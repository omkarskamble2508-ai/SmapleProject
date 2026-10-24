package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.SafetyBadgePill
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * Dedicated Citizen / Visitor Identity Pass & Village Water Safety Alerts Screen
 * (Tab 3 of the User / Citizen Portal)
 */
@Composable
fun CitizenProfileAndAlertsScreen(
    account: ActorAccountEntity,
    visitorRadar: VisitorRadarStatus?,
    villages: List<VillageEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("citizen_profile_alerts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Verified Aadhaar-Linked Citizen Identity Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(SovereignNavy, CivicTeal)),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "नागरिक आरोग्य ओळखपत्र • VERIFIED CITIZEN PASS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTeal
                            )
                            Text(
                                text = account.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = SafeGreenBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "आधार व फोटो सत्यापित ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• मोबाईल क्रमांक (Mobile): ${account.mobileNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• आधार क्रमांक (Masked Aadhaar): ${account.aadhaarNumberMasked ?: "XXXX-XXXX-4829"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• फोटो स्कॅन स्वाक्षरी (Photo Scan Token): ${account.photoScanSignature}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SovereignNavy
                    )
                }
            }
        }

        // 2. Citizen Water Boiling & ORS Home Preparation Guide (Marathi + English)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "नागरिकांसाठी महत्त्वाच्या सूचना • CITIZEN WATER SAFETY GUIDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    Text(
                        text = "पाणी शुद्धीकरण आणि ORS वापरण्याची पद्धत",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val rules = listOf(
                        "१. अनोळखी गावात गेल्यावर फक्त हिरव्या खुणेच्या (SAFE • GREEN) नळाचे किंवा झऱ्याचे पाणी प्या.",
                        "२. लाल (CONTAMINATED) किंवा पिवळ्या (RISKY) स्रोताचे पाणी किमान ३ मिनिटे खळखळून उकळून व गाळून प्या.",
                        "३. २० लिटर पाण्यासाठी १ क्लोरिन गोळी (Chlorine Tablet) टाकून ३० मिनिटांनंतर पाणी वापरा.",
                        "४. जुलाब किंवा उलट्या सुरू झाल्यास १ लिटर उकळून थंड केलेल्या पाण्यात १ पूर्ण पाकीट ORS मिसळून थोडे-थोडे पाजा."
                    )
                    rules.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // 3. Active Flood & Village Water Advisories
        item {
            Text(
                text = "गावानुसार पाणी व पूर सूचना • VILLAGE WATER ADVISORIES",
                style = MaterialTheme.typography.labelLarge,
                color = SovereignNavy,
                fontWeight = FontWeight.Bold
            )
        }

        items(villages, key = { it.id }) { v ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${v.name} (${v.district}, ${v.stateName})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        SafetyBadgePill(safetyLevel = v.overallRisk)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = v.visitorAdvisoryNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Local Doctor Clinical Triage & Cluster Dashboard
 * (Tab 0 of the Local Doctor Portal)
 */
@Composable
fun DoctorClinicalDashboardScreen(
    doctorAccount: ActorAccountEntity,
    symptomReports: List<SymptomReportEntity>,
    detectedClusters: List<DetectedCluster>,
    custodySamples: List<SampleCustodyEntity>,
    onNavigateToLabQrTab: () -> Unit,
    onNavigateToMapTab: () -> Unit
) {
    val severeCount = symptomReports.count { it.severity == "SEVERE_URGENT" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("doctor_clinical_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Doctor Clinical Summary Banner
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "स्थानिक डॉक्टर क्लिनिकल कक्ष • LOCAL DOCTOR CLINICAL DESK",
                        style = MaterialTheme.typography.labelSmall,
                        color = CivicTeal
                    )
                    Text(
                        text = "${doctorAccount.fullName} • रुग्ण तपासणी व क्लस्टर विश्लेषण",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "आशा स्वयंसेविका आणि ग्रामस्थांकडून आलेले गंभीर रुग्ण अहवाल (Voice Triage #7), साथरोग क्लस्टर (#10) आणि प्रयोगशाळा QR नमुने (#5) येथे तपासा.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToLabQrTab,
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR नमुने (${custodySamples.size})", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onNavigateToMapTab,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("पाणी स्रोत नकाशा", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 2. Feature #10: Active Disease Clusters Flagged for Doctor Review
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वैशिष्ट्य #१० • साथरोग क्लस्टर निदान (CLUSTER DETECTION FOR SPARSE DATA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ContaminatedRed
                    )
                    Text(
                        text = "कमी माहिती असतानाही आढळलेले स्थानिक रुग्ण गट (${detectedClusters.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    detectedClusters.forEach { cluster ->
                        Surface(
                            color = ContaminatedRedBg.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, ContaminatedRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${cluster.villageName} (${cluster.districtState})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    SafetyBadgePill(safetyLevel = cluster.severity.name)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "संशयित पाणी स्रोत (Epicentre): ${cluster.epicentreSourceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = cluster.explanationReason,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ContaminatedRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Patient Referral Queue from Voice Symptom Triage (#7)
        item {
            Text(
                text = "रुग्ण ट्रायएज यादी • PATIENT TRIAGE QUEUE (${symptomReports.size} Cases • $severeCount Urgent)",
                style = MaterialTheme.typography.labelLarge,
                color = SovereignNavy,
                fontWeight = FontWeight.Bold
            )
        }

        items(symptomReports, key = { it.id }) { report ->
            val isSevere = report.severity == "SEVERE_URGENT"
            Surface(
                color = if (isSevere) ContaminatedRedBg.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSevere) ContaminatedRed else SafeGreen.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${report.patientDescription} • ${report.villageName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (isSevere) ContaminatedRed else SafeGreen,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isSevere) "तातडीचे • URGENT PHC" else "मध्यम • ORS HOME",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "रुग्णाचे वर्णन (${report.languageName}): \"${report.voiceTranscript}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "कालावधी: ${report.durationDays} दिवस • पाणी पितो का: ${if (report.isDrinkingWater) "होय" else "नाही"} • डोळे खोल गेले: ${if (report.hasSunkenEyes) "होय" else "नाही"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "डॉक्टर / ट्रायएज सल्ला: ${report.triageGuidance}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSevere) ContaminatedRed else SafeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
