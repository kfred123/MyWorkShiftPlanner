# Working Hours Feature - Implementation Summary

## Overview
A new "Working Hours" (Wochenarbeitszeit) feature has been implemented that allows users to manage their weekly working hours by month. This feature is accessible from the main menu in the top right corner.

## Features Implemented

### 1. Database Structure
- **New Entity**: `WorkingHours` entity to store weekly working hours per month
  - `id`: Auto-generated primary key
  - `yearMonth`: String in format "YYYY-MM" (e.g., "2025-12")
  - `weeklyHours`: Double value allowing decimals

- **DAO**: `WorkingHoursDao` with operations:
  - Get all working hours
  - Get working hours by specific month
  - Get previous month's working hours
  - Get earliest working hours entry
  - Insert/update working hours
  - Delete by month or from a month onwards

- **Repository**: `WorkingHoursRepository` to manage data operations

- **Database Update**: Updated `ShiftDatabase` from version 4 to version 5 to include the new entity

### 2. User Interface

#### Working Hours Screen (`WorkingHoursActivity`)
- Accessible via the main menu: "Wochenarbeitszeit"
- Displays a scrollable list of months with their working hours
- Current month is highlighted with a different background color
- Each month shows:
  - Month name and year (e.g., "Dezember 2025")
  - Working hours value with "h" suffix
  - Status: "Manuell eingegeben" (manually entered) or "Vom Vormonat übernommen" (inherited from previous month)
  - Manual entries are shown in primary color, inherited values in gray

#### Edit Dialog
- Tap any month to edit its working hours
- Input field accepts decimal values (e.g., 40 or 40.5)
- Validation ensures only positive numbers are accepted
- Options:
  - **Save**: Saves the working hours for the selected month
  - **Delete**: Removes manual entry (month will inherit from previous month)
  - **Cancel**: Closes dialog without changes

### 3. Business Logic

#### Automatic Value Inheritance
- Months without manual entries automatically inherit the value from the previous month
- If no previous month exists, the value is shown as "Nicht festgelegt" (not set)

#### Future Month Updates
- When updating working hours for the current month or any future month, the value is automatically applied to all future months
- This is done by deleting all manual entries from the next month onwards
- Past months are updated only individually

#### Scrolling Behavior
- User can scroll backwards as long as there is a history of manually entered numbers
- User can scroll up to 12 months into the future from the current month
- The list automatically scrolls to the current month on first load

#### Current Month Display
- The current month is always displayed in the middle of the list (when possible)
- Current month has a distinct visual highlight (primary container color)

### 4. Navigation
- Added new menu item "Wochenarbeitszeit" in the main screen's dropdown menu
- Menu order: Schichtverwaltung → Wochenarbeitszeit → Einstellungen
- Activity registered in `AndroidManifest.xml`

## Files Created/Modified

### New Files
1. `app/src/main/java/com/pb/myworkshiftplanner/data/WorkingHours.kt`
2. `app/src/main/java/com/pb/myworkshiftplanner/data/WorkingHoursDao.kt`
3. `app/src/main/java/com/pb/myworkshiftplanner/data/WorkingHoursRepository.kt`
4. `app/src/main/java/com/pb/myworkshiftplanner/ui/workinghours/WorkingHoursViewModel.kt`
5. `app/src/main/java/com/pb/myworkshiftplanner/ui/workinghours/WorkingHoursActivity.kt`

### Modified Files
1. `app/src/main/java/com/pb/myworkshiftplanner/data/ShiftDatabase.kt` - Added WorkingHours entity and DAO
2. `app/src/main/java/com/pb/myworkshiftplanner/MainActivity.kt` - Added menu item
3. `app/src/main/AndroidManifest.xml` - Registered WorkingHoursActivity

## Technical Details

### Dependencies
- Uses `org.threeten.bp` for date handling (YearMonth)
- Uses Jetpack Compose for UI
- Uses Room database for persistence
- Uses Kotlin Coroutines and Flow for async operations

### UI Components
- Material 3 design system
- Lazy scrolling list for performance
- Dialog for editing
- Proper validation and error handling

## Usage Example

1. User opens the app and taps the menu (three dots) in the top right
2. Selects "Wochenarbeitszeit"
3. Sees a list of months, scrolled to the current month
4. Taps on any month (e.g., December 2025)
5. Enters working hours (e.g., 40)
6. Taps "Speichern"
7. The value is saved for December and automatically applied to all future months
8. If user later changes January 2026 to 38 hours, only February onwards will inherit 38 hours

## Build Status
✅ Build successful - No compilation errors

