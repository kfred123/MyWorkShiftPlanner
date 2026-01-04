# Monthly Summary - Quick Reference

## What It Shows

### At the bottom of the main calendar screen:

```
┌─────────────────────────────────────────┐
│ Monatsübersicht                         │
│ ────────────────────────────────────    │
│ Über-/Minusstunden Vormonat    +2:30 h │ ← Previous month balance
│ ────────────────────────────────────    │
│ Soll-Arbeitszeit              160:00 h │ ← Target for current month
│ Geplante Arbeitszeit          162:30 h │ ← Planned for current month
│ ────────────────────────────────────    │
│ Differenz (Plan - Soll)        +2:30 h │ ← Difference (bold)
└─────────────────────────────────────────┘
```

## Metrics Explained

### 1. Über-/Minusstunden Vormonat
**Previous Month Overtime/Deficit**
- Shows how many hours over or under target you worked last month
- **Green** (+) = Worked more than target (overtime)
- **Red** (-) = Worked less than target (deficit)
- Uses actual work times when available, planned shifts otherwise

### 2. Soll-Arbeitszeit
**Target Working Hours**
- Expected work hours for current month
- Based on your weekly working hours setting
- Calculated: (weekly hours ÷ 5) × working days (Mon-Fri)
- Example: 40h/week × 22 days = 176 hours

### 3. Geplante Arbeitszeit
**Planned Working Hours**
- Total hours from shifts assigned in current month
- Sum of all shift durations (start to end minus breaks)
- Updates automatically when you add/remove shifts

### 4. Differenz (Plan - Soll)
**Difference (Planned - Target)**
- How much over or under target you've planned
- **Green** (+) = Planned more than needed
- **Red** (-) = Planned less than needed
- **Bold** for emphasis

## How It Updates

### Automatically updates when:
- ✅ You navigate to a different month
- ✅ You add or remove a shift assignment
- ✅ You enter actual work times
- ✅ You change weekly working hours

### No manual refresh needed!

## Color Guide

| Value | Color | Meaning |
|-------|-------|---------|
| +2:30 h | 🟢 Green | Positive (overtime/surplus) |
| -1:15 h | 🔴 Red | Negative (deficit/shortfall) |
| 0:00 h | ⚪ Gray | Balanced/Neutral |

## Usage Examples

### Example 1: Planning Your Month
```
Soll-Arbeitszeit:        160:00 h  (target)
Geplante Arbeitszeit:    155:30 h  (currently planned)
Differenz:                -4:30 h  (need 4.5 more hours)
```
**Action:** Add more shifts to reach your target!

### Example 2: Month End Check
```
Über-/Minusstunden:       +3:00 h  (3 hours overtime)
```
**Info:** You started this month with 3 hours credit from last month.

### Example 3: Overplanned
```
Soll-Arbeitszeit:        160:00 h  (target)
Geplante Arbeitszeit:    168:00 h  (planned)
Differenz:                +8:00 h  (8 hours extra)
```
**Info:** You've planned 8 hours more than needed this month.

## Related Features

- **Working Hours** (Menu → Wochenarbeitszeit)
  - Set your weekly working hours (default: 40.0)
  - Affects target hours calculation

- **Actual Time Tracking** (Tap past days)
  - Enter actual work times
  - Affects previous month balance

- **Shift Management** (Menu → Schichtverwaltung)
  - Create and manage shift templates
  - Assign to days to build planned hours

## Tips

1. **Check at month start:** See your starting balance from last month
2. **Plan ahead:** Ensure planned hours meet your target
3. **Track actual time:** Affects next month's starting balance
4. **Adjust weekly hours:** Changes target calculation for current and future months

## Technical Details

- **Always visible** - No scrolling needed
- **Real-time updates** - Uses reactive Kotlin Flow
- **Efficient** - Only calculates when data changes
- **Format** - HH:MM h (hours:minutes)
- **Working days** - Counts Monday-Friday only (excludes weekends)

## Documentation

For more details, see:
- [MONTHLY_SUMMARY_README.md](MONTHLY_SUMMARY_README.md) - Full documentation
- [MONTHLY_SUMMARY_IMPLEMENTATION.md](MONTHLY_SUMMARY_IMPLEMENTATION.md) - Technical details
- [APP_NAVIGATION_GUIDE.md](APP_NAVIGATION_GUIDE.md) - App structure

