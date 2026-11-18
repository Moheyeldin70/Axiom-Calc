package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.math.AngleUnit
import com.example.math.NotationMode
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "axiom_calc_prefs")

data class CalculatorSettings(
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val notationMode: NotationMode = NotationMode.NORMAL,
    val precisionDigits: Int = 14,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val memoryValue: String = "0",
    val lastAns: String = "0"
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val NOTATION_MODE = stringPreferencesKey("notation_mode")
        val PRECISION_DIGITS = intPreferencesKey("precision_digits")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val MEMORY_VALUE = stringPreferencesKey("memory_value")
        val LAST_ANS = stringPreferencesKey("last_ans")
    }

    val settingsFlow: Flow<CalculatorSettings> = context.dataStore.data.map { prefs ->
        CalculatorSettings(
            angleUnit = prefs[Keys.ANGLE_UNIT]?.let {
                runCatching { AngleUnit.valueOf(it) }.getOrNull()
            } ?: AngleUnit.DEG,
            notationMode = prefs[Keys.NOTATION_MODE]?.let {
                runCatching { NotationMode.valueOf(it) }.getOrNull()
            } ?: NotationMode.NORMAL,
            precisionDigits = (prefs[Keys.PRECISION_DIGITS] ?: 14).coerceIn(6, 28),
            themePreference = prefs[Keys.THEME_PREFERENCE]?.let {
                runCatching { ThemePreference.valueOf(it) }.getOrNull()
            } ?: ThemePreference.DARK,
            memoryValue = prefs[Keys.MEMORY_VALUE] ?: "0",
            lastAns = prefs[Keys.LAST_ANS] ?: "0"
        )
    }

    suspend fun setAngleUnit(unit: AngleUnit) {
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun setNotationMode(mode: NotationMode) {
        context.dataStore.edit { it[Keys.NOTATION_MODE] = mode.name }
    }

    suspend fun setPrecisionDigits(digits: Int) {
        context.dataStore.edit { it[Keys.PRECISION_DIGITS] = digits.coerceIn(6, 28) }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_PREFERENCE] = theme.name }
    }

    suspend fun setMemoryValue(value: String) {
        context.dataStore.edit { it[Keys.MEMORY_VALUE] = value }
    }

    suspend fun setLastAns(value: String) {
        context.dataStore.edit { it[Keys.LAST_ANS] = value }
    }
}
