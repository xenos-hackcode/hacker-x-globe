package com.xhacker.cedal.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope

@Composable
fun CedalCard(
    modifier: Modifier = Modifier,
    // Small phones / an open keyboard can leave less vertical space than the
    // card's content needs — scroll by default so nothing gets clipped.
    // TermsGateScreen opts out since it manages its own internal scroll
    // region (a fixed header/button with only the middle text scrolling).
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CedalColors.CardBackground)
            .border(1.5.dp, CedalColors.BorderCyan, RoundedCornerShape(24.dp))
            .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it }
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun CedalHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CedalColors.BackgroundBlob)
                .border(1.dp, CedalColors.BorderCyan, RoundedCornerShape(12.dp))
                .let {
                    if (onBack != null) {
                        it.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack)
                    } else it
                },
            contentAlignment = Alignment.Center,
        ) {
            // Same logo glyph always — just wired to act as a back button
            // when onBack is provided, no visual change.
            Text("◎", color = CedalColors.AccentCyan, fontSize = 18.sp)
        }
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                title.uppercase(),
                color = CedalColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )
            Text(
                subtitle.uppercase(),
                color = CedalColors.AccentSky,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 16.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.AccentCyan),
    )
}

@Composable
fun CedalSectionLabel(label: String, badge: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(), color = CedalColors.TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
        CedalChip(badge)
    }
}

@Composable
fun CedalChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CedalColors.BackgroundBlob)
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text.uppercase(), color = CedalColors.AccentIndigo, fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}

@Composable
fun CedalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CedalColors.Background)
            .border(1.5.dp, CedalColors.BorderSlate, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(prefix, color = CedalColors.TextMuted, fontSize = 13.sp)
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            if (value.isEmpty()) {
                Text(placeholder, color = CedalColors.TextSecondary, fontSize = 14.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = CedalColors.TextPrimary, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(CedalColors.AccentCyan),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isPassword) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(CedalColors.BackgroundBlob)
                    .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { visible = !visible }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(if (visible) "HIDE" else "SHOW", color = CedalColors.TextPrimary, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
        }
    }
}

@Composable
fun CedalPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (enabled) CedalColors.AccentCyan else CedalColors.AccentCyan.copy(alpha = 0.4f))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = CedalColors.Background, modifier = Modifier.size(18.dp))
        } else {
            Text(text.uppercase(), color = CedalColors.Background, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
        }
    }
}

@Composable
fun CedalGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(CedalColors.Background.copy(alpha = 0.95f))
            .border(1.dp, CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(text.uppercase(), color = CedalColors.TextLink, fontSize = 11.sp, letterSpacing = 2.sp)
    }
}

@Composable
fun CedalLinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(text.uppercase(), color = CedalColors.TextLink, fontSize = 11.sp, letterSpacing = 1.5.sp)
    }
}

@Composable
fun CedalToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp, 22.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) CedalColors.Success.copy(alpha = 0.4f) else CedalColors.BackgroundBlob)
            .border(1.dp, if (checked) CedalColors.Success else CedalColors.BorderSlate, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) CedalColors.SuccessKnob else CedalColors.ToggleOffKnob),
        )
    }
}

// A glowing progress bar for xp/exp-style meters (Shop's Tier, Profile's
// Rank) - a plain flat-color bar reads as "just a loading bar", the glow is
// what makes it read as a currency/progress meter instead. The glow is a
// real elevation shadow tinted with the bar's own color (works on every API
// level this app supports, unlike a true RenderEffect blur which needs 31+),
// not a fake blurred-box trick. Tall enough (20dp) to fit label directly on
// it - e.g. "900 / 1500 xp" - since the numbers above the bar are the
// account's running total, not what this specific bar's fill represents
// (progress within the current level), which reads as ambiguous on its own.
@Composable
fun CedalNeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = CedalColors.AccentCyan,
    label: String? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(50))
            .background(CedalColors.BorderSlate),
        contentAlignment = Alignment.Center,
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(50), ambientColor = color, spotColor = color)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
        label?.let {
            Text(it, color = CedalColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CedalErrorText(text: String?) {
    text?.let {
        Text(it, color = CedalColors.Error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
