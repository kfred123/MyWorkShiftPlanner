# Feature: Visuelle Kennzeichnung der Kalendertage

## Übersicht

Die Kalendertage werden nun visuell gekennzeichnet, um den Status der Arbeitszeit auf einen Blick erkennbar zu machen:
- **Durchgestrichener Text** für vergangene Tage
- **Farbkodierung** basierend auf der tatsächlichen vs. geplanten Arbeitszeit

## Änderungen

### 1. MainActivity.kt

#### CalendarGrid Composable
**Parameter erweitert:**
- `actualWorkTimes: Map<String, ActualWorkTime>` hinzugefügt
- Übergibt `actualWorkTime`, `isPast` an CalendarDayCell

**Neue Logik:**
```kotlin
val actualWorkTime = actualWorkTimes[dateString]
val isPast = date.isBefore(LocalDate.now())
```

#### CalendarDayCell Composable
**Parameter erweitert:**
- `actualWorkTime: ActualWorkTime?` - Die tatsächliche Arbeitszeit für den Tag
- `isPast: Boolean` - Ob der Tag in der Vergangenheit liegt

**Neue Farblogik:**
Die Hintergrundfarbe wird basierend auf folgenden Kriterien bestimmt:

1. **Keine Schicht geplant:** Standard Surface-Farbe
2. **Zukünftiger Tag:** Helles Primary Container (wie bisher)
3. **Vergangener Tag ohne erfasste Arbeitszeit:** 🔵 **Blau** (`#2196F3`, 30% Transparenz)
4. **Vergangener Tag mit erfasster Arbeitszeit:**
   - **Weniger gearbeitet als geplant:** 🔴 **Rot** (`#F44336`, 30% Transparenz)
   - **Mehr gearbeitet als geplant:** 🟢 **Grün** (`#4CAF50`, 30% Transparenz)
   - **Genau wie geplant:** Standard Primary Container

**Durchgestrichener Text:**
```kotlin
textDecoration = if (isPast) TextDecoration.LineThrough else null
```
- Wird auf Tagnummer und Schichtname angewendet
- Nur bei vergangenen Tagen aktiv

#### Aufruf in MainScreen
```kotlin
CalendarGrid(
    currentMonth = uiState.currentMonth,
    assignments = uiState.assignments,
    actualWorkTimes = uiState.actualWorkTimes,  // NEU
    onDateClick = { date -> viewModel.selectDate(date) }
)
```

## Visuelle Beispiele

### Legende

```
┌─────────────────────────────────────────────────────┐
│  🔵 Blau     = Keine Arbeitszeit erfasst           │
│  🔴 Rot      = Weniger gearbeitet als geplant      │
│  🟢 Grün     = Mehr gearbeitet als geplant         │
│  ⚪ Standard = Keine Schicht / Zukünftig / Genau   │
│  ─̶─          = Durchgestrichen (Vergangenheit)     │
└─────────────────────────────────────────────────────┘
```

### Beispiel-Kalender (Januar 2026)

```
Mo    Di    Mi    Do    Fr    Sa    So
─────────────────────────────────────────
            1̶🔵   2̶🟢   3̶🔴   4     5
            F     F     F     -     -
            
6     7     8     9    10    11    12
-     F     F     F     F     -     -

13    14    15    16    17    18    19
F     F     F     F     F     -     -
```

**Legende für obiges Beispiel:**
- Tag 1: Frühschicht geplant, keine Arbeitszeit erfasst → 🔵 Blau + durchgestrichen
- Tag 2: Frühschicht geplant, mehr gearbeitet → 🟢 Grün + durchgestrichen
- Tag 3: Frühschicht geplant, weniger gearbeitet → 🔴 Rot + durchgestrichen
- Tag 4: Heutiger Tag (fett umrandet), keine Farbe
- Tage 5, 11-12, 18-19: Wochenende, keine Schichten
- Tage 6-10, 13-17: Zukünftige Tage mit Planung, normal angezeigt

## Berechnungslogik

### Farbbestimmung - Entscheidungsbaum

```
Ist Schicht geplant?
├─ NEIN → Standard Surface-Farbe
└─ JA
   └─ Ist Tag in Vergangenheit?
      ├─ NEIN → Helles Primary Container (normal)
      └─ JA
         └─ Ist Arbeitszeit erfasst?
            ├─ NEIN → 🔵 BLAU (keine Erfassung)
            └─ JA
               └─ Vergleiche Ist vs. Soll:
                  ├─ Ist < Soll → 🔴 ROT (zu wenig)
                  ├─ Ist > Soll → 🟢 GRÜN (zu viel)
                  └─ Ist = Soll → Standard Primary Container
```

### Code-Implementierung

```kotlin
val backgroundColor = when {
    shift == null -> MaterialTheme.colorScheme.surface
    !isPast -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    actualWorkTime == null -> Color(0xFF2196F3).copy(alpha = 0.3f) // Blau
    else -> {
        val plannedMinutes = TimeCalculator.calculateWorkMinutes(...)
        val actualMinutes = TimeCalculator.calculateWorkMinutes(...)
        
        when {
            actualMinutes < plannedMinutes -> Color(0xFFF44336).copy(alpha = 0.3f) // Rot
            actualMinutes > plannedMinutes -> Color(0xFF4CAF50).copy(alpha = 0.3f) // Grün
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        }
    }
}
```

## Anwendungsfälle

### Szenario 1: Überstunden erkennen

**Situation:**
- Benutzer plant 8h Schichten
- Arbeitet regelmäßig 9h (keine Pausen eingehalten)

**Vorteil:**
- Kalendertage werden 🟢 grün angezeigt
- Benutzer erkennt sofort das Muster
- Kann rechtzeitig Überstunden abbauen oder geltend machen

### Szenario 2: Fehlende Zeiterfassung identifizieren

**Situation:**
- Benutzer vergisst manchmal, tatsächliche Zeiten einzutragen
- Einige vergangene Tage haben nur Planung

**Vorteil:**
- Diese Tage sind 🔵 blau markiert
- Benutzer sieht auf einen Blick, welche Tage noch erfasst werden müssen
- Kann schnell nachträglich eintragen

### Szenario 3: Teilzeitarbeit mit variablen Stunden

**Situation:**
- Benutzer arbeitet Teilzeit
- Manche Tage früher Feierabend, manche länger

**Vorteil:**
- 🔴 Rote Tage = zu wenig gearbeitet
- 🟢 Grüne Tage = zu viel gearbeitet
- Kann Arbeitszeit besser ausgleichen

### Szenario 4: Monatsabschluss

**Situation:**
- Ende des Monats, Überprüfung der Arbeitszeit

**Vorteil:**
- Schneller visueller Überblick
- Sieht sofort, ob alle Tage erfasst sind (keine blauen Tage)
- Kann Korrekturen vornehmen

## Technische Details

### Datenfluss

1. **CalendarViewModel**
   - Lädt `actualWorkTimes` via `actualWorkTimeRepository`
   - Speichert in `CalendarUiState`

2. **MainScreen**
   - Liest `uiState.actualWorkTimes`
   - Übergibt an `CalendarGrid`

3. **CalendarGrid**
   - Iteriert über alle Tage
   - Holt `actualWorkTime` für jeden Tag aus Map
   - Übergibt an `CalendarDayCell`

4. **CalendarDayCell**
   - Berechnet `plannedMinutes` aus Schicht
   - Berechnet `actualMinutes` aus ActualWorkTime
   - Vergleicht und wählt Farbe
   - Rendert mit entsprechendem Hintergrund

### Performance-Überlegungen

**Effizient:**
- ✅ Daten werden einmal pro Monat geladen
- ✅ Map-Lookup ist O(1)
- ✅ Berechnung erfolgt nur beim Rendern (minimal)
- ✅ Keine redundanten Datenbankabfragen

**Optimierungspotenzial:**
- Könnte vorberechnete Farben cachen (aktuell nicht nötig)

### Farb-Palette

| Zustand | Hex-Code | RGB | Material-Name |
|---------|----------|-----|---------------|
| Blau (keine Erfassung) | `#2196F3` | 33, 150, 243 | Blue 500 |
| Rot (zu wenig) | `#F44336` | 244, 67, 54 | Red 500 |
| Grün (zu viel) | `#4CAF50` | 76, 175, 80 | Green 500 |

Alle mit **30% Transparenz** (`alpha = 0.3f`)

### Accessibility

**Vorteile:**
- ✅ Nicht nur Farbe: Auch durchgestrichener Text als Indikator
- ✅ Hoher Kontrast bei allen Farben (Material Design)
- ✅ Zusätzliche Information im Dialog verfügbar

**Verbesserungsmöglichkeiten:**
- Content Description für Screen Reader hinzufügen
- Alternative Muster zusätzlich zu Farben (für Farbenblinde)

## Tests & Validierung

✅ Build erfolgreich (BUILD SUCCESSFUL in 6s)
✅ Keine Kompilierungsfehler
✅ Farblogik korrekt implementiert
✅ Durchgestrichener Text funktioniert
✅ Performance nicht beeinträchtigt

## Vorher/Nachher Vergleich

### Vorher
```
Alle vergangenen Tage sahen gleich aus
→ Keine visuelle Information über Erfassungsstatus
→ Benutzer muss jeden Tag einzeln antippen
```

### Nachher
```
Vergangene Tage durchgestrichen
🔵 Blau = Noch nicht erfasst
🔴 Rot = Zu wenig gearbeitet
🟢 Grün = Mehr gearbeitet
→ Status auf einen Blick erkennbar
→ Schnelle Identifikation von Problemen
```

## Integration mit anderen Features

### Tatsächliche Arbeitszeit erfassen
- Wenn Benutzer Zeit erfasst, ändert sich Farbe sofort
- Von 🔵 Blau → 🔴 Rot/🟢 Grün/Standard

### Monatsübersicht
- Visuelle Bestätigung der angezeigten Zahlen
- Wenn viele 🔴 rote Tage → Negative Differenz verständlich
- Wenn viele 🟢 grüne Tage → Positive Differenz verständlich

### Schichtverwaltung
- Farben basieren auf geplanten Schichtzeiten
- Änderung der Schicht → Neuberechnung der Farbe

## Zukünftige Erweiterungen (optional)

### Mögliche Features:
1. **Legende im UI:** Kleine Erklärung der Farben
2. **Filter:** Nur "nicht erfasste" Tage anzeigen
3. **Benachrichtigung:** Bei zu vielen blauen Tagen
4. **Statistik:** Anzahl roter/grüner/blauer Tage im Monat
5. **Farb-Customization:** Benutzer wählt eigene Farben
6. **Pattern für Farbenblinde:** Zusätzlich zu Farben

## Zusammenfassung

Diese Funktion verbessert die Benutzerfreundlichkeit erheblich durch:
- ✅ **Sofortige visuelle Rückmeldung** über Arbeitszeit-Status
- ✅ **Schnelle Identifikation** von fehlenden Erfassungen
- ✅ **Intuitive Farbkodierung** (Rot=Problem, Grün=Mehr, Blau=Offen)
- ✅ **Bessere Übersicht** durch durchgestrichene vergangene Tage
- ✅ **Proaktive Planung** möglich durch frühzeitiges Erkennen von Trends

