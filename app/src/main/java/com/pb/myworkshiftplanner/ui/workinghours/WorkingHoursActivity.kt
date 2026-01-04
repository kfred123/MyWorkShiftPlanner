package com.pb.myworkshiftplanner.ui.workinghours

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pb.myworkshiftplanner.ui.theme.MyWorkShiftPlannerTheme
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class WorkingHoursActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWorkShiftPlannerTheme {
                WorkingHoursScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingHoursScreen(
    onBackClick: () -> Unit,
    viewModel: WorkingHoursViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf<YearMonth?>(null) }
    var selectedHours by remember { mutableStateOf<Double?>(null) }

    // Scroll to current month on first load
    LaunchedEffect(uiState.monthsList) {
        if (uiState.monthsList.isNotEmpty()) {
            val currentMonth = YearMonth.now()
            val index = uiState.monthsList.indexOfFirst { it.yearMonth == currentMonth }
            if (index >= 0) {
                listState.scrollToItem(index.coerceAtLeast(0))
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Wochenarbeitszeit") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.monthsList) { monthData ->
                    MonthWorkingHoursItem(
                        monthData = monthData,
                        onClick = {
                            selectedMonth = monthData.yearMonth
                            selectedHours = monthData.hours
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog && selectedMonth != null) {
        EditWorkingHoursDialog(
            yearMonth = selectedMonth!!,
            currentHours = selectedHours,
            onDismiss = {
                showEditDialog = false
                selectedMonth = null
                selectedHours = null
            },
            onSave = { hours ->
                viewModel.saveWorkingHours(selectedMonth!!, hours)
                showEditDialog = false
                selectedMonth = null
                selectedHours = null
            },
            onDelete = {
                viewModel.deleteWorkingHours(selectedMonth!!)
                showEditDialog = false
                selectedMonth = null
                selectedHours = null
            }
        )
    }
}

@Composable
fun MonthWorkingHoursItem(
    monthData: MonthWorkingHours,
    onClick: () -> Unit
) {
    val isCurrentMonth = monthData.yearMonth == YearMonth.now()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMonth)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
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
                    text = monthData.yearMonth.format(monthFormatter)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                )

                if (monthData.hours != null) {
                    Text(
                        text = if (monthData.isManual) "Manuell eingegeben" else "Vom Vormonat übernommen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Nicht festgelegt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (monthData.hours != null) {
                Text(
                    text = "${monthData.hours} h",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (monthData.isManual)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EditWorkingHoursDialog(
    yearMonth: YearMonth,
    currentHours: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var hoursText by remember { mutableStateOf(currentHours?.toString() ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = yearMonth.format(monthFormatter)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hoursText,
                    onValueChange = {
                        hoursText = it
                        errorMessage = null
                    },
                    label = { Text("Wochenarbeitszeit (Stunden)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (yearMonth >= YearMonth.now()) {
                        "Hinweis: Die Änderung wird für alle zukünftigen Monate übernommen."
                    } else {
                        "Hinweis: Die Änderung gilt nur für diesen Monat."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentHours != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Löschen")
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            val hours = hoursText.replace(',', '.').toDoubleOrNull()
                            if (hours == null || hours <= 0) {
                                errorMessage = "Bitte geben Sie eine gültige Stundenzahl ein"
                            } else {
                                onSave(hours)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}

