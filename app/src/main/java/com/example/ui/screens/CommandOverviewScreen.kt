package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.SafetyBadgePill
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * Tailored Action Tasks (#4) & Flood Pre-Symptom Warning (#2) Screen
 * Implements all Marathi PDF details from Pages 2–5:
 * - ASHA: "आजची कामे" checklist
 * - Officer: Pending / In progress / Done status & percentage per village
 * - Population-based ORS & Chlorine calculation
 * - Officer ability to Add, Edit, and Remove tasks
 * - Configurable Flood Rainfall Threshold in mm + "Flood alert" label + precautionary message
 */
@Composable
fun CommandOverviewScreen(
    selectedRole: ActorRole,
    villages: List<VillageEntity>,
    waterSources: List<WaterSourceEntity>,
    actionTasks: List<ActionTaskEntity>,
    detectedClusters: List<DetectedCluster>,
    visitorRadar: VisitorRadarStatus?,
    isUsingRealGps: Boolean,
    floodThresholdMm: Double = 110.0,
    onUpdateFloodThreshold: (Double) -> Unit = {},
    onRefreshDeviceGps: () -> Unit,
    onSelectVisitorVillage: (VillageEntity) -> Unit,
    onTriggerFloodCheck: (VillageEntity, Boolean) -> Unit,
    onToggleTask: (ActionTaskEntity) -> Unit,
    onDeleteTask: (ActionTaskEntity) -> Unit = {},
    onEditTask: (ActionTaskEntity, String, String) -> Unit = { _, _, _ -> },
    onCreateCustomTask: (VillageEntity, String, String, String) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onInspectSourceOnMap: (String) -> Unit
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<ActionTaskEntity?>(null) }
    val isPhcOfficial = selectedRole == ActorRole.PHC_OFFICIAL

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("command_overview_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Actor-Tailored Mission Header
        item {
            RoleMissionBriefCard(
                role = selectedRole,
                pendingTasksCount = actionTasks.count { it.taskStatus != "DONE" },
                redSourcesCount = waterSources.count { it.safetyLevel == "CONTAMINATED" },
                activeFloodVillagesCount = villages.count { it.floodWarningActive }
            )
        }

        // 2. FEATURE #4 (Pages 4–5): Alert-to-Action Engine
        item {
            AlertToActionHeaderCard(
                tasks = actionTasks,
                isPhcOfficial = isPhcOfficial,
                onOpenNewTaskDialog = { showAddTaskDialog = true }
            )
        }

        items(actionTasks, key = { it.id }) { task ->
            ActionTaskRowCard(
                task = task,
                isOfficer = isPhcOfficial,
                onCycleTaskStatus = { onToggleTask(task) },
                onEditTask = { taskBeingEdited = task },
                onDeleteTask = { onDeleteTask(task) }
            )
        }

        // 3. FEATURE #2 (Pages 2–3): Flood-Triggered Pre-Symptom Warning Card
        item {
            FloodPreSymptomWarningSection(
                villages = villages,
                isOfficer = isPhcOfficial,
                floodThresholdMm = floodThresholdMm,
                onUpdateFloodThreshold = onUpdateFloodThreshold,
                onTriggerFloodCheck = onTriggerFloodCheck
            )
        }
    }

    if (showAddTaskDialog && villages.isNotEmpty()) {
        CreateActionTaskDialog(
            villages = villages,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { v, src, title, team ->
                onCreateCustomTask(v, src, title, team)
                showAddTaskDialog = false
            }
        )
    }

    taskBeingEdited?.let { editTarget ->
        EditActionTaskDialog(
            task = editTarget,
            onDismiss = { taskBeingEdited = null },
            onConfirm = { newTitle, newTeam ->
                onEditTask(editTarget, newTitle, newTeam)
                taskBeingEdited = null
            }
        )
    }
}

@Composable
private fun RoleMissionBriefCard(
    role: ActorRole,
    pendingTasksCount: Int,
    redSourcesCount: Int,
    activeFloodVillagesCount: Int
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "स्वतंत्र विभाग डॅशबोर्ड • ${role.ministryCode}",
                style = MaterialTheme.typography.labelSmall,
                color = CivicTeal
            )
            Text(
                text = role.badgeTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = role.marathiSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatSummaryMiniBox(
                    label = "दूषित स्रोत (Red)",
                    value = "$redSourcesCount",
                    accentColor = ContaminatedRed,
                    modifier = Modifier.weight(1f)
                )
                StatSummaryMiniBox(
                    label = "Flood alert",
                    value = "$activeFloodVillagesCount गावे",
                    accentColor = RiskyAmber,
                    modifier = Modifier.weight(1f)
                )
                StatSummaryMiniBox(
                    label = "आजची कामे",
                    value = "$pendingTasksCount बाकी",
                    accentColor = SovereignNavy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatSummaryMiniBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.09f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
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
private fun AlertToActionHeaderCard(
    tasks: List<ActionTaskEntity>,
    isPhcOfficial: Boolean,
    onOpenNewTaskDialog: () -> Unit
) {
    val pendingCount = tasks.count { it.taskStatus == "PENDING" }
    val inProgressCount = tasks.count { it.taskStatus == "IN_PROGRESS" }
    val doneCount = tasks.count { it.taskStatus == "DONE" || it.isCompleted }
    val total = tasks.size.coerceAtLeast(1)
    val percentDone = ((doneCount.toFloat() / total.toFloat()) * 100).roundToInt()

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
                        text = "वैशिष्ट्य #४ • ALERT-TO-ACTION ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SovereignNavy
                    )
                    Text(
                        text = if (isPhcOfficial) {
                            "प्रत्येक गावासाठी प्रगती: $percentDone% पूर्ण (Pending: $pendingCount | In Progress: $inProgressCount | Done: $doneCount)"
                        } else {
                            "ASHA कार्यकर्त्यासाठी 'आजची कामे' Checklist ($doneCount/${tasks.size} पूर्ण)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalButton(
                    onClick = onOpenNewTaskDialog,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("dispatch_action_task_btn")
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("काम वाढवा (Add)", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "गाव लाल झाले की लोकसंख्येवर / घरांच्या संख्येवर आधारित ORS packets व chlorine tablets ची यादी आपोआप तयार होते. माहिती बदलण्यासाठी (Edit) किंवा काढण्यासाठी (Delete) खालील पर्याय वापरा.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { doneCount.toFloat() / total.toFloat() },
                color = SafeGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ActionTaskRowCard(
    task: ActionTaskEntity,
    isOfficer: Boolean,
    onCycleTaskStatus: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit
) {
    val (statusLabel, statusColor, bgCol) = when (task.taskStatus) {
        "DONE" -> Triple("DONE • पूर्ण", SafeGreen, SafeGreenBg.copy(alpha = 0.4f))
        "IN_PROGRESS" -> Triple("IN PROGRESS • सुरू आहे", RiskyAmber, RiskyAmberBg.copy(alpha = 0.35f))
        else -> Triple("PENDING • बाकी", ContaminatedRed, MaterialTheme.colorScheme.surface)
    }

    Surface(
        color = bgCol,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable { onCycleTaskStatus() }
            .testTag("action_task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = task.taskStatus == "DONE",
                    onCheckedChange = { onCycleTaskStatus() },
                    colors = CheckboxDefaults.colors(checkedColor = SafeGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${task.villageName} • ${task.sourceName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.taskStatus == "DONE") TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Team: ${task.assignedTeam}" +
                                    (task.completedBy?.let { " • $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = SovereignNavy,
                            modifier = Modifier.weight(1f)
                        )

                        // Page 5 ("लक्षात ठेवा"): "कामांची यादी officer ला बदलता आली पाहिजे (काम वाढवणे किंवा काढणे)"
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = onCycleTaskStatus,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("स्थिती बदला", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = onEditTask,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Task",
                                    tint = SovereignNavy,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onDeleteTask,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Task",
                                    tint = ContaminatedRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloodPreSymptomWarningSection(
    villages: List<VillageEntity>,
    isOfficer: Boolean,
    floodThresholdMm: Double,
    onUpdateFloodThreshold: (Double) -> Unit,
    onTriggerFloodCheck: (VillageEntity, Boolean) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Thunderstorm,
                    contentDescription = "Flood-triggered Pre-symptom Warning",
                    tint = RiskyAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "वैशिष्ट्य #२ • FLOOD-TRIGGERED PRE-SYMPTOM WARNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = RiskyAmber
                    )
                    Text(
                        text = "कोणी आजारी पडायच्या आधीच पाऊस व पुराचा इशारा",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Page 3 Requirement: "Threshold ची किंमत स्थानिक आरोग्य किंवा आपत्ती विभागाकडून विचारून ठरवावी; ती settings मध्ये बदलता येईल अशी ठेवावी."
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "पावसाची मर्यादा (Configurable Flood Threshold Setting): ${floodThresholdMm.roundToInt()} मिमी / दिवस",
                        style = MaterialTheme.typography.labelLarge,
                        color = SovereignNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = floodThresholdMm.toFloat(),
                        onValueChange = { onUpdateFloodThreshold(it.toDouble()) },
                        valueRange = 70f..180f
                    )
                    Text(
                        text = "लक्षात ठेवा (Page 3): पावसाचा data अंदाज असतो, त्यामुळे संदेशात \"पाण्याचे स्रोत तपासा आणि ORS तयार ठेवा\" असे सावध शब्द वापरले आहेत, \"रोगराई येणार\" असे नाही.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            villages.take(4).forEach { village ->
                val isFlood = village.floodWarningActive || village.dailyRainfallMm >= floodThresholdMm
                Surface(
                    color = if (isFlood) RiskyAmberBg.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            1.dp,
                            if (isFlood) RiskyAmber.copy(alpha = 0.5f) else Color.Transparent,
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${village.name} (${village.stateName})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isFlood) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        // Page 3: Officer च्या dashboard वर गावाचे नाव आणि "Flood alert" असे लेबल दिसते
                                        Surface(
                                            color = ContaminatedRed,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Flood alert",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "नदी खोरे: ${village.riverBasin} • पाऊस: ${village.dailyRainfallMm} मिमी (मर्यादा: ${floodThresholdMm.roundToInt()} मिमी)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SafetyBadgePill(safetyLevel = village.overallRisk)
                        }

                        if (isFlood) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = ContaminatedRedBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = "ASHA Broadcast",
                                        tint = ContaminatedRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ASHA Message: \"तुमच्या भागात पूर आहे. पाण्याचे स्रोत तपासा आणि ORS तयार ठेवा.\"",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ContaminatedRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onTriggerFloodCheck(village, false) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 36.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open-Meteo API", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = { onTriggerFloodCheck(village, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = RiskyAmber),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 36.dp)
                                    .testTag("simulate_flood_${village.id}")
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Flood Alert चाचणी", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateActionTaskDialog(
    villages: List<VillageEntity>,
    onDismiss: () -> Unit,
    onConfirm: (VillageEntity, String, String, String) -> Unit
) {
    var selectedVillage by remember { mutableStateOf(villages.first()) }
    val suggestedOrs = remember(selectedVillage) { ((selectedVillage.population * 0.06).roundToInt() / 10) * 10 }
    val suggestedChlorine = remember(selectedVillage) { ((selectedVillage.population * 0.015).roundToInt() / 5) * 5 }

    var sourceName by remember { mutableStateOf("Community Spring #1") }
    var taskTitle by remember(selectedVillage) {
        mutableStateOf("$suggestedOrs ORS packets आणि $suggestedChlorine chlorine tablets पाठवा (लोकसंख्या ${selectedVillage.population}). ASHA team 2 ला नेमा. आधी झऱ्याला भेट द्या.")
    }
    var assignedTeam by remember { mutableStateOf("ASHA Team 2 & Block Health Worker") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("नवीन काम जोडा (लोकसंख्येवर आधारित) • Add Task (#4)", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "गाव निवडा (लोकसंख्येनुसार ORS व Chlorine आपोआप मोजले जाते):",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    villages.forEach { v ->
                        FilterChip(
                            selected = v.id == selectedVillage.id,
                            onClick = { selectedVillage = v },
                            label = { Text("${v.name.substringBefore(" ")} (Pop: ${v.population})") }
                        )
                    }
                }
                OutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text("पाणी स्रोताचे नाव • Water Source Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("कामाचा तपशील • Task Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = assignedTeam,
                    onValueChange = { assignedTeam = it },
                    label = { Text("नेमलेले पथक • Assigned Team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskTitle.isNotBlank()) {
                        onConfirm(selectedVillage, sourceName, taskTitle, assignedTeam)
                    }
                }
            ) {
                Text("यादीत जोडा / Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करा / Cancel")
            }
        }
    )
}

@Composable
private fun EditActionTaskDialog(
    task: ActionTaskEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var team by remember { mutableStateOf(task.assignedTeam) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("काम बदला • Edit Task (#4)", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("कामाचा मजकूर (Task Steps)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = team,
                    onValueChange = { team = it },
                    label = { Text("नेमलेली व्यक्ती / Team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, team) }) {
                Text("जतन करा / Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करा")
            }
        }
    )
}
