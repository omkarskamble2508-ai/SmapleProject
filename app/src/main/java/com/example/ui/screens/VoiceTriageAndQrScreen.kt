package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.theme.*
import java.util.Locale

enum class VoiceAndQrScreenMode {
    CITIZEN_VOICE_ONLY,
    DOCTOR_LAB_QR_ONLY,
    ASHA_FULL_FIELD_SYNC
}

data class NeLanguagePreset(
    val name: String,
    val localeCode: String,
    val samplePhrase: String,
    val englishTranslation: String,
    val voiceQuestionPrompt: String
)

private val neLanguagePresets = listOf(
    NeLanguagePreset(
        name = "Marathi (मराठी)",
        localeCode = "mr-IN",
        samplePhrase = "माझ्या बाळाला दोन दिवसांपासून पातळ जुलाब होत आहेत आणि तो खूप अशक्त झाला आहे.",
        englishTranslation = "My child has loose motion for 2 days and is very weak.",
        voiceQuestionPrompt = "किती दिवसांपासून आहे? पाणी पीत आहे का? डोळे खोल गेले आहेत का?"
    ),
    NeLanguagePreset(
        name = "English (इंग्रजी)",
        localeCode = "en-IN",
        samplePhrase = "My child has loose motion for 2 days after drinking spring water and is very weak.",
        englishTranslation = "My child has loose motion for 2 days and is very weak.",
        voiceQuestionPrompt = "How many days? Is the child drinking water? Any sunken eyes?"
    ),
    NeLanguagePreset(
        name = "Assamese (অসমীয়া)",
        localeCode = "as-IN",
        samplePhrase = "মোৰ ল’ৰাটোৰ দুদিন ধৰি পাতল শৌচ হৈ আছে আৰু বৰ দুৰ্বল হৈ পৰিছে।",
        englishTranslation = "My child has loose motion for 2 days and is very weak.",
        voiceQuestionPrompt = "কিমান দিন ধৰি হৈছে? পানী খাব পাৰিছেনে? চকু ভিতৰলৈ সোমাই গৈছে নেকি?"
    ),
    NeLanguagePreset(
        name = "Khasi (Meghalaya)",
        localeCode = "en-IN",
        samplePhrase = "Khynnah u prie kpoh la ar sngi bad u tlot bha.",
        englishTranslation = "My child has loose motion for two days and is very weak.",
        voiceQuestionPrompt = "Katno sngi? U lah ban dih um? Ki khmat ki dkhir?"
    ),
    NeLanguagePreset(
        name = "Manipuri (মৈতৈলোন্)",
        localeCode = "mni-IN",
        samplePhrase = "ইচাগী নুমিৎ নিনি খোং হাম্বা লেপ্তে অমসুং পাঙ্গল শোনথরে।",
        englishTranslation = "My child has continuous diarrhea for 2 days and has become weak.",
        voiceQuestionPrompt = "নুমিৎ কয়ানি শুরে? ঈশিং থকপা ঙমব্রা?"
    ),
    NeLanguagePreset(
        name = "Hindi (हिन्दी)",
        localeCode = "hi-IN",
        samplePhrase = "मेरे बच्चे को दो दिन से दस्त हो रहे हैं और वह बहुत कमजोर है।",
        englishTranslation = "My child has loose motion for 2 days and is very weak.",
        voiceQuestionPrompt = "कितने दिनों से है? क्या बच्चा पानी पी रहा है? क्या आँखें धँसी हुई हैं?"
    )
)

@Composable
fun VoiceTriageAndQrScreen(
    mode: VoiceAndQrScreenMode = VoiceAndQrScreenMode.ASHA_FULL_FIELD_SYNC,
    villages: List<VillageEntity>,
    samples: List<SampleCustodyEntity>,
    symptomReports: List<SymptomReportEntity>,
    isOfflineHillMode: Boolean,
    pendingOfflineCount: Int = 0,
    lastP2pSyncMessage: String? = null,
    onToggleOfflineMode: () -> Unit,
    onSubmitVoiceTriage: (
        village: VillageEntity,
        patientDesc: String,
        ageYears: Int,
        languageName: String,
        voiceTranscript: String,
        durationDays: Int,
        isDrinkingWater: Boolean,
        hasSunkenEyes: Boolean,
        hasBloodOrFever: Boolean
    ) -> Unit,
    onCollectQrSample: (VillageEntity, String, String, String) -> Unit,
    onScanAdvanceSample: (SampleCustodyEntity, String) -> Unit,
    onSimulateTamperCheck: (SampleCustodyEntity) -> Unit,
    onTriggerP2pMeshSync: () -> Unit,
    onTriggerCloudGatewaySync: () -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember(mode) {
        mutableIntStateOf(
            when (mode) {
                VoiceAndQrScreenMode.CITIZEN_VOICE_ONLY -> 0
                VoiceAndQrScreenMode.DOCTOR_LAB_QR_ONLY -> 1
                VoiceAndQrScreenMode.ASHA_FULL_FIELD_SYNC -> 0
            }
        )
    }

    var selectedVillage by remember(villages) {
        mutableStateOf(villages.firstOrNull())
    }
    var selectedLang by remember { mutableStateOf(neLanguagePresets.first()) }
    var voiceTranscript by remember {
        mutableStateOf("${neLanguagePresets.first().samplePhrase} (${neLanguagePresets.first().englishTranslation})")
    }
    var patientDesc by remember { mutableStateOf("बालक (वय ४ वर्षे) • Child (Age 4)") }
    var durationDays by remember { mutableIntStateOf(2) }
    var isDrinkingWater by remember { mutableStateOf(true) }
    var hasSunkenEyes by remember { mutableStateOf(false) }
    var hasBloodOrFever by remember { mutableStateOf(false) }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.forLanguageTag("mr-IN")
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                voiceTranscript = spoken
            }
        }
    }

    val liveTriageSevere = !isDrinkingWater || hasSunkenEyes || hasBloodOrFever || durationDays >= 3
    val liveTriageVerdict = if (liveTriageSevere) {
        "ताबडतोब आरोग्य केंद्रात जा! (Go to the health centre immediately!)"
    } else {
        "आता ORS द्या! (Give ORS now after every loose stool + Zinc tablets.)"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_triage_and_qr_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (mode == VoiceAndQrScreenMode.ASHA_FULL_FIELD_SYNC) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activeSubTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSubTab == 0) SovereignNavy else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeSubTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtab_voice_triage")
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("आवाज तपासणी (#7)", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = { activeSubTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSubTab == 1) SovereignNavy else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subtab_qr_custody")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR साखळी (#5) व P2P (#6)", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (activeSubTab == 0) {
            // ================= FEATURE #7: VOICE SYMPTOM TRIAGE (MARATHI + ENGLISH + NE LANGUAGES) =================
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "वैशिष्ट्य #७ • आवाजाद्वारे लक्षणे तपासणी (VOICE SYMPTOM TRIAGE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicTeal
                        )
                        Text(
                            text = "मोठे माईक बटण, आवाजात प्रश्न आणि मोठी 'हो / नाही' बटणे",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ज्यांना लांब फॉर्म वाचणे किंवा टाईप करणे कठीण जाते त्यांच्यासाठी: माईक दाबा आणि स्थानिक भाषेत बोला (उदा. 'माझ्या बाळाला दोन दिवसांपासून पातळ जुलाब आहेत आणि तो अशक्त आहे'). अ‍ॅप Decision Tree नियमांनुसार सोपे प्रश्न विचारून सल्ला देते.",
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
                            neLanguagePresets.forEach { preset ->
                                FilterChip(
                                    selected = preset.name == selectedLang.name,
                                    onClick = {
                                        selectedLang = preset
                                        voiceTranscript = "${preset.samplePhrase} (${preset.englishTranslation})"
                                    },
                                    label = { Text(preset.name, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large prominent Mic Button + Voice Readout button (Page 8)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLang.localeCode)
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            "लक्षणे बोला / Speak symptoms in ${selectedLang.name}"
                                        )
                                    }
                                    try {
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        voiceTranscript =
                                            "${selectedLang.samplePhrase} (${selectedLang.englishTranslation})"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CivicTeal),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .defaultMinSize(minHeight = 54.dp)
                                    .testTag("voice_mic_input_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Tap Mic to Speak", modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("मोठे माईक बटण (बोला)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    ttsEngine?.speak(
                                        "${selectedLang.voiceQuestionPrompt} $liveTriageVerdict",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "TRIAGE_PROMPT"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 54.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Speak Triage Questions")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("आवाजात प्रश्न ऐका", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = voiceTranscript,
                            onValueChange = { voiceTranscript = it },
                            label = { Text("आवाज नोंद • Spoken Symptom Transcript (${selectedLang.name})") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Decision Tree with Big "हो / नाही" (Yes / No) Buttons as required by Page 8
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "सोपा Decision Tree • सोपी मोठी 'हो / नाही' बटणे:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SovereignNavy,
                                    fontWeight = FontWeight.Bold
                                )

                                // Question 1: How many days?
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "१. किती दिवसांपासून आहे? (How many days of loose motion?)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(1 to "१ दिवस", 2 to "२ दिवस", 3 to "३ दिवस", 5 to "५+ दिवस").forEach { (d, label) ->
                                            Button(
                                                onClick = { durationDays = d },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (durationDays == d) SovereignNavy else Color.White,
                                                    contentColor = if (durationDays == d) Color.White else SovereignNavy
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 44.dp)
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }

                                // Question 2: Is the child drinking water? (Big हो / नाही buttons)
                                BigYesNoQuestionRow(
                                    question = "२. पाणी पीत आहे का? (Is the patient able to drink water?)",
                                    selectedYes = isDrinkingWater,
                                    onSelectYes = { isDrinkingWater = it },
                                    yesIsSafe = true
                                )

                                // Question 3: Are eyes sunken? (Big हो / नाही buttons)
                                BigYesNoQuestionRow(
                                    question = "३. डोळे खोल गेले आहेत का? (Are the eyes sunken / severe dehydration?)",
                                    selectedYes = hasSunkenEyes,
                                    onSelectYes = { hasSunkenEyes = it },
                                    yesIsSafe = false,
                                    testTagPrefix = "sunken_eyes"
                                )

                                // Question 4: Blood in stool or high fever? (Big हो / नाही buttons)
                                BigYesNoQuestionRow(
                                    question = "४. शौचावाटे रक्त किंवा तीव्र ताप आहे का? (Blood in stool or high fever?)",
                                    selectedYes = hasBloodOrFever,
                                    onSelectYes = { hasBloodOrFever = it },
                                    yesIsSafe = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Colored Advice Card + Mandatory Non-Diagnosis Warning (Page 8)
                        Surface(
                            color = if (liveTriageSevere) ContaminatedRedBg else SafeGreenBg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    2.dp,
                                    if (liveTriageSevere) ContaminatedRed else SafeGreen,
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (liveTriageSevere) Icons.Default.LocalHospital else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (liveTriageSevere) ContaminatedRed else SafeGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "रंगीत सल्ला कार्ड • TRIAGE GUIDANCE CARD:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (liveTriageSevere) ContaminatedRed else SafeGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = liveTriageVerdict,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (liveTriageSevere) ContaminatedRed else SafeGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "लक्षात ठेवा (Mandatory Disclaimer): गंभीर वाटल्यास डॉक्टर किंवा आरोग्य कार्यकर्त्याला भेटा — हे फक्त प्राथमिक मार्गदर्शन आहे, वैद्यकीय निदान (diagnosis) नाही.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                selectedVillage?.let { v ->
                                    onSubmitVoiceTriage(
                                        v,
                                        patientDesc,
                                        4,
                                        selectedLang.name,
                                        voiceTranscript,
                                        durationDays,
                                        isDrinkingWater,
                                        hasSunkenEyes,
                                        hasBloodOrFever
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("submit_voice_triage_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("लक्षणे अहवाल जतन करा • Save Voice Triage Report")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "नुकतेच नोंदवलेले आवाज अहवाल • RECENT VOICE REPORTS (${symptomReports.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = SovereignNavy,
                    fontWeight = FontWeight.Bold
                )
            }

            items(symptomReports, key = { it.id }) { report ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${report.patientDescription} • ${report.villageName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = report.languageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = CivicTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${report.voiceTranscript}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "सल्ला: ${report.triageGuidance} • Sync: ${report.syncStatus}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (report.severity == "SEVERE_URGENT") ContaminatedRed else SafeGreen
                        )
                    }
                }
            }
        } else {
            // ================= FEATURE #5: SAMPLE CHAIN-OF-CUSTODY (QR + HASH) & FEATURE #6: P2P OFFLINE SYNC =================
            if (mode == VoiceAndQrScreenMode.ASHA_FULL_FIELD_SYNC) {
                item {
                    PeerToPeerOfflineSyncCard(
                        isOfflineHillMode = isOfflineHillMode,
                        pendingOfflineCount = pendingOfflineCount,
                        lastP2pSyncMessage = lastP2pSyncMessage,
                        onToggleOfflineMode = onToggleOfflineMode,
                        onTriggerP2pMeshSync = onTriggerP2pMeshSync,
                        onTriggerCloudGatewaySync = onTriggerCloudGatewaySync
                    )
                }
            }

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
                                    text = "वैशिष्ट्य #५ • नमुना साखळी सुरक्षा (SAMPLE CHAIN-OF-CUSTODY QR + HASH)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CivicTeal
                                )
                                Text(
                                    text = "प्रवासाची रेषा: गाव → block → district lab (SHA-256 Hash Chain)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (mode == VoiceAndQrScreenMode.ASHA_FULL_FIELD_SYNC) {
                                Button(
                                    onClick = {
                                        selectedVillage?.let { v ->
                                            onCollectQrSample(
                                                v,
                                                "पाणी व शौच नमुना • Water + Stool Sample",
                                                "Spring Cluster Point #1",
                                                "ASHA Jonali Borah"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SovereignNavy),
                                    modifier = Modifier.testTag("create_new_qr_sample_btn")
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("नवीन QR नमुना", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "प्रत्येक नमुन्यावर QR कोड लावला जातो. प्रत्येक हस्तांतरण (गाव → block → district lab) वेळ आणि स्कॅन करणाऱ्याच्या नावासह नोंदवले जाते. मागील हॅश + नवीन माहिती मिळून नवीन SHA-256 Hash बनतो (PrevHash + Event → NewHash).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = EmblemSaffronLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "लक्षात ठेवा: हा प्रकार फसवणूक रोखतो असे नाही, तर फसवणूक पकडतो (Tamper Detection). फक्त अधिकृत व्यक्तींनाच माहिती बदलण्याची परवानगी (Role-based permission) आहे.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            items(samples, key = { it.sampleCode }) { sample ->
                SampleCustodyQrCard(
                    sample = sample,
                    onScanHandover = {
                        val nextHandler = if (sample.currentStage == HandoverStage.VILLAGE_COLLECTED.name) {
                            "तालुका वाहक • Block Cold-Chain Courier"
                        } else {
                            "जिल्हा प्रयोगशाळा डॉक्टर • District Lab Microbiologist"
                        }
                        onScanAdvanceSample(sample, nextHandler)
                    },
                    onToggleTamperSimulation = { onSimulateTamperCheck(sample) }
                )
            }
        }
    }
}

@Composable
private fun BigYesNoQuestionRow(
    question: String,
    selectedYes: Boolean,
    onSelectYes: (Boolean) -> Unit,
    yesIsSafe: Boolean,
    testTagPrefix: String = "yes_no"
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val yesColor = if (yesIsSafe) SafeGreen else ContaminatedRed
            val noColor = if (yesIsSafe) ContaminatedRed else SafeGreen

            Button(
                onClick = { onSelectYes(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedYes) yesColor else Color.White,
                    contentColor = if (selectedYes) Color.White else Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 46.dp)
                    .testTag("${testTagPrefix}_yes_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("हो (YES)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onSelectYes(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!selectedYes) noColor else Color.White,
                    contentColor = if (!selectedYes) Color.White else Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 46.dp)
                    .testTag("${testTagPrefix}_no_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("नाही (NO)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PeerToPeerOfflineSyncCard(
    isOfflineHillMode: Boolean,
    pendingOfflineCount: Int,
    lastP2pSyncMessage: String?,
    onToggleOfflineMode: () -> Unit,
    onTriggerP2pMeshSync: () -> Unit,
    onTriggerCloudGatewaySync: () -> Unit
) {
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
                        text = "वैशिष्ट्य #६ • ऑफलाईन पीअर-टू-पीअर सिंक (PEER-TO-PEER OFFLINE SYNC)",
                        style = MaterialTheme.typography.labelSmall,
                        color = RiskyAmber
                    )
                    Text(
                        text = "इंटरनेटशिवाय फोन-टू-फोन माहितीची देवाणघेवाण",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = isOfflineHillMode,
                    onCheckedChange = { onToggleOfflineMode() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "डोंगराळ भागात इंटरनेट नसताना सर्व नोंदी फोनवर स्थानिक डेटाबेसमध्ये (Room SQLite) साठवल्या जातात. दोन आशा कार्यकर्ते भेटल्यावर ब्लूटूथ / Wi-Fi Direct (Nearby Connections) द्वारे नोंदी एकमेकांना देतात. प्रत्येक नोंदीला स्वतंत्र UUID असल्याने ती दुप्पट (Duplicate) होत नाही.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Page 7 UI requirements: "Upload बाकी असलेल्या नोंदींची संख्या" + "12 नवीन नोंदी मिळाल्या"
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upload बाकी असलेल्या नोंदींची संख्या (Pending Upload):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = if (pendingOfflineCount > 0) RiskyAmberBg else SafeGreenBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$pendingOfflineCount नोंदी बाकी",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pendingOfflineCount > 0) Color(0xFFB45309) else SafeGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (lastP2pSyncMessage != null) {
                        Text(
                            text = "✓ $lastP2pSyncMessage",
                            style = MaterialTheme.typography.labelMedium,
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "स्थिती: अद्याप कोणताही जवळचा फोन सिंक केलेला नाही. खालील बटण दाबा.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onTriggerP2pMeshSync,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("p2p_bluetooth_share_btn")
                ) {
                    Icon(Icons.Default.BluetoothConnected, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("जवळच्या फोनशी sync करा", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onTriggerCloudGatewaySync,
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cloud_gateway_sync_btn")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("सर्व्हरवर अपलोड", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "तांत्रिक टीप: ऑफलाईन जतन (Room DB) आणि नंतर क्लाउड सिंक पूर्णपणे कार्यरत आहे; प्रत्यक्ष फोन-टू-फोन Google Nearby Connections P2P सिम्युलेशन आणि भविष्यकालीन विस्तार (Future Scope) म्हणून समाविष्ट आहे.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SampleCustodyQrCard(
    sample: SampleCustodyEntity,
    onScanHandover: () -> Unit,
    onToggleTamperSimulation: () -> Unit
) {
    val recomputedHash = remember(sample.currentPayloadString) {
        CryptoAndGeoUtils.sha256(sample.currentPayloadString)
    }
    val hashMatches = recomputedHash == sample.expectedSha256Hash && !sample.isTampered

    val historySteps = remember(sample.handoverHistory) {
        sample.handoverHistory.split("|").map { it.trim() }
    }
    val currentStageIndex = when (sample.currentStage) {
        HandoverStage.VILLAGE_COLLECTED.name -> 0
        HandoverStage.BLOCK_TRANSIT.name -> 1
        HandoverStage.DISTRICT_LAB_VERIFIED.name -> 2
        else -> 0
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hashMatches) MaterialTheme.colorScheme.surface else ContaminatedRedBg.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (hashMatches) SovereignNavy.copy(alpha = 0.3f) else ContaminatedRed,
                RoundedCornerShape(16.dp)
            )
            .testTag("sample_custody_card_${sample.sampleCode}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                QrMatrixBadgeCanvas(
                    seedString = sample.sampleCode + sample.expectedSha256Hash,
                    isTampered = !hashMatches
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sample.sampleCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (hashMatches) SafeGreenBg else ContaminatedRed,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (hashMatches) "हॅश सुरक्षित ✓" else "लाल चिन्ह: फेरफार आढळला ✗",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hashMatches) SafeGreen else Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "${sample.sampleType} • ${sample.villageName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "स्रोत: ${sample.sourceOrPatientName} • GPS: ${sample.gpsCoordinates}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SHA-256 Chain Hash: ${sample.expectedSha256Hash}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    if (!hashMatches) {
                        Text(
                            text = "बदललेला हॅश (Altered Hash): $recomputedHash (TAMPER ALERT!)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ContaminatedRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page 6 Requirement: Visual Journey Line (गाव → block → district lab) with red tamper marker
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "नमुन्याच्या प्रवासाची रेषा (Sample Journey: गाव → block → district lab):",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        JourneyStageNode(
                            label = "१. गाव (Village)",
                            subText = historySteps.getOrNull(0)?.substringAfter(":")?.trim() ?: sample.collectedBy,
                            isCompleted = currentStageIndex >= 0,
                            isTamperedHere = !hashMatches && currentStageIndex == 0,
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        JourneyStageNode(
                            label = "२. तालुका (Block)",
                            subText = historySteps.getOrNull(1)?.substringAfter(":")?.trim() ?: "प्रतीक्षेत",
                            isCompleted = currentStageIndex >= 1,
                            isTamperedHere = !hashMatches && currentStageIndex == 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        JourneyStageNode(
                            label = "३. District Lab",
                            subText = historySteps.getOrNull(2)?.substringAfter(":")?.trim() ?: "प्रतीक्षेत",
                            isCompleted = currentStageIndex >= 2,
                            isTamperedHere = !hashMatches && currentStageIndex >= 2,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider()

                    historySteps.forEach { step ->
                        Text(
                            text = "• $step",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "सद्यस्थिती: ${sample.labFindings}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hashMatches) CivicTeal else ContaminatedRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScanHandover,
                    enabled = sample.currentStage != HandoverStage.DISTRICT_LAB_VERIFIED.name,
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignNavy),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_handover_btn_${sample.sampleCode}")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sample.currentStage == HandoverStage.DISTRICT_LAB_VERIFIED.name) {
                            "प्रयोगशाळा तपासणी पूर्ण"
                        } else {
                            "पुढील टप्पा QR स्कॅन"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedButton(
                    onClick = onToggleTamperSimulation,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_tamper_btn_${sample.sampleCode}")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hashMatches) "फेरफार चाचणी (Tamper)" else "मूळ हॅश पूर्ववत करा",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun JourneyStageNode(
    label: String,
    subText: String,
    isCompleted: Boolean,
    isTamperedHere: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isTamperedHere -> ContaminatedRedBg
        isCompleted -> SafeGreenBg
        else -> Color.White
    }
    val borderColor = when {
        isTamperedHere -> ContaminatedRed
        isCompleted -> SafeGreen
        else -> Color(0xFF94A3B8)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .padding(horizontal = 2.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(borderColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isTamperedHere) ContaminatedRed else Color(0xFF0F172A)
                )
            }
            Text(
                text = if (isTamperedHere) "⚠️ फेरफार (लाल चिन्ह)" else subText.take(18),
                style = MaterialTheme.typography.labelSmall,
                color = if (isTamperedHere) ContaminatedRed else Color(0xFF475569),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QrMatrixBadgeCanvas(seedString: String, isTampered: Boolean) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(2.dp, if (isTampered) ContaminatedRed else SovereignNavy, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = 9
            val cellW = size.width / grid
            val cellH = size.height / grid
            val ink = if (isTampered) Color(0xFFB91C1C) else Color(0xFF0F172A)

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    val isFinderCorner =
                        (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                    val hashBit = ((seedString.hashCode() shr ((r * grid + c) % 28)) and 1) == 1
                    if (isFinderCorner || hashBit) {
                        drawRect(
                            color = ink,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW * 0.88f, cellH * 0.88f)
                        )
                    }
                }
            }
        }
    }
}
