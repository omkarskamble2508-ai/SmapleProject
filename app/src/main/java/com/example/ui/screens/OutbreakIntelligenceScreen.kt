package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.SafetyBadgePill
import com.example.ui.theme.*
import kotlin.math.exp
import kotlin.math.roundToInt

@Composable
fun OutbreakIntelligenceScreen(
    villages: List<VillageEntity>,
    detectedClusters: List<DetectedCluster>,
    earlySignals: List<EarlyNonMedicalSignal>,
    onUpdateNonMedicalSignals: (VillageEntity, Int, Int, Int) -> Unit
) {
    var selectedSimVillage by remember(villages) {
        mutableStateOf(villages.firstOrNull())
    }
    var chlorinateSpringToday by remember { mutableStateOf(true) }
    var deployMobileOrsCamp by remember { mutableStateOf(true) }
    var doNothingDays by remember { mutableIntStateOf(5) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("outbreak_intelligence_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. FEATURE #9: OUTBREAK WHAT-IF SIMULATOR (Page 9-10)
        item {
            OutbreakWhatIfSimulatorCard(
                villages = villages,
                selectedVillage = selectedSimVillage,
                onSelectVillage = { selectedSimVillage = it },
                chlorinateSpringToday = chlorinateSpringToday,
                onToggleChlorinate = { chlorinateSpringToday = it },
                deployOrsCamp = deployMobileOrsCamp,
                onToggleOrsCamp = { deployMobileOrsCamp = it },
                doNothingDays = doNothingDays,
                onChangeDoNothingDays = { doNothingDays = it }
            )
        }

        // 2. FEATURE #10: CLUSTER DETECTION FOR SPARSE DATA (Page 10-11)
        item {
            SparseDataClusterDetectionSection(clusters = detectedClusters)
        }

        // 3. FEATURE #8: EARLY SIGNALS FROM NON-MEDICAL SOURCES (Page 8-9)
        item {
            EarlyNonMedicalSignalsSection(
                villages = villages,
                signals = earlySignals,
                onSubmitWeeklySignalForm = { village, orsPackets, absenteePct, ashaOrsStockPct ->
                    onUpdateNonMedicalSignals(village, orsPackets, absenteePct, ashaOrsStockPct)
                }
            )
        }
    }
}

@Composable
private fun OutbreakWhatIfSimulatorCard(
    villages: List<VillageEntity>,
    selectedVillage: VillageEntity?,
    onSelectVillage: (VillageEntity) -> Unit,
    chlorinateSpringToday: Boolean,
    onToggleChlorinate: (Boolean) -> Unit,
    deployOrsCamp: Boolean,
    onToggleOrsCamp: (Boolean) -> Unit,
    doNothingDays: Int,
    onChangeDoNothingDays: (Int) -> Unit
) {
    val population = selectedVillage?.population ?: 4200

    val projections = remember(selectedVillage, chlorinateSpringToday, deployOrsCamp, doNothingDays) {
        val baseMultiplier = when (selectedVillage?.overallRisk) {
            "CONTAMINATED" -> 1.35f
            "RISKY" -> 1.0f
            else -> 0.55f
        }
        val doNothingCurve = (1..14).map { day ->
            val growth = if (day <= doNothingDays + 4) {
                (4f * exp(0.24f * day) * baseMultiplier).coerceAtMost(95f)
            } else {
                (90f * exp(-0.12f * (day - (doNothingDays + 4))) * baseMultiplier).coerceAtLeast(12f)
            }
            growth.roundToInt()
        }

        val reductionFactor = (if (chlorinateSpringToday) 0.58f else 0.0f) +
                (if (deployOrsCamp) 0.22f else 0.0f)
        val interventionCurve = doNothingCurve.mapIndexed { idx, baseVal ->
            if (idx == 0) baseVal
            else (baseVal * (1f - (reductionFactor * ((idx + 1) / 14f).coerceAtMost(1f)).coerceAtMost(0.82f)))
                .roundToInt()
                .coerceAtLeast(1)
        }
        doNothingCurve to interventionCurve
    }

    val totalDoNothingCases = projections.first.sum()
    val totalInterventionCases = projections.second.sum()
    val estimatedCasesAverted = (totalDoNothingCases - totalInterventionCases).coerceAtLeast(0)

    // SIR Model Compartments (Page 10: Susceptible, Infected, Recovered)
    val sirInfected = if (chlorinateSpringToday || deployOrsCamp) totalInterventionCases else totalDoNothingCases
    val sirRecovered = (sirInfected * 0.78f).roundToInt()
    val sirSusceptible = (population - sirInfected).coerceAtLeast(0)

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("what_if_simulator_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Outbreak What-If Simulator",
                    tint = SovereignNavy,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "वैशिष्ट्य #९ • साथरोग 'काय होईल जर' सिम्युलेटर (OUTBREAK WHAT-IF SIMULATOR)",
                        style = MaterialTheme.typography.labelSmall,
                        color = CivicTeal
                    )
                    Text(
                        text = "SIR Model निर्णय सहाय्यक आलेख (उपाययोजना केली विरुद्ध काहीच केले नाही)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "'आज विहिरीत क्लोरिन टाकले तर किती केसेस वाचतील?' आणि '$doNothingDays दिवस काहीच केले नाही तर किती केसेस वाढतील?' याचा तुलनात्मक अंदाज.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                villages.forEach { v ->
                    FilterChip(
                        selected = v.id == selectedVillage?.id,
                        onClick = { onSelectVillage(v) },
                        label = { Text(v.name.substringBefore(" "), style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = chlorinateSpringToday,
                    onClick = { onToggleChlorinate(!chlorinateSpringToday) },
                    label = { Text("आजच क्लोरिन टाका (-58% प्रसार)") },
                    leadingIcon = {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("sim_toggle_chlorinate")
                )
                FilterChip(
                    selected = deployOrsCamp,
                    onClick = { onToggleOrsCamp(!deployOrsCamp) },
                    label = { Text("ORS वाटप कॅम्प (-22% तीव्रता)") },
                    leadingIcon = {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "उशीर झाल्यास ('काहीच केले नाही - $doNothingDays दिवस'):",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 5, 7).forEach { d ->
                        FilterChip(
                            selected = doNothingDays == d,
                            onClick = { onChangeDoNothingDays(d) },
                            label = { Text("${d}d") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TwoLineWhatIfGraphCanvas(
                doNothingPoints = projections.first,
                interventionPoints = projections.second
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimulatorMetricPill(
                    title = "काहीच केले नाही (${doNothingDays}d)",
                    value = "~$totalDoNothingCases केसेस",
                    color = ContaminatedRed,
                    modifier = Modifier.weight(1f)
                )
                SimulatorMetricPill(
                    title = "उपाययोजना केली",
                    value = "~$totalInterventionCases केसेस",
                    color = SafeGreen,
                    modifier = Modifier.weight(1f)
                )
                SimulatorMetricPill(
                    title = "वाचवलेले रुग्ण",
                    value = "~$estimatedCasesAverted सुरक्षित",
                    color = CivicTeal,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page 10 Requirement: SIR Model breakdown + explicit Assumptions (गृहीतके) + Demo Data note
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "तांत्रिक मॉडेल: सोपे SIR Model (Susceptible = $sirSusceptible • Infected = $sirInfected • Recovered = $sirRecovered)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SovereignNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "वापरलेली गृहीतके (Model Assumptions): १) दैनंदिन संसर्ग वाढ दर R0 = 1.24x  २) क्लोरिनेशनमुळे पाण्यातून होणारा प्रसार ५८% कमी होतो  ३) ORS कॅम्पमुळे गंभीर गुंतागुंत २२% कमी होते.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = EmblemSaffronLight,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "लक्षात ठेवा (Disclaimer): हे अंदाज आहेत, खात्री नाही. ऐतिहासिक माहिती नसल्याने येथे नमुना (demo) आकडे आणि SIR सूत्रे वापरली आहेत.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF78350F),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TwoLineWhatIfGraphCanvas(
    doNothingPoints: List<Int>,
    interventionPoints: List<Int>
) {
    val maxVal = (doNothingPoints.maxOrNull() ?: 100).coerceAtLeast(30).toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .padding(14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            for (i in 1..3) {
                val y = h * (i / 4f)
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            val stepX = w / (doNothingPoints.size - 1).coerceAtLeast(1)

            val redPath = Path()
            doNothingPoints.forEachIndexed { idx, value ->
                val x = idx * stepX
                val y = h - (value / maxVal) * (h * 0.85f)
                if (idx == 0) redPath.moveTo(x, y) else redPath.lineTo(x, y)
                drawCircle(Color(0xFFEF4444), radius = 4.5f, center = Offset(x, y))
            }
            drawPath(
                path = redPath,
                color = Color(0xFFEF4444),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            )

            val greenPath = Path()
            interventionPoints.forEachIndexed { idx, value ->
                val x = idx * stepX
                val y = h - (value / maxVal) * (h * 0.85f)
                if (idx == 0) greenPath.moveTo(x, y) else greenPath.lineTo(x, y)
                drawCircle(Color(0xFF22C55E), radius = 4.5f, center = Offset(x, y))
            }
            drawPath(
                path = greenPath,
                color = Color(0xFF22C55E),
                style = Stroke(width = 4.5f)
            )
        }

        // Exact line labels from Page 10: "उपाययोजना केली" vs "काहीच केले नाही"
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "काहीच केले नाही (Do Nothing)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "उपाययोजना केली (Intervention)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SimulatorMetricPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SparseDataClusterDetectionSection(clusters: List<DetectedCluster>) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Sparse Data Cluster Detection",
                    tint = ContaminatedRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "वैशिष्ट्य #१० • कमी डेटामधून क्लस्टर शोध (CLUSTER DETECTION FOR SPARSE DATA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ContaminatedRed
                    )
                    Text(
                        text = "ग्रामीण भागातील छोट्या उद्रेकांचे तात्काळ निदान (उदा. '3 दिवसांत 9 केसेस, सामान्य संख्या 2')",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "लहान गावात खूप कमी तक्रारी नोंदतात. त्यामुळे १ कि.मी. परिसरात आणि ३ दिवसांच्या वेळेत नेहमीपेक्षा जास्त केसेस आल्यास अ‍ॅप त्यांना एकत्र गट (Cluster) म्हणून ओळखते.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Page 11 Requirement: Show the 2 statistical rules preventing false alarms
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "खोटे अलर्ट टाळण्यासाठी २ सांख्यिकीय नियम (Anti-False-Positive Rules):",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "✓ नियम १ (किमान केसेसची संख्या): ३ दिवसांत किमान ५ किंवा अधिक केसेस असाव्यात.\n✓ नियम २ (बेसलाइनशी तुलना): केसेसची संख्या सामान्य संख्येच्या किमान दुप्पट (≥ 2x Baseline) असावी.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            clusters.forEach { cluster ->
                Surface(
                    color = ContaminatedRedBg.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, ContaminatedRed.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
                            text = "केंद्रबिंदू स्रोत: ${cluster.epicentreSourceName} (परिसर ${cluster.radiusKm} किमी वर्तुळ)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun EarlyNonMedicalSignalsSection(
    villages: List<VillageEntity>,
    signals: List<EarlyNonMedicalSignal>,
    onSubmitWeeklySignalForm: (VillageEntity, Int, Int, Int) -> Unit
) {
    var showWeeklyFormDialog by remember { mutableStateOf(false) }
    var selectedFormVillage by remember(villages) { mutableStateOf(villages.firstOrNull()) }
    var orsSalesInput by remember { mutableStateOf("230") }
    var schoolAbsenteeInput by remember { mutableStateOf("29") }
    var ashaStockUsedInput by remember { mutableStateOf("84") }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = "Early Signals from Non-medical Sources",
                        tint = RiskyAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "वैशिष्ट्य #८ • वैद्यकीय बाह्य पूर्व-संकेत (NON-MEDICAL EARLY SIGNALS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = RiskyAmber
                        )
                        Text(
                            text = "गैरहजेरी आणि ORS विक्री नेहमीपेक्षा वाढली आहे का?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { showWeeklyFormDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignNavy),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_weekly_signal_form_btn")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("साप्ताहिक फॉर्म भरा", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "लोक क्लिनिकला जाण्याआधी इतर गोष्टी घडतात: मेडिकल दुकानात ORS ची विक्री वाढते, शाळेत गैरहजेरी वाढते आणि आशा वर्करकडील ORS साठा लवकर संपतो. दुकानदार, शिक्षक आणि आशा कार्यकर्ते आठवड्यातून एकदा सोपा फॉर्म भरतात.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Page 9 Caution Note: Possible signal vs confirmed disease (festivals/other reasons)
            Surface(
                color = EmblemSaffronLight,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "लक्षात ठेवा: शाळेत गैरहजेरी सण-उत्सव, पाऊस किंवा इतर कारणांनीही वाढू शकते. म्हणून हा संकेत 'निश्चित रोग (Confirmed Outbreak)' नाही, तर फक्त 'शक्यता (Possible Early Signal)' म्हणून दाखवला जातो.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF78350F),
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            signals.forEach { sig ->
                val villageObj = villages.firstOrNull { it.id == sig.villageId }
                Surface(
                    color = if (sig.isSignalTriggered) {
                        RiskyAmberBg.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.dp,
                            if (sig.isSignalTriggered) RiskyAmber else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sig.villageName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (sig.isSignalTriggered) {
                                    Text(
                                        text = "सूचना कार्ड: गैरहजेरी आणि ORS विक्री नेहमीपेक्षा वाढली आहे!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ContaminatedRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (sig.isSignalTriggered) {
                                Surface(
                                    color = ContaminatedRed,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "शक्यता संकेत (Possible Signal)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (villageObj != null) {
                                TextButton(onClick = { onSubmitWeeklySignalForm(villageObj, 260, 31, 88) }) {
                                    Text("ORS विक्री वाढवा", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sig.headlineSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Page 9 Requirement: Mini comparative graph inside the card (Baseline vs Today)
                        MiniEarlySignalBarGraph(
                            orsBaseline = 75,
                            orsToday = 75 + (75 * sig.shopOrsSpikePercent) / 100,
                            absenteePct = sig.schoolAbsencePercent,
                            ashaStockDepletionPct = sig.ashaStockDepletionPercent
                        )
                    }
                }
            }
        }
    }

    if (showWeeklyFormDialog) {
        val currentV = selectedFormVillage
        AlertDialog(
            onDismissRequest = { showWeeklyFormDialog = false },
            title = {
                Text(
                    "साप्ताहिक गैर-वैद्यकीय फॉर्म (दुकानदार / शिक्षक / आशा)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "गाव निवडा आणि या आठवड्याची आकडेवारी भरा (Baseline च्या २ पटीपेक्षा जास्त असल्यास अलर्ट तयार होतो):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        villages.forEach { v ->
                            FilterChip(
                                selected = v.id == currentV?.id,
                                onClick = { selectedFormVillage = v },
                                label = { Text(v.name.substringBefore(" "), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = orsSalesInput,
                        onValueChange = { orsSalesInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("१. मेडिकल दुकान: ORS पॅकेट विक्री (सामान्य ~75)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = schoolAbsenteeInput,
                        onValueChange = { schoolAbsenteeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("२. शाळा शिक्षक: विद्यार्थी गैरहजेरी % (सामान्य ~8%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ashaStockUsedInput,
                        onValueChange = { ashaStockUsedInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("३. आशा वर्कर: संपलेला ORS साठा % (सामान्य ~30%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = selectedFormVillage
                        if (v != null) {
                            onSubmitWeeklySignalForm(
                                v,
                                orsSalesInput.toIntOrNull() ?: 210,
                                (schoolAbsenteeInput.toIntOrNull() ?: 26).coerceIn(0, 100),
                                (ashaStockUsedInput.toIntOrNull() ?: 80).coerceIn(0, 100)
                            )
                        }
                        showWeeklyFormDialog = false
                    }
                ) {
                    Text("साप्ताहिक नोंद जतन करा")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeeklyFormDialog = false }) {
                    Text("रद्द करा")
                }
            }
        )
    }
}

@Composable
private fun MiniEarlySignalBarGraph(
    orsBaseline: Int,
    orsToday: Int,
    absenteePct: Int,
    ashaStockDepletionPct: Int
) {
    Surface(
        color = Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "छोटा तुलनात्मक आलेख (Baseline vs आज):",
                    style = MaterialTheme.typography.labelSmall,
                    color = SovereignNavy,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ORS: $orsBaseline → $orsToday पॅकेट्स",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (orsToday >= orsBaseline * 2) ContaminatedRed else SafeGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                val maxOrs = maxOf(orsToday, orsBaseline * 2, 150).toFloat()
                val barH = size.height / 2.6f

                // Bar 1: Normal Baseline ORS (Green/Teal)
                val baseW = (orsBaseline / maxOrs) * (size.width * 0.65f)
                drawRect(
                    color = Color(0xFF0D9488),
                    topLeft = Offset(0f, 2f),
                    size = Size(baseW.coerceAtLeast(18f), barH)
                )

                // Bar 2: Current ORS Sales + School Absenteeism Spike (Amber/Red)
                val todayW = (orsToday / maxOrs) * (size.width * 0.65f)
                val spikeColor = if (orsToday >= orsBaseline * 2 || absenteePct >= 20) Color(0xFFDC2626) else Color(0xFFD97706)
                drawRect(
                    color = spikeColor,
                    topLeft = Offset(0f, barH + 8f),
                    size = Size(todayW.coerceAtLeast(18f), barH)
                )

                // Right-side mini bars for School Absentee % and ASHA ORS Stock Used %
                val rightStart = size.width * 0.72f
                val rightMaxW = size.width * 0.26f
                drawRect(
                    color = Color(0xFF6366F1),
                    topLeft = Offset(rightStart, 2f),
                    size = Size((absenteePct / 100f) * rightMaxW, barH)
                )
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(rightStart, barH + 8f),
                    size = Size((ashaStockDepletionPct / 100f) * rightMaxW, barH)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "हिरवी पट्टी: सामान्य ORS ($orsBaseline) • लाल पट्टी: आजची विक्री ($orsToday)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "शाळा गैरहजेरी: $absenteePct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}
