# Fix: Infinite Constraints Crash

## Problem

The app was crashing with the following error:
```
java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed.
```

## Root Cause

In `MainActivity.kt`, the layout structure had nested scrollable components:

**Before (INCORRECT):**
```kotlin
Column(
    modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())  // ❌ Scrollable container
        .padding(16.dp)
) {
    CalendarHeader(...)
    CalendarGrid(...)  // Contains LazyVerticalGrid ❌
}
```

The issue was:
1. **Outer Column** had `.verticalScroll()` modifier - making it vertically scrollable with infinite height
2. **CalendarGrid** inside contained `LazyVerticalGrid` components - which are also vertically scrollable
3. `LazyVerticalGrid` requires a bounded height constraint, but the parent `verticalScroll()` provides infinite height

This is a common Jetpack Compose anti-pattern: **Never nest LazyColumn/LazyRow/LazyVerticalGrid/LazyHorizontalGrid inside a scrollable container**.

## Solution

Removed the `.verticalScroll()` modifier from the outer Column:

**After (CORRECT):**
```kotlin
Column(
    modifier = Modifier
        .weight(1f)
        .padding(16.dp)  // ✅ No verticalScroll
) {
    CalendarHeader(...)
    CalendarGrid(...)  // Contains LazyVerticalGrid ✅
}
```

## Changes Made

**File:** `app/src/main/java/com/pb/myworkshiftplanner/MainActivity.kt`

- **Line 127:** Removed `.verticalScroll(rememberScrollState())` from the Column containing CalendarGrid

## Why This Works

- The `LazyVerticalGrid` components inside `CalendarGrid` have `userScrollEnabled = false`, meaning they don't scroll themselves
- They calculate their content height based on the number of items
- The parent Column with `.weight(1f)` provides bounded constraints (takes remaining space)
- The outer Column (with Scaffold padding) handles the overall scrolling if needed through its size constraints

## Verification

✅ Build successful
✅ No compilation errors
✅ Layout hierarchy is now correct (no nested scrollable components)

## Additional Notes

There's still a `.verticalScroll()` modifier at line 443 in the `DayDialog` composable, but this is **correct** because:
- It's inside an AlertDialog
- The Column contains regular UI elements (Text, OutlinedButton, TextField, etc.)
- No LazyColumn/LazyGrid components are nested inside

## Related Documentation

- [Jetpack Compose Layouts Best Practices](https://developer.android.com/jetpack/compose/layouts/basics)
- [LazyColumn/LazyGrid Guidelines](https://developer.android.com/jetpack/compose/lists)

