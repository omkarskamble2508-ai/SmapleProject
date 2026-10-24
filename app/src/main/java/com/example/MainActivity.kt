package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ActorRole
import com.example.ui.JalSurakshaViewModel
import com.example.ui.components.GovtHeaderAndRoleBar
import com.example.ui.screens.*
import com.example.ui.theme.JalSurakshaTheme

private data class ActorNavTabItem(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private fun tabsForActor(role: ActorRole): List<ActorNavTabItem> = when (role) {
    // 1) USER / CITIZEN ROLE: Prioritizes Safe Water Map #1, Voice Health Check #2, Citizen Aadhaar Pass & Alerts #3
    ActorRole.VILLAGE_RESIDENT -> listOf(
        ActorNavTabItem(0, "सुरक्षित पाणी • Map", Icons.Filled.Map, Icons.Outlined.Map, "user_tab_safe_map"),
        ActorNavTabItem(1, "आवाज तपासणी • Voice", Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver, "user_tab_voice"),
        ActorNavTabItem(2, "ओळखपत्र व सूचना • Pass", Icons.Filled.Badge, Icons.Outlined.Badge, "user_tab_profile")
    )

    // 2) ASHA WORKER ROLE: Camera Water Test #1, Action Tasks & Flood #2, QR & Offline Sync #3, Source Risk Map #4
    ActorRole.ASHA_WORKER -> listOf(
        ActorNavTabItem(0, "पाणी चाचणी • Test", Icons.Filled.Science, Icons.Outlined.Science, "asha_tab_water_test"),
        ActorNavTabItem(1, "कृती यादी • Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist, "asha_tab_tasks"),
        ActorNavTabItem(2, "QR व सिंक • Sync", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "asha_tab_qr_sync"),
        ActorNavTabItem(3, "पाणी नकाशा • Map", Icons.Filled.Map, Icons.Outlined.Map, "asha_tab_map")
    )

    // 3) LOCAL DOCTOR ROLE: Clinical Triage & Clusters #1, Lab QR Custody #2, Source Risk Map #3
    ActorRole.LOCAL_DOCTOR -> listOf(
        ActorNavTabItem(0, "रुग्ण व क्लस्टर • Clinic", Icons.Filled.MedicalServices, Icons.Outlined.MedicalServices, "doc_tab_clinic"),
        ActorNavTabItem(1, "प्रयोगशाळा QR • Lab QR", Icons.Filled.QrCode2, Icons.Outlined.QrCode2, "doc_tab_lab_qr"),
        ActorNavTabItem(2, "पाणी नकाशा • Map", Icons.Filled.Map, Icons.Outlined.Map, "doc_tab_map")
    )

    // 4) PHC OFFICIAL ROLE: Command & Action Dispatch #1, What-If Simulator & Signals #2, District Risk Map #3
    ActorRole.PHC_OFFICIAL -> listOf(
        ActorNavTabItem(0, "कमांड व आदेश • Command", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "phc_tab_command"),
        ActorNavTabItem(1, "सिम्युलेटर • Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics, "phc_tab_analytics"),
        ActorNavTabItem(2, "जिल्हा नकाशा • Map", Icons.Filled.Map, Icons.Outlined.Map, "phc_tab_map")
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JalSurakshaTheme {
                JalSurakshaPortalApp()
            }
        }
    }
}

@Composable
fun JalSurakshaPortalApp(
    viewModel: JalSurakshaViewModel = viewModel()
) {
    val loggedInAccount by viewModel.loggedInAccount.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.actorAccounts.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
    val isOfflineHillMode by viewModel.isOfflineHillMode.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val villages by viewModel.villages.collectAsStateWithLifecycle()
    val waterSources by viewModel.waterSources.collectAsStateWithLifecycle()
    val actionTasks by viewModel.actionTasks.collectAsStateWithLifecycle()
    val custodySamples by viewModel.custodySamples.collectAsStateWithLifecycle()
    val symptomReports by viewModel.symptomReports.collectAsStateWithLifecycle()
    val visitorRadar by viewModel.visitorRadarStatus.collectAsStateWithLifecycle()
    val detectedClusters by viewModel.detectedClusters.collectAsStateWithLifecycle()
    val earlySignals by viewModel.earlyNonMedicalSignals.collectAsStateWithLifecycle()
    val selectedWaterSourceId by viewModel.selectedWaterSourceId.collectAsStateWithLifecycle()
    val userLat by viewModel.userLatitude.collectAsStateWithLifecycle()
    val userLon by viewModel.userLongitude.collectAsStateWithLifecycle()
    val isUsingRealGps by viewModel.isUsingRealGps.collectAsStateWithLifecycle()
    val floodThresholdMm by viewModel.floodRainfallThresholdMm.collectAsStateWithLifecycle()
    val lastP2pSyncMessage by viewModel.lastP2pSyncMessage.collectAsStateWithLifecycle()

    // If no actor is logged in, show the 4-Actor Secure Registration & Login Gateway
    val currentAccount = loggedInAccount
    if (currentAccount == null) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            ActorAuthScreen(
                savedAccounts = savedAccounts,
                onRegisterOrLogin = { role, name, phone, aadhaar, photoHash, w, h, pin ->
                    viewModel.registerOrLoginActor(role, name, phone, aadhaar, photoHash, w, h, pin)
                },
                onQuickLogin = { account ->
                    viewModel.quickLoginWithSavedAccount(account)
                }
            )
        }
        return
    }

    var currentTab by rememberSaveable(selectedRole) { mutableIntStateOf(0) }
    val activeRoleTabs = remember(selectedRole) { tabsForActor(selectedRole) }

    val pendingOfflineItemsCount = remember(waterSources, actionTasks, custodySamples, symptomReports) {
        waterSources.count { it.syncStatus != "CLOUD_SYNCED" } +
                actionTasks.count { it.syncStatus != "CLOUD_SYNCED" } +
                custodySamples.count { it.syncStatus != "CLOUD_SYNCED" } +
                symptomReports.count { it.syncStatus != "CLOUD_SYNCED" }
    }

    BackHandler(enabled = true) {
        if (currentTab != 0) {
            currentTab = 0
        } else {
            viewModel.logoutCurrentActor()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            GovtHeaderAndRoleBar(
                loggedInAccount = currentAccount,
                selectedRole = selectedRole,
                isOfflineHillMode = isOfflineHillMode,
                onToggleOfflineMode = { viewModel.toggleOfflineHillMode() },
                pendingOfflineCount = pendingOfflineItemsCount,
                bannerMessage = bannerMessage,
                onDismissBanner = { viewModel.dismissBanner() },
                onLogoutSwitchPortal = { viewModel.logoutCurrentActor() }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                activeRoleTabs.forEach { tab ->
                    val selected = currentTab == tab.index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab.index },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedRole) {
                // ==================== 1) USER / CITIZEN DASHBOARD ====================
                ActorRole.VILLAGE_RESIDENT -> {
                    when (currentTab) {
                        // Tab 0 (Prioritized Default): Safe Water Map + GPS Unfamiliar Village Radar
                        0 -> WaterRiskMapScreen(
                            selectedRole = selectedRole,
                            villages = villages,
                            waterSources = waterSources,
                            selectedSourceId = selectedWaterSourceId,
                            visitorRadar = visitorRadar,
                            isUsingRealGps = isUsingRealGps,
                            userLat = userLat,
                            userLon = userLon,
                            onRefreshDeviceGps = { viewModel.refreshDeviceGpsLocation() },
                            onSelectVisitorVillage = { v -> viewModel.simulateVisitorEnteringVillage(v) },
                            onSelectSource = { srcId -> viewModel.selectWaterSourceOnMap(srcId) },
                            onMarkSourceCleaned = { },
                            onJumpToCameraTestForSource = null
                        )

                        // Tab 1: Citizen Voice Symptom Triage (Marathi + English + NE Languages)
                        1 -> VoiceTriageAndQrScreen(
                            mode = VoiceAndQrScreenMode.CITIZEN_VOICE_ONLY,
                            villages = villages,
                            samples = custodySamples,
                            symptomReports = symptomReports,
                            isOfflineHillMode = isOfflineHillMode,
                            pendingOfflineCount = pendingOfflineItemsCount,
                            lastP2pSyncMessage = lastP2pSyncMessage,
                            onToggleOfflineMode = { viewModel.toggleOfflineHillMode() },
                            onSubmitVoiceTriage = { v, pDesc, age, lang, transcript, days, drinking, sunken, blood ->
                                viewModel.submitVoiceSymptomTriage(
                                    v, pDesc, age, lang, transcript, days, drinking, sunken, blood
                                )
                            },
                            onCollectQrSample = { _, _, _, _ -> },
                            onScanAdvanceSample = { _, _ -> },
                            onSimulateTamperCheck = { },
                            onTriggerP2pMeshSync = { },
                            onTriggerCloudGatewaySync = { }
                        )

                        // Tab 2: Citizen Verified Aadhaar Pass & Village Water Safety Guide
                        else -> CitizenProfileAndAlertsScreen(
                            account = currentAccount,
                            visitorRadar = visitorRadar,
                            villages = villages
                        )
                    }
                }

                // ==================== 2) ASHA WORKER DASHBOARD ====================
                ActorRole.ASHA_WORKER -> {
                    when (currentTab) {
                        // Tab 0: Camera Water Test Reader (#1)
                        0 -> CameraWaterTestScreen(
                            waterSources = waterSources,
                            preselectedSourceId = selectedWaterSourceId,
                            defaultTesterName = currentAccount.fullName,
                            onSaveCameraTest = { src, ph, turb, h2s, cl, tester ->
                                viewModel.recordCameraWaterTest(src, ph, turb, h2s, cl, tester)
                            },
                            onRequestCollectLabQrSample = { src ->
                                val village = villages.firstOrNull { it.id == src.villageId }
                                if (village != null) {
                                    viewModel.collectNewQrSample(
                                        village = village,
                                        sampleType = "पाणी नमुना (प्रयोगशाळा पुष्टीकरण) • Lab Confirmation Water Sample",
                                        sourceOrPatient = src.name,
                                        collectorName = currentAccount.fullName
                                    )
                                }
                                currentTab = 2
                            }
                        )

                        // Tab 1: Field Action Tasks (#4) & Flood Pre-Symptom Alerts (#2)
                        1 -> CommandOverviewScreen(
                            selectedRole = selectedRole,
                            villages = villages,
                            waterSources = waterSources,
                            actionTasks = actionTasks,
                            detectedClusters = detectedClusters,
                            visitorRadar = visitorRadar,
                            isUsingRealGps = isUsingRealGps,
                            floodThresholdMm = floodThresholdMm,
                            onUpdateFloodThreshold = { viewModel.updateFloodRainfallThreshold(it) },
                            onRefreshDeviceGps = { viewModel.refreshDeviceGpsLocation() },
                            onSelectVisitorVillage = { v -> viewModel.simulateVisitorEnteringVillage(v) },
                            onTriggerFloodCheck = { v, sim ->
                                viewModel.checkOrSimulateFloodPreSymptomWarning(v, sim)
                            },
                            onToggleTask = { task -> viewModel.toggleTaskCompletion(task) },
                            onDeleteTask = { task -> viewModel.deleteActionTask(task) },
                            onEditTask = { task, newTitle, newTeam ->
                                viewModel.updateExistingActionTask(task, newTitle, newTeam)
                            },
                            onCreateCustomTask = { v, src, title, team ->
                                viewModel.createCustomActionTask(v, src, title, team)
                            },
                            onNavigateToTab = { idx -> currentTab = idx },
                            onInspectSourceOnMap = { srcId -> viewModel.selectWaterSourceOnMap(srcId) }
                        )

                        // Tab 2: QR Sample Collection (#5), Voice Triage (#7) & Offline P2P Sync (#6)
                        2 -> VoiceTriageAndQrScreen(
                            mode = VoiceAndQrScreenMode.ASHA_FULL_FIELD_SYNC,
                            villages = villages,
                            samples = custodySamples,
                            symptomReports = symptomReports,
                            isOfflineHillMode = isOfflineHillMode,
                            pendingOfflineCount = pendingOfflineItemsCount,
                            lastP2pSyncMessage = lastP2pSyncMessage,
                            onToggleOfflineMode = { viewModel.toggleOfflineHillMode() },
                            onSubmitVoiceTriage = { v, pDesc, age, lang, transcript, days, drinking, sunken, blood ->
                                viewModel.submitVoiceSymptomTriage(
                                    v, pDesc, age, lang, transcript, days, drinking, sunken, blood
                                )
                            },
                            onCollectQrSample = { v, type, src, _ ->
                                viewModel.collectNewQrSample(v, type, src, currentAccount.fullName)
                            },
                            onScanAdvanceSample = { sample, handler ->
                                viewModel.scanAndAdvanceSampleCustody(sample, handler)
                            },
                            onSimulateTamperCheck = { sample ->
                                viewModel.simulateSampleTamperTest(sample)
                            },
                            onTriggerP2pMeshSync = { viewModel.triggerPeerToPeerMeshShare() },
                            onTriggerCloudGatewaySync = { viewModel.triggerGatewayCloudSync() }
                        )

                        // Tab 3: Water-Source Level Risk Map (#3)
                        else -> WaterRiskMapScreen(
                            selectedRole = selectedRole,
                            villages = villages,
                            waterSources = waterSources,
                            selectedSourceId = selectedWaterSourceId,
                            visitorRadar = visitorRadar,
                            isUsingRealGps = isUsingRealGps,
                            userLat = userLat,
                            userLon = userLon,
                            onRefreshDeviceGps = { viewModel.refreshDeviceGpsLocation() },
                            onSelectVisitorVillage = { v -> viewModel.simulateVisitorEnteringVillage(v) },
                            onSelectSource = { srcId -> viewModel.selectWaterSourceOnMap(srcId) },
                            onMarkSourceCleaned = { src ->
                                viewModel.markWaterSourceCleanedAndChlorinated(src, currentAccount.fullName)
                            },
                            onJumpToCameraTestForSource = { src ->
                                viewModel.selectWaterSourceOnMap(src.id)
                                currentTab = 0
                            }
                        )
                    }
                }

                // ==================== 3) LOCAL DOCTOR DASHBOARD ====================
                ActorRole.LOCAL_DOCTOR -> {
                    when (currentTab) {
                        // Tab 0: Clinical Triage Queue & Cluster Diagnosis (#7, #10)
                        0 -> DoctorClinicalDashboardScreen(
                            doctorAccount = currentAccount,
                            symptomReports = symptomReports,
                            detectedClusters = detectedClusters,
                            custodySamples = custodySamples,
                            onNavigateToLabQrTab = { currentTab = 1 },
                            onNavigateToMapTab = { currentTab = 2 }
                        )

                        // Tab 1: Lab Sample QR Chain-of-Custody Verification (#5)
                        1 -> VoiceTriageAndQrScreen(
                            mode = VoiceAndQrScreenMode.DOCTOR_LAB_QR_ONLY,
                            villages = villages,
                            samples = custodySamples,
                            symptomReports = symptomReports,
                            isOfflineHillMode = isOfflineHillMode,
                            pendingOfflineCount = pendingOfflineItemsCount,
                            lastP2pSyncMessage = lastP2pSyncMessage,
                            onToggleOfflineMode = { viewModel.toggleOfflineHillMode() },
                            onSubmitVoiceTriage = { _, _, _, _, _, _, _, _, _ -> },
                            onCollectQrSample = { _, _, _, _ -> },
                            onScanAdvanceSample = { sample, _ ->
                                viewModel.scanAndAdvanceSampleCustody(sample, currentAccount.fullName)
                            },
                            onSimulateTamperCheck = { sample ->
                                viewModel.simulateSampleTamperTest(sample)
                            },
                            onTriggerP2pMeshSync = { },
                            onTriggerCloudGatewaySync = { }
                        )

                        // Tab 2: Water-Source Level Risk Map (#3)
                        else -> WaterRiskMapScreen(
                            selectedRole = selectedRole,
                            villages = villages,
                            waterSources = waterSources,
                            selectedSourceId = selectedWaterSourceId,
                            visitorRadar = visitorRadar,
                            isUsingRealGps = isUsingRealGps,
                            userLat = userLat,
                            userLon = userLon,
                            onRefreshDeviceGps = { viewModel.refreshDeviceGpsLocation() },
                            onSelectVisitorVillage = { v -> viewModel.simulateVisitorEnteringVillage(v) },
                            onSelectSource = { srcId -> viewModel.selectWaterSourceOnMap(srcId) },
                            onMarkSourceCleaned = { }
                        )
                    }
                }

                // ==================== 4) PHC OFFICIAL DASHBOARD ====================
                ActorRole.PHC_OFFICIAL -> {
                    when (currentTab) {
                        // Tab 0: Command Center: Alert-to-Action Dispatcher (#4) & Flood Pre-Symptom Warning (#2)
                        0 -> CommandOverviewScreen(
                            selectedRole = selectedRole,
                            villages = villages,
                            waterSources = waterSources,
                            actionTasks = actionTasks,
                            detectedClusters = detectedClusters,
                            visitorRadar = visitorRadar,
                            isUsingRealGps = isUsingRealGps,
                            floodThresholdMm = floodThresholdMm,
                            onUpdateFloodThreshold = { viewModel.updateFloodRainfallThreshold(it) },
                            onRefreshDeviceGps = { viewModel.refreshDeviceGpsLocation() },
                            onSelectVisitorVillage = { v -> viewModel.simulateVisitorEnteringVillage(v) },
                            onTriggerFloodCheck = { v, sim ->
                                viewModel.checkOrSimulateFloodPreSymptomWarning(v, sim)
                            },
                            onToggleTask = { task -> viewModel.toggleTaskCompletion(task) },
                            onDeleteTask = { task -> viewModel.deleteActionTask(task) },
                            onEditTask = { task, newTitle, newTeam ->
                                viewModel.updateExistingActionTask(task, newTitle, newTeam)
                            },
                            onCreateCustomTask = { v, src, title, team ->
                                viewModel.createCustomActionTask(v, src, title, team)
                            },
                            onNavigateToTab = { idx -> currentTab = idx },
                            onInspectSourceOnMap = { srcId -> viewModel.selectWaterSourceOnMap(srcId) }
                        )

                        // Tab 1: Outbreak What-If Simulator (#9), Cluster Detection (#10) & Non-Medical Early Signals (#8)
                        1 -> OutbreakIntelligenceScreen(
                            villages = villages,
                            detectedClusters = detectedClusters,
                            earlySignals = earlySignals,
                            onUpdateNonMedicalSignals = { v, ors, school, asha ->
                                viewModel.updateNonMedicalSignals(v, ors, school, asha)
                            }
                        )

                        // Tab 2: District Water-Source Risk Map (#3)
                        else -> WaterRiskMapScreen(
                            selectedRole = selectedRole,
                            villages = villages,
                            waterSources = waterSources,
                            selectedSourceId = selectedWaterSourceId,
                            visitorRadar = visitorRadar,
                            isUsingRealGps = isUsingRealGps,
                            userLat = userLat,
                            userLon = userLon,
                            onRefreshDeviceGps = { viewModel.refreshDeviceGpsLocation() },
                            onSelectVisitorVillage = { v -> viewModel.simulateVisitorEnteringVillage(v) },
                            onSelectSource = { srcId -> viewModel.selectWaterSourceOnMap(srcId) },
                            onMarkSourceCleaned = { src ->
                                viewModel.markWaterSourceCleanedAndChlorinated(src, currentAccount.fullName)
                            }
                        )
                    }
                }
            }
        }
    }
}
