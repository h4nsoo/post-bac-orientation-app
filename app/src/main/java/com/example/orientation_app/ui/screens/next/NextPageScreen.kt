package com.example.orientation_app.ui.screens.next

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orientation_app.ui.theme.DarkCard
import com.example.orientation_app.ui.theme.DarkCardBorder
import com.example.orientation_app.ui.theme.InputFieldBg
import com.example.orientation_app.ui.theme.InputFieldBorder
import com.example.orientation_app.ui.theme.PeriwinkleButton
import com.example.orientation_app.ui.theme.TealPrimary
import com.example.orientation_app.ui.theme.TextGray
import com.example.orientation_app.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NextPageScreen(
    onBackClick: () -> Unit = {},
    onContinueClick: (String) -> Unit = {}
) {
    var interestText by remember { mutableStateOf("") }
    val quickSuggestions = listOf("سيبيرسيكورتي", "روبوتيك", "ديزاين", "بيزنس")

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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = TealPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.28f))
            ) {
                Text(
                    text = "توجيه ذكي بالذكاء الاصطناعي",
                    style = MaterialTheme.typography.labelLarge,
                    color = TealPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "اش تحب تعمل في حياتك؟",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                lineHeight = 44.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "صفّي اختيارات متاعك. أحكيلنا على طموحاتك باش نرشحولك مسارات أقرب ليك.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextGray,
                textAlign = TextAlign.End,
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        placeholder = {
                            Text(
                                text = "اكتب حلمك أو المجال اللي تحب تصنع فيه فرق...",
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        minLines = 6,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InputFieldBorder,
                            unfocusedBorderColor = InputFieldBorder,
                            focusedContainerColor = InputFieldBg,
                            unfocusedContainerColor = InputFieldBg,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        )
                    )

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onContinueClick(interestText.trim()) },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeriwinkleButton,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "التالي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "مقترحات ليك",
                style = MaterialTheme.typography.labelLarge,
                color = TextGray,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                quickSuggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DarkCard,
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.clickable { interestText = suggestion }
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
