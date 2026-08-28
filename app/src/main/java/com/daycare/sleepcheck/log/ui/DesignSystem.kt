package com.daycare.sleepcheck.log.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object DaycareColors {
    val DeepGreen = Color(0xFF236044)
    val LeafGreen = Color(0xFF3B8B63)
    val SoftGreen = Color(0xFFDDEFE3)
    val Mint = Color(0xFFF1F8F3)
    val WarmBackground = Color(0xFFF8FAF7)
    val Ink = Color(0xFF18352A)
    val MutedInk = Color(0xFF60756A)
    val Amber = Color(0xFF9A6500)
    val AmberSurface = Color(0xFFFFF1D2)
    val Error = Color(0xFFB3261E)
    val ErrorSurface = Color(0xFFFFE8E5)
}

object DaycareSpacing {
    val Page = 20.dp
    val Section = 24.dp
    val Card = 18.dp
    val Compact = 12.dp
    val Tiny = 8.dp
}

object DaycareShapes {
    val LargeCard = RoundedCornerShape(24.dp)
    val Card = RoundedCornerShape(18.dp)
    val SmallCard = RoundedCornerShape(14.dp)
    val Pill = RoundedCornerShape(50)
}

object DaycareElevation {
    val Card = 2.dp
    val Hero = 5.dp
}

object DaycareTypography {
    val PageTitle: androidx.compose.ui.text.TextStyle
        @Composable get() = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
    val SectionTitle: androidx.compose.ui.text.TextStyle
        @Composable get() = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    val CardTitle: androidx.compose.ui.text.TextStyle
        @Composable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
}

enum class StatusTone { Success, Warning, Error, Neutral }

data class DaycareSnackbarVisuals(
    override val message: String,
    val tone: StatusTone,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Long,
) : SnackbarVisuals

@Composable
fun DaycareCard(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = if (hero) DaycareShapes.LargeCard else DaycareShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hero) DaycareElevation.Hero else DaycareElevation.Card),
        content = { Box(Modifier.padding(DaycareSpacing.Card), content = { content() }) },
    )
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = DaycareShapes.SmallCard,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = DaycareShapes.SmallCard,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(8.dp))
        }
        Text(text)
    }
}

@Composable
fun IconActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 116.dp)
            .clickable(onClick = onClick),
        shape = DaycareShapes.Card,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Text(title, style = DaycareTypography.CardTitle)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int? = null, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = DaycareTypography.SectionTitle, modifier = Modifier.weight(1f))
        if (count != null) CountBadge(count)
        if (action != null) action()
    }
}

@Composable
fun CountBadge(count: Int) {
    Surface(shape = DaycareShapes.Pill, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(count.toString(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun StatusPill(text: String, tone: StatusTone = StatusTone.Neutral) {
    val dark = isSystemInDarkTheme()
    val (background, foreground) = when (tone) {
        StatusTone.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StatusTone.Warning -> (if (dark) Color(0xFF4A3716) else DaycareColors.AmberSurface) to (if (dark) Color(0xFFFFDFA3) else DaycareColors.Amber)
        StatusTone.Error -> (if (dark) Color(0xFF5F201B) else DaycareColors.ErrorSurface) to (if (dark) Color(0xFFFFB4AB) else DaycareColors.Error)
        StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = DaycareShapes.Pill, color = background) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}

@Composable
fun InfoCard(title: String, body: String, tone: StatusTone = StatusTone.Neutral, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val background = when (tone) {
        StatusTone.Warning -> if (dark) Color(0xFF4A3716) else DaycareColors.AmberSurface
        StatusTone.Error -> if (dark) Color(0xFF5F201B) else DaycareColors.ErrorSurface
        StatusTone.Success -> if (dark) Color(0xFF1D4632) else DaycareColors.Mint
        StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val foreground = when (tone) {
        StatusTone.Warning -> if (dark) Color(0xFFFFDFA3) else DaycareColors.Amber
        StatusTone.Error -> if (dark) Color(0xFFFFB4AB) else DaycareColors.Error
        StatusTone.Success -> if (dark) Color(0xFFD0F2DE) else DaycareColors.Ink
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(modifier = modifier.fillMaxWidth(), shape = DaycareShapes.SmallCard, color = background) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = foreground)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = foreground)
        }
    }
}

@Composable
fun DaycareTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        shape = DaycareShapes.SmallCard,
    )
}

@Composable
fun DaycareListRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        trailing?.invoke()
    }
}

@Composable
fun SuccessSnackbar(data: SnackbarData) {
    Snackbar(
        shape = DaycareShapes.SmallCard,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.padding(12.dp),
    ) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.CheckCircle, contentDescription = null); Text(data.visuals.message) } }
}

@Composable
fun SuccessDialog(title: String, body: String, onDismiss: () -> Unit, confirmText: String) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(confirmText) } },
    )
}

@Composable
fun WarningBanner(title: String, body: String, tone: StatusTone = StatusTone.Warning) {
    InfoCard(title, body, tone)
}
