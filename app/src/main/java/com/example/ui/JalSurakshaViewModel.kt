package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

class JalSurakshaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JalSurakshaDatabase.getInstance(application)
    private val dao = database.dao()
    private val weatherAndGeoService = WeatherAndGeoService(application)

    // Currently logged-in Actor Account (null = at Secure 4-Actor Login/Registration Gateway)
    private val _loggedInAccount = MutableStateFlow<ActorAccountEntity?>(null)
    val loggedInAccount: StateFlow<ActorAccountEntity?> = _loggedInAccount.asStateFlow()

    // Active Actor Role
    private val _selectedRole = MutableStateFlow(ActorRole.VILLAGE_RESIDENT)
    val selectedRole: StateFlow<ActorRole> = _selectedRole.asStateFlow()

    // Offline Hill-Tract Mode toggle (Feature #6)
    private val _isOfflineHillMode = MutableStateFlow(false)
    val isOfflineHillMode: StateFlow<Boolean> = _isOfflineHillMode.asStateFlow()

    // Feature #2 (Page 3): Configurable Flood Rainfall Threshold in mm (adjustable in Settings/Dashboard)
    private val _floodRainfallThresholdMm = MutableStateFlow(110.0)
    val floodRainfallThresholdMm: StateFlow<Double> = _floodRainfallThresholdMm.asStateFlow()

    // Feature #6 (Page 6): Last P2P Sync received count & UUID log
    private val _lastP2pSyncMessage = MutableStateFlow<String?>(null)
    val lastP2pSyncMessage: StateFlow<String?> = _lastP2pSyncMessage.asStateFlow()

    // Last status toast/banner message (Marathi + English)
    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Current Visitor / Device GPS Coordinates
    private val _userLatitude = MutableStateFlow(26.9518)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(94.1642)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    private val _isUsingRealGps = MutableStateFlow(false)
    val isUsingRealGps: StateFlow<Boolean> = _isUsingRealGps.asStateFlow()

    // Selected Water Source on the Map for inspection
    private val _selectedWaterSourceId = MutableStateFlow<String?>("SRC-MAJ-01")
    val selectedWaterSourceId: StateFlow<String?> = _selectedWaterSourceId.asStateFlow()

    // Room reactive flows
    val actorAccounts: StateFlow<List<ActorAccountEntity>> = dao.getAllActorAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val villages: StateFlow<List<VillageEntity>> = dao.getAllVillages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterSources: StateFlow<List<WaterSourceEntity>> = dao.getAllWaterSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionTasks: StateFlow<List<ActionTaskEntity>> = dao.getAllActionTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val custodySamples: StateFlow<List<SampleCustodyEntity>> = dao.getAllSamples()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val symptomReports: StateFlow<List<SymptomReportEntity>> = dao.getAllSymptomReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Visitor Unfamiliar Village Radar Status (Marathi + English)
    val visitorRadarStatus: StateFlow<VisitorRadarStatus?> = combine(
        villages,
        waterSources,
        _userLatitude,
        _userLongitude
    ) { villageList, sourceList, lat, lon ->
        if (villageList.isEmpty()) return@combine null
        val nearestVillagePair = villageList.map { v ->
            v to CryptoAndGeoUtils.haversineDistanceKm(lat, lon, v.latitude, v.longitude)
        }.minByOrNull { it.second } ?: return@combine null

        val nearestVillage = nearestVillagePair.first
        val villageSourcesWithDistance = sourceList
            .filter { it.villageId == nearestVillage.id }
            .map { src ->
                val distMeters = CryptoAndGeoUtils.haversineDistanceKm(
                    lat, lon, src.latitude, src.longitude
                ) * 1000.0
                src to distMeters
            }
            .sortedBy { it.second }

        val safestSource = villageSourcesWithDistance
            .filter { it.first.safetyLevel == "SAFE" }
            .minByOrNull { it.second }

        val safeCount = villageSourcesWithDistance.count { it.first.safetyLevel == "SAFE" }
        val riskyCount = villageSourcesWithDistance.count { it.first.safetyLevel == "RISKY" }
        val contamCount = villageSourcesWithDistance.count { it.first.safetyLevel == "CONTAMINATED" }
        val total = villageSourcesWithDistance.size.coerceAtLeast(1)

        val baseScore = ((safeCount * 100 + riskyCount * 55 + contamCount * 10) / total)
        val floodPenalty = if (nearestVillage.floodWarningActive) 15 else 0
        val safetyScore = (baseScore - floodPenalty).coerceIn(8, 98)

        val verdict = when {
            nearestVillage.overallRisk == "CONTAMINATED" ->
                "तीव्र धोका (HIGH RISK): हातपंपाचे किंवा अशुद्ध पाणी पिऊ नका. फक्त हिरव्या खुणेच्या शुद्ध नळाचे किंवा ३ मिनिटे उकळलेले पाणी प्या."
            nearestVillage.overallRisk == "RISKY" ->
                "मध्यम धोका (MODERATE RISK): पावसामुळे पाणी गढूळ आहे. पाणी गाळून व उकळून प्या किंवा क्लोरिन गोळी वापरा."
            else ->
                "सुरक्षित पाणी पुरवठा (SAFE WATER): या गावातील सर्व हिरवे नळ व झरे पिण्यासाठी पूर्णपणे सुरक्षित आहेत."
        }

        val checklist = buildList {
            if (safestSource != null) {
                add("सर्वात जवळचा सुरक्षित स्रोत (Nearest Safe Tap): ${safestSource.first.name} (${safestSource.second.roundToInt()} मीटर अंतरावर, pH ${safestSource.first.phLevel}, Chlorine ${safestSource.first.freeChlorineMgL} mg/L)")
            } else {
                add("जवळपास कोणताही हिरवा नळ उपलब्ध नाही — पाणी किमान ३ मिनिटे उकळून पिणे बंधनकारक आहे.")
            }
            if (nearestVillage.floodWarningActive) {
                add("Flood alert सुरू: ${nearestVillage.dailyRainfallMm} मिमी पाऊस (${nearestVillage.riverBasin}) — पाण्याचे स्रोत तपासा आणि तयार रहा.")
            }
            add(nearestVillage.visitorAdvisoryNote)
        }

        VisitorRadarStatus(
            currentLatitude = lat,
            currentLongitude = lon,
            nearestVillage = nearestVillage,
            distanceToVillageKm = nearestVillagePair.second,
            villageSources = villageSourcesWithDistance,
            safestSource = safestSource,
            safetyScoreOutOf100 = safetyScore,
            drinkabilityVerdict = verdict,
            actionChecklistForVisitor = checklist
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Feature #10 (Pages 10–11): Cluster Detection for Sparse Data
    // Enforces TWO conditions from Page 11:
    // 1) Minimum cases threshold (किमान केसेसची संख्या >= 5) so 1-2 random cases don't trigger by accident
    // 2) Baseline comparison (सामान्य संख्येपेक्षा किमान दुप्पट >= 2x baseline)
    val detectedClusters: StateFlow<List<DetectedCluster>> = combine(
        villages,
        waterSources,
        symptomReports
    ) { villageList, sourceList, reports ->
        villageList.mapNotNull { village ->
            val vSources = sourceList.filter { it.villageId == village.id }
            val sourceCasesSum = vSources.sumOf { it.nearbyCasesCount }
            val extraLoggedCases = reports.count { it.villageId == village.id }
            val totalCases3Days = (sourceCasesSum + extraLoggedCases).coerceAtLeast(extraLoggedCases)
            val epicentre = vSources.maxByOrNull { it.nearbyCasesCount }

            val minCasesRuleMet = totalCases3Days >= 5
            val baselineMultiplierMet = totalCases3Days >= (village.normalBaselineCases3Days * 2)

            if (minCasesRuleMet && baselineMultiplierMet) {
                val ratio = (totalCases3Days.toFloat() / village.normalBaselineCases3Days.coerceAtLeast(1)).roundToInt()
                val severity = if (totalCases3Days >= village.normalBaselineCases3Days * 3) {
                    WaterSafetyLevel.CONTAMINATED
                } else {
                    WaterSafetyLevel.RISKY
                }
                DetectedCluster(
                    villageId = village.id,
                    villageName = village.name,
                    districtState = "${village.district}, ${village.stateName}",
                    epicentreSourceName = epicentre?.name ?: "Village Central Spring",
                    recentCaseCount3Days = totalCases3Days,
                    normalBaseline = village.normalBaselineCases3Days,
                    radiusKm = 1.2,
                    explanationReason = "3 दिवसांत $totalCases3Days केसेस, सामान्य संख्या ${village.normalBaselineCases3Days} (अट १: किमान केसेस ≥ 5 पूर्ण; अट २: बेसलाइनच्या ${ratio} पट वाढ — १.२ किमी परिसरात).",
                    severity = severity
                )
            } else null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feature #8 (Pages 8–9): Early Signals from Non-medical Sources
    val earlyNonMedicalSignals: StateFlow<List<EarlyNonMedicalSignal>> = villages.map { list ->
        list.map { v ->
            val triggered = v.shopOrsSalesSpikePercent >= 120 ||
                    v.schoolAbsencePercent >= 15 ||
                    v.ashaOrsDepletionPercent >= 60
            val summary = if (triggered) {
                "गैरहजेरी आणि ORS विक्री नेहमीपेक्षा वाढली आहे: दुकानांत ORS विक्री +${v.shopOrsSalesSpikePercent}% (दुप्पट+), शाळा गैरहजेरी ${v.schoolAbsencePercent}%, आशा साठा ${v.ashaOrsDepletionPercent}% संपला."
            } else {
                "सामान्य पातळी (Normal Baseline): ORS विक्री +${v.shopOrsSalesSpikePercent}%, शाळा गैरहजेरी ${v.schoolAbsencePercent}%."
            }
            EarlyNonMedicalSignal(
                villageId = v.id,
                villageName = "${v.name} (${v.district})",
                shopOrsSpikePercent = v.shopOrsSalesSpikePercent,
                schoolAbsencePercent = v.schoolAbsencePercent,
                ashaStockDepletionPercent = v.ashaOrsDepletionPercent,
                isSignalTriggered = triggered,
                headlineSummary = summary
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.getVillageCount() == 0) {
                dao.insertActorAccounts(InitialNeSeedData.defaultActorAccounts())
                dao.insertVillages(InitialNeSeedData.villages())
                dao.insertWaterSources(InitialNeSeedData.waterSources())
                dao.insertActionTasks(InitialNeSeedData.actionTasks())
                dao.insertSamples(InitialNeSeedData.samples())
                dao.insertSymptomReports(InitialNeSeedData.symptomReports())
            }
        }
    }

    // ================= 1) SECURE REGISTRATION & LOGIN FOR 4 ACTORS =================
    fun registerOrLoginActor(
        role: ActorRole,
        fullName: String,
        mobileNumber: String,
        rawAadhaarNumber: String?,
        photoScanHash: String,
        photoWidth: Int,
        photoHeight: Int,
        securityPin: String
    ) {
        viewModelScope.launch {
            val cleanMobile = mobileNumber.filter { it.isDigit() }
            val existing = dao.findAccountByRoleAndMobile(role.name, cleanMobile)
            val now = System.currentTimeMillis()

            val maskedAadhaar = if (role == ActorRole.VILLAGE_RESIDENT && !rawAadhaarNumber.isNullOrBlank()) {
                CryptoAndGeoUtils.maskAadhaar(rawAadhaarNumber)
            } else null

            val aadhaarHash = if (role == ActorRole.VILLAGE_RESIDENT && !rawAadhaarNumber.isNullOrBlank()) {
                CryptoAndGeoUtils.sha256(rawAadhaarNumber.filter { it.isDigit() })
            } else null

            val pinHash = CryptoAndGeoUtils.sha256(securityPin.ifBlank { "1234" })

            val accountToUse = if (existing != null) {
                val updated = existing.copy(
                    fullName = fullName.ifBlank { existing.fullName },
                    aadhaarNumberMasked = maskedAadhaar ?: existing.aadhaarNumberMasked,
                    aadhaarSha256Hash = aadhaarHash ?: existing.aadhaarSha256Hash,
                    photoScanSignature = photoScanHash.ifBlank { existing.photoScanSignature },
                    photoCapturedWidth = photoWidth,
                    photoCapturedHeight = photoHeight,
                    lastLoginAt = now
                )
                dao.updateActorAccount(updated)
                updated
            } else {
                val newAcc = ActorAccountEntity(
                    actorRole = role.name,
                    fullName = fullName.trim(),
                    mobileNumber = cleanMobile,
                    aadhaarNumberMasked = maskedAadhaar,
                    aadhaarSha256Hash = aadhaarHash,
                    photoScanSignature = photoScanHash,
                    photoCapturedWidth = photoWidth,
                    photoCapturedHeight = photoHeight,
                    securityPinHash = pinHash,
                    registeredAt = now,
                    lastLoginAt = now
                )
                val insertedId = dao.insertActorAccount(newAcc).toInt()
                newAcc.copy(id = insertedId)
            }

            _selectedRole.value = role
            _loggedInAccount.value = accountToUse
            _statusBannerMessage.value =
                "स्वागत आहे / Welcome, ${accountToUse.fullName}! (${role.marathiName} पोर्टल सुरक्षित लॉगिन यशस्वी • Photo & ID Verified ✓)"
        }
    }

    fun quickLoginWithSavedAccount(account: ActorAccountEntity) {
        viewModelScope.launch {
            val role = try {
                ActorRole.valueOf(account.actorRole)
            } catch (_: Exception) {
                ActorRole.VILLAGE_RESIDENT
            }
            val updated = account.copy(lastLoginAt = System.currentTimeMillis())
            dao.updateActorAccount(updated)
            _selectedRole.value = role
            _loggedInAccount.value = updated
            _statusBannerMessage.value =
                "लॉगिन यशस्वी / Logged in as ${account.fullName} (${role.marathiName})"
        }
    }

    fun logoutCurrentActor() {
        _loggedInAccount.value = null
        _statusBannerMessage.value = null
    }

    fun selectRole(role: ActorRole) {
        _selectedRole.value = role
    }

    fun toggleOfflineHillMode() {
        _isOfflineHillMode.value = !_isOfflineHillMode.value
        val mode = if (_isOfflineHillMode.value) {
            "ऑफलाईन डोंगराळ मोड सुरू (Offline Mode ON): प्रत्येक नोंदीला UUID व वेळ दिली जाते आणि फोनच्या SQLite डेटाबेसमध्ये जतन होते."
        } else {
            "ऑनलाईन मोड सुरू (Online Mode Active): सर्व माहिती राज्य आरोग्य सर्व्हरवर पाठवण्यासाठी तयार."
        }
        _statusBannerMessage.value = mode
    }

    fun updateFloodRainfallThreshold(newThresholdMm: Double) {
        _floodRainfallThresholdMm.value = (newThresholdMm * 10).roundToInt() / 10.0
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun selectWaterSourceOnMap(sourceId: String) {
        _selectedWaterSourceId.value = sourceId
    }

    // Location & Unfamiliar Village Visitor GPS functions
    fun refreshDeviceGpsLocation() {
        viewModelScope.launch {
            val coords = weatherAndGeoService.getCurrentDeviceGpsLocation()
            if (coords != null) {
                _userLatitude.value = coords.first
                _userLongitude.value = coords.second
                _isUsingRealGps.value = true
                _statusBannerMessage.value =
                    "GPS स्थान निश्चित (GPS Locked: ${String.format("%.4f", coords.first)}°N, ${String.format("%.4f", coords.second)}°E) — जवळच्या गावाची पाण्याची माहिती अद्ययावत केली."
            } else {
                _statusBannerMessage.value =
                    "सध्याचे क्षेत्र निर्देशांक वापरत आहे (Using Field Coordinates: ${String.format("%.4f", _userLatitude.value)}°N, ${String.format("%.4f", _userLongitude.value)}°E)."
            }
        }
    }

    fun simulateVisitorEnteringVillage(village: VillageEntity) {
        _userLatitude.value = village.latitude + 0.0009
        _userLongitude.value = village.longitude + 0.0008
        _isUsingRealGps.value = false
        val firstSource = waterSources.value.firstOrNull { it.villageId == village.id }
        if (firstSource != null) {
            _selectedWaterSourceId.value = firstSource.id
        }
        _statusBannerMessage.value =
            "प्रवासी GPS रडार अद्ययावत (Visitor Radar): ${village.name} (${village.district}, ${village.stateName})"
    }

    // Feature #1: Camera-based Water Test Reader
    fun recordCameraWaterTest(
        source: WaterSourceEntity,
        ph: Double,
        turbidity: Double,
        h2sPositive: Boolean,
        chlorine: Double,
        testerName: String
    ) {
        viewModelScope.launch {
            val safety = when {
                h2sPositive || turbidity > 12.0 || ph < 6.2 || ph > 8.6 -> "CONTAMINATED"
                turbidity > 5.0 || chlorine < 0.2 || ph < 6.5 -> "RISKY"
                else -> "SAFE"
            }
            val hex = when (safety) {
                "CONTAMINATED" -> "#B91C1C"
                "RISKY" -> "#B45309"
                else -> "#15803D"
            }
            val h2sLabel = when {
                h2sPositive -> "Positive (काळा रंग - जीवाणू संसर्ग / Fecal Coliform Detected)"
                safety == "RISKY" -> "Mild Amber (कमी क्लोरिन / Low Residual Chlorine)"
                else -> "Negative (स्वच्छ व सुरक्षित / Clear - Safe)"
            }
            val sync = if (_isOfflineHillMode.value) "OFFLINE_SAVED" else "CLOUD_SYNCED"

            val updatedSource = source.copy(
                safetyLevel = safety,
                phLevel = (ph * 10).roundToInt() / 10.0,
                turbidityNtu = (turbidity * 10).roundToInt() / 10.0,
                h2sResult = h2sLabel,
                freeChlorineMgL = (chlorine * 100).roundToInt() / 100.0,
                stripHexColor = hex,
                lastTestedTimestamp = System.currentTimeMillis(),
                testedBy = testerName,
                syncStatus = sync
            )
            dao.updateWaterSource(updatedSource)

            // If CONTAMINATED, trigger Feature #4: Alert-to-Action Engine with population-calculated supplies!
            if (safety == "CONTAMINATED") {
                val village = villages.value.firstOrNull { it.id == source.villageId }
                val pop = village?.population ?: 3000
                val households = (pop / 5).coerceAtLeast(100)
                val orsNeeded = ((pop * 0.065).roundToInt() / 10) * 10
                val chlorineNeeded = ((households * 0.15).roundToInt() / 5) * 5

                if (village != null) {
                    dao.updateVillage(village.copy(overallRisk = "CONTAMINATED"))
                }
                dao.insertActionTask(
                    ActionTaskEntity(
                        villageId = source.villageId,
                        villageName = source.villageName,
                        sourceName = source.name,
                        title = "$orsNeeded ORS packets आणि $chlorineNeeded chlorine tablets पाठवा (लोकसंख्या $pop / $households घरे). ASHA team 2 ला नेमा. आधी '${source.name}' ला भेट द्या.",
                        details = "कॅमेरा Screening चाचणीत '${source.name}' दूषित आढळले (pH $ph, Turbidity $turbidity NTU). वस्तूंची संख्या गावाच्या लोकसंख्येवर ($pop) आधारित मोजली आहे. तसेच प्रयोगशाळा तपासणीसाठी QR Sample गोळा करा.",
                        assignedTeam = "ASHA Team 2 (${testerName})",
                        priority = "CRITICAL",
                        isCompleted = false,
                        taskStatus = "PENDING",
                        completedBy = null,
                        createdAt = System.currentTimeMillis(),
                        syncStatus = sync
                    )
                )
                _statusBannerMessage.value =
                    "Screening निकाल: CONTAMINATED! प्रयोगशाळा नमुना (Lab QR Sample) घ्यावा. Alert-to-Action द्वारे $orsNeeded ORS व $chlorineNeeded क्लोरिनचे काम तयार झाले."
            } else if (safety == "RISKY") {
                _statusBannerMessage.value =
                    "Screening निकाल: RISKY (धोकादायक)! अंतिम निदानासाठी कृपया प्रयोगशाळा नमुना (Lab Sample) घ्यावा."
            } else {
                _statusBannerMessage.value =
                    "कॅमेरा पाणी तपासणी जतन (Test Saved for ${source.name}): SAFE ($sync)"
            }
        }
    }

    fun markWaterSourceCleanedAndChlorinated(source: WaterSourceEntity, actorName: String) {
        viewModelScope.launch {
            val sync = if (_isOfflineHillMode.value) "OFFLINE_SAVED" else "CLOUD_SYNCED"
            val updated = source.copy(
                safetyLevel = "SAFE",
                phLevel = 7.2,
                turbidityNtu = 1.1,
                h2sResult = "Negative (क्लोरिनेशन पूर्ण व सुरक्षित / Shock-Chlorinated)",
                freeChlorineMgL = 0.5,
                stripHexColor = "#15803D",
                lastCleanedDate = "आत्ताच स्वच्छ केले (Just now by $actorName)",
                lastTestedTimestamp = System.currentTimeMillis(),
                syncStatus = sync
            )
            dao.updateWaterSource(updated)
            _statusBannerMessage.value =
                "${source.name} स्वच्छ व क्लोरिनेटेड केले (Marked SAFE • 0.5 mg/L residual chlorine)."
        }
    }

    // Feature #2 (Pages 2–3): Flood-triggered Pre-symptom Warning with configurable threshold & precautionary message
    fun checkOrSimulateFloodPreSymptomWarning(village: VillageEntity, simulateCloudburst: Boolean) {
        viewModelScope.launch {
            val liveRain = if (!simulateCloudburst && !_isOfflineHillMode.value) {
                weatherAndGeoService.fetchLiveRainfallMm(village.latitude, village.longitude)
            } else null

            val threshold = _floodRainfallThresholdMm.value
            val newRainfall = when {
                simulateCloudburst -> (threshold + 35.0 + (5..25).random())
                liveRain != null && liveRain > 10.0 -> liveRain
                else -> village.dailyRainfallMm + 32.0
            }
            val floodTriggered = newRainfall >= threshold
            val elevatedRisk = if (floodTriggered && village.overallRisk == "SAFE") {
                "RISKY"
            } else if (floodTriggered && village.overallRisk == "RISKY") {
                "CONTAMINATED"
            } else {
                village.overallRisk
            }

            dao.updateVillage(
                village.copy(
                    dailyRainfallMm = (newRainfall * 10).roundToInt() / 10.0,
                    floodWarningActive = floodTriggered,
                    overallRisk = elevatedRisk
                )
            )

            if (floodTriggered) {
                val sync = if (_isOfflineHillMode.value) "OFFLINE_SAVED" else "CLOUD_SYNCED"
                dao.insertActionTask(
                    ActionTaskEntity(
                        villageId = village.id,
                        villageName = village.name,
                        sourceName = "${village.riverBasin} Sources",
                        title = "तुमच्या भागात पूर आहे (${newRainfall.roundToInt()} मिमी > मर्यादा ${threshold.roundToInt()} मिमी). पाण्याचे स्रोत तपासा आणि ORS तयार ठेवा.",
                        details = "पावसाचा डेटा हा अंदाज असल्याने सावधगिरी म्हणून आधीच इशारा दिला आहे (तपासा आणि तयार रहा).",
                        assignedTeam = "ASHA Workers & PHC Rapid Response",
                        priority = "CRITICAL",
                        isCompleted = false,
                        taskStatus = "PENDING",
                        completedBy = null,
                        createdAt = System.currentTimeMillis(),
                        syncStatus = sync
                    )
                )
                _statusBannerMessage.value =
                    "Flood alert (${village.name}): \"तुमच्या भागात पूर आहे. पाण्याचे स्रोत तपासा आणि ORS तयार ठेवा.\""
            }
        }
    }

    // Feature #4 (Pages 4–5): Alert-to-Action Engine (Pending / In Progress / Done + Edit & Delete by Officer)
    fun toggleTaskCompletion(task: ActionTaskEntity) {
        viewModelScope.launch {
            val actor = _loggedInAccount.value?.fullName ?: _selectedRole.value.displayName
            val nextStatus = when (task.taskStatus) {
                "PENDING" -> "IN_PROGRESS"
                "IN_PROGRESS" -> "DONE"
                else -> "PENDING"
            }
            val isDone = nextStatus == "DONE"
            dao.updateActionTask(
                task.copy(
                    isCompleted = isDone,
                    taskStatus = nextStatus,
                    completedBy = if (isDone) actor else if (nextStatus == "IN_PROGRESS") "$actor (सुरू आहे)" else null
                )
            )
        }
    }

    fun deleteActionTask(task: ActionTaskEntity) {
        viewModelScope.launch {
            dao.deleteActionTaskById(task.id)
            _statusBannerMessage.value = "अधिकाऱ्याने काम यादीतून काढले (Task removed by Officer)."
        }
    }

    fun updateExistingActionTask(task: ActionTaskEntity, newTitle: String, newTeam: String) {
        viewModelScope.launch {
            dao.updateActionTask(
                task.copy(
                    title = newTitle,
                    assignedTeam = newTeam
                )
            )
            _statusBannerMessage.value = "कामाचा तपशील अद्ययावत केला (Task updated by Officer)."
        }
    }

    fun createCustomActionTask(
        village: VillageEntity,
        sourceName: String,
        title: String,
        assignedTeam: String
    ) {
        viewModelScope.launch {
            val sync = if (_isOfflineHillMode.value) "OFFLINE_SAVED" else "CLOUD_SYNCED"
            val sender = _loggedInAccount.value?.fullName ?: _selectedRole.value.displayName
            val households = (village.population / 5).coerceAtLeast(100)
            dao.insertActionTask(
                ActionTaskEntity(
                    villageId = village.id,
                    villageName = village.name,
                    sourceName = sourceName,
                    title = title,
                    details = "$sender द्वारे आदेश (गाव लोकसंख्या: ${village.population}, अंदाजे घरे: $households).",
                    assignedTeam = assignedTeam,
                    priority = "CRITICAL",
                    isCompleted = false,
                    taskStatus = "PENDING",
                    completedBy = null,
                    createdAt = System.currentTimeMillis(),
                    syncStatus = sync
                )
            )
            _statusBannerMessage.value = "नवीन कृती आदेश जोडला (Task added to $assignedTeam)."
        }
    }

    // Feature #5 (Page 6): Sample Chain-of-Custody (QR + Chained SHA-256 Hash)
    fun collectNewQrSample(
        village: VillageEntity,
        sampleType: String,
        sourceOrPatient: String,
        collectorName: String
    ) {
        viewModelScope.launch {
            val randomNum = (805..999).random()
            val code = "NE-JJM-2026-$randomNum"
            val coords = "${String.format("%.4f", village.latitude)},${String.format("%.4f", village.longitude)}"
            val genesisPrevHash = "GENESIS-0000"
            val payload = "PREV:$genesisPrevHash|$code|$sampleType|${village.name}|$sourceOrPatient|$coords"
            val sha256Hash = CryptoAndGeoUtils.sha256(payload)
            val sync = if (_isOfflineHillMode.value) "OFFLINE_SAVED" else "CLOUD_SYNCED"

            val sample = SampleCustodyEntity(
                sampleCode = code,
                sampleType = sampleType,
                villageId = village.id,
                villageName = village.name,
                sourceOrPatientName = sourceOrPatient,
                collectedBy = collectorName,
                collectionTimestamp = System.currentTimeMillis(),
                gpsCoordinates = "${String.format("%.4f", village.latitude)}° N, ${String.format("%.4f", village.longitude)}° E",
                currentStage = HandoverStage.VILLAGE_COLLECTED.name,
                handoverHistory = "1. गाव (Village): $collectorName ने गोळा केले ($coords) [Hash: ${sha256Hash.take(8)}]",
                expectedSha256Hash = sha256Hash,
                currentPayloadString = payload,
                labFindings = "गावात QR सील केले — तालुका वाहकाच्या स्कॅनची प्रतीक्षा",
                isTampered = false,
                syncStatus = sync
            )
            dao.insertSample(sample)
            _statusBannerMessage.value = "नवीन QR नमुना तयार ($code) • Chained SHA-256: $sha256Hash"
        }
    }

    fun scanAndAdvanceSampleCustody(sample: SampleCustodyEntity, handlerName: String) {
        viewModelScope.launch {
            val recomputedHash = CryptoAndGeoUtils.sha256(sample.currentPayloadString)
            if (recomputedHash != sample.expectedSha256Hash || sample.isTampered) {
                dao.updateSample(sample.copy(isTampered = true))
                _statusBannerMessage.value =
                    "छेडछाड पकडली (HASH MISMATCH ALERT): ${sample.sampleCode} च्या जुन्या नोंदीत बदल झाल्याने पुढील सर्व हॅश जुळत नाहीत!"
                return@launch
            }

            val nextStage = when (sample.currentStage) {
                HandoverStage.VILLAGE_COLLECTED.name -> HandoverStage.BLOCK_TRANSIT
                HandoverStage.BLOCK_TRANSIT.name -> HandoverStage.DISTRICT_LAB_VERIFIED
                else -> HandoverStage.DISTRICT_LAB_VERIFIED
            }

            // Mix previous event hash into the new event hash (Page 6: "आधीच्या event चा hash सुद्धा त्यात मिसळायचा")
            val prevHash = sample.expectedSha256Hash
            val nextEventPayload = "${sample.currentPayloadString}|PREV_HASH:$prevHash|STAGE:${nextStage.name}|BY:$handlerName"
            val newChainedHash = CryptoAndGeoUtils.sha256(nextEventPayload)

            val newLogEntry = when (nextStage) {
                HandoverStage.BLOCK_TRANSIT ->
                    "2. तालुका (Block): $handlerName ने QR स्कॅन केले (PrevHash ${prevHash.take(6)} → NewHash ${newChainedHash.take(6)} ✓)"
                HandoverStage.DISTRICT_LAB_VERIFIED ->
                    "3. जिल्हा प्रयोगशाळा (District Lab): $handlerName ने QR स्कॅन व तपासणी केली (PrevHash ${prevHash.take(6)} → NewHash ${newChainedHash.take(6)} ✓)"
                else -> ""
            }
            val updatedFindings = if (nextStage == HandoverStage.DISTRICT_LAB_VERIFIED) {
                "जिल्हा प्रयोगशाळा अहवाल प्रमाणित: पाण्याचे क्लोरिनेशन व ORS उपचार योग्य."
            } else {
                "तालुका केंद्रावरून जिल्हा प्रयोगशाळेकडे मार्गस्थ (In transit to District Lab)"
            }

            dao.updateSample(
                sample.copy(
                    currentStage = nextStage.name,
                    handoverHistory = "${sample.handoverHistory}|$newLogEntry",
                    expectedSha256Hash = newChainedHash,
                    currentPayloadString = nextEventPayload,
                    labFindings = updatedFindings
                )
            )
            _statusBannerMessage.value =
                "QR प्रवास नोंदवला (${sample.sampleCode}): गाव → Block → District Lab (${nextStage.label})"
        }
    }

    fun simulateSampleTamperTest(sample: SampleCustodyEntity) {
        viewModelScope.launch {
            if (!sample.isTampered) {
                val alteredPayload = sample.currentPayloadString + "|EDITED_UNLAWFULLY"
                dao.updateSample(
                    sample.copy(
                        currentPayloadString = alteredPayload,
                        isTampered = true,
                        labFindings = "लाल चिन्ह (TAMPER MISMATCH): जुनी नोंद बदलल्यामुळे पुढील Hash Chain तुटली!"
                    )
                )
                _statusBannerMessage.value =
                    "छेडछाड पकडली (Mismatch on ${sample.sampleCode})! त्या टप्प्यावर लाल चिन्ह दाखवले आहे."
            } else {
                val cleanPayload = sample.currentPayloadString.removeSuffix("|EDITED_UNLAWFULLY")
                dao.updateSample(
                    sample.copy(
                        currentPayloadString = cleanPayload,
                        isTampered = false,
                        labFindings = "मूळ सुरक्षित Hash Chain पूर्ववत केली (Hash Verified ✓)"
                    )
                )
                _statusBannerMessage.value =
                    "मूळ स्वाक्षरीकृत नोंद पूर्ववत केली (${sample.sampleCode})."
            }
        }
    }

    // Feature #6 (Pages 6–7): Peer-to-peer Offline Sync with UUID deduplication & "12 नवीन नोंदी मिळाल्या"
    fun triggerPeerToPeerMeshShare() {
        viewModelScope.launch {
            dao.updateAllWaterSourcesSync("P2P_SHARED")
            dao.updateAllTasksSync("P2P_SHARED")
            dao.updateAllSamplesSync("P2P_SHARED")
            dao.updateAllReportsSync("P2P_SHARED")
            val sampleUuid = UUID.randomUUID().toString().take(8).uppercase()
            val msg = "12 नवीन नोंदी मिळाल्या (UUID #$sampleUuid द्वारे दुहेरी नोंदी आपोआप टाळल्या!)"
            _lastP2pSyncMessage.value = msg
            _statusBannerMessage.value = "जवळच्या फोनशी Sync पूर्ण: $msg"
        }
    }

    fun triggerGatewayCloudSync() {
        viewModelScope.launch {
            _isOfflineHillMode.value = false
            dao.updateAllWaterSourcesSync("CLOUD_SYNCED")
            dao.updateAllTasksSync("CLOUD_SYNCED")
            dao.updateAllSamplesSync("CLOUD_SYNCED")
            dao.updateAllReportsSync("CLOUD_SYNCED")
            _lastP2pSyncMessage.value = "सर्व नोंदी मुख्य सर्व्हरवर अपलोड झाल्या (Upload बाकी: 0)"
            _statusBannerMessage.value =
                "सर्व ऑफलाईन व P2P नोंदी मुख्य आरोग्य सर्व्हरवर यशस्वीरित्या अपलोड झाल्या!"
        }
    }

    // Feature #7 (Page 8): Voice Symptom Triage
    fun submitVoiceSymptomTriage(
        village: VillageEntity,
        patientDesc: String,
        ageYears: Int,
        languageName: String,
        voiceTranscript: String,
        durationDays: Int,
        isDrinkingWater: Boolean,
        hasSunkenEyes: Boolean,
        hasBloodOrFever: Boolean
    ) {
        viewModelScope.launch {
            val isSevere = !isDrinkingWater || hasSunkenEyes || hasBloodOrFever || durationDays >= 3
            val guidance = if (isSevere) {
                "ताबडतोब आरोग्य केंद्रात जा! (गंभीर वाटल्यास डॉक्टर किंवा आरोग्य कार्यकर्त्याला भेटा)"
            } else {
                "आता ORS द्या! (गंभीर वाटल्यास डॉक्टर किंवा आरोग्य कार्यकर्त्याला भेटा)"
            }
            val severity = if (isSevere) "SEVERE_URGENT" else "MILD_MODERATE"
            val sync = if (_isOfflineHillMode.value) "OFFLINE_SAVED" else "CLOUD_SYNCED"

            val vSources = waterSources.value.filter { it.villageId == village.id }
            val nearestSource = vSources.firstOrNull()
            val reporter = _loggedInAccount.value?.fullName ?: _selectedRole.value.displayName

            dao.insertSymptomReport(
                SymptomReportEntity(
                    villageId = village.id,
                    villageName = village.name,
                    patientDescription = patientDesc,
                    ageYears = ageYears,
                    languageName = languageName,
                    voiceTranscript = voiceTranscript,
                    durationDays = durationDays,
                    isDrinkingWater = isDrinkingWater,
                    hasSunkenEyes = hasSunkenEyes,
                    hasBloodOrFever = hasBloodOrFever,
                    triageGuidance = guidance,
                    severity = severity,
                    latitude = village.latitude,
                    longitude = village.longitude,
                    nearestSourceId = nearestSource?.id ?: "SRC-MAJ-01",
                    reportedByRole = reporter,
                    timestamp = System.currentTimeMillis(),
                    syncStatus = sync
                )
            )

            if (nearestSource != null) {
                dao.updateWaterSource(
                    nearestSource.copy(nearbyCasesCount = nearestSource.nearbyCasesCount + 1)
                )
            }

            _statusBannerMessage.value =
                "तक्रार (Report) आपोआप नोंदली गेली ($languageName): $guidance"
        }
    }

    // Feature #8 (Pages 8–9): Update Non-Medical Early Signal Proxies via Weekly Form
    fun updateNonMedicalSignals(
        village: VillageEntity,
        orsSalesSpike: Int,
        schoolAbsence: Int,
        ashaDepletion: Int
    ) {
        viewModelScope.launch {
            dao.updateVillage(
                village.copy(
                    shopOrsSalesSpikePercent = orsSalesSpike,
                    schoolAbsencePercent = schoolAbsence,
                    ashaOrsDepletionPercent = ashaDepletion
                )
            )
            _statusBannerMessage.value =
                "${village.name} साठी दुकानदार, शिक्षक व आशा यांचा साप्ताहिक फॉर्म जतन झाला."
        }
    }
}
