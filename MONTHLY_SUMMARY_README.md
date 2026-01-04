# Monthly Summary Feature

## Overview
The main calendar screen now displays a monthly summary at the bottom showing:
1. **Über-/Minusstunden Vormonat** (Previous month overtime/deficit)
2. **Soll-Arbeitszeit** (Target working hours for current month)
3. **Geplante Arbeitszeit** (Planned working hours for current month)
4. **Differenz (Plan - Soll)** (Difference between planned and target hours)

## Display Layout

```
┌─────────────────────────────────────────┐
│  Calendar View                          │
│  (Scrollable)                           │
│                                         │
├─────────────────────────────────────────┤
│  Monatsübersicht                        │
│  ─────────────────────────────────────  │
│  Über-/Minusstunden Vormonat    +2:30 h │ ← Green if positive
│  ─────────────────────────────────────  │
│  Soll-Arbeitszeit              160:00 h │
│  Geplante Arbeitszeit          162:30 h │
│  ─────────────────────────────────────  │
│  Differenz (Plan - Soll)        +2:30 h │ ← Bold, colored
└─────────────────────────────────────────┘
```

## Calculations

### 1. Previous Month Balance (Über-/Minusstunden Vormonat)
**Formula:** Actual Work Time - Target Hours for Previous Month

**Logic:**
- For each day in the previous month:
  - If **actual work time** exists → use actual time
  - If **no actual time** but date is in the past and shift is assigned → use planned shift time
  - Otherwise → 0 minutes
- Compare total actual/worked minutes against target hours for that month
- **Positive value** = overtime (worked more than target)
- **Negative value** = deficit (worked less than target)

### 2. Target Working Hours (Soll-Arbeitszeit)
**Formula:** (Weekly Hours ÷ 5) × Working Days in Month

**Logic:**
- Get weekly working hours for the current month from WorkingHours table
- If not set, inherit from previous month
- Count working days (Monday-Friday, excluding weekends) in current month
- Calculate: `(weeklyHours / 5) * workingDays`
- Default weekly hours: 40.0 if not set

**Example:**
- Weekly hours: 40.0
- Working days in January 2026: 22 days
- Target: (40 / 5) × 22 = 176 hours = 176:00 h

### 3. Planned Working Hours (Geplante Arbeitszeit)
**Formula:** Sum of all planned shift durations in current month

**Logic:**
- For each shift assignment in the current month:
  - Calculate shift duration: `endTime - startTime - breakDuration`
  - Sum all shift durations
- Only counts shifts that are actually assigned to days

**Example:**
- 5 shifts of 8 hours each
- Planned: 5 × 8 = 40:00 h

### 4. Difference (Differenz)
**Formula:** Planned Hours - Target Hours

**Logic:**
- **Positive value** = planned more than target (will have overtime)
- **Negative value** = planned less than target (will have deficit)
- **Zero** = exactly on target

## Color Coding

### Previous Month Balance
- **Green (Primary)**: Positive balance (overtime)
- **Red (Error)**: Negative balance (deficit)
- **Gray**: Zero balance

### Difference
- **Green (Primary)**: Positive difference (planned more than needed)
- **Red (Error)**: Negative difference (planned less than needed)
- **Gray**: Zero difference (exactly on target)
- **Bold text** for emphasis

## Implementation Details

### Files Modified

#### 1. CalendarViewModel.kt
**New Data Class:**
```kotlin
data class MonthlySummary(
    val previousMonthBalance: Int = 0, // in minutes
    val targetHours: Int = 0,          // in minutes
    val plannedHours: Int = 0,         // in minutes
    val difference: Int = 0             // in minutes
)
```

**New Methods:**
- `loadMonthlySummary()` - Triggers calculation when data changes
- `calculateMonthlySummary(month: YearMonth)` - Main calculation coordinator
- `calculateMonthBalance(month: YearMonth)` - Calculates previous month overtime/deficit
- `calculateTargetHours(month: YearMonth)` - Calculates target based on weekly hours
- `calculatePlannedHours(month: YearMonth)` - Sums planned shift durations

**Dependencies Added:**
- `WorkingHoursRepository` for accessing weekly working hours
- `TimeCalculator` for time calculations
- `DayOfWeek` for counting working days

#### 2. MainActivity.kt
**New Composables:**
- `MonthlySummarySection()` - Main summary card display
- `SummaryRow()` - Individual row for each metric

**Layout Changes:**
- Main screen split into two sections:
  - Scrollable calendar view (weight = 1f)
  - Fixed summary section at bottom (padding = 16.dp)

## Data Flow

```
User Changes Month
       ↓
CalendarViewModel.nextMonth() / previousMonth()
       ↓
loadMonth() called
       ↓
Assignments & ActualWorkTimes loaded (Flow)
       ↓
loadMonthlySummary() triggered
       ↓
calculateMonthlySummary(currentMonth)
       ↓
├─ calculateMonthBalance(previousMonth)
│  └─ Gets actual work times + shift assignments
│     └─ Compares against target hours
│
├─ calculateTargetHours(currentMonth)
│  └─ Gets weekly hours from WorkingHours
│     └─ Counts working days (Mon-Fri)
│        └─ Calculates: (weeklyHours / 5) * workingDays
│
├─ calculatePlannedHours(currentMonth)
│  └─ Gets all shift assignments
│     └─ Sums shift durations
│
└─ Returns MonthlySummary
       ↓
UI State updated
       ↓
MonthlySummarySection renders
```

## Integration with Existing Features

### Google Calendar Sync
- Summary calculations are independent of calendar sync
- Only uses local database data
- Calendar sync only affects shift assignments

### Working Hours Management
- Summary automatically updates when weekly hours change
- Uses working hours inheritance (future months inherit from past)
- Default to 40.0 hours if not set

### Actual Time Tracking
- Previous month balance uses actual work times when available
- Falls back to planned shift times for past days without actual time
- Ignores future dates in previous month calculation

## Time Format

All times displayed using `TimeCalculator.formatMinutesToHoursString()`:
- Format: `+HH:MM h` or `-HH:MM h`
- Examples:
  - `+2:30 h` = 2 hours 30 minutes overtime
  - `-0:45 h` = 45 minutes deficit
  - `+160:00 h` = 160 hours

For target and planned (no sign needed):
- Remove the `+` prefix for display
- Format: `HH:MM h`

## Performance Considerations

### Calculation Triggers
- Calculations only run when:
  - Month changes (next/previous)
  - Assignments change (Flow updates)
  - Actual work times change (Flow updates)

### Optimization
- Uses Kotlin Flow for reactive updates
- Calculations run in viewModelScope (background)
- Results cached in UI state
- Only recalculates when necessary

### Database Queries
- Efficient range queries for assignments and actual times
- Single query for working hours by month
- No nested loops for date iteration (uses LocalDate iteration)

## User Benefits

1. **Quick Overview**: See monthly status at a glance without navigating
2. **Previous Month Context**: Know if starting with overtime or deficit
3. **Planning Aid**: See if planned shifts meet target hours
4. **Visual Feedback**: Color coding helps identify issues quickly
5. **Always Visible**: Summary stays at bottom, no scrolling needed

## Future Enhancements

Potential improvements:
- Add "Current Month Balance" (actual work vs target for days already passed)
- Show remaining working days in current month
- Display projection for end-of-month balance
- Add weekly breakdown within month
- Export summary data to CSV/PDF
- Historical summary view (last 12 months)

## Testing Checklist

- [ ] Summary displays correctly on main screen
- [ ] Previous month balance calculates correctly with actual times
- [ ] Previous month balance uses planned shifts when no actual time
- [ ] Target hours calculated based on weekly working hours setting
- [ ] Target hours counts only working days (Mon-Fri)
- [ ] Planned hours sums all shift assignments correctly
- [ ] Difference shows correct sign and value
- [ ] Colors display correctly (green for positive, red for negative)
- [ ] Summary updates when changing months
- [ ] Summary updates when adding/removing shift assignments
- [ ] Summary updates when adding/removing actual work times
- [ ] Summary updates when changing weekly working hours
- [ ] Works correctly with no working hours set (uses default 40.0)
- [ ] Works correctly with no shifts assigned (shows all zeros)
- [ ] Time formatting displays correctly (HH:MM h format)

## Related Documentation

- [Working Hours README](WORKING_HOURS_README.md) - Weekly hours management
- [Calendar README](CALENDAR_README.md) - Calendar view functionality
- [Actual Time Tracking README](ACTUAL_TIME_TRACKING_README.md) - Actual work time tracking
- [App Navigation Guide](APP_NAVIGATION_GUIDE.md) - Overall app structure

