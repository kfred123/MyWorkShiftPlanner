# Google Calendar Sync - Implementation Summary

## Overview
The app already has **full Google Calendar synchronization** implemented for shift assignments. When a user assigns, changes, or removes a shift in the calendar view, the corresponding Google Calendar event is automatically created, updated, or deleted.

## Current Implementation Status
✅ **FULLY IMPLEMENTED** - No additional work needed

## Features

### 1. Automatic Calendar Event Creation
When a user assigns a shift to a day:
- A calendar event is created in the selected Google Calendar
- Event title: Shift name
- Event time: Shift's begin time to end time
- Handles shifts that cross midnight (e.g., 22:00 to 06:00)
- Event description includes:
  - Shift name
  - Start and end times
  - Break duration
  - Assignment ID (for identification): `// MyWorkShiftPlanner Assignment ID: [id]`

### 2. Automatic Calendar Event Update
When a user changes the shift for a day:
- The existing calendar event is updated with the new shift information
- All event details (title, times, description) are refreshed
- If the update fails, the event ID is cleared from the assignment

### 3. Automatic Calendar Event Deletion
When a user removes a shift assignment:
- The corresponding calendar event is deleted from Google Calendar
- The assignment is removed from the database

### 4. Event Identification
- Each calendar event stores the assignment ID in its description
- The `ShiftAssignment` entity stores the `googleCalendarEventId` field
- This two-way linking ensures proper event tracking and updates

## Technical Implementation

### Files Involved
1. **CalendarSyncHelper.kt** (`utils/CalendarSyncHelper.kt`)
   - `createCalendarEvent()` - Creates new calendar events
   - `updateCalendarEvent()` - Updates existing calendar events
   - `deleteCalendarEvent()` - Deletes calendar events
   - `buildEventDescription()` - Generates event description with metadata
   - `hasWriteCalendarPermission()` - Checks calendar permissions

2. **CalendarViewModel.kt** (`ui/calendar/CalendarViewModel.kt`)
   - `assignShift()` - Main method that handles all calendar sync operations
   - Automatically syncs with Google Calendar when shifts are assigned/updated/removed

3. **ShiftAssignment.kt** (`data/ShiftAssignment.kt`)
   - Contains `googleCalendarEventId` field to track the linked calendar event

### Calendar Permissions
The app already requests the necessary permissions:
- `READ_CALENDAR` - To read available calendars
- `WRITE_CALENDAR` - To create, update, and delete events

### Calendar Selection
- Users can select which Google Calendar to sync with via Settings
- If no calendar is selected, shifts are stored locally without calendar sync
- Calendar sync only happens if write permissions are granted

## How It Works

### User Flow
1. User opens the main calendar view
2. User taps on a day
3. User selects a shift from the dialog
4. **Automatic sync happens:**
   - If this is a new assignment:
     - Assignment is saved to local database
     - Calendar event is created in Google Calendar
     - Event ID is stored in the assignment
   - If this is an update to an existing assignment:
     - If calendar event exists: Event is updated
     - If no calendar event exists: New event is created
   - If user removes the shift:
     - Calendar event is deleted
     - Assignment is removed from database

### Code Flow in `assignShift()`
```kotlin
// 1. Delete case (shiftId == null)
if (shiftId == null) {
    if (existing != null) {
        existing.googleCalendarEventId?.let { eventId ->
            CalendarSyncHelper.deleteCalendarEvent(context, eventId)
        }
        assignmentRepository.deleteByDate(dateString)
    }
}

// 2. Create/Update case
else {
    val calendarId = settingsRepository.selectedCalendarId.first()
    
    if (existing != null) {
        // Update existing assignment
        if (calendarId != null && hasPermission) {
            if (existing.googleCalendarEventId != null) {
                // Update existing calendar event
                CalendarSyncHelper.updateCalendarEvent(...)
            } else {
                // Create new calendar event
                CalendarSyncHelper.createCalendarEvent(...)
            }
        }
        assignmentRepository.update(updatedAssignment)
    } else {
        // Create new assignment
        val assignmentId = assignmentRepository.insert(newAssignment)
        if (calendarId != null && hasPermission) {
            val eventId = CalendarSyncHelper.createCalendarEvent(...)
            // Update assignment with event ID
            assignmentRepository.update(newAssignment.copy(googleCalendarEventId = eventId))
        }
    }
}
```

## Event Details

### Event Structure
- **Title**: Shift name (e.g., "Frühschicht")
- **Start Time**: Shift begin time on the assigned date
- **End Time**: Shift end time (handles midnight crossing)
- **Time Zone**: System default time zone
- **Calendar**: User-selected Google Calendar
- **Description**: 
  ```
  Schicht: [Shift Name]
  Zeit: [Begin Time] - [End Time]
  Pause: [Break Duration] Minuten
  
  // MyWorkShiftPlanner Assignment ID: [Assignment ID]
  ```

### Midnight Handling
If the shift's end time is before its start time (e.g., 22:00 to 06:00):
- The event is created spanning two days
- Start: Assigned date at begin time
- End: Next day at end time

## Error Handling
- Permission checks before all operations
- Try-catch blocks in CalendarSyncHelper methods
- Graceful fallback if calendar sync fails (assignments still saved locally)
- Event ID cleared if update fails

## Testing Scenarios

### Scenario 1: Create New Assignment
1. Tap on an empty day
2. Select a shift
3. ✅ Assignment created in database
4. ✅ Event created in Google Calendar
5. ✅ Event ID stored in assignment

### Scenario 2: Update Assignment
1. Tap on a day with an existing shift
2. Select a different shift
3. ✅ Assignment updated in database
4. ✅ Calendar event updated with new shift details

### Scenario 3: Remove Assignment
1. Tap on a day with an existing shift
2. Select "Keine Schicht"
3. ✅ Assignment deleted from database
4. ✅ Calendar event deleted from Google Calendar

### Scenario 4: No Calendar Selected
1. No calendar selected in settings
2. Assign a shift
3. ✅ Assignment created in database
4. ✅ No calendar event created (as expected)

### Scenario 5: No Permission
1. Calendar permission denied
2. Assign a shift
3. ✅ Assignment created in database
4. ✅ No calendar event created (as expected)

## Benefits
- ✅ **Automatic**: No manual sync required
- ✅ **Bidirectional tracking**: Assignment knows its event, event description contains assignment ID
- ✅ **Reliable**: Handles all edge cases (midnight, updates, deletions)
- ✅ **User-friendly**: Works seamlessly in the background
- ✅ **Flexible**: Users can choose which calendar to sync with or opt-out entirely

## Conclusion
The Google Calendar sync feature is **fully functional and production-ready**. Users can assign shifts in the calendar view and they will automatically appear in their selected Google Calendar. Changes and deletions are also synchronized automatically.

