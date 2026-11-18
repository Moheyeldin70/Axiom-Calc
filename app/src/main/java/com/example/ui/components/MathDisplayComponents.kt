package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

enum class KeyRole {
    DIGIT,
    OPERATOR,
    SCIENTIFIC,
    ACTION,
    EQUALS,
    MODIFIER_ACTIVE
}

@Composable
fun PrecisionKeyButton(
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    role: KeyRole = KeyRole.DIGIT,
    fontSize: TextUnit = 18.sp,
    minHeight: Dp = 50.dp,
    cornerRadius: Dp = 12.dp
) {
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme

    val targetContainer = when (role) {
        KeyRole.DIGIT -> cs.surfaceContainerHigh
        KeyRole.OPERATOR -> cs.tertiaryContainer
        KeyRole.SCIENTIFIC -> cs.surfaceContainer
        KeyRole.ACTION -> cs.surfaceContainerHighest
        KeyRole.EQUALS -> cs.primary
        KeyRole.MODIFIER_ACTIVE -> cs.secondaryContainer
    }
    val targetContent = when (role) {
        KeyRole.DIGIT -> cs.onBackground
        KeyRole.OPERATOR -> cs.onTertiaryContainer
        KeyRole.SCIENTIFIC -> cs.onSurface
        KeyRole.ACTION -> cs.error
        KeyRole.EQUALS -> cs.onPrimary
        KeyRole.MODIFIER_ACTIVE -> cs.secondary
    }
    val targetBorder = when (role) {
        KeyRole.EQUALS -> cs.primary
        KeyRole.MODIFIER_ACTIVE -> cs.secondary
        KeyRole.OPERATOR -> cs.tertiary.copy(alpha = 0.35f)
        else -> cs.outlineVariant
    }

    val containerColor by animateColorAsState(targetValue = targetContainer, label = "keyBg")
    val contentColor by animateColorAsState(targetValue = targetContent, label = "keyFg")

    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .minimumInteractiveComponentSize()
            .clip(shape)
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple()
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, targetBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (subLabel != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = subLabel,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = label,
                        fontFamily = if (role == KeyRole.DIGIT) JetBrainsMonoFontFamily else SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = fontSize,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = label,
                    fontFamily = if (role == KeyRole.DIGIT || role == KeyRole.OPERATOR) {
                        JetBrainsMonoFontFamily
                    } else {
                        SpaceGroteskFontFamily
                    },
                    fontWeight = if (role == KeyRole.EQUALS) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = fontSize,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (active) accentColor.copy(alpha = 0.18f) else cs.surfaceContainer
    val border = if (active) accentColor.copy(alpha = 0.7f) else cs.outlineVariant
    val fg = if (active) accentColor else cs.onSurfaceVariant

    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PrecisionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 1.dp
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun QuickTokenRow(
    tokens: List<Pair<String, String>>,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tokens.forEach { (label, insert) ->
            Surface(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("quick_token_${label.lowercase()}")
                    .clickable { onInsert(insert) },
                shape = RoundedCornerShape(8.dp),
                color = cs.surfaceContainer,
                border = BorderStroke(1.dp, cs.outlineVariant)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = cs.tertiary
                    )
                }
            }
        }
    }
}
