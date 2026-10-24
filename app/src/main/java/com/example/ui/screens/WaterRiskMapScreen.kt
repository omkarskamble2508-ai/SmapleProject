package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.SafetyBadgePill
import com.example.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun WaterRiskMapScreen(
    selectedRole: ActorRole,
    villages: List<VillageEntity>,
    waterSources: List<WaterSourceEntity>,
    selectedSourceId: String?,
    visitorRadar: VisitorRadarStatus?,
    isUsingRealGps: Boolean,
    userLat: Double,
    userLon: Double,
    onRefreshDeviceGps: () -> Unit,
    onSelectVisitorVillage: (VillageEntity) -> Unit,
    onSelectSource: (String) -> Unit,
    onMarkSourceCleaned: (WaterSourceEntity) -> Unit,
    onJumpToCameraTestForSource: ((WaterSourceEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var filterSafety by remember { mutableStateOf("ALL") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRefreshDeviceGps()
        }
    }

    val filteredSources = remember(waterSources, filterSafety) {
        if (filterSafety == "ALL") waterSources
        else waterSources.filter { it.safetyLevel == filterSafety }
    }

    val selectedSource = remember(waterSources, selectedSourceId) {
        waterSources.firstOrNull { it.id == selectedSourceId } ?: waterSources.firstOrNull()
    }

    val isCitizenRole = selectedRole == ActorRole.VILLAGE_RESIDENT
    val canEditOrCleanSource = selectedRole == ActorRole.ASHA_WORKER || selectedRole == ActorRole.PHC_OFFICIAL

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("water_risk_map_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Citizen Priority: GPS Unfamiliar Village Water Safety Radar
        if (isCitizenRole && visitorRadar != null) {
            item {
                CitizenSafeWaterRadarBanner(
                    radar = visitorRadar,
                    villages = villages,
                    isUsingRealGps = isUsingRealGps,
                    onRequestRealGps = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onSelectVillagePreset = onSelectVisitorVillage,
                    onSelectSafeSourceOnMap = { srcId -> onSelectSource(srcId) },
                    onNavigateGoogleMaps = { lat, lon, label ->
                        val gmmUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, gmmUri))
                        } catch (_: Exception) {
                            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    }
                )
            }
        }

        // 1. Header & Legend for Feature #3: Water-Source Level Risk Map (Page 4)
        item {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "वैशिष्ट्य #३ • WATER-SOURCE LEVEL RISK MAP",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTeal
                            )
                            Text(
                                text = "\"गाव धोक्यात आहे\" ऐवजी नेमका कोणता झरा / विहीर / हँडपंप धोकादायक आहे ते पहा",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "प्रत्येक बिंदूचा रंग ३ गोष्टींवरून ठरतो: १) त्या स्रोताचा ताजा test निकाल, २) जवळचा पाऊस/पूर (Flood flag), आणि ३) जवळ (५०० मीटर परिसरात) आढळलेले आजारी रुग्ण. बिंदूवर tap केल्यावर खालील कार्डात इतिहास व 'हा बिंदू लाल का?' याचे कारण दिसते.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "सर्व स्रोत • All (${waterSources.size})",
                            "SAFE" to "हिरवा • Safe (${waterSources.count { it.safetyLevel == "SAFE" }})",
                            "RISKY" to "पिवळा • Amber (${waterSources.count { it.safetyLevel == "RISKY" }})",
                            "CONTAMINATED" to "लाल • Red (${waterSources.count { it.safetyLevel == "CONTAMINATED" }})"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = filterSafety == key,
                                onClick = { filterSafety = key },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Multi-Layer GIS Canvas Map
        item {
            InteractiveNeWaterGisCanvas(
                sources = filteredSources,
                villages = villages,
                selectedSource = selectedSource,
                userLat = userLat,
                userLon = userLon,
                onSelectSource = onSelectSource
            )
        }

        // 3. Selected Source Detailed Inspection Sheet + "हा बिंदू लाल का?" Risk Score Explanation (Page 4)
        item {
            selectedSource?.let { src ->
                val parentVillage = villages.firstOrNull { it.id == src.villageId }
                val distKm = CryptoAndGeoUtils.haversineDistanceKm(
                    userLat, userLon, src.latitude, src.longitude
                )
                SelectedWaterSourceDetailCard(
                    source = src,
                    floodActiveInVillage = parentVillage?.floodWarningActive == true,
                    distanceKm = distKm,
                    canEditOrClean = canEditOrCleanSource,
                    onMarkCleaned = { onMarkSourceCleaned(src) },
                    onTestWithCamera = onJumpToCameraTestForSource?.let { callback ->
                        { callback(src) }
                    },
                    onLaunchGoogleMaps = {
                        val gmmUri = Uri.parse(
                            "geo:${src.latitude},${src.longitude}?q=${src.latitude},${src.longitude}(${Uri.encode(src.name)})"
                        )
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, gmmUri))
                        } catch (_: Exception) {
                            val webUri = Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query=${src.latitude},${src.longitude}"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    }
                )
            }
        }

        // 4. All Water Sources Directory List
        item {
            Text(
                text = "पाणी स्रोतांची यादी • MONITORED WATER SOURCES (${filteredSources.size})",
                style = MaterialTheme.typography.labelLarge,
                color = SovereignNavy,
                fontWeight = FontWeight.Bold
            )
        }

        items(filteredSources, key = { it.id }) { src ->
            val distKm = CryptoAndGeoUtils.haversineDistanceKm(
                userLat, userLon, src.latitude, src.longitude
            )
            WaterSourceDirectoryItem(
                source = src,
                isSelected = src.id == selectedSource?.id,
                distanceKm = distKm,
                onClick = { onSelectSource(src.id) }
            )
        }
    }
}

@Composable
private fun CitizenSafeWaterRadarBanner(
    radar: VisitorRadarStatus,
    villages: List<VillageEntity>,
    isUsingRealGps: Boolean,
    onRequestRealGps: () -> Unit,
    onSelectVillagePreset: (VillageEntity) -> Unit,
    onSelectSafeSourceOnMap: (String) -> Unit,
    onNavigateGoogleMaps: (Double, Double, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(listOf(SovereignNavy, CivicTeal)),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("visitor_water_radar_card")
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CivicTealContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS Visitor Water Radar",
                            tint = CivicTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "अनोळखी गावातील पाणी गुणवत्ता रडार • GPS WATER RADAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicTeal
                        )
                        Text(
                            text = "तुमच्या स्थानानुसार सुरक्षित पाणी मार्गदर्शन",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onRequestRealGps,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .testTag("gps_locate_me_button")
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUsingRealGps) "GPS सुरू" else "माझे GPS स्थान",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                villages.forEach { v ->
                    FilterChip(
                        selected = v.id == radar.nearestVillage.id,
                        onClick = { onSelectVillagePreset(v) },
                        label = {
                            Text(
                                text = "${v.name.substringBefore(" ")} (${v.stateName})",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${radar.nearestVillage.name}, ${radar.nearestVillage.district}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "GPS: ${String.format("%.4f", radar.currentLatitude)}°N, ${String.format("%.4f", radar.currentLongitude)}°E",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SafetyBadgePill(safetyLevel = radar.nearestVillage.overallRisk)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = radar.drinkabilityVerdict,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    radar.actionChecklistForVisitor.forEach { tip ->
                        Text(
                            text = "✓ $tip",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val targetSource = radar.safestSource?.first ?: radar.villageSources.firstOrNull()?.first
                    if (targetSource != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onNavigateGoogleMaps(
                                        targetSource.latitude,
                                        targetSource.longitude,
                                        targetSource.name
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("visitor_google_maps_button")
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("सुरक्षित नळाकडे मार्ग (Google Maps)", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { onSelectSafeSourceOnMap(targetSource.id) }
                            ) {
                                Text("नकाशावर पहा", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveNeWaterGisCanvas(
    sources: List<WaterSourceEntity>,
    villages: List<VillageEntity>,
    selectedSource: WaterSourceEntity?,
    userLat: Double,
    userLon: Double,
    onSelectSource: (String) -> Unit
) {
    val minLat = 23.0
    val maxLat = 28.0
    val minLon = 91.2
    val maxLon = 94.8

    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(275.dp)
            .border(1.5.dp, SovereignNavy.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .testTag("interactive_gis_canvas")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E2439))
                    .pointerInput(sources) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val nearest = sources.minByOrNull { src ->
                                val px = ((src.longitude - minLon) / (maxLon - minLon)).toFloat() * (w * 0.84f) + w * 0.08f
                                val py = (1f - ((src.latitude - minLat) / (maxLat - minLat)).toFloat()) * (h * 0.78f) + h * 0.11f
                                hypot(tapOffset.x - px, tapOffset.y - py)
                            }
                            if (nearest != null) {
                                onSelectSource(nearest.id)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val gridColor = Color.White.copy(alpha = 0.08f)
                for (i in 1..5) {
                    val y = h * (i / 6f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                for (j in 1..6) {
                    val x = w * (j / 7f)
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                }

                val riverPath = Path().apply {
                    moveTo(w * 0.92f, h * 0.14f)
                    cubicTo(w * 0.72f, h * 0.32f, w * 0.55f, h * 0.38f, w * 0.12f, h * 0.56f)
                }
                drawPath(
                    path = riverPath,
                    color = Color(0xFF38BDF8).copy(alpha = 0.28f),
                    style = Stroke(width = 14f)
                )

                villages.filter { it.floodWarningActive }.forEach { fv ->
                    val vx = ((fv.longitude - minLon) / (maxLon - minLon)).toFloat() * (w * 0.84f) + w * 0.08f
                    val vy = (1f - ((fv.latitude - minLat) / (maxLat - minLat)).toFloat()) * (h * 0.78f) + h * 0.11f
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                        radius = 56f,
                        center = Offset(vx, vy)
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                        radius = 56f,
                        center = Offset(vx, vy),
                        style = Stroke(
                            width = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    )
                }

                val ux = (((userLon - minLon) / (maxLon - minLon)).toFloat().coerceIn(0.05f, 0.95f)) * (w * 0.84f) + w * 0.08f
                val uy = (1f - ((userLat - minLat) / (maxLat - minLat)).toFloat().coerceIn(0.05f, 0.95f)) * (h * 0.78f) + h * 0.11f
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.28f),
                    radius = 28f,
                    center = Offset(ux, uy)
                )
                drawCircle(
                    color = Color(0xFF0284C7),
                    radius = 10f,
                    center = Offset(ux, uy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = Offset(ux, uy),
                    style = Stroke(width = 3f)
                )

                sources.forEach { src ->
                    val px = ((src.longitude - minLon) / (maxLon - minLon)).toFloat() * (w * 0.84f) + w * 0.08f
                    val py = (1f - ((src.latitude - minLat) / (maxLat - minLat)).toFloat()) * (h * 0.78f) + h * 0.11f
                    val isSelected = src.id == selectedSource?.id

                    val pinColor = when (src.safetyLevel) {
                        "CONTAMINATED" -> Color(0xFFEF4444)
                        "RISKY" -> Color(0xFFF59E0B)
                        else -> Color(0xFF22C55E)
                    }

                    // Page 10: Cluster circle around flagged area on map
                    if (src.nearbyCasesCount >= 3) {
                        drawCircle(
                            color = pinColor.copy(alpha = 0.28f),
                            radius = 42f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = pinColor,
                            radius = 42f,
                            center = Offset(px, py),
                            style = Stroke(width = 2f)
                        )
                    }

                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 22f,
                            center = Offset(px, py),
                            style = Stroke(width = 4f)
                        )
                    }

                    drawCircle(
                        color = pinColor,
                        radius = if (isSelected) 15f else 11f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF0E2439),
                        radius = if (isSelected) 5f else 3.5f,
                        center = Offset(px, py)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC091624))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapLegendDot(color = Color(0xFF22C55E), label = "हिरवा (Safe)")
                MapLegendDot(color = Color(0xFFF59E0B), label = "पिवळा (Amber)")
                MapLegendDot(color = Color(0xFFEF4444), label = "लाल (Red + Cluster)")
            }
        }
    }
}

@Composable
private fun MapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun SelectedWaterSourceDetailCard(
    source: WaterSourceEntity,
    floodActiveInVillage: Boolean,
    distanceKm: Double,
    canEditOrClean: Boolean,
    onMarkCleaned: () -> Unit,
    onTestWithCamera: (() -> Unit)?,
    onLaunchGoogleMaps: () -> Unit
) {
    // Page 4 Requirement: Simple explainable Risk Score ("हा बिंदू लाल का?" याचे कारण)
    val testScore = when (source.safetyLevel) {
        "CONTAMINATED" -> 50
        "RISKY" -> 25
        else -> 5
    }
    val floodScore = if (floodActiveInVillage) 20 else 0
    val casesScore = (source.nearbyCasesCount * 5).coerceAtMost(35)
    val totalRiskScore = (testScore + floodScore + casesScore).coerceAtMost(100)

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_water_source_detail_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${source.sourceType.uppercase()} • ${source.villageName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CivicTeal
                    )
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${source.districtState} • तुमच्यापासून अंतर: ${
                            if (distanceKm < 1.0) "${(distanceKm * 1000).roundToInt()} मीटर"
                            else "${String.format("%.1f", distanceKm)} किमी"
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SafetyBadgePill(safetyLevel = source.safetyLevel)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page 4 ("लक्षात ठेवा"): "हा बिंदू लाल/पिवळा का?" (Why is this source Red/Amber/Green? Explainable Risk Score)
            Surface(
                color = when (source.safetyLevel) {
                    "CONTAMINATED" -> ContaminatedRedBg.copy(alpha = 0.6f)
                    "RISKY" -> RiskyAmberBg.copy(alpha = 0.6f)
                    else -> SafeGreenBg.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "हा बिंदू ${
                            when (source.safetyLevel) {
                                "CONTAMINATED" -> "लाल (RED)"
                                "RISKY" -> "पिवळा (AMBER)"
                                else -> "हिरवा (GREEN)"
                            }
                        } का आहे? • Risk Score: $totalRiskScore/100 गुण",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SovereignNavyDark
                    )
                    Text(
                        text = "१) ताजा Test निकाल (+$testScore गुण) + २) जवळचा पाऊस/पूर (+$floodScore गुण) + ३) ५०० मीटर परिसरातील ${source.nearbyCasesCount} आजारी रुग्ण (+$casesScore गुण) = एकूण $totalRiskScore गुण.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceMetricBox("pH पातळी", "${source.phLevel}", Modifier.weight(1f))
                SourceMetricBox("गढूळपणा", "${source.turbidityNtu} NTU", Modifier.weight(1f))
                SourceMetricBox("क्लोरिन", "${source.freeChlorineMgL} mg/L", Modifier.weight(1f))
                SourceMetricBox("जवळचे रुग्ण", "${source.nearbyCasesCount}", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• शेवटचा Test निकाल (Last Test): ${source.h2sResult}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• शेवटची स्वच्छता तारीख (Last Cleaning): ${source.lastCleanedDate}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• जवळच्या केसेसची संख्या (Nearby Cases within 500m): ${source.nearbyCasesCount} रुग्ण",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (canEditOrClean) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onTestWithCamera != null) {
                        Button(
                            onClick = onTestWithCamera,
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("कॅमेरा चाचणी (#1)", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    FilledTonalButton(
                        onClick = onMarkCleaned,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mark_source_cleaned_btn")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("स्वच्छता व क्लोरिनेशन", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            OutlinedButton(
                onClick = onLaunchGoogleMaps,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("source_google_maps_nav_btn")
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Google Maps वर दिशा पहा • Navigate to ${source.name}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SourceMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WaterSourceDirectoryItem(
    source: WaterSourceEntity,
    isSelected: Boolean,
    distanceKm: Double,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) SovereignNavy else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${source.villageName} • स्वच्छता: ${source.lastCleanedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "pH ${source.phLevel} • गढूळपणा ${source.turbidityNtu} NTU • जवळचे रुग्ण: ${source.nearbyCasesCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SovereignNavy
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SafetyBadgePill(safetyLevel = source.safetyLevel)
        }
    }
}
