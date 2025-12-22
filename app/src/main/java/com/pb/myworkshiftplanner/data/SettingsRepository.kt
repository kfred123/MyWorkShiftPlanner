package com.pb.myworkshiftplanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val CALENDAR_ID_KEY = stringPreferencesKey("selected_calendar_id")
        private val CALENDAR_NAME_KEY = stringPreferencesKey("selected_calendar_name")
    }

    val selectedCalendarId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CALENDAR_ID_KEY]
        }

    val selectedCalendarName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CALENDAR_NAME_KEY]
        }

    suspend fun saveSelectedCalendar(calendarId: String, calendarName: String) {
        context.dataStore.edit { preferences ->
            preferences[CALENDAR_ID_KEY] = calendarId
            preferences[CALENDAR_NAME_KEY] = calendarName
        }
    }

    suspend fun clearSelectedCalendar() {
        context.dataStore.edit { preferences ->
            preferences.remove(CALENDAR_ID_KEY)
            preferences.remove(CALENDAR_NAME_KEY)
        }
    }
}

