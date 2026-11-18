package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculatorRepository(
    private val calculationDao: CalculationDao,
    private val preferencesRepository: PreferencesRepository
) {
    val historyFlow: Flow<List<CalculationRecord>> = calculationDao.observeHistory()
    val settingsFlow: Flow<CalculatorSettings> = preferencesRepository.settingsFlow

    suspend fun saveCalculation(expression: String, result: String, angleUnit: String) {
        calculationDao.insert(
            CalculationRecord(
                expression = expression,
                result = result,
                angleUnit = angleUnit
            )
        )
        preferencesRepository.setLastAns(result)
    }

    suspend fun deleteHistoryItem(id: Long) = calculationDao.deleteById(id)

    suspend fun clearHistory() = calculationDao.clearAll()

    suspend fun updateAngleUnit(unit: com.example.math.AngleUnit) =
        preferencesRepository.setAngleUnit(unit)

    suspend fun updateNotationMode(mode: com.example.math.NotationMode) =
        preferencesRepository.setNotationMode(mode)

    suspend fun updatePrecisionDigits(digits: Int) =
        preferencesRepository.setPrecisionDigits(digits)

    suspend fun updateThemePreference(theme: com.example.ui.theme.ThemePreference) =
        preferencesRepository.setThemePreference(theme)

    suspend fun updateMemoryValue(value: String) =
        preferencesRepository.setMemoryValue(value)
}
