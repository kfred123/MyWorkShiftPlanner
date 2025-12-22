# Tatsächliche Arbeitszeiten und Überstundenberechnung

## Neue Features

### ✅ **Erfassung tatsächlicher Arbeitszeiten**

Für Tage, die in der Vergangenheit liegen oder heute sind, können nun die tatsächlich geleisteten Arbeitszeiten erfasst werden.

#### Funktionsweise:

1. **Tag im Kalender auswählen** (heute oder früher)
2. **Dialog öffnet sich** mit zwei Bereichen:
   - **Geplante Schicht** (oben) - Dropdown zur Auswahl der Schicht
   - **Tatsächliche Arbeitszeit** (unten) - Eingabefelder für die echten Zeiten

3. **Automatische Vorbefüllung:**
   - Wenn eine Schicht geplant ist, werden die Zeiten automatisch übernommen
   - Benutzer kann die Zeiten manuell anpassen

4. **Eingabefelder:**
   - **Beginn**: Tatsächliche Startzeit (Format: HH:mm)
   - **Ende**: Tatsächliche Endzeit (Format: HH:mm)
   - **Pause (min)**: Tatsächliche Pausendauer in Minuten

### ✅ **Überstunden-Berechnung**

Das System berechnet automatisch die Über- oder Fehlzeiten im Vergleich zum Plan.

#### Berechnung:
```
Überstunden = (Tatsächliche Arbeitszeit - Pause) - (Geplante Arbeitszeit - Pause)
```

#### Anzeige:
- **Grüne Karte**: Mehrarbeit (positive Überstunden)
- **Rote Karte**: Fehlzeit (negative Stunden)
- **Graue Karte**: Planmäßig (0:00 Differenz)

Format: `+2:30 h` oder `-0:15 h`

#### Beispiele:
- **Plan**: 08:00 - 16:00, Pause 30 min = 7:30 h
- **Tatsächlich**: 07:45 - 16:30, Pause 30 min = 8:15 h
- **Überstunden**: +0:45 h (Mehrarbeit)

### ✅ **Benutzerführung im Dialog**

#### Für zukünftige Tage:
- Nur Schichtauswahl verfügbar
- Keine Zeiterfassung möglich

#### Für heute und vergangene Tage:
- **Oberer Bereich**: Schichtplanung (Dropdown)
  - "Keine Schicht" oder Liste aller Schichten
  - Ausgewählte Schicht ist hervorgehoben
  
- **Unterer Bereich**: Zeiterfassung
  - Drei Eingabefelder für Start, Ende, Pause
  - Automatische Vorbefüllung mit Schichtdaten
  - Live-Berechnung der Überstunden

#### Buttons:
- **Speichern**: Speichert die tatsächlichen Zeiten (nur bei ausgefüllten Feldern)
- **Löschen**: Entfernt erfasste Zeiten (nur wenn bereits vorhanden, rot)
- **Schließen**: Dialog schließen ohne Speichern

## Datenbank-Struktur

### ActualWorkTime Entity
```kotlin
@Entity(tableName = "actual_work_times")
data class ActualWorkTime(
    id: Long,
    date: String,              // "yyyy-MM-dd"
    actualStartTime: String,   // "HH:mm"
    actualEndTime: String,     // "HH:mm"
    actualBreakDuration: Int   // Minuten
)
```

### Database Version
- **Version 3** (erweitert um ActualWorkTime)
- Automatische Migration durch fallbackToDestructiveMigration

## TimeCalculator Utility

### Funktionen:

#### `calculateWorkMinutes(startTime, endTime, breakMinutes): Int`
Berechnet Netto-Arbeitszeit in Minuten
- Unterstützt Nachtschichten (über Mitternacht)
- Subtrahiert Pausenzeit

#### `calculateOvertime(...): Int`
Berechnet Überstunden/Fehlzeit
- Positiv = Mehrarbeit
- Negativ = Fehlzeit
- 0 = Planmäßig

#### `formatMinutesToHoursString(minutes): String`
Formatiert Minuten zu "±H:MM h"
- Beispiel: `+2:30 h` oder `-0:15 h`

## UI-Komponenten

### DayDialog
Zentraler Dialog für Tag-Interaktion mit zwei Modi:

#### Zukünftige Tage:
```
┌─────────────────────────┐
│ [Datum]                 │
├─────────────────────────┤
│ Geplante Schicht        │
│ [Dropdown ▼]            │
└─────────────────────────┘
```

#### Vergangene/Heutige Tage:
```
┌─────────────────────────┐
│ [Datum]                 │
├─────────────────────────┤
│ Geplante Schicht        │
│ [Dropdown ▼]            │
├─────────────────────────┤
│ Tatsächliche Arbeitszeit│
│ [Beginn] [Ende]         │
│ [Pause (min)]           │
│                         │
│ ┌───────────────────┐   │
│ │ Überstunden       │   │
│ │ +1:30 h           │   │
│ │ Mehrarbeit        │   │
│ └───────────────────┘   │
└─────────────────────────┘
```

## Workflow

### Schicht planen:
1. Tag auswählen
2. Im Dropdown Schicht wählen
3. Dialog schließen → Gespeichert

### Tatsächliche Zeit erfassen:
1. Vergangenen Tag auswählen
2. Falls nicht vorhanden: Schicht wählen (befüllt Felder)
3. Zeiten bei Bedarf anpassen
4. "Speichern" klicken

### Zeit korrigieren:
1. Tag mit erfasster Zeit auswählen
2. Zeiten ändern
3. "Speichern" klicken

### Zeit löschen:
1. Tag mit erfasster Zeit auswählen
2. "Löschen" Button (rot)
3. Dialog schließen

## Kalenderansicht

### Visuelle Hinweise:
- **Grauer Hintergrund**: Keine Schicht geplant
- **Farbiger Hintergrund**: Schicht geplant (zeigt Namen)
- **Blauer Rahmen**: Heutiger Tag
- *Zukünftig*: Indikator für erfasste Zeiten

## API-Änderungen

### CalendarViewModel

#### Neue State-Felder:
```kotlin
actualWorkTimes: Map<String, ActualWorkTime>
showDayDialog: Boolean  // vorher: showShiftDialog
```

#### Neue Methoden:
```kotlin
fun saveActualWorkTime(startTime: String, endTime: String, breakDuration: Int)
fun deleteActualWorkTime()
fun getActualWorkTimeForDate(date: LocalDate): ActualWorkTime?
```

### Repository
```kotlin
class ActualWorkTimeRepository {
    fun getActualWorkTimesInRange(startDate, endDate): Flow<List<ActualWorkTime>>
    suspend fun getActualWorkTimeByDate(date): ActualWorkTime?
    suspend fun insert/update/delete/deleteByDate(...)
}
```

## Technische Details

### Zeitberechnung
- Verwendet `org.threeten.bp.LocalTime` für Parsing
- `ChronoUnit.MINUTES.between()` für Differenzberechnung
- Automatische Behandlung von Nachtschichten

### Compose Best Practices
- `runCatching` statt try-catch (Composable-Einschränkungen)
- `remember` für Dialog-State-Management
- Automatische Vorbefüllung durch `remember(dependencies)`

### Validierung
- `toIntOrNull()` für sichere Zahleneingabe
- Prüfung auf leere/ungültige Zeiten
- Fehlertolerante Überstundenberechnung

## Zukünftige Erweiterungen (Optional)

- [ ] Monatliche Überstunden-Summe
- [ ] Export der Arbeitszeiten
- [ ] Statistiken und Diagramme
- [ ] Farbindikator im Kalender für erfasste Zeiten
- [ ] Notizfeld für besondere Vorkommnisse
- [ ] Automatische Erinnerung zur Zeiterfassung

## Dateien

### Neu erstellt:
```
data/
  ActualWorkTime.kt              # Entity
  ActualWorkTimeDao.kt           # DAO
  ActualWorkTimeRepository.kt    # Repository

utils/
  TimeCalculator.kt              # Berechnungslogik
```

### Geändert:
```
data/
  ShiftDatabase.kt               # Version 3

ui/calendar/
  CalendarViewModel.kt           # Erweitert

MainActivity.kt                  # Neuer DayDialog
```

## Build-Status
✅ **BUILD SUCCESSFUL**
- Database Version: 3
- Alle Tests bestanden
- Keine Compile-Fehler

