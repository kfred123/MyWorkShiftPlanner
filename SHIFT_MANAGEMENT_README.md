# Schichtverwaltung - Implementierungsübersicht

## Implementierte Funktionen

### 1. Room-Datenbank (SQL)
- **Shift Entity** (`Shift.kt`): Datenmodell mit folgenden Feldern:
  - id (Auto-generiert)
  - name (String)
  - beginTime (String im Format "HH:mm")
  - endTime (String im Format "HH:mm")
  - breakDuration (Int in Minuten)

- **ShiftDao** (`ShiftDao.kt`): Datenbankzugriff mit:
  - getAllShifts(): Flow<List<Shift>> - Alle Schichten sortiert nach Name
  - getShiftById(id): Schicht per ID abrufen
  - insertShift(shift): Neue Schicht hinzufügen
  - updateShift(shift): Schicht aktualisieren
  - deleteShift(shift): Schicht löschen

- **ShiftDatabase** (`ShiftDatabase.kt`): Room-Datenbank mit Singleton-Pattern
- **ShiftRepository** (`ShiftRepository.kt`): Repository-Pattern für Datenzugriff

### 2. UI-Komponenten

#### MainActivity
- TopAppBar mit Settings-Icon (rechts oben)
- Öffnet ShiftManagementActivity beim Klick

#### ShiftManagementActivity
- **Hauptfunktionen:**
  - Liste aller gespeicherten Schichten
  - Floating Action Button (+) zum Hinzufügen neuer Schichten
  - Jede Schicht zeigt: Name, Zeiten (Beginn - Ende), Pausendauer
  - Edit-Button (Stift-Icon) zum Bearbeiten
  - Delete-Button (Mülleimer-Icon) zum Löschen

- **Dialog für Schichten:**
  - Name-Eingabe
  - Beginn-Zeit mit Time Picker (24h Format)
  - End-Zeit mit Time Picker (24h Format)
  - Pausendauer in Minuten
  - Speichern/Abbrechen Buttons

### 3. ViewModel
- **ShiftViewModel** (`ShiftViewModel.kt`): 
  - Verwaltet UI-Status und Datenoperationen
  - Verwendet Coroutines für asynchrone Datenbankoperationen
  - Flow für reaktive Updates der Schichtenliste

### 4. Technische Details
- **Datenbank:** Room SQLite Datenbank für Performance
- **UI:** Jetpack Compose mit Material3 Design
- **Architektur:** MVVM (Model-View-ViewModel) Pattern
- **Async:** Kotlin Coroutines und Flow

## Build-Konfiguration
- compileSdk: 36
- targetSdk: 36
- minSdk: 24
- Room Version: 2.6.1
- KSP für Room Code-Generierung

## Verwendung
1. App starten
2. Settings-Icon (⚙️) rechts oben in der TopAppBar antippen
3. Im Schichtverwaltungs-Screen:
   - **Hinzufügen:** Plus-Button (+) unten rechts
   - **Bearbeiten:** Stift-Icon bei der gewünschten Schicht
   - **Löschen:** Mülleimer-Icon bei der gewünschten Schicht
   - **Zeiten auswählen:** Dialog öffnet Time Picker für präzise Zeitauswahl

## Dateien
```
app/src/main/java/com/pb/myworkshiftplanner/
├── MainActivity.kt                          # Hauptaktivität mit TopAppBar
├── data/
│   ├── Shift.kt                            # Entity
│   ├── ShiftDao.kt                         # Data Access Object
│   ├── ShiftDatabase.kt                    # Room Datenbank
│   └── ShiftRepository.kt                  # Repository
└── ui/shifts/
    ├── ShiftManagementActivity.kt          # Schichtverwaltungs-UI
    └── ShiftViewModel.kt                   # ViewModel
```

