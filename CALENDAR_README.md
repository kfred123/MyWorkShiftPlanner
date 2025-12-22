# Kalenderansicht - Implementierungsübersicht

## Implementierte Funktionen

### 1. Erweiterte Datenbank-Strukturen

#### ShiftAssignment Entity (`ShiftAssignment.kt`)
- Verknüpft Datum mit Schicht
- Felder:
  - id (Auto-generiert)
  - date (String im Format "yyyy-MM-dd")
  - shiftId (Foreign Key zu Shift)
- Unique Index auf date (nur eine Schicht pro Tag)

#### ShiftAssignmentWithShift (`ShiftAssignmentWithShift.kt`)
- Room Relation für Join-Queries
- Kombiniert ShiftAssignment mit der zugehörigen Shift

#### ShiftAssignmentDao (`ShiftAssignmentDao.kt`)
- getAssignmentByDate(date): Zuordnung für bestimmtes Datum
- getAssignmentsWithShiftsInRange(startDate, endDate): Alle Zuordnungen für Zeitraum
- insertAssignment(assignment): Neue Zuordnung hinzufügen
- updateAssignment(assignment): Zuordnung aktualisieren
- deleteAssignmentByDate(date): Zuordnung für Datum löschen

#### ShiftAssignmentRepository (`ShiftAssignmentRepository.kt`)
- Repository-Pattern für Schicht-Zuordnungen
- Konvertiert Daten zu Map<String, Shift> für einfache Verwendung

#### Aktualisierte ShiftDatabase
- Version 2 (inkludiert ShiftAssignment Entity)
- Fallback zu destructive migration bei Schema-Änderungen

### 2. UI-Komponenten

#### MainActivity - Kalenderansicht
- **Kalender-Header:**
  - Zeigt aktuellen Monat und Jahr
  - Navigation: Pfeil-Links (vorheriger Monat) und Pfeil-Rechts (nächster Monat)

- **Kalender-Grid:**
  - 7 Spalten für Wochentage (Mo-So)
  - Zeigt alle Tage des aktuellen Monats
  - Heute ist mit farbigem Rahmen markiert
  - Tage mit zugewiesener Schicht haben farbigen Hintergrund und zeigen Schicht-Namen

- **Tag-Auswahl:**
  - Klick auf einen Tag öffnet Auswahl-Dialog
  - Dialog zeigt alle verfügbaren Schichten
  - Aktuelle Zuordnung ist hervorgehoben
  - Option "Keine Schicht" zum Entfernen der Zuordnung

#### ShiftSelectionDialog
- **Anzeige:**
  - Titel mit ausgewähltem Datum (Format: "dd.MM.yyyy")
  - Liste aller definierten Schichten
  - Jede Schicht zeigt: Name, Zeiten (Beginn - Ende), Pausendauer
  - "Keine Schicht" Option zum Entfernen

- **Interaktion:**
  - Klick auf Schicht weist sie dem Tag zu
  - Klick auf "Keine Schicht" entfernt Zuordnung
  - "Schließen" Button zum Abbrechen

### 3. ViewModel

#### CalendarViewModel (`CalendarViewModel.kt`)
- **UI State Management:**
  - currentMonth: Aktuell angezeigter Monat
  - selectedDate: Ausgewähltes Datum für Dialog
  - assignments: Map von Datum zu Schicht
  - allShifts: Liste aller verfügbaren Schichten
  - showShiftDialog: Dialog-Sichtbarkeit

- **Funktionen:**
  - nextMonth(): Navigiert zum nächsten Monat
  - previousMonth(): Navigiert zum vorherigen Monat
  - selectDate(date): Öffnet Dialog für Datum
  - assignShift(shiftId): Weist Schicht zu oder entfernt Zuordnung
  - getShiftForDate(date): Ruft Schicht für Datum ab

### 4. Technische Details

#### Datum/Zeit-Bibliothek
- **ThreeTenABP** (org.threeten:threetenbp:1.6.8)
- Backport von Java 8 Time API für Android < API 26
- Ersetzt java.time.* mit org.threeten.bp.*
- Unterstützt LocalDate, YearMonth, DateTimeFormatter

#### Kalender-Logik
- Berechnet ersten Wochentag des Monats
- Füllt Grid mit leeren Zellen vor erstem Tag
- Zeigt alle Tage des Monats in 7-Spalten-Grid
- Markiert heutiges Datum visuell

#### Reaktive Updates
- Flow-basierte Datenbeobachtung
- Automatische UI-Updates bei Änderungen
- StateFlow für UI-State-Management

### 5. Dependencies

Neue/Aktualisierte Dependencies:
```kotlin
// ThreeTenABP für Datum/Zeit auf älteren Android-Versionen
implementation("org.threeten:threetenbp:1.6.8")

// Lifecycle ViewModel Compose
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
```

## Verwendung

### Kalender navigieren
1. App starten → Kalenderansicht wird angezeigt
2. Aktueller Monat wird geladen
3. Pfeil-Buttons zum Wechseln zwischen Monaten

### Schicht zuweisen
1. Auf beliebigen Tag im Kalender tippen
2. Dialog öffnet sich mit allen verfügbaren Schichten
3. Gewünschte Schicht antippen
4. Schicht wird zugewiesen und im Kalender angezeigt

### Schicht entfernen
1. Auf Tag mit zugewiesener Schicht tippen
2. "Keine Schicht" im Dialog wählen
3. Zuordnung wird entfernt

### Schicht bearbeiten
1. Settings-Icon (⚙️) in TopAppBar antippen
2. In Schichtverwaltung gewünschte Schicht bearbeiten
3. Zurück zum Kalender → Änderungen werden automatisch angezeigt

## Dateien

```
app/src/main/java/com/pb/myworkshiftplanner/
├── MainActivity.kt                          # Hauptaktivität mit Kalenderansicht
├── data/
│   ├── Shift.kt                            # Schicht Entity
│   ├── ShiftDao.kt                         # Schicht DAO
│   ├── ShiftAssignment.kt                  # Zuordnung Entity (NEU)
│   ├── ShiftAssignmentDao.kt               # Zuordnung DAO (NEU)
│   ├── ShiftAssignmentWithShift.kt         # Relation Klasse (NEU)
│   ├── ShiftAssignmentRepository.kt        # Zuordnung Repository (NEU)
│   ├── DayWithShift.kt                     # Daten-Modell (NEU)
│   ├── ShiftDatabase.kt                    # Room Datenbank (Version 2)
│   └── ShiftRepository.kt                  # Schicht Repository
└── ui/
    ├── calendar/
    │   └── CalendarViewModel.kt            # Kalender ViewModel (NEU)
    └── shifts/
        ├── ShiftManagementActivity.kt      # Schichtverwaltung
        └── ShiftViewModel.kt               # Schichtverwaltung ViewModel
```

## Build-Konfiguration
- compileSdk: 36
- targetSdk: 36
- minSdk: 24
- Room Version: 2.6.1
- ThreeTenABP Version: 1.6.8
- Database Version: 2 (mit ShiftAssignment)

## Features
✅ Monatsansicht mit allen Tagen
✅ Navigation zwischen Monaten
✅ Visuelle Kennzeichnung des heutigen Tages
✅ Anzeige zugewiesener Schichten im Kalender
✅ Dialog zur Schicht-Auswahl
✅ Zuordnung von Schichten zu Tagen
✅ Entfernen von Zuordnungen
✅ Persistente Speicherung in Room Datenbank
✅ Reaktive Updates bei Änderungen
✅ Foreign Key Constraints für Datenintegrität

