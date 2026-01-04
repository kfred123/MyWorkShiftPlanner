# Fix: Previous Month Balance Calculation

## Problem

Die App berechnete Über-/Minusstunden für Vormonate, auch wenn für diese Monate noch keine wöchentliche Arbeitszeit erfasst wurde. Dies führte zu falschen Berechnungen und verwirrenden Anzeigen für Benutzer, die gerade erst mit der App begonnen haben.

## Lösung

Die `calculateMonthBalance()` Funktion in `CalendarViewModel.kt` wurde so angepasst, dass sie **0 Minuten** (keine Über-/Minusstunden) zurückgibt, wenn:

1. **Keine Wochenarbeitszeit jemals konfiguriert wurde** (Datenbank ist leer)
2. **Der zu berechnende Monat VOR dem frühesten konfigurierten Monat liegt**

## Implementierungsdetails

**Datei:** `app/src/main/java/com/pb/myworkshiftplanner/ui/calendar/CalendarViewModel.kt`

**Funktion:** `calculateMonthBalance(month: YearMonth)`

### Änderungen

```kotlin
private suspend fun calculateMonthBalance(month: YearMonth): Int {
    // Check if working hours were ever configured for this month or earlier
    val yearMonthString = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val earliestWorkingHours = workingHoursRepository.getEarliestWorkingHours()
    
    // If no working hours were ever configured, return 0
    if (earliestWorkingHours == null) {
        return 0
    }
    
    // If the month is before the earliest configured working hours, return 0
    if (yearMonthString < earliestWorkingHours.yearMonth) {
        return 0
    }
    
    // ... rest of calculation logic
}
```

## Logik

1. **Früheste Wochenarbeitszeit abrufen:**
   - Nutzt `workingHoursRepository.getEarliestWorkingHours()` um den frühesten Eintrag in der Datenbank zu finden

2. **Prüfung 1 - Keine Daten vorhanden:**
   - Wenn `earliestWorkingHours == null`, dann gibt es keine Wochenarbeitszeit-Einträge
   - → Return `0` (keine Über-/Minusstunden)

3. **Prüfung 2 - Monat vor erstem Eintrag:**
   - Wenn der zu berechnende Monat (`yearMonthString`) vor dem frühesten Eintrag liegt (`earliestWorkingHours.yearMonth`)
   - → Return `0` (keine Über-/Minusstunden)

4. **Normale Berechnung:**
   - Wenn beide Prüfungen bestanden sind, wird die normale Berechnung durchgeführt:
     - Soll-Arbeitszeit basierend auf Wochenarbeitszeit berechnen
     - Ist-Arbeitszeit aus tatsächlichen oder geplanten Schichten ermitteln
     - Differenz zurückgeben (Ist - Soll)

## Beispiel-Szenario

### Vorher (Fehlerhaft)

1. Benutzer startet App im Januar 2026
2. Erfasst erstmals Wochenarbeitszeit (40h) für Januar 2026
3. Schaut Hauptansicht für Januar 2026 an
4. **Problem:** Über-/Minusstunden für Dezember 2025 werden berechnet, obwohl damals keine Arbeitszeit erfasst war
5. Anzeige: `-173h` (unrealistisch und verwirrend)

### Nachher (Korrekt)

1. Benutzer startet App im Januar 2026
2. Erfasst erstmals Wochenarbeitszeit (40h) für Januar 2026
3. Schaut Hauptansicht für Januar 2026 an
4. **Lösung:** Über-/Minusstunden für Dezember 2025 = `0h` (korrekt, da keine Arbeitszeit erfasst)
5. Anzeige: `±0h` (korrekt)

### Weiteres Beispiel

1. Benutzer hat Wochenarbeitszeit ab März 2025 erfasst
2. Schaut Hauptansicht für April 2025 an
3. Über-/Minusstunden für März 2025 werden **normal berechnet** (da März >= März)
4. Schaut Hauptansicht für März 2025 an
5. Über-/Minusstunden für Februar 2025 = `0h` (korrekt, da Februar < März)

## Betroffene UI-Komponenten

Die Änderung wirkt sich auf die **Monatsübersicht** in der Hauptansicht aus:

```
┌─────────────────────────────────┐
│      Monatsübersicht            │
├─────────────────────────────────┤
│ Über-/Minusstunden Vormonat     │  ← Diese Zeile
│                      ±0h        │
├─────────────────────────────────┤
│ Soll-Arbeitszeit                │
│                     173h        │
│ Geplante Arbeitszeit            │
│                     160h        │
├─────────────────────────────────┤
│ Differenz (Plan - Soll)         │
│                     -13h        │
└─────────────────────────────────┘
```

## Verifizierung

✅ Build erfolgreich
✅ Keine Kompilierungsfehler
✅ Logik berücksichtigt alle Edge-Cases:
   - Keine Wochenarbeitszeit erfasst
   - Erste Nutzung der App
   - Monate vor dem ersten Eintrag
   - Normale Monate mit erfasster Arbeitszeit

## Zusammenhang mit anderen Features

Diese Änderung harmoniert mit der bestehenden Logik:

- **WorkingHoursActivity:** Benutzer kann weiterhin Wochenarbeitszeit für beliebige Monate erfassen
- **Automatische Übernahme:** Wenn für einen Monat keine Wochenarbeitszeit erfasst ist, wird die des Vormonats verwendet (via `getPreviousWorkingHours()`)
- **Vormonat-Bilanz:** Wird jetzt nur noch berechnet, wenn tatsächlich Arbeitszeit erfasst war

## Getestet mit

- Android Studio Koala | 2024.1.1
- Gradle 8.7
- Kotlin 1.9.0
- Build: Erfolgreich (BUILD SUCCESSFUL in 1m 15s)

