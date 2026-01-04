# App Navigation Structure

## Main Screen Menu (Top Right ⋮)

```
┌─────────────────────────────────────┐
│  My Work Shift Planner              │ ← Main Screen
│                             [⋮]     │ ← Menu Button
└─────────────────────────────────────┘
                              │
                              ├─ Click menu opens:
                              │
                              ▼
        ┌─────────────────────────────────┐
        │  • Schichtverwaltung           │ → Shift Management
        │  • Wochenarbeitszeit    ✨ NEW │ → Working Hours
        │  • Einstellungen               │ → Settings
        └─────────────────────────────────┘
```

## Screen Hierarchy

```
Main Calendar Screen
│
├── Calendar View (Top)
│   └── Monthly calendar with shift assignments
│
├── Monthly Summary (Bottom) ✨ NEW
│   ├── Previous month overtime/deficit
│   ├── Current month target hours
│   ├── Current month planned hours
│   └── Difference (planned - target)
│
├── Schichtverwaltung (Shift Management)
│   └── Create, edit, delete shifts
│
├── Wochenarbeitszeit (Working Hours) ✨ NEW
│   └── Manage weekly working hours by month
│
└── Einstellungen (Settings)
    └── Select Google Calendar for sync
```

## Main Screen Layout ✨ NEW

```
┌─────────────────────────────────────────┐
│ ← My Work Shift Planner            [⋮]  │ ← Top Bar
├─────────────────────────────────────────┤
│        ← Dezember 2024 →                │ ← Month Nav
├─────────────────────────────────────────┤
│ Mo Di Mi Do Fr Sa So                    │
│                                         │
│  1  2  3  4  5  6  7                    │ ← Scrollable
│  8  9 10 11 12 13 14                    │   Calendar
│ 15 16 17 18 19 20 21                    │
│ 22 23 24 25 26 27 28                    │
│ 29 30 31                                │
│                                         │
├─────────────────────────────────────────┤
│ Monatsübersicht                         │ ← Fixed
│ ────────────────────────────────────    │   Summary
│ Über-/Minusstunden Vormonat    +2:30 h │   (Always
│ ────────────────────────────────────    │   visible)
│ Soll-Arbeitszeit              160:00 h │
│ Geplante Arbeitszeit          162:30 h │
│ ────────────────────────────────────    │
│ Differenz (Plan - Soll)        +2:30 h │
└─────────────────────────────────────────┘
```

## Working Hours Screen Layout

```
┌─────────────────────────────────────────┐
│ ← Wochenarbeitszeit                     │ ← Top Bar
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ November 2024                   │   │
│  │ Vom Vormonat übernommen         │   │
│  │                          40.0 h │   │ ← Scrollable
│  └─────────────────────────────────┘   │   List
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ Dezember 2024 (highlighted)     │   │
│  │ Manuell eingegeben              │   │ ← Current
│  │                          40.0 h │   │   Month
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ Januar 2025                     │   │
│  │ Vom Vormonat übernommen         │   │
│  │                          40.0 h │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ...more months...                      │
│                                         │
└─────────────────────────────────────────┘
```

## Edit Dialog (Tap on any month)

```
        ┌─────────────────────────────┐
        │ Dezember 2024               │
        ├─────────────────────────────┤
        │                             │
        │ Wochenarbeitszeit (Stunden) │
        │ ┌─────────────────────────┐ │
        │ │ 40.0                    │ │ ← Input field
        │ └─────────────────────────┘ │
        │                             │
        │ Hinweis: Die Änderung wird  │
        │ für alle zukünftigen Monate │
        │ übernommen.                 │
        │                             │
        ├─────────────────────────────┤
        │ [Löschen] [Abbrechen] [OK]  │ ← Buttons
        └─────────────────────────────┘
```

## Feature Interactions

### Calendar Sync Flow
```
User Action                    System Response
───────────                    ───────────────
1. Open Calendar              
2. Tap a day                  → Shows shift selection dialog
3. Select shift               → Saves to database
                              → Creates Google Calendar event ✨
                              → Stores event ID in assignment

4. Change shift               → Updates database
                              → Updates Google Calendar event ✨

5. Remove shift               → Deletes from database
                              → Deletes Google Calendar event ✨
```

### Working Hours Flow
```
User Action                    System Response
───────────                    ───────────────
1. Open menu                  
2. Tap "Wochenarbeitszeit"    → Opens working hours screen
3. View months                → Shows list with current month highlighted
                              → Manual entries in bold/color
                              → Inherited values in gray

4. Tap a month                → Opens edit dialog
5. Enter hours (e.g., 40.5)   → Validates input
6. Save                       → Stores in database
                              → If current/future: propagates to future months ✨
                              → If past: only affects that month
```

### Monthly Summary Flow ✨ NEW
```
User Action                    System Response
───────────                    ───────────────
1. Open app                   → Calculates and displays summary
                              → Previous month balance
                              → Current month target/planned/difference

2. Navigate months            → Recalculates for new month
                              → Updates summary display

3. Add/change shift           → Recalculates planned hours
                              → Updates difference

4. Add/change actual time     → Affects previous month balance (if applicable)
                              → Updates summary

5. Change working hours       → Recalculates target hours
                              → Updates difference

Color Indicators:
• Green (positive): Overtime or surplus
• Red (negative): Deficit or shortfall
• Gray (zero): Balanced
```

## Color Coding

### Working Hours List
- **Primary Container Color**: Current month
- **Primary Color (Bold)**: Manually entered hours
- **Gray**: Inherited hours
- **Error Color**: Not set

### Calendar Sync
- Uses Material 3 color scheme throughout
- Visual feedback during sync operations

## Data Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Room DB    │ ←→  │  Repository  │ ←→  │  ViewModel   │
└──────────────┘     └──────────────┘     └──────────────┘
      ↑                                           ↑
      │                                           │
      ├── Shifts                                  │
      ├── ShiftAssignments (with eventId)         │
      ├── ActualWorkTimes                         │
      └── WorkingHours ✨ NEW                     │
                                                  │
                                                  ↓
                                           ┌──────────────┐
                                           │   Compose    │
                                           │      UI      │
                                           └──────────────┘
                                                  ↑
                                                  │
                                           ┌──────────────┐
                                           │    User      │
                                           └──────────────┘
```

## Quick Reference

### Menu Access
- Main Screen → Top Right (⋮) → Wochenarbeitszeit

### Decimal Input
- Supported formats: `40`, `40.0`, `40.5`, `37.5`
- Uses decimal point (.) or comma (,)

### Month Navigation
- Automatic scroll to current month on open
- Scroll up for future months (up to +12)
- Scroll down for past months (unlimited if manual entries exist)

### Calendar Selection
- Settings → Select Google Calendar
- Required for calendar sync to work
- Can be disabled by selecting "Kein Kalender"

