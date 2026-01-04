# Feature: Tatsächliche Arbeitszeit in Monatsübersicht

## Übersicht

Die Monatsübersicht zeigt jetzt zusätzlich zur geplanten Arbeitszeit auch die **tatsächlich absolvierte Arbeitszeit** an.

## Änderungen

### 1. CalendarViewModel.kt

#### MonthlySummary Datenklasse
- **Hinzugefügt:** `actualHours: Int = 0` - Speichert die tatsächlich gearbeiteten Minuten für den aktuellen Monat

#### calculateMonthlySummary()
- **Erweitert:** Ruft nun auch `calculateActualWorkedHours(month)` auf
- Setzt `actualHours` im zurückgegebenen `MonthlySummary` Objekt

#### Neue Funktion: calculateActualWorkedHours()
Diese Funktion berechnet die tatsächlich gearbeiteten Stunden für einen Monat:

```kotlin
private suspend fun calculateActualWorkedHours(month: YearMonth): Int
```

**Logik:**
1. Lädt alle Schichtzuweisungen und tatsächlichen Arbeitszeiten für den Monat
2. Iteriert über jeden Tag im Monat:
   - **Falls tatsächliche Arbeitszeit erfasst:** Verwendet diese Zeit
   - **Falls keine tatsächliche Zeit, aber Schicht geplant und Tag in Vergangenheit:** Verwendet geplante Schichtzeit
   - **Sonst:** Zählt nichts
3. Gibt die Summe in Minuten zurück

**Unterschied zu calculateMonthBalance():**
- `calculateMonthBalance()` berechnet die Differenz (Ist - Soll) für vergangene Monate
- `calculateActualWorkedHours()` berechnet nur die absoluten Ist-Stunden für den aktuellen Monat

### 2. MainActivity.kt

#### MonthlySummarySection Composable
- **Hinzugefügt:** Neue Zeile "Tatsächliche Arbeitszeit" unter "Geplante Arbeitszeit"
- Zeigt `summary.actualHours` formatiert als Stunden-String an
- Verwendet die gleiche Farbe wie andere Informationswerte (onSurfaceVariant)

## Neue UI-Struktur

```
┌─────────────────────────────────────┐
│      Monatsübersicht                │
├─────────────────────────────────────┤
│ Über-/Minusstunden Vormonat         │
│                          ±0h        │
├─────────────────────────────────────┤
│ Soll-Arbeitszeit                    │
│                         173h        │
│ Geplante Arbeitszeit                │
│                         160h        │
│ Tatsächliche Arbeitszeit    ← NEU   │
│                         152h        │
├─────────────────────────────────────┤
│ Differenz (Plan - Soll)             │
│                         -13h        │
└─────────────────────────────────────┘
```

## Anwendungsfälle

### Szenario 1: Aktueller Monat mit gemischten Daten

**Situation:**
- Heutiges Datum: 15. Januar 2026
- Geplante Schichten: 1.-31. Januar (160h)
- Tatsächliche Zeiten erfasst: 1.-10. Januar (80h)
- Restliche Tage (11.-15.) haben geplante Schichten (40h)

**Anzeige:**
- **Geplante Arbeitszeit:** 160h (alle geplanten Schichten)
- **Tatsächliche Arbeitszeit:** 120h (80h erfasst + 40h aus Planung für vergangene Tage ohne Erfassung)

### Szenario 2: Vergangener Monat komplett erfasst

**Situation:**
- Monat: Dezember 2025
- Alle Arbeitstage haben tatsächliche Zeiten erfasst

**Anzeige:**
- **Geplante Arbeitszeit:** 168h (ursprüngliche Planung)
- **Tatsächliche Arbeitszeit:** 173h (was wirklich gearbeitet wurde)

### Szenario 3: Zukünftiger Monat

**Situation:**
- Monat: Februar 2026
- Nur Planungen, keine tatsächlichen Zeiten

**Anzeige:**
- **Geplante Arbeitszeit:** 160h
- **Tatsächliche Arbeitszeit:** 0h (nichts gearbeitet, da alles in der Zukunft)

### Szenario 4: Aktueller Monat mit abweichenden tatsächlichen Zeiten

**Situation:**
- Einige Tage wurden länger gearbeitet als geplant
- Einige Tage wurden kürzer gearbeitet als geplant

**Nutzen:**
- Benutzer sieht sofort, ob er mehr oder weniger arbeitet als geplant
- Kann rechtzeitig Anpassungen vornehmen

## Berechnungslogik im Detail

### calculateActualWorkedHours()

```kotlin
Für jeden Tag im Monat:
    IF (tatsächliche Arbeitszeit erfasst):
        → Addiere erfasste Arbeitszeit
    ELSE IF (Schicht geplant UND Tag <= heute):
        → Addiere geplante Schichtzeit
    ELSE:
        → Addiere nichts
```

**Wichtig:**
- Zukünftige Tage werden NICHT gezählt (auch wenn Schicht geplant)
- Nur vergangene Tage mit Planung (aber ohne Erfassung) werden als "gearbeitet" gezählt
- Dies gibt eine realistische Einschätzung der bisher geleisteten Arbeit

## Technische Details

### Datenfluss

1. **loadMonth()** in CalendarViewModel
   - Lädt Zuweisungen und tatsächliche Zeiten
   - Triggert `loadMonthlySummary()`

2. **loadMonthlySummary()**
   - Ruft `calculateMonthlySummary()` auf

3. **calculateMonthlySummary()**
   - Ruft `calculateActualWorkedHours()` auf
   - Erstellt `MonthlySummary` mit `actualHours`

4. **UI Update**
   - `_uiState.update()` mit neuem MonthlySummary
   - `MonthlySummarySection` zeigt neue Zeile an

### Performance

- Die Berechnung erfolgt nur bei Änderungen (via StateFlow)
- Verwendet effiziente Datenbankabfragen (`.first()` auf Flows)
- Keine unnötigen Neuberechnungen

## Tests & Validierung

✅ Build erfolgreich (BUILD SUCCESSFUL in 17s)
✅ Keine Kompilierungsfehler
✅ Logik konsistent mit bestehenden Berechnungen
✅ UI-Integration ohne Layout-Probleme

## Vorteile für den Benutzer

1. **Transparenz:** Sieht sofort, wie viel wirklich gearbeitet wurde
2. **Vergleich:** Kann Plan vs. Ist-Arbeitszeit direkt vergleichen
3. **Kontrolle:** Erkennt frühzeitig, wenn zu viel oder zu wenig gearbeitet wird
4. **Motivation:** Kann Fortschritt im aktuellen Monat verfolgen

## Zusammenhang mit anderen Features

- **Tatsächliche Arbeitszeit erfassen:** Nutzt die bereits vorhandene Funktion zur Erfassung
- **Schichtverwaltung:** Nutzt geplante Schichten als Fallback
- **Wochenarbeitszeit:** Unabhängig, aber beide relevant für Soll-Ist-Vergleich
- **Vormonat-Bilanz:** Verwendet ähnliche Logik, aber für vergangene Monate

