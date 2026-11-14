package com.example.orientation_app.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OrientationLambdaHandlerTest {

    @Test
    fun `computePromptHash produces identical hash for scores in the same 0_5 bucket and whitespace variations`() {
        val h1 = OrientationLambdaHandler.computePromptHash(
            sectionName = "رياضيات",
            fgScore = 165.12,
            interestText = "مهتم   بالذكاء الاصطناعي",
            candidateIds = listOf(10, 5, 2)
        )
        val h2 = OrientationLambdaHandler.computePromptHash(
            sectionName = "  رياضيات ",
            fgScore = 165.20,
            interestText = "مهتم بالذكاء الاصطناعي",
            candidateIds = listOf(2, 5, 10)
        )
        assertThat(h1).isEqualTo(h2)
        assertThat(h1).hasLength(64)
    }

    @Test
    fun `computePromptHash produces different hash when interestText changes`() {
        val h1 = OrientationLambdaHandler.computePromptHash(
            sectionName = "رياضيات",
            fgScore = 165.0,
            interestText = "ذكاء اصطناعي",
            candidateIds = listOf(1, 2, 3)
        )
        val h2 = OrientationLambdaHandler.computePromptHash(
            sectionName = "رياضيات",
            fgScore = 165.0,
            interestText = "طب عام",
            candidateIds = listOf(1, 2, 3)
        )
        assertThat(h1).isNotEqualTo(h2)
    }

    @Test
    fun `cleanAndValidateJsonArray strips markdown fences and filters out invalid program IDs`() {
        val raw = """
            ```json
            [
              {"id": 10, "reason": "يناسب شغفك بالبرمجة"},
              {"id": 999, "reason": "خارج القائمة"},
              {"id": 20, "reason": "فرصة ممتازة للهندسة"}
            ]
            ```
        """.trimIndent()

        val result = OrientationLambdaHandler.cleanAndValidateJsonArray(raw, setOf(10, 20, 30))
        assertThat(result.length()).isEqualTo(2)
        assertThat(result.getJSONObject(0).getInt("id")).isEqualTo(10)
        assertThat(result.getJSONObject(1).getInt("id")).isEqualTo(20)
    }
}
