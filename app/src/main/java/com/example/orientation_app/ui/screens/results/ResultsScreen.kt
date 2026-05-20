package com.example.orientation_app.ui.screens.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orientation_app.domain.model.Recommendation
import com.example.orientation_app.ui.theme.DarkCard
import com.example.orientation_app.ui.theme.DarkCardBorder
import com.example.orientation_app.ui.theme.PeriwinkleButton
import com.example.orientation_app.ui.theme.TealPrimary
import com.example.orientation_app.ui.theme.TextGray
import com.example.orientation_app.viewmodel.AiUiState
import com.example.orientation_app.viewmodel.AiViewModel

@Composable
fun ResultsScreen(
    sectionId: String,
    sectionName: String,
    fgScore: Double,
    interestText: String,
    onBackClick: () -> Unit = {},
    viewModel: AiViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sectionId, fgScore, interestText) {
        viewModel.getRecommendation(sectionId, sectionName, fgScore, interestText)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(14.dp))

            Surface(
                shape  = RoundedCornerShape(999.dp),
                color  = TealPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.28f))
            ) {
                Text(
                    text     = "توجيه ذكي بالذكاء الاصطناعي",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = TealPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text      = "التوصيات الجامعية",
                style     = MaterialTheme.typography.displayLarge,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                lineHeight = 44.sp,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = DarkCard,
                border   = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text       = String.format("%.2f", fgScore),
                        style      = MaterialTheme.typography.titleMedium,
                        color      = TealPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "نقطة الفرز :$sectionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val state = uiState) {
                is AiUiState.Idle, is AiUiState.Loading -> LoadingContent()
                is AiUiState.Success -> SuccessContent(state.recommendations)
                is AiUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.retry(sectionId, sectionName, fgScore, interestText) }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content states
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier          = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = TealPrimary, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
        Spacer(Modifier.height(20.dp))
        Text(text = "يتم تحليل ملفك الدراسي...", style = MaterialTheme.typography.bodyLarge, color = TextGray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuccessContent(recommendations: List<Recommendation>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        recommendations.forEach { rec -> RecommendationCard(rec) }
    }
}

@Composable
private fun RecommendationCard(rec: Recommendation) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = DarkCard,
        border   = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text       = rec.program.nameAr.stripDuration(),
                style      = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Rtl),
                color      = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.End,
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = rec.program.nameFr.stripDuration(),
                style     = MaterialTheme.typography.bodySmall,
                color     = TextGray,
                textAlign = TextAlign.End,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Surface(
                    shape  = RoundedCornerShape(999.dp),
                    color  = TextGray.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TextGray.copy(alpha = 0.2f))
                ) {
                    Text(
                        text     = "مقاعد: ${rec.program.capacity}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextGray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape  = RoundedCornerShape(999.dp),
                    color  = TealPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.28f))
                ) {
                    Text(
                        text     = "الفرز: ${String.format("%.2f", rec.program.lastYearScore)}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TealPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(Modifier.height(10.dp))
            Text(
                text       = rec.reason,
                style      = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                color      = TextGray,
                textAlign  = TextAlign.End,
                lineHeight = 22.sp,
                modifier   = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun String.stripDuration(): String = substringBefore("(").trim()

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = DarkCard, border = BorderStroke(1.dp, DarkCardBorder)) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextGray, modifier = Modifier.padding(18.dp), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            shape   = RoundedCornerShape(999.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = PeriwinkleButton, contentColor = Color.White)
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "إعادة المحاولة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
