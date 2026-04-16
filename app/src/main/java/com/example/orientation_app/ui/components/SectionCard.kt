package com.example.orientation_app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.orientation_app.data.model.SectionIconType
import com.example.orientation_app.ui.theme.DarkCard
import com.example.orientation_app.ui.theme.DarkCardBorder
import com.example.orientation_app.ui.theme.TealGlow
import com.example.orientation_app.ui.theme.TealPrimary
import com.example.orientation_app.ui.theme.TextGray

/**
 * A toggleable card representing a baccalaureate section.
 */
@Composable
fun SectionCard(
    name: String,
    iconType: SectionIconType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) TealPrimary else DarkCardBorder,
        animationSpec = tween(250),
        label = "borderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) TealGlow else DarkCard,
        animationSpec = tween(250),
        label = "containerColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = iconForType(iconType),
                contentDescription = name,
                tint = if (isSelected) TealPrimary else TextGray,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Map [SectionIconType] to a Material icon vector. */
private fun iconForType(type: SectionIconType): ImageVector = when (type) {
    SectionIconType.MATH -> Icons.Outlined.Calculate
    SectionIconType.SCIENCE -> Icons.Outlined.Science
    SectionIconType.TECH -> Icons.Outlined.Engineering
    SectionIconType.INFORMATICS -> Icons.Outlined.Computer
    SectionIconType.LITERATURE -> Icons.Outlined.AutoStories
    SectionIconType.ECONOMY -> Icons.AutoMirrored.Outlined.TrendingUp
    SectionIconType.SPORTS -> Icons.Outlined.SportsSoccer
}
