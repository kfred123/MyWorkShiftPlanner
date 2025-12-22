package com.pb.myworkshiftplanner.ui.shifts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pb.myworkshiftplanner.data.Shift
import com.pb.myworkshiftplanner.data.ShiftDatabase
import com.pb.myworkshiftplanner.data.ShiftRepository
import com.pb.myworkshiftplanner.ui.theme.MyWorkShiftPlannerTheme
import java.util.Locale

class ShiftManagementActivity : ComponentActivity() {

    private val viewModel: ShiftViewModel by viewModels {
        ShiftViewModelFactory(
            ShiftRepository(
                ShiftDatabase.getDatabase(applicationContext).shiftDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWorkShiftPlannerTheme {
                ShiftManagementScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementScreen(
    viewModel: ShiftViewModel,
    onBackPressed: () -> Unit
) {
    val shifts by viewModel.allShifts.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<Shift?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schichtverwaltung") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingShift = null
                    showDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Schicht hinzufügen")
            }
        }
    ) { paddingValues ->
        if (shifts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Schichten vorhanden.\nTippen Sie auf +, um eine hinzuzufügen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shifts) { shift ->
                    ShiftItem(
                        shift = shift,
                        onEdit = {
                            editingShift = shift
                            showDialog = true
                        },
                        onDelete = {
                            viewModel.deleteShift(shift)
                        }
                    )
                }
            }
        }

        if (showDialog) {
            ShiftDialog(
                shift = editingShift,
                onDismiss = { showDialog = false },
                onSave = { shift ->
                    if (editingShift != null) {
                        viewModel.updateShift(shift)
                    } else {
                        viewModel.insertShift(shift)
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ShiftItem(
    shift: Shift,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shift.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${shift.beginTime} - ${shift.endTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pause: ${shift.breakDuration} Min.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Bearbeiten",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftDialog(
    shift: Shift?,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit
) {
    var name by remember { mutableStateOf(shift?.name ?: "") }
    var beginTime by remember { mutableStateOf(shift?.beginTime ?: "08:00") }
    var endTime by remember { mutableStateOf(shift?.endTime ?: "16:00") }
    var breakDuration by remember { mutableStateOf(shift?.breakDuration?.toString() ?: "30") }

    var showBeginTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (shift == null) "Neue Schicht" else "Schicht bearbeiten")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = beginTime,
                    onValueChange = { },
                    label = { Text("Beginn") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBeginTimePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { },
                    label = { Text("Ende") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndTimePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = breakDuration,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            breakDuration = it
                        }
                    },
                    label = { Text("Pause (Minuten)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && breakDuration.isNotBlank()) {
                        val newShift = Shift(
                            id = shift?.id ?: 0,
                            name = name,
                            beginTime = beginTime,
                            endTime = endTime,
                            breakDuration = breakDuration.toIntOrNull() ?: 0
                        )
                        onSave(newShift)
                    }
                },
                enabled = name.isNotBlank() && breakDuration.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    if (showBeginTimePicker) {
        TimePickerDialog(
            initialTime = beginTime,
            onDismiss = { showBeginTimePicker = false },
            onConfirm = { time ->
                beginTime = time
                showBeginTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { time ->
                endTime = time
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val timeParts = initialTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zeit auswählen",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val hour = String.format(Locale.getDefault(), "%02d", timePickerState.hour)
                            val minute = String.format(Locale.getDefault(), "%02d", timePickerState.minute)
                            onConfirm("$hour:$minute")
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

