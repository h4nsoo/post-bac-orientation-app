package com.example.orientation_app.ui.screens.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orientation_app.R
import com.example.orientation_app.ui.components.SectionCard
import com.example.orientation_app.ui.theme.DarkCard
import com.example.orientation_app.ui.theme.DarkCardBorder
import com.example.orientation_app.ui.theme.TealPrimary
import com.example.orientation_app.ui.theme.TextGray
import com.example.orientation_app.ui.theme.TextMuted
import com.example.orientation_app.viewmodel.WelcomeViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = viewModel(),
    onContinue: (sectionId: String, optionalSubject: String, isSportExempt: Boolean) -> Unit = { _, _, _ -> }
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            TopBar()
            Spacer(Modifier.height(28.dp))
            WelcomeHeader()
            Spacer(Modifier.height(20.dp))
            TipCard()
            Spacer(Modifier.height(28.dp))
            SectionFilterHeader()
            Spacer(Modifier.height(14.dp))
            SectionsGrid(
                sections = state.sections,
                selectedIds = state.selectedSectionIds,
                onToggle = viewModel::toggleSection
            )
            Spacer(Modifier.height(14.dp))
            OptionalSubjectSection(
                subjects = state.optionalSubjects,
                selected = state.selectedOptionalSubject,
                expanded = state.isDropdownExpanded,
                onToggle = viewModel::toggleDropdown,
                onSelect = viewModel::selectOptionalSubject
            )
            Spacer(Modifier.height(28.dp))
            BacSportSection(
                isExempt = state.isSportExempt,
                onToggle = viewModel::toggleSportExempt
            )
            Spacer(Modifier.height(24.dp))
            ContinueButton(
                enabled = state.canProceed,
                onClick = {
                    val selectedSectionId = state.selectedSectionIds.firstOrNull() ?: return@ContinueButton
                    val optionalSubject = state.selectedOptionalSubject ?: return@ContinueButton
                    onContinue(selectedSectionId, optionalSubject, state.isSportExempt)
                }
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(86.dp).clip(CircleShape)) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "شعار التطبيق",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = 1.35f,
                        scaleY = 1.35f
                    )
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "مستقبلك يبدأ هنا",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TealPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "بوصلتك للنجاح",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Welcome header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeHeader() {
    Text(
        text = "مرحبا بيك",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
        lineHeight = 44.sp
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "صفّي الاختيارات متاعك و ابدأ رسمات المستقبل.\nاختار السيكسيون متاعك و خلّينا نعاونوك.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextGray,
        lineHeight = 24.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Recommendation tip card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TipCard() {
    val accentColor = TealPrimary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Vertical accent bar (drawn on the start side = right in RTL)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "توصية ملقاوحاش:",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "بناءً على البروفيل متاعك، نتصحّوك تركّز على السيكسيونات اللي فيها آفاق تقنية قوية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section filter header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionFilterHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TealPrimary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "اختار السيكسيون متاعك",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sections grid (2 columns, fixed height)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionsGrid(
    sections: List<com.example.orientation_app.data.model.SectionItem>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    val rows = (sections.size + 1) / 2
    val gridHeight = (rows * 92 + (rows - 1) * 12).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(sections, key = { it.id }) { section ->
            SectionCard(
                name = section.nameRes,
                iconType = section.iconType,
                isSelected = section.id in selectedIds,
                onClick = { onToggle(section.id) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Optional subject dropdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptionalSubjectSection(
    subjects: List<String>,
    selected: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String?) -> Unit
) {
    Text(
        text = "مادة اختيارية",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(10.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            shape = RoundedCornerShape(14.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected ?: "اختار مادة اختيارية...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected != null) MaterialTheme.colorScheme.onSurface else TextMuted
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onToggle,
            modifier = Modifier.background(DarkCard)
        ) {
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSelect(subject) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bac Sport section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BacSportSection(isExempt: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Outlined.SportsMartialArts,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "باك سبور",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "معفي من الرياضة؟ (Dispense)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }

            Switch(
                checked = isExempt,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TealPrimary,
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = DarkCardBorder
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Continue button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContinueButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TealPrimary,
            contentColor = Color.White,
            disabledContainerColor = DarkCard,
            disabledContentColor = TextMuted
        )
    ) {
        Text(
            text = "واصل",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

