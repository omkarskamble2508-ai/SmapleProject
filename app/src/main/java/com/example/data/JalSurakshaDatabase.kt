package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ActorAccountEntity::class,
        VillageEntity::class,
        WaterSourceEntity::class,
        ActionTaskEntity::class,
        SampleCustodyEntity::class,
        SymptomReportEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class JalSurakshaDatabase : RoomDatabase() {
    abstract fun dao(): JalSurakshaDao

    companion object {
        @Volatile
        private var INSTANCE: JalSurakshaDatabase? = null

        fun getInstance(context: Context): JalSurakshaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JalSurakshaDatabase::class.java,
                    "jalsuraksha_ne.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object InitialNeSeedData {
    private val now = System.currentTimeMillis()
    private const val HOUR_MS = 3600_000L

    fun defaultActorAccounts(): List<ActorAccountEntity> = listOf(
        ActorAccountEntity(
            actorRole = ActorRole.VILLAGE_RESIDENT.name,
            fullName = "सागर कांबळे • Sagar Kamble",
            mobileNumber = "9876543210",
            aadhaarNumberMasked = "XXXX-XXXX-4829",
            aadhaarSha256Hash = CryptoAndGeoUtils.sha256("847291034829"),
            photoScanSignature = "FACE-SCAN-VERIFIED-${CryptoAndGeoUtils.sha256("SAGAR-PHOTO").take(10)}",
            photoCapturedWidth = 640,
            photoCapturedHeight = 480,
            securityPinHash = CryptoAndGeoUtils.sha256("1234"),
            registeredAt = now - 24 * HOUR_MS,
            lastLoginAt = now - HOUR_MS
        ),
        ActorAccountEntity(
            actorRole = ActorRole.ASHA_WORKER.name,
            fullName = "आशा जोनाली बोरा • ASHA Jonali Borah",
            mobileNumber = "9435011223",
            aadhaarNumberMasked = null,
            aadhaarSha256Hash = null,
            photoScanSignature = "ID-PHOTO-ASHA-${CryptoAndGeoUtils.sha256("JONALI-ID").take(10)}",
            photoCapturedWidth = 640,
            photoCapturedHeight = 480,
            securityPinHash = CryptoAndGeoUtils.sha256("1234"),
            registeredAt = now - 48 * HOUR_MS,
            lastLoginAt = now - 2 * HOUR_MS
        ),
        ActorAccountEntity(
            actorRole = ActorRole.LOCAL_DOCTOR.name,
            fullName = "डॉ. देशमुख / Dr. R. Deshmukh (MO)",
            mobileNumber = "9435044556",
            aadhaarNumberMasked = null,
            aadhaarSha256Hash = null,
            photoScanSignature = "ID-PHOTO-DOC-${CryptoAndGeoUtils.sha256("DOCTOR-ID").take(10)}",
            photoCapturedWidth = 640,
            photoCapturedHeight = 480,
            securityPinHash = CryptoAndGeoUtils.sha256("1234"),
            registeredAt = now - 72 * HOUR_MS,
            lastLoginAt = now - 3 * HOUR_MS
        ),
        ActorAccountEntity(
            actorRole = ActorRole.PHC_OFFICIAL.name,
            fullName = "श्री. कुलकर्णी / PHC Officer A. Kulkarni",
            mobileNumber = "9435077889",
            aadhaarNumberMasked = null,
            aadhaarSha256Hash = null,
            photoScanSignature = "ID-PHOTO-PHC-${CryptoAndGeoUtils.sha256("PHC-ID").take(10)}",
            photoCapturedWidth = 640,
            photoCapturedHeight = 480,
            securityPinHash = CryptoAndGeoUtils.sha256("1234"),
            registeredAt = now - 96 * HOUR_MS,
            lastLoginAt = now - 4 * HOUR_MS
        )
    )

    fun villages(): List<VillageEntity> = listOf(
        VillageEntity(
            id = "VIL-AS-MAJULI",
            name = "Kamalabari Satra Village",
            district = "Majuli Island",
            stateName = "Assam",
            riverBasin = "Brahmaputra Main Channel",
            latitude = 26.9512,
            longitude = 94.1635,
            population = 3420,
            dailyRainfallMm = 146.5,
            floodWarningActive = true,
            overallRisk = "CONTAMINATED",
            orsPacketsStock = 180,
            chlorineTabletsStock = 95,
            shopOrsSalesSpikePercent = 240,
            schoolAbsencePercent = 28,
            ashaOrsDepletionPercent = 82,
            normalBaselineCases3Days = 2,
            visitorAdvisoryNote = "सूचना: ब्रह्मपुत्रा नदीच्या पुरामुळे हातपंपाचे पाणी दूषित आहे. पाणी उकळून प्या किंवा PHC RO टाकी #2 चा वापर करा. (Avoid untreated hand-pump water; use boiled water or PHC RO Tank #2.)"
        ),
        VillageEntity(
            id = "VIL-ML-SOHRA",
            name = "Nongriat & Upper Sohra",
            district = "East Khasi Hills",
            stateName = "Meghalaya",
            riverBasin = "Wahrew Gorge Spring Basin",
            latitude = 25.2508,
            longitude = 91.7154,
            population = 1890,
            dailyRainfallMm = 178.0,
            floodWarningActive = true,
            overallRisk = "RISKY",
            orsPacketsStock = 310,
            chlorineTabletsStock = 210,
            shopOrsSalesSpikePercent = 165,
            schoolAbsencePercent = 19,
            ashaOrsDepletionPercent = 64,
            normalBaselineCases3Days = 2,
            visitorAdvisoryNote = "सूचना: मुसळधार पावसामुळे झऱ्याचे पाणी गढूळ आहे. फक्त क्लोरिनयुक्त नळ #1 चे पाणी प्या. (Heavy rainfall runoff; visitors should use chlorinated Gravity Tap #1.)"
        ),
        VillageEntity(
            id = "VIL-AS-KARBI",
            name = "Dongkamukam Hill Block",
            district = "West Karbi Anglong",
            stateName = "Assam",
            riverBasin = "Kopili Tributary Stream",
            latitude = 25.9834,
            longitude = 92.5912,
            population = 2640,
            dailyRainfallMm = 92.0,
            floodWarningActive = false,
            overallRisk = "CONTAMINATED",
            orsPacketsStock = 140,
            chlorineTabletsStock = 60,
            shopOrsSalesSpikePercent = 210,
            schoolAbsencePercent = 24,
            ashaOrsDepletionPercent = 76,
            normalBaselineCases3Days = 2,
            visitorAdvisoryNote = "सूचना: डोंगकामुकाम झऱ्याजवळ साथरोगाचे रुग्ण आढळले आहेत. फक्त बाजार बोरवेल हातपंप #4 वापरा. (Cluster alert near spring; use Deep Tube Well #4 near Bazaar.)"
        ),
        VillageEntity(
            id = "VIL-MN-MOIRANG",
            name = "Thanga Phumdi Hamlet",
            district = "Bishnupur (Loktak)",
            stateName = "Manipur",
            riverBasin = "Loktak Lake Basin",
            latitude = 24.5162,
            longitude = 93.7964,
            population = 2910,
            dailyRainfallMm = 68.0,
            floodWarningActive = false,
            overallRisk = "RISKY",
            orsPacketsStock = 420,
            chlorineTabletsStock = 340,
            shopOrsSalesSpikePercent = 130,
            schoolAbsencePercent = 14,
            ashaOrsDepletionPercent = 45,
            normalBaselineCases3Days = 3,
            visitorAdvisoryNote = "सूचना: तलावाकाठचे पाणी साचलेले आहे. फक्त सोलर शुद्धीकरण नळाचे पाणी प्या. (Stagnant shore water; drink only from JJM Solar Treated Tap.)"
        ),
        VillageEntity(
            id = "VIL-MZ-CHAMPHAI",
            name = "Zotlang Border Village",
            district = "Champhai",
            stateName = "Mizoram",
            riverBasin = "Tuipui Hill Catchment",
            latitude = 23.4566,
            longitude = 93.3282,
            population = 2150,
            dailyRainfallMm = 34.0,
            floodWarningActive = false,
            overallRisk = "SAFE",
            orsPacketsStock = 540,
            chlorineTabletsStock = 480,
            shopOrsSalesSpikePercent = 15,
            schoolAbsencePercent = 4,
            ashaOrsDepletionPercent = 18,
            normalBaselineCases3Days = 1,
            visitorAdvisoryNote = "सुरक्षित पाणी: या गावातील सर्व पाणी साठे आणि झरे पूर्णपणे शुद्ध व सुरक्षित आहेत. (All village springs passed H2S & residual chlorine checks.)"
        ),
        VillageEntity(
            id = "VIL-AR-ZIRO",
            name = "Hong Apatani Village",
            district = "Lower Subansiri",
            stateName = "Arunachal Pradesh",
            riverBasin = "Kley River Valley",
            latitude = 27.5565,
            longitude = 93.8392,
            population = 3100,
            dailyRainfallMm = 41.0,
            floodWarningActive = false,
            overallRisk = "SAFE",
            orsPacketsStock = 490,
            chlorineTabletsStock = 410,
            shopOrsSalesSpikePercent = 20,
            schoolAbsencePercent = 5,
            ashaOrsDepletionPercent = 22,
            normalBaselineCases3Days = 2,
            visitorAdvisoryNote = "सुरक्षित पाणी: सर्व हिरव्या खुणेचे नळ पर्यटकांसाठी आणि ग्रामस्थांसाठी पिण्यास योग्य आहेत. (Highland spring water is clean and safe at all marked green JJM taps.)"
        )
    )

    fun waterSources(): List<WaterSourceEntity> = listOf(
        WaterSourceEntity(
            id = "SRC-MAJ-01",
            villageId = "VIL-AS-MAJULI",
            villageName = "Kamalabari Satra Village",
            districtState = "Majuli, Assam",
            name = "Dakshinpat Shallow Hand Pump #1",
            sourceType = "Hand Pump (हातपंप)",
            latitude = 26.9525,
            longitude = 94.1650,
            safetyLevel = "CONTAMINATED",
            phLevel = 6.1,
            turbidityNtu = 18.4,
            h2sResult = "Positive (काळा रंग - जीवाणू संसर्ग / Fecal Coliform)",
            freeChlorineMgL = 0.0,
            stripHexColor = "#B91C1C",
            lastTestedTimestamp = now - 3 * HOUR_MS,
            lastCleanedDate = "18 days ago (पुरामुळे पाण्याखाली)",
            testedBy = "ASHA Jonali Borah",
            nearbyCasesCount = 9,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-MAJ-02",
            villageId = "VIL-AS-MAJULI",
            villageName = "Kamalabari Satra Village",
            districtState = "Majuli, Assam",
            name = "Kamalabari PHC Raised RO Tank #2",
            sourceType = "Gravity Tap (शुद्ध पाण्याचा नळ)",
            latitude = 26.9498,
            longitude = 94.1618,
            safetyLevel = "SAFE",
            phLevel = 7.3,
            turbidityNtu = 0.9,
            h2sResult = "Negative (सुरक्षित व स्वच्छ / Clear)",
            freeChlorineMgL = 0.5,
            stripHexColor = "#15803D",
            lastTestedTimestamp = now - 2 * HOUR_MS,
            lastCleanedDate = "कालच क्लोरिनेशन केले (Yesterday)",
            testedBy = "ASHA Jonali Borah",
            nearbyCasesCount = 0,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-MAJ-03",
            villageId = "VIL-AS-MAJULI",
            villageName = "Kamalabari Satra Village",
            districtState = "Majuli, Assam",
            name = "Luit Ghat Ring Well #3",
            sourceType = "Ring Well (विहीर)",
            latitude = 26.9540,
            longitude = 94.1672,
            safetyLevel = "RISKY",
            phLevel = 6.5,
            turbidityNtu = 7.8,
            h2sResult = "गढूळ पाणी / कमी क्लोरिन (Mild Turbidity)",
            freeChlorineMgL = 0.1,
            stripHexColor = "#B45309",
            lastTestedTimestamp = now - 6 * HOUR_MS,
            lastCleanedDate = "9 days ago",
            testedBy = "Volunteer B. Hazarika",
            nearbyCasesCount = 3,
            syncStatus = "P2P_SHARED"
        ),
        WaterSourceEntity(
            id = "SRC-SOH-01",
            villageId = "VIL-ML-SOHRA",
            villageName = "Nongriat & Upper Sohra",
            districtState = "East Khasi Hills, Meghalaya",
            name = "Kaiewaca Gorge Natural Spring",
            sourceType = "Spring (नैसर्गिक झरा)",
            latitude = 25.2520,
            longitude = 91.7170,
            safetyLevel = "RISKY",
            phLevel = 6.4,
            turbidityNtu = 8.9,
            h2sResult = "पावसाचे गढूळ पाणी (Mild Amber Runoff)",
            freeChlorineMgL = 0.05,
            stripHexColor = "#B45309",
            lastTestedTimestamp = now - 4 * HOUR_MS,
            lastCleanedDate = "11 days ago",
            testedBy = "ASHA Ibadasuk Syiem",
            nearbyCasesCount = 4,
            syncStatus = "OFFLINE_SAVED"
        ),
        WaterSourceEntity(
            id = "SRC-SOH-02",
            villageId = "VIL-ML-SOHRA",
            villageName = "Nongriat & Upper Sohra",
            districtState = "East Khasi Hills, Meghalaya",
            name = "Nongriat JJM Chlorinated Gravity Tap #1",
            sourceType = "Gravity Tap (शुद्ध नळ)",
            latitude = 25.2495,
            longitude = 91.7140,
            safetyLevel = "SAFE",
            phLevel = 7.2,
            turbidityNtu = 1.1,
            h2sResult = "Negative (सुरक्षित / Clear)",
            freeChlorineMgL = 0.4,
            stripHexColor = "#15803D",
            lastTestedTimestamp = now - 5 * HOUR_MS,
            lastCleanedDate = "2 days ago",
            testedBy = "ASHA Ibadasuk Syiem",
            nearbyCasesCount = 0,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-KAR-01",
            villageId = "VIL-AS-KARBI",
            villageName = "Dongkamukam Hill Block",
            districtState = "West Karbi Anglong, Assam",
            name = "Dongkamukam Lower Hill Spring",
            sourceType = "Spring (झरा)",
            latitude = 25.9845,
            longitude = 92.5928,
            safetyLevel = "CONTAMINATED",
            phLevel = 5.9,
            turbidityNtu = 22.0,
            h2sResult = "Positive (काळा रंग - E. coli / H2S संसर्ग)",
            freeChlorineMgL = 0.0,
            stripHexColor = "#B91C1C",
            lastTestedTimestamp = now - 2 * HOUR_MS,
            lastCleanedDate = "24 days ago",
            testedBy = "ASHA Serdihun Terangpi",
            nearbyCasesCount = 8,
            syncStatus = "OFFLINE_SAVED"
        ),
        WaterSourceEntity(
            id = "SRC-KAR-02",
            villageId = "VIL-AS-KARBI",
            villageName = "Dongkamukam Hill Block",
            districtState = "West Karbi Anglong, Assam",
            name = "Bazaar Deep Borewell Hand Pump #4",
            sourceType = "Hand Pump (हातपंप)",
            latitude = 25.9820,
            longitude = 92.5895,
            safetyLevel = "SAFE",
            phLevel = 7.4,
            turbidityNtu = 1.4,
            h2sResult = "Negative (सुरक्षित / Clear)",
            freeChlorineMgL = 0.35,
            stripHexColor = "#15803D",
            lastTestedTimestamp = now - 7 * HOUR_MS,
            lastCleanedDate = "3 days ago",
            testedBy = "ASHA Serdihun Terangpi",
            nearbyCasesCount = 0,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-MOI-01",
            villageId = "VIL-MN-MOIRANG",
            villageName = "Thanga Phumdi Hamlet",
            districtState = "Bishnupur, Manipur",
            name = "Thanga Shore Community Ring Well",
            sourceType = "Ring Well (विहीर)",
            latitude = 24.5175,
            longitude = 93.7980,
            safetyLevel = "RISKY",
            phLevel = 6.6,
            turbidityNtu = 6.5,
            h2sResult = "क्लोरिनेशन आवश्यक (Needs Chlorination)",
            freeChlorineMgL = 0.1,
            stripHexColor = "#B45309",
            lastTestedTimestamp = now - 8 * HOUR_MS,
            lastCleanedDate = "12 days ago",
            testedBy = "ASHA Thoibi Devi",
            nearbyCasesCount = 3,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-MOI-02",
            villageId = "VIL-MN-MOIRANG",
            villageName = "Thanga Phumdi Hamlet",
            districtState = "Bishnupur, Manipur",
            name = "Loktak Solar JJM Treated Tap",
            sourceType = "Gravity Tap (सोलर शुद्ध नळ)",
            latitude = 24.5150,
            longitude = 93.7950,
            safetyLevel = "SAFE",
            phLevel = 7.2,
            turbidityNtu = 0.8,
            h2sResult = "Negative (सुरक्षित / Clear)",
            freeChlorineMgL = 0.45,
            stripHexColor = "#15803D",
            lastTestedTimestamp = now - 4 * HOUR_MS,
            lastCleanedDate = "Yesterday",
            testedBy = "ASHA Thoibi Devi",
            nearbyCasesCount = 0,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-CHA-01",
            villageId = "VIL-MZ-CHAMPHAI",
            villageName = "Zotlang Border Village",
            districtState = "Champhai, Mizoram",
            name = "Zotlang Community Tuikhur Spring #1",
            sourceType = "Spring (झरा)",
            latitude = 23.4572,
            longitude = 93.3290,
            safetyLevel = "SAFE",
            phLevel = 7.1,
            turbidityNtu = 1.2,
            h2sResult = "Negative (सुरक्षित / Clear)",
            freeChlorineMgL = 0.4,
            stripHexColor = "#15803D",
            lastTestedTimestamp = now - 5 * HOUR_MS,
            lastCleanedDate = "3 days ago",
            testedBy = "ASHA Lalrinmawii",
            nearbyCasesCount = 0,
            syncStatus = "CLOUD_SYNCED"
        ),
        WaterSourceEntity(
            id = "SRC-ZIR-01",
            villageId = "VIL-AR-ZIRO",
            villageName = "Hong Apatani Village",
            districtState = "Lower Subansiri, Arunachal Pradesh",
            name = "Hong Clan Bamboo-Piped Gravity Tap",
            sourceType = "Gravity Tap (शुद्ध नळ)",
            latitude = 27.5570,
            longitude = 93.8400,
            safetyLevel = "SAFE",
            phLevel = 7.3,
            turbidityNtu = 0.7,
            h2sResult = "Negative (सुरक्षित / Clear)",
            freeChlorineMgL = 0.3,
            stripHexColor = "#15803D",
            lastTestedTimestamp = now - 6 * HOUR_MS,
            lastCleanedDate = "2 days ago",
            testedBy = "ASHA Yachang Tabyo",
            nearbyCasesCount = 1,
            syncStatus = "CLOUD_SYNCED"
        )
    )

    fun actionTasks(): List<ActionTaskEntity> = listOf(
        ActionTaskEntity(
            villageId = "VIL-AS-MAJULI",
            villageName = "Kamalabari Satra Village",
            sourceName = "Dakshinpat Shallow Hand Pump #1",
            title = "२०० ORS पाकीटे आणि ५० क्लोरिन गोळ्या पाठवा • Send 200 ORS packets & 50 chlorine tablets",
            details = "लाल धोक्याच्या सूचनेनुसार तयार केलेले काम: दक्षिणपाट हातपंप #1 जवळील घरांमध्ये तातडीने वाटप करा. (Auto-generated by Red Source + Flood Alert.)",
            assignedTeam = "ASHA Team 2 (Jonali Borah)",
            priority = "CRITICAL",
            isCompleted = false,
            completedBy = null,
            createdAt = now - 2 * HOUR_MS,
            syncStatus = "CLOUD_SYNCED"
        ),
        ActionTaskEntity(
            villageId = "VIL-AS-MAJULI",
            villageName = "Kamalabari Satra Village",
            sourceName = "Dakshinpat Shallow Hand Pump #1",
            title = "हातपंप #1 ला भेट द्या व क्लोरिनेशन करा • Visit Hand Pump #1 first & shock-chlorinate",
            details = "ब्लिचिंग पावडर द्रावण (2.5g/L) वापरा आणि नागरिकांना PHC RO टाकी #2 चा वापर करण्यास सांगा.",
            assignedTeam = "ASHA Team 2 & JJM Jal Mitra",
            priority = "CRITICAL",
            isCompleted = false,
            completedBy = null,
            createdAt = now - 2 * HOUR_MS,
            syncStatus = "CLOUD_SYNCED"
        ),
        ActionTaskEntity(
            villageId = "VIL-AS-KARBI",
            villageName = "Dongkamukam Hill Block",
            sourceName = "Dongkamukam Lower Hill Spring",
            title = "झऱ्याचे सुपर-क्लोरिनेशन करा व प्रयोगशाळेसाठी नमुने घ्या • Super-chlorinate Spring & collect QR vials",
            details = "झऱ्याजवळ ८ अतिसाराचे रुग्ण आढळले आहेत. तातडीने झिंक व ORS किट वाटप करा.",
            assignedTeam = "ASHA Team 1 (Serdihun Terangpi)",
            priority = "CRITICAL",
            isCompleted = false,
            completedBy = null,
            createdAt = now - 3 * HOUR_MS,
            syncStatus = "OFFLINE_SAVED"
        ),
        ActionTaskEntity(
            villageId = "VIL-ML-SOHRA",
            villageName = "Nongriat & Upper Sohra",
            sourceName = "Kaiewaca Gorge Natural Spring",
            title = "पूर पूर्व-दक्षता तपासणी: झऱ्याची संरक्षक भिंत तपासा • Flood Pre-Symptom Spring Check",
            details = "१७८ मिमी पाऊस झाल्यामुळे झरा तपासा आणि उपकेंद्रात १५० ORS पाकीटे तयार ठेवा.",
            assignedTeam = "ASHA Team Sohra (Ibadasuk Syiem)",
            priority = "HIGH",
            isCompleted = true,
            completedBy = "ASHA Ibadasuk Syiem",
            createdAt = now - 6 * HOUR_MS,
            syncStatus = "P2P_SHARED"
        )
    )

    fun samples(): List<SampleCustodyEntity> {
        val payload1 = "NE-JJM-2026-801|Water Vial (H2S + Coliform)|Kamalabari Satra Village|Dakshinpat Shallow Hand Pump #1|26.9525,94.1650"
        val hash1 = CryptoAndGeoUtils.sha256(payload1)

        val payload2 = "NE-JJM-2026-802|Clinical Stool Sample|Dongkamukam Hill Block|Patient Cluster Case #4|25.9845,92.5928"
        val hash2 = CryptoAndGeoUtils.sha256(payload2)

        return listOf(
            SampleCustodyEntity(
                sampleCode = "NE-JJM-2026-801",
                sampleType = "पाणी नमुना • Water Vial (H2S + Coliform)",
                villageId = "VIL-AS-MAJULI",
                villageName = "Kamalabari Satra Village",
                sourceOrPatientName = "Dakshinpat Shallow Hand Pump #1",
                collectedBy = "ASHA Jonali Borah",
                collectionTimestamp = now - 3 * HOUR_MS,
                gpsCoordinates = "26.9525° N, 94.1650° E",
                currentStage = HandoverStage.BLOCK_TRANSIT.name,
                handoverHistory = "1. गावातून संकलन • Collected at Village by ASHA Jonali Borah (26.9525, 94.1650)|2. तालुका वाहकाकडे सुपूर्द • Scanned & Handed to Block Cold-Chain Runner Pranjal Das",
                expectedSha256Hash = hash1,
                currentPayloadString = payload1,
                labFindings = "जिल्हा प्रयोगशाळेत पाठवले • In transit to District Public Health Lab",
                isTampered = false,
                syncStatus = "CLOUD_SYNCED"
            ),
            SampleCustodyEntity(
                sampleCode = "NE-JJM-2026-802",
                sampleType = "शौच नमुना • Clinical Stool Sample (Cary-Blair)",
                villageId = "VIL-AS-KARBI",
                villageName = "Dongkamukam Hill Block",
                sourceOrPatientName = "Dongkamukam Lower Hill Spring (Cluster Case #4)",
                collectedBy = "ASHA Serdihun Terangpi",
                collectionTimestamp = now - 4 * HOUR_MS,
                gpsCoordinates = "25.9845° N, 92.5928° E",
                currentStage = HandoverStage.VILLAGE_COLLECTED.name,
                handoverHistory = "1. उपकेंद्रात QR सील केले • Collected & QR-sealed at Dongkamukam Sub-Centre by ASHA Serdihun Terangpi",
                expectedSha256Hash = hash2,
                currentPayloadString = payload2,
                labFindings = "तालुका रुग्णवाहिकेची प्रतीक्षा • Awaiting Block Ambulance pickup",
                isTampered = false,
                syncStatus = "OFFLINE_SAVED"
            )
        )
    }

    fun symptomReports(): List<SymptomReportEntity> = listOf(
        SymptomReportEntity(
            villageId = "VIL-AS-MAJULI",
            villageName = "Kamalabari Satra Village",
            patientDescription = "बालक (मुनू पायेंग, ४ वर्षे) • Child (4 yrs)",
            ageYears = 4,
            languageName = "Marathi / Assamese (मराठी / অসমীয়া)",
            voiceTranscript = "माझ्या बाळाला दोन दिवसांपासून जुलाब होत आहेत आणि तो खूप अशक्त झाला आहे (My child has loose motion for 2 days and is very weak)",
            durationDays = 3,
            isDrinkingWater = false,
            hasSunkenEyes = true,
            hasBloodOrFever = false,
            triageGuidance = "ताबडतोब प्राथमिक आरोग्य केंद्रात जा! (Go to the health centre immediately & give sips of ORS on the way)",
            severity = "SEVERE_URGENT",
            latitude = 26.9527,
            longitude = 94.1648,
            nearestSourceId = "SRC-MAJ-01",
            reportedByRole = "ASHA Worker",
            timestamp = now - 2 * HOUR_MS,
            syncStatus = "CLOUD_SYNCED"
        ),
        SymptomReportEntity(
            villageId = "VIL-AS-KARBI",
            villageName = "Dongkamukam Hill Block",
            patientDescription = "ज्येष्ठ नागरिक (६२ वर्षे) • Elder (62 yrs)",
            ageYears = 62,
            languageName = "Marathi / English (मराठी / इंग्रजी)",
            voiceTranscript = "कालपासून झऱ्याचे पाणी प्यायल्यानंतर पातळ जुलाब सुरू झाले आहेत, पण पाणी पिऊ शकतात (Watery diarrhea since yesterday, able to drink water).",
            durationDays = 2,
            isDrinkingWater = true,
            hasSunkenEyes = false,
            hasBloodOrFever = false,
            triageGuidance = "आताच ORS द्या! (Give ORS now: 1 packet in 1L boiled cooled water + Zinc 20mg)",
            severity = "MILD_MODERATE",
            latitude = 25.9842,
            longitude = 92.5925,
            nearestSourceId = "SRC-KAR-01",
            reportedByRole = "User / Citizen",
            timestamp = now - 4 * HOUR_MS,
            syncStatus = "OFFLINE_SAVED"
        )
    )
}
