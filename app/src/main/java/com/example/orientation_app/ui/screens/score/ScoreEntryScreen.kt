package com.example.orientation_app.ui.screens.score

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orientation_app.R
import com.example.orientation_app.ui.theme.DarkCard
import com.example.orientation_app.ui.theme.DarkCardBorder
import com.example.orientation_app.ui.theme.InputFieldBg
import com.example.orientation_app.ui.theme.InputFieldBorder
import com.example.orientation_app.ui.theme.PeriwinkleButton
import com.example.orientation_app.ui.theme.ScoreCardBg
import com.example.orientation_app.ui.theme.TealPrimary
import com.example.orientation_app.ui.theme.TextGray
import com.example.orientation_app.ui.theme.TextMuted
import com.example.orientation_app.viewmodel.ScoreViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreEntryScreen(
    viewModel: ScoreViewModel = viewModel(),
    sectionId: String = "",
    selectedOptionalSubject: String = "",
    isSportExempt: Boolean = false,
    onBackClick: () -> Unit = {},
    onFilterClick: () -> Unit = {}
) {
    LaunchedEffect(sectionId, selectedOptionalSubject, isSportExempt) {
        viewModel.configureSubjects(
            sectionId = sectionId,
            subjectsForSection(
                sectionId = sectionId,
                optionalSubject = selectedOptionalSubject,
                isSportExempt = isSportExempt
            )
        )
    }

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
            ScoreTopBar(onBackClick = onBackClick)
            Spacer(Modifier.height(24.dp))
            ScoreHeader()
            Spacer(Modifier.height(20.dp))
            FgScoreCard(score = state.fgScore)
            Spacer(Modifier.height(24.dp))
            AverageScoreCard(computedAverage = state.computedAverage)
            Spacer(Modifier.height(20.dp))
            SubjectGradesGrid(
                grades = state.subjectGrades,
                onGradeChange = viewModel::updateSubjectGrade
            )
            Spacer(Modifier.height(28.dp))
            FilterButton(onClick = onFilterClick)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "كمّل صبّ الأعداد باش نوريوك الفاكولتات",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun subjectsForSection(
    sectionId: String,
    optionalSubject: String,
    isSportExempt: Boolean
): List<String> {
    val baseSubjects = when (sectionId) {
        "math", "science" -> mutableListOf(
            "الرياضيات", "الفيزياء", "العلوم", "الإعلامية", "العربية", "الفرنسية", "الإنجليزية", "الفلسفة"
        )

        "info" -> mutableListOf(
            "الخوارزميات", "STI", "الرياضيات", "الفيزياء", "العربية", "الفرنسية", "الإنجليزية", "الفلسفة"
        )

        "tech" -> mutableListOf(
            "الكهرباء", "الميكانيك", "الرياضيات", "الفيزياء", "الإعلامية", "العربية", "الفرنسية", "الإنجليزية", "الفلسفة"
        )

        "economy" -> mutableListOf(
            "الاقتصاد", "التصرف", "الرياضيات", "الإعلامية", "العربية", "تاريخ و جغرافيا", "الفرنسية", "الإنجليزية", "الفلسفة"
        )

        "literature" -> mutableListOf(
            "التربية الإسلامية (PISL)", "تاريخ و جغرافيا", "الإعلامية", "العربية", "الفرنسية", "الإنجليزية", "الفلسفة"
        )

        else -> mutableListOf(
            "الرياضيات", "الفيزياء", "العلوم", "الإعلامية", "العربية", "الفرنسية", "الإنجليزية", "الفلسفة"
        )
    }

    if (!isSportExempt) {
        baseSubjects.add("الرياضة")
    }

    if (optionalSubject.isNotBlank()) {
        baseSubjects.add("اختياري: $optionalSubject")
    }

    return baseSubjects
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar – app logo (from drawables) + Unicompass brand + bell icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + brand name (start side = right in RTL)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape)) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "شعار يونيكومباس",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.35f, scaleY = 1.35f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Unicompass",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Actions (end side = left in RTL)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "الإشعارات",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreHeader() {
    Text(
        text = "صبّ الأعداد متاعك",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "حطّ المعدّل و السكورات باش نعرفو وين نجّمو نوصلو.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextGray,
        lineHeight = 24.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// FG Score card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FgScoreCard(score: Double) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ScoreCardBg,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: score + delta badge
            Column {
                Text(
                    text = "سكور الـ FG توّا",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format("%.2f", score),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: math icon
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dynamic average display (score-style card)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AverageScoreCard(computedAverage: Double) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ScoreCardBg,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "المعدّل العام",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format("%.2f /20", computedAverage),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subject grades – 2-column grid of text fields
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SubjectGradesGrid(
    grades: Map<String, String>,
    onGradeChange: (String, String) -> Unit
) {
    val entries = grades.entries.toList()
    // Process in pairs (2 columns)
    for (i in entries.indices step 2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GradeField(
                label = entries[i].key,
                value = entries[i].value,
                onValueChange = { onGradeChange(entries[i].key, it) },
                modifier = Modifier.weight(1f)
            )
            if (i + 1 < entries.size) {
                GradeField(
                    label = entries[i + 1].key,
                    value = entries[i + 1].value,
                    onValueChange = { onGradeChange(entries[i + 1].key, it) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        if (i + 2 < entries.size) {
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun GradeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text("00.00", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = InputFieldBg,
                focusedContainerColor = InputFieldBg,
                unfocusedBorderColor = InputFieldBorder,
                focusedBorderColor = TealPrimary,
                cursorColor = TealPrimary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter / CTA button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PeriwinkleButton,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "صفّي الاختيارات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

