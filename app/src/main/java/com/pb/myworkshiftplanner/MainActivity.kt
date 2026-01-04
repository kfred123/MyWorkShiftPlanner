package com.pb.myworkshiftplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pb.myworkshiftplanner.data.ActualWorkTime
import com.pb.myworkshiftplanner.data.Shift
import com.pb.myworkshiftplanner.ui.calendar.CalendarViewModel
import com.pb.myworkshiftplanner.ui.shifts.ShiftManagementActivity
import com.pb.myworkshiftplanner.ui.theme.MyWorkShiftPlannerTheme
import com.pb.myworkshiftplanner.utils.TimeCalculator
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWorkShiftPlannerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CalendarViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("My Work Shift Planner") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menü"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Schichtverwaltung") },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, ShiftManagementActivity::class.java)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Wochenarbeitszeit") },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, com.pb.myworkshiftplanner.ui.workinghours.WorkingHoursActivity::class.java)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Einstellungen") },
                            onClick = {
                                showMenu = false
                                context.startActivity(
                                    Intent(context, com.pb.myworkshiftplanner.ui.settings.SettingsActivity::class.java)
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                CalendarHeader(
                    currentMonth = uiState.currentMonth,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                CalendarGrid(
                    currentMonth = uiState.currentMonth,
                    assignments = uiState.assignments,
                    onDateClick = { date -> viewModel.selectDate(date) }
                )
            }

            // Monthly Summary Section
            MonthlySummarySection(
                summary = uiState.monthlySummary,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (uiState.showDayDialog && uiState.selectedDate != null) {
            DayDialog(
                date = uiState.selectedDate!!,
                shifts = uiState.allShifts,
                currentShift = viewModel.getShiftForDate(uiState.selectedDate!!),
                actualWorkTime = viewModel.getActualWorkTimeForDate(uiState.selectedDate!!),
                onDismiss = { viewModel.dismissDialog() },
                onShiftSelected = { shiftId -> viewModel.assignShift(shiftId) },
                onActualTimeSaved = { start, end, breakMin ->
                    viewModel.saveActualWorkTime(start, end, breakMin)
                },
                onActualTimeDeleted = { viewModel.deleteActualWorkTime() }
            )
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Month"
            )
        }

        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${currentMonth.year}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Month"
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    assignments: Map<String, Shift>,
    onDateClick: (LocalDate) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val daysOfWeek = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    // Calculate the first day of the month and how many days to show
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // Monday = 1, Sunday = 7
    val daysInMonth = currentMonth.lengthOfMonth()

    // Calculate empty cells before the first day
    val emptyCellsBefore = firstDayOfWeek - 1

    Column {
        // Week day headers
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            items(daysOfWeek) { day ->
                Text(
                    text = day,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Calendar days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            // Empty cells before first day
            items(emptyCellsBefore) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            // Days of the month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val date = currentMonth.atDay(day)
                val dateString = date.format(dateFormatter)
                val shift = assignments[dateString]
                val isToday = date == LocalDate.now()

                CalendarDayCell(
                    day = day,
                    shift = shift,
                    isToday = isToday,
                    onClick = { onDateClick(date) }
                )
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    shift: Shift?,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (shift != null)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                fontSize = 16.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (shift != null) {
                Text(
                    text = shift.name,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MonthlySummarySection(
    summary: com.pb.myworkshiftplanner.ui.calendar.MonthlySummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Monatsübersicht",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Previous month balance
            SummaryRow(
                label = "Über-/Minusstunden Vormonat",
                value = TimeCalculator.formatMinutesToHoursString(summary.previousMonthBalance),
                valueColor = when {
                    summary.previousMonthBalance > 0 -> MaterialTheme.colorScheme.primary
                    summary.previousMonthBalance < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Target hours
            SummaryRow(
                label = "Soll-Arbeitszeit",
                value = TimeCalculator.formatMinutesToHoursString(summary.targetHours).replace("+", ""),
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Planned hours
            SummaryRow(
                label = "Geplante Arbeitszeit",
                value = TimeCalculator.formatMinutesToHoursString(summary.plannedHours).replace("+", ""),
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Difference
            SummaryRow(
                label = "Differenz (Plan - Soll)",
                value = TimeCalculator.formatMinutesToHoursString(summary.difference),
                valueColor = when {
                    summary.difference > 0 -> MaterialTheme.colorScheme.primary
                    summary.difference < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                isBold = true
            )
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
fun DayDialog(
    date: LocalDate,
    shifts: List<Shift>,
    currentShift: Shift?,
    actualWorkTime: ActualWorkTime?,
    onDismiss: () -> Unit,
    onShiftSelected: (Long?) -> Unit,
    onActualTimeSaved: (String, String, Int) -> Unit,
    onActualTimeDeleted: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val isPastOrToday = !date.isAfter(LocalDate.now())

    // State for actual work time editing
    var startTime by remember(actualWorkTime, currentShift) {
        mutableStateOf(actualWorkTime?.actualStartTime ?: currentShift?.beginTime ?: "08:00")
    }
    var endTime by remember(actualWorkTime, currentShift) {
        mutableStateOf(actualWorkTime?.actualEndTime ?: currentShift?.endTime ?: "16:00")
    }
    var breakMinutes by remember(actualWorkTime, currentShift) {
        mutableStateOf((actualWorkTime?.actualBreakDuration ?: currentShift?.breakDuration ?: 30).toString())
    }

    var selectedShiftId by remember(currentShift) { mutableStateOf(currentShift?.id) }
    var showShiftSelection by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "${date.format(dateFormatter)}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Shift Planning Section
                Text(
                    text = "Geplante Schicht",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = { showShiftSelection = !showShiftSelection },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = shifts.find { it.id == selectedShiftId }?.name ?: "Keine Schicht",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showShiftSelection)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }

                if (showShiftSelection) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column {
                            // Option to remove shift
                            ListItem(
                                headlineContent = { Text("Keine Schicht") },
                                modifier = Modifier.clickable {
                                    selectedShiftId = null
                                    onShiftSelected(null)
                                    showShiftSelection = false
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (selectedShiftId == null)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            shifts.forEach { shift ->
                                ListItem(
                                    headlineContent = { Text(shift.name) },
                                    supportingContent = {
                                        Text("${shift.beginTime} - ${shift.endTime}, Pause: ${shift.breakDuration} min")
                                    },
                                    modifier = Modifier.clickable {
                                        selectedShiftId = shift.id
                                        onShiftSelected(shift.id)
                                        showShiftSelection = false
                                        // Update actual time fields with shift data if not already set
                                        if (actualWorkTime == null) {
                                            startTime = shift.beginTime
                                            endTime = shift.endTime
                                            breakMinutes = shift.breakDuration.toString()
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (selectedShiftId == shift.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                // Actual Work Time Section (only for past/today)
                if (isPastOrToday) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tatsächliche Arbeitszeit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Beginn") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Ende") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = breakMinutes,
                        onValueChange = { breakMinutes = it },
                        label = { Text("Pause (min)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Calculate and show overtime if there's a planned shift
                    if (currentShift != null && startTime.isNotBlank() && endTime.isNotBlank() && breakMinutes.isNotBlank()) {
                        val breakInt = breakMinutes.toIntOrNull() ?: 0
                        val overtime = runCatching {
                            TimeCalculator.calculateOvertime(
                                currentShift.beginTime,
                                currentShift.endTime,
                                currentShift.breakDuration,
                                startTime,
                                endTime,
                                breakInt
                            )
                        }.getOrNull()

                        if (overtime != null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        overtime > 0 -> MaterialTheme.colorScheme.primaryContainer
                                        overtime < 0 -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "Überstunden",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = TimeCalculator.formatMinutesToHoursString(overtime),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            overtime > 0 -> "Mehrarbeit"
                                            overtime < 0 -> "Fehlzeit"
                                            else -> "Planmäßig"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isPastOrToday && startTime.isNotBlank() && endTime.isNotBlank()) {
                TextButton(
                    onClick = {
                        val breakInt = breakMinutes.toIntOrNull() ?: 0
                        onActualTimeSaved(startTime, endTime, breakInt)
                        onDismiss()
                    }
                ) {
                    Text("Speichern")
                }
            }
        },
        dismissButton = {
            Row {
                if (isPastOrToday && actualWorkTime != null) {
                    TextButton(onClick = {
                        onActualTimeDeleted()
                    }) {
                        Text("Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

