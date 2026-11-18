package com.example.ui.converter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.math.EngineeringUnit
import com.example.math.UnitCategory
import com.example.ui.components.PrecisionCard
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    onSendToCalculator: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    fun copyResult(value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Converted Value", value))
        onShowMessage("Copied $value")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Engineering Unit Converter",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "High-precision conversions across 14 physical dimensions",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }

        // Category Horizontal Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.selectedCategory == cat,
                    onClick = { viewModel.selectCategory(cat) },
                    label = { Text(cat.title) },
                    modifier = Modifier.testTag("unit_cat_${cat.name.lowercase()}")
                )
            }
        }

        // Primary Conversion Card
        PrecisionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // From Input + Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputValue,
                        onValueChange = viewModel::updateInputValue,
                        label = { Text("Input (${state.fromUnit.symbol})") },
                        isError = state.inputError != null,
                        supportingText = state.inputError?.let { err -> { Text(err) } },
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 18.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("converter_input_field")
                    )
                    UnitSelectorDropdown(
                        selectedUnit = state.fromUnit,
                        units = state.availableUnits,
                        onSelect = viewModel::selectFromUnit,
                        testTag = "converter_from_dropdown",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Swap Divider Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = cs.outlineVariant)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = cs.secondaryContainer,
                        border = BorderStroke(1.dp, cs.secondary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .testTag("converter_swap_button")
                            .clickable(onClick = viewModel::swapUnits)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.SwapVert,
                                contentDescription = stringResource(R.string.converter_swap_units),
                                tint = cs.secondary
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = cs.outlineVariant)
                }

                // Result + To Unit Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cs.surfaceContainerLowest,
                        border = BorderStroke(1.dp, cs.secondary.copy(alpha = 0.45f)),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = "Converted (${state.toUnit.symbol})",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.secondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = state.convertedResult,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = cs.onBackground,
                                modifier = Modifier.testTag("converter_result_text")
                            )
                        }
                    }
                    UnitSelectorDropdown(
                        selectedUnit = state.toUnit,
                        units = state.availableUnits,
                        onSelect = viewModel::selectToUnit,
                        testTag = "converter_to_dropdown",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Actions
                if (state.inputError == null && state.convertedResult != "—") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { copyResult(state.convertedResult) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("converter_copy_button")
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                        OutlinedButton(
                            onClick = { onSendToCalculator(state.convertedResult) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("converter_send_calc_button")
                        ) {
                            Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.converter_send_to_calc), maxLines = 1)
                        }
                    }
                }
            }
        }

        // All Equivalents Comparison Table
        if (state.equivalents.isNotEmpty()) {
            PrecisionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "All ${state.selectedCategory.title} Equivalents",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.tertiary
                    )
                    HorizontalDivider(color = cs.outlineVariant)
                    state.equivalents.forEach { eq ->
                        val isTarget = eq.unit.id == state.toUnit.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectToUnit(eq.unit) }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = eq.unit.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTarget) cs.secondary else cs.onSurface
                                )
                                Text(
                                    text = eq.unit.symbol,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = eq.formattedValue,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isTarget) cs.secondary else cs.onSurface
                                )
                                IconButton(
                                    onClick = { copyResult(eq.formattedValue) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy ${eq.unit.name}",
                                        modifier = Modifier.size(15.dp),
                                        tint = cs.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitSelectorDropdown(
    selectedUnit: EngineeringUnit,
    units: List<EngineeringUnit>,
    onSelect: (EngineeringUnit) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cs.surfaceContainer,
            border = BorderStroke(1.dp, cs.outline),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .testTag(testTag)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedUnit.symbol,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = cs.onSurface
                    )
                    Text(
                        text = selectedUnit.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = "Select unit",
                    tint = cs.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = unit.symbol,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = cs.secondary
                            )
                            Text(text = unit.name)
                        }
                    },
                    onClick = {
                        onSelect(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
