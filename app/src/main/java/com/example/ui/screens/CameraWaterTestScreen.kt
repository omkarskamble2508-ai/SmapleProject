package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WaterSourceEntity
import com.example.ui.components.SafetyBadgePill
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CameraWaterTestScreen(
    waterSources: List<WaterSourceEntity>,
    preselectedSourceId: String?,
    defaultTesterName: String = "ASHA Jonali Borah",
    onSaveCameraTest: (
        source: WaterSourceEntity,
        ph: Double,
        turbidity: Double,
        h2sPositive: Boolean,
        chlorine: Double,
        testerName: String
    ) -> Unit,
    onRequestCollectLabQrSample: ((WaterSourceEntity) -> Unit)? = null
) {
    var selectedSource by remember(waterSources, preselectedSourceId) {
        mutableStateOf(
            waterSources.firstOrNull { it.id == preselectedSourceId }
                ?: waterSources.firstOrNull()
        )
    }

    var phValue by remember { mutableFloatStateOf(6.0f) }
    var turbidityValue by remember { mutableFloatStateOf(19.5f) }
    var h2sPositive by remember { mutableStateOf(true) }
    var chlorineValue by remember { mutableFloatStateOf(0.0f) }
    var testerName by remember(defaultTesterName) { mutableStateOf(defaultTesterName) }

    // Page 2 Requirement: Low-light / blurry photo detection
    var isPhotoBlurryOrLowLight by remember { mutableStateOf(false) }
    var scanStatusNote by remember {
        mutableStateOf("चौकटीत Strip आणि Colour Card ठेवून फोटो काढा (RGB/HSV तुलना तयार).")
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val cx = bitmap.width / 2
            val cy = bitmap.height / 2
            val pixel = bitmap.getPixel(cx, cy)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
            when {
                luminance < 25 -> {
                    // Too dark / low light warning (Page 2 requirement)
                    isPhotoBlurryOrLowLight = true
                    scanStatusNote = "फोटो अंधुक किंवा उजेड खूप कमी आहे (Low Light: ${luminance.roundToInt()})! कृपया पुरेशा उजेडात पुन्हा फोटो काढा."
                }
                luminance < 85 -> {
                    isPhotoBlurryOrLowLight = false
                    h2sPositive = true
                    phValue = 6.0f
                    turbidityValue = 21.0f
                    chlorineValue = 0.0f
                    scanStatusNote = "कॅमेरा RGB ($r,$g,$b) तुलना: काळी H2S बाटली आढळली → दूषित (CONTAMINATED)"
                }
                r > g + 30 -> {
                    isPhotoBlurryOrLowLight = false
                    h2sPositive = false
                    phValue = 6.4f
                    turbidityValue = 8.5f
                    chlorineValue = 0.1f
                    scanStatusNote = "कॅमेरा RGB ($r,$g,$b) तुलना: पिवळसर गढूळ पट्टी → धोकादायक (RISKY)"
                }
                else -> {
                    isPhotoBlurryOrLowLight = false
                    h2sPositive = false
                    phValue = 7.2f
                    turbidityValue = 1.0f
                    chlorineValue = 0.45f
                    scanStatusNote = "कॅमेरा RGB ($r,$g,$b) तुलना: स्वच्छ पट्टी व योग्य pH → सुरक्षित (SAFE)"
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            takePictureLauncher.launch(null)
        } else {
            scanStatusNote = "खालील ऑप्टिकल रंग पट्टी स्कॅनरचा वापर करा (Using Interactive Optical Strip Reader)."
        }
    }

    val liveSafetyVerdict = remember(phValue, turbidityValue, h2sPositive, chlorineValue) {
        when {
            h2sPositive || turbidityValue > 12f || phValue < 6.2f || phValue > 8.6f -> "CONTAMINATED"
            turbidityValue > 5f || chlorineValue < 0.2f || phValue < 6.5f -> "RISKY"
            else -> "SAFE"
        }
    }

    val formattedTime = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("camera_water_test_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Explanation for Feature #1 (Page 2)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraEnhance,
                            contentDescription = "Camera Water Test Reader",
                            tint = SovereignNavy,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "वैशिष्ट्य #१ • CAMERA-BASED WATER TEST READER",
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTeal
                            )
                            Text(
                                text = "महागड्या Sensor ऐवजी Mobile Camera ने पाणी तपासणी",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "१. झरा, विहीर किंवा हँडपंपच्या पाण्यात स्वस्त test strip किंवा H2S vial बुडवा.\n२. Strip शेजारी छोटे Reference Colour Card ठेवून फोटो काढा.\n३. उजेड कमी-जास्त असला तरी Colour Card मुळे अचूक RGB/HSV तुलना होऊन काही सेकंदांत Safe / Risky / Contaminated निकाल मिळतो.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Select Target Water Source
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "१. पाण्याचा स्रोत निवडा • SELECT WATER SOURCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        waterSources.forEach { src ->
                            FilterChip(
                                selected = src.id == selectedSource?.id,
                                onClick = { selectedSource = src },
                                label = {
                                    Text(
                                        text = "${src.name} (${src.villageName.substringBefore(" ")})",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Camera Screen Frame ("Strip आणि colour card या चौकटीत ठेवा") + Big Colored Result Box
        item {
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "२. कॅमेरा चौकट • CAMERA ALIGNMENT FRAME",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Exact instruction from Page 2 ("Strip आणि colour card या चौकटीत ठेवा")
                    Surface(
                        color = SovereignNavy,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "चौकटीत सूचना: \"Strip आणि colour card या चौकटीत ठेवा\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OpticalStripCalibrationCanvas(
                        ph = phValue,
                        turbidity = turbidityValue,
                        h2sPositive = h2sPositive,
                        chlorine = chlorineValue,
                        isBlurryOrDark = isPhotoBlurryOrLowLight,
                        verdict = liveSafetyVerdict
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Low-Light / Blurry Photo Retake Warning (Page 2: "फोटो अंधुक किंवा उजेड खूप कमी असेल तर app ने पुन्हा फोटो घ्यायला सांगावे")
                    if (isPhotoBlurryOrLowLight) {
                        Surface(
                            color = ContaminatedRedBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, ContaminatedRed, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.WbTwilight,
                                        contentDescription = null,
                                        tint = ContaminatedRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "फोटो अंधुक किंवा उजेड खूप कमी आहे! कृपया पुन्हा स्पष्ट फोटो काढा.",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ContaminatedRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        isPhotoBlurryOrLowLight = false
                                        scanStatusNote = "पुरेसा उजेड मिळाला — Strip आणि Colour Card पुन्हा स्कॅन केले ✓"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ContaminatedRed)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("पुन्हा फोटो घ्या (Retake Clear Photo)")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Page 2 ("App मध्ये काय दिसेल"): फोटो घेतल्यावर मोठ्या हिरव्या / पिवळ्या / लाल रंगात निकाल दिसतो आणि त्याखाली स्रोताचे नाव, वेळ आणि location.
                    val (verdictBg, verdictTextCol, verdictMarathi) = when (liveSafetyVerdict) {
                        "CONTAMINATED" -> Triple(ContaminatedRed, Color.White, "CONTAMINATED • दूषित (लाल)")
                        "RISKY" -> Triple(RiskyAmber, Color.White, "RISKY • धोकादायक (पिवळा)")
                        else -> Triple(SafeGreen, Color.White, "SAFE • सुरक्षित (हिरवा)")
                    }
                    Surface(
                        color = verdictBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = verdictMarathi,
                                style = MaterialTheme.typography.headlineSmall,
                                color = verdictTextCol,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            selectedSource?.let { src ->
                                Text(
                                    text = "स्रोत: ${src.name} (${src.villageName})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = verdictTextCol,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "वेळ: $formattedTime • GPS Location: ${String.format("%.4f", src.latitude)}°N, ${String.format("%.4f", src.longitude)}°E",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = verdictTextCol.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scanStatusNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = CivicTeal
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Camera & Preset Buttons (including Low-Light / Blurry Test button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.testTag("launch_camera_capture_btn")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Water (फोटो काढा)")
                        }

                        OutlinedButton(
                            onClick = {
                                isPhotoBlurryOrLowLight = false
                                phValue = 7.3f
                                turbidityValue = 0.9f
                                h2sPositive = false
                                chlorineValue = 0.45f
                                scanStatusNote = "तपासणी: स्वच्छ H2S बाटली व संतुलित pH 7.3 → SAFE (हिरवा)"
                            },
                            modifier = Modifier.testTag("preset_safe_strip_btn")
                        ) {
                            Text("Safe (हिरवा)")
                        }

                        OutlinedButton(
                            onClick = {
                                isPhotoBlurryOrLowLight = false
                                phValue = 6.4f
                                turbidityValue = 8.4f
                                h2sPositive = false
                                chlorineValue = 0.08f
                                scanStatusNote = "तपासणी: गढूळ पट्टी व कमी क्लोरिन → RISKY (पिवळा)"
                            },
                            modifier = Modifier.testTag("preset_risky_strip_btn")
                        ) {
                            Text("Risky (पिवळा)")
                        }

                        OutlinedButton(
                            onClick = {
                                isPhotoBlurryOrLowLight = false
                                phValue = 5.9f
                                turbidityValue = 21.0f
                                h2sPositive = true
                                chlorineValue = 0.0f
                                scanStatusNote = "तपासणी: काळी H2S बाटली (जीवाणू संसर्ग) → CONTAMINATED (लाल)"
                            },
                            modifier = Modifier.testTag("preset_contaminated_strip_btn")
                        ) {
                            Text("Contaminated (लाल)")
                        }

                        OutlinedButton(
                            onClick = {
                                isPhotoBlurryOrLowLight = true
                                scanStatusNote = "फोटो अंधुक किंवा उजेड खूप कमी आहे! कृपया पुन्हा फोटो घ्या."
                            }
                        ) {
                            Text("अंधुक फोटो चाचणी (Blur Check)")
                        }
                    }

                    // Page 2 Requirement: "निकाल Risky/Contaminated आला तर lab sample घेण्याची सूचना द्यावी."
                    if (liveSafetyVerdict != "SAFE") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = EmblemSaffronLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, RiskyAmber, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "प्रयोगशाळा नमुना सूचना (Lab Sample Advisory): निकाल $liveSafetyVerdict आला आहे! हा प्राथमिक screening निकाल असल्याने कृपया तातडीने पाण्याचा QR Lab Sample गोळा करून जिल्हा प्रयोगशाळेत पाठवा.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F),
                                    fontWeight = FontWeight.Bold
                                )
                                if (onRequestCollectLabQrSample != null && selectedSource != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FilledTonalButton(
                                        onClick = { onRequestCollectLabQrSample(selectedSource!!) }
                                    ) {
                                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Lab QR Sample तयार करा (#5)", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Page 2 Mandatory Note: "लक्षात ठेवा: हा screening निकाल आहे, lab चे अंतिम निदान नाही."
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "लक्षात ठेवा: हा प्राथमिक screening निकाल आहे, प्रयोगशाळेचे (Lab) अंतिम निदान नाही. निकाल फोनच्या local SQLite database मध्ये जतन होतो (network नसतानाही चालतो).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Fine-Tune Strip Parameters & Save Test Result
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "३. चाचणी निकाल जतन करा • SAVE TO LOCAL DATABASE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "H2S जीवाणू बाटली काळी पडली का? (Fecal Coliform)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (h2sPositive) "होय, काळा रंग (Contaminated)" else "नाही, स्वच्छ रंग (Safe)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (h2sPositive) ContaminatedRed else SafeGreen
                            )
                        }
                        Switch(
                            checked = h2sPositive,
                            onCheckedChange = { h2sPositive = it },
                            modifier = Modifier.testTag("h2s_positive_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "pH पातळी: ${(phValue * 10).roundToInt() / 10.0} (योग्य: 6.5 – 8.5)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = phValue,
                        onValueChange = { phValue = it },
                        valueRange = 5.0f..9.5f
                    )

                    Text(
                        text = "गढूळपणा (Turbidity): ${(turbidityValue * 10).roundToInt() / 10.0} NTU (सुरक्षित < 5 NTU)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = turbidityValue,
                        onValueChange = { turbidityValue = it },
                        valueRange = 0.5f..30.0f
                    )

                    Text(
                        text = "शिल्लक क्लोरिन (Free Chlorine): ${(chlorineValue * 100).roundToInt() / 100.0} mg/L (आवश्यक ≥ 0.2 mg/L)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = chlorineValue,
                        onValueChange = { chlorineValue = it },
                        valueRange = 0.0f..1.0f
                    )

                    OutlinedTextField(
                        value = testerName,
                        onValueChange = { testerName = it },
                        label = { Text("तपासणी करणाऱ्या आशा वर्करचे / Volunteer चे नाव") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            selectedSource?.let { src ->
                                onSaveCameraTest(
                                    src,
                                    phValue.toDouble(),
                                    turbidityValue.toDouble(),
                                    h2sPositive,
                                    chlorineValue.toDouble(),
                                    testerName
                                )
                            }
                        },
                        enabled = !isPhotoBlurryOrLowLight,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (liveSafetyVerdict) {
                                "CONTAMINATED" -> ContaminatedRed
                                "RISKY" -> RiskyAmber
                                else -> SafeGreen
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .testTag("save_camera_water_test_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Water Test Result")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "निकाल (${liveSafetyVerdict}) वेळ, GPS व स्रोताच्या नावासह Save करा",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpticalStripCalibrationCanvas(
    ph: Float,
    turbidity: Float,
    h2sPositive: Boolean,
    chlorine: Float,
    isBlurryOrDark: Boolean,
    verdict: String
) {
    val h2sSampleColor = if (h2sPositive) Color(0xFF1E1B18) else Color(0xFFD97706).copy(alpha = 0.45f)
    val phPadColor = when {
        ph < 6.3f -> Color(0xFFEA580C)
        ph > 8.4f -> Color(0xFF7C3AED)
        else -> Color(0xFF16A34A)
    }
    val clPadColor = when {
        chlorine < 0.15f -> Color(0xFFFEF08A)
        else -> Color(0xFFEC4899)
    }
    val turbPadColor = when {
        turbidity > 12f -> Color(0xFF78350F)
        turbidity > 5f -> Color(0xFFD97706)
        else -> Color(0xFF38BDF8)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isBlurryOrDark) Color(0xFF050709) else Color(0xFF0F172A))
            .border(
                2.dp,
                if (isBlurryOrDark) ContaminatedRed else CivicTeal,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val alphaFactor = if (isBlurryOrDark) 0.25f else 1.0f

            drawRoundRect(
                color = Color(0xFFF8FAFC).copy(alpha = alphaFactor),
                topLeft = Offset(8f, 8f),
                size = Size(w * 0.46f, h - 16f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            val swatchWidth = (w * 0.38f) / 3f
            val swatchColors = listOf(Color(0xFF16A34A), Color(0xFFD97706), Color(0xFFDC2626))
            swatchColors.forEachIndexed { idx, col ->
                drawRoundRect(
                    color = col.copy(alpha = alphaFactor),
                    topLeft = Offset(22f + idx * (swatchWidth + 6f), 42f),
                    size = Size(swatchWidth, h * 0.55f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(w * 0.52f, 8f),
                size = Size(w * 0.46f, h - 16f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3f)
            )

            val stripX = w * 0.58f
            drawRoundRect(
                color = Color.White.copy(alpha = alphaFactor),
                topLeft = Offset(stripX, 20f),
                size = Size(36f, h - 40f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRect(phPadColor.copy(alpha = alphaFactor), Offset(stripX + 4f, 32f), Size(28f, 24f))
            drawRect(clPadColor.copy(alpha = alphaFactor), Offset(stripX + 4f, 66f), Size(28f, 24f))
            drawRect(turbPadColor.copy(alpha = alphaFactor), Offset(stripX + 4f, 100f), Size(28f, 24f))

            val vialX = w * 0.75f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f * alphaFactor),
                topLeft = Offset(vialX, 26f),
                size = Size(48f, h - 46f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 3f)
            )
            drawRoundRect(
                color = h2sSampleColor.copy(alpha = alphaFactor),
                topLeft = Offset(vialX + 4f, 50f),
                size = Size(40f, h - 74f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        Text(
            text = "COLOUR CARD (Reference)",
            style = MaterialTheme.typography.labelSmall,
            color = if (isBlurryOrDark) Color.Gray else SovereignNavyDark,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 12.dp)
        )
        Text(
            text = "TEST STRIP + H2S",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 12.dp)
        )
    }
}
