package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest
import kotlin.math.*

enum class ActorRole(
    val displayName: String,
    val marathiName: String,
    val badgeTitle: String,
    val marathiSubtitle: String,
    val subtitle: String,
    val ministryCode: String
) {
    VILLAGE_RESIDENT(
        displayName = "User / Citizen",
        marathiName = "नागरिक / प्रवासी (User)",
        badgeTitle = "नागरिक व प्रवासी सुरक्षित पाणी पोर्टल • Citizen & Visitor Portal",
        marathiSubtitle = "अनोळखी गावातील पाण्याची गुणवत्ता (GPS), सुरक्षित पाणी नकाशा आणि मराठी आवाज तपासणी",
        subtitle = "Safe Water Map prioritized, GPS Unfamiliar Village Water Radar & Marathi/English Voice Triage",
        ministryCode = "JJM-CITIZEN-MR"
    ),
    ASHA_WORKER(
        displayName = "ASHA Worker",
        marathiName = "आशा स्वयंसेविका (ASHA)",
        badgeTitle = "आशा क्षेत्र सर्वेक्षण विभाग • ASHA Field Surveillance Unit",
        marathiSubtitle = "कॅमेरा पाणी तपासणी, कृती यादी, QR नमुना संकलन आणि ऑफलाईन ब्लूटूथ सिंक",
        subtitle = "Camera strip testing, action task checklist, QR sample collection & offline P2P sync",
        ministryCode = "NHM-ASHA-FIELD"
    ),
    LOCAL_DOCTOR(
        displayName = "Local Doctor",
        marathiName = "स्थानिक डॉक्टर (Doctor)",
        badgeTitle = "ग्रामीण वैद्यकीय अधिकारी • Rural Medical Officer Desk",
        marathiSubtitle = "रुग्ण ट्रायएज रांग, प्रयोगशाळा QR नमुना पडताळणी आणि पाणी स्रोत नकाशा",
        subtitle = "Clinical triage queue, lab sample QR custody verification & water source risk map",
        ministryCode = "DGHS-MO-CLINIC"
    ),
    PHC_OFFICIAL(
        displayName = "PHC Official",
        marathiName = "आरोग्य अधिकारी (PHC)",
        badgeTitle = "प्राथमिक आरोग्य केंद्र कमांड • PHC Early Warning Command",
        marathiSubtitle = "पूर पूर्व-सूचना, कृती आदेश, साथरोग सिम्युलेटर, क्लस्टर आणि जिल्हा नकाशा",
        subtitle = "Flood pre-symptom alerts, Alert-to-Action dispatcher, What-If simulator & cluster detection",
        ministryCode = "IDSP-PHC-CMD"
    )
}

enum class WaterSafetyLevel(val label: String, val marathiEnglishTag: String) {
    SAFE("SAFE", "सुरक्षित • SAFE (GREEN)"),
    RISKY("RISKY (AMBER)", "धोकादायक • RISKY (AMBER)"),
    CONTAMINATED("CONTAMINATED (RED)", "दूषित • CONTAMINATED (RED)")
}

enum class SyncStatus(val label: String) {
    OFFLINE_SAVED("ऑफलाईन जतन • Saved Offline on Phone"),
    P2P_SHARED("P2P शेअर • Shared via Bluetooth/Wi-Fi Direct"),
    CLOUD_SYNCED("सर्व्हरवर अपलोड • Uploaded to State NHM Server")
}

enum class HandoverStage(val stepIndex: Int, val label: String) {
    VILLAGE_COLLECTED(1, "1. गाव संकलन • Village Collection (ASHA)"),
    BLOCK_TRANSIT(2, "2. तालुका वाहतूक • Block Transit Courier"),
    DISTRICT_LAB_VERIFIED(3, "3. जिल्हा प्रयोगशाळा • District Public Health Lab")
}

// Secure Actor Registration / Login Account Entity
@Entity(tableName = "actor_accounts")
data class ActorAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actorRole: String, // VILLAGE_RESIDENT, ASHA_WORKER, LOCAL_DOCTOR, PHC_OFFICIAL
    val fullName: String,
    val mobileNumber: String,
    val aadhaarNumberMasked: String?, // Required for VILLAGE_RESIDENT (User), null for others
    val aadhaarSha256Hash: String?,
    val photoScanSignature: String, // Camera/ID Photo verification hash & timestamp
    val photoCapturedWidth: Int,
    val photoCapturedHeight: Int,
    val securityPinHash: String,
    val registeredAt: Long,
    val lastLoginAt: Long
)

@Entity(tableName = "villages")
data class VillageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val district: String,
    val stateName: String,
    val riverBasin: String,
    val latitude: Double,
    val longitude: Double,
    val population: Int,
    val dailyRainfallMm: Double,
    val floodWarningActive: Boolean,
    val overallRisk: String, // SAFE, RISKY, CONTAMINATED
    val orsPacketsStock: Int,
    val chlorineTabletsStock: Int,
    // Feature #8: Early Signals from Non-medical Sources
    val shopOrsSalesSpikePercent: Int,
    val schoolAbsencePercent: Int,
    val ashaOrsDepletionPercent: Int,
    val normalBaselineCases3Days: Int,
    val visitorAdvisoryNote: String
)

@Entity(tableName = "water_sources")
data class WaterSourceEntity(
    @PrimaryKey val id: String,
    val villageId: String,
    val villageName: String,
    val districtState: String,
    val name: String,
    val sourceType: String, // Spring, Ring Well, Hand Pump, Gravity Tap
    val latitude: Double,
    val longitude: Double,
    val safetyLevel: String, // SAFE, RISKY, CONTAMINATED
    val phLevel: Double,
    val turbidityNtu: Double,
    val h2sResult: String,
    val freeChlorineMgL: Double,
    val stripHexColor: String,
    val lastTestedTimestamp: Long,
    val lastCleanedDate: String,
    val testedBy: String,
    val nearbyCasesCount: Int,
    val syncStatus: String // OFFLINE_SAVED, P2P_SHARED, CLOUD_SYNCED
)

@Entity(tableName = "action_tasks")
data class ActionTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val villageId: String,
    val villageName: String,
    val sourceName: String,
    val title: String,
    val details: String,
    val assignedTeam: String,
    val priority: String, // CRITICAL, HIGH, ROUTINE
    val isCompleted: Boolean,
    val taskStatus: String = if (isCompleted) "DONE" else "PENDING", // PENDING, IN_PROGRESS, DONE
    val completedBy: String?,
    val createdAt: Long,
    val syncStatus: String
)

@Entity(tableName = "sample_custody")
data class SampleCustodyEntity(
    @PrimaryKey val sampleCode: String,
    val sampleType: String,
    val villageId: String,
    val villageName: String,
    val sourceOrPatientName: String,
    val collectedBy: String,
    val collectionTimestamp: Long,
    val gpsCoordinates: String,
    val currentStage: String,
    val handoverHistory: String,
    val expectedSha256Hash: String,
    val currentPayloadString: String,
    val labFindings: String,
    val isTampered: Boolean,
    val syncStatus: String
)

@Entity(tableName = "symptom_reports")
data class SymptomReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val villageId: String,
    val villageName: String,
    val patientDescription: String,
    val ageYears: Int,
    val languageName: String,
    val voiceTranscript: String,
    val durationDays: Int,
    val isDrinkingWater: Boolean,
    val hasSunkenEyes: Boolean,
    val hasBloodOrFever: Boolean,
    val triageGuidance: String,
    val severity: String, // MILD_MODERATE, SEVERE_URGENT
    val latitude: Double,
    val longitude: Double,
    val nearestSourceId: String,
    val reportedByRole: String,
    val timestamp: Long,
    val syncStatus: String
)

// Computed data class for Feature #10: Cluster Detection for Sparse Data
data class DetectedCluster(
    val villageId: String,
    val villageName: String,
    val districtState: String,
    val epicentreSourceName: String,
    val recentCaseCount3Days: Int,
    val normalBaseline: Int,
    val radiusKm: Double,
    val explanationReason: String,
    val severity: WaterSafetyLevel
)

// Computed data class for Feature #8: Early Signals from Non-medical Sources
data class EarlyNonMedicalSignal(
    val villageId: String,
    val villageName: String,
    val shopOrsSpikePercent: Int,
    val schoolAbsencePercent: Int,
    val ashaStockDepletionPercent: Int,
    val isSignalTriggered: Boolean,
    val headlineSummary: String
)

// Computed data class for Visitor Unfamiliar Village Radar
data class VisitorRadarStatus(
    val currentLatitude: Double,
    val currentLongitude: Double,
    val nearestVillage: VillageEntity,
    val distanceToVillageKm: Double,
    val villageSources: List<Pair<WaterSourceEntity, Double>>, // Source + distance in meters
    val safestSource: Pair<WaterSourceEntity, Double>?,
    val safetyScoreOutOf100: Int,
    val drinkabilityVerdict: String,
    val actionChecklistForVisitor: List<String>
)

object CryptoAndGeoUtils {
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(24).uppercase()
    }

    fun maskAadhaar(rawDigits: String): String {
        val clean = rawDigits.filter { it.isDigit() }
        val last4 = if (clean.length >= 4) clean.takeLast(4) else "4821"
        return "XXXX-XXXX-$last4"
    }

    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
