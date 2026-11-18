package com.example.ui.converter

import androidx.lifecycle.ViewModel
import com.example.math.BigMath
import com.example.math.NotationMode
import com.example.math.UnitCategory
import com.example.math.UnitConverterEngine
import com.example.math.EngineeringUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

data class UnitEquivalent(
    val unit: EngineeringUnit,
    val formattedValue: String
)

data class ConverterUiState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val availableUnits: List<EngineeringUnit> = UnitConverterEngine.unitsByCategory.getValue(UnitCategory.LENGTH),
    val fromUnit: EngineeringUnit = UnitConverterEngine.unitsByCategory.getValue(UnitCategory.LENGTH)[0],
    val toUnit: EngineeringUnit = UnitConverterEngine.unitsByCategory.getValue(UnitCategory.LENGTH)[6], // inch or km
    val inputValue: String = "1",
    val convertedResult: String = "",
    val inputError: String? = null,
    val equivalents: List<UnitEquivalent> = emptyList()
)

class ConverterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(computeState(ConverterUiState()))
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    fun selectCategory(category: UnitCategory) {
        val units = UnitConverterEngine.unitsByCategory.getValue(category)
        _uiState.update {
            computeState(
                it.copy(
                    selectedCategory = category,
                    availableUnits = units,
                    fromUnit = units[0],
                    toUnit = units.getOrElse(1) { units[0] },
                    inputError = null
                )
            )
        }
    }

    fun selectFromUnit(unit: EngineeringUnit) {
        _uiState.update { computeState(it.copy(fromUnit = unit)) }
    }

    fun selectToUnit(unit: EngineeringUnit) {
        _uiState.update { computeState(it.copy(toUnit = unit)) }
    }

    fun swapUnits() {
        _uiState.update {
            computeState(
                it.copy(
                    fromUnit = it.toUnit,
                    toUnit = it.fromUnit
                )
            )
        }
    }

    fun updateInputValue(text: String) {
        _uiState.update { computeState(it.copy(inputValue = text)) }
    }

    private fun computeState(state: ConverterUiState): ConverterUiState {
        val raw = state.inputValue.trim()
        if (raw.isEmpty() || raw == "-" || raw == ".") {
            return state.copy(convertedResult = "0", inputError = null, equivalents = emptyList())
        }
        val bd = try {
            BigDecimal(raw)
        } catch (_: NumberFormatException) {
            return state.copy(convertedResult = "—", inputError = "Enter a valid number", equivalents = emptyList())
        }

        val primary = UnitConverterEngine.convert(bd, state.fromUnit, state.toUnit)
        val primaryFormatted = BigMath.formatResult(primary, precision = 12, notationMode = NotationMode.NORMAL)

        val eqs = state.availableUnits.map { unit ->
            val v = UnitConverterEngine.convert(bd, state.fromUnit, unit)
            UnitEquivalent(
                unit = unit,
                formattedValue = BigMath.formatResult(v, precision = 10, notationMode = NotationMode.NORMAL)
            )
        }

        return state.copy(
            convertedResult = primaryFormatted,
            inputError = null,
            equivalents = eqs
        )
    }
}
