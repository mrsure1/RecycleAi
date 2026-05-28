package app.trashai.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import app.trashai.gemini.GeminiClient
import app.trashai.gemini.GeminiResult

/**
 * Legacy class name: on-device ML Kit label heuristics or direct Gemini keyword extraction.
 * Runtime does not call Supabase PostgREST or Edge Functions.
 */
object TrashAiConfig {
    /** true: ML Kit label → local keyword heuristics only (no Gemini). */
    const val USE_LOCAL_VECTOR_SEARCH = false
}

class SupabaseVectorClient {
    private val gemini = GeminiClient()

    val isConfigured: Boolean get() = gemini.isConfigured

    @Serializable
    data class VectorResult(
        val id: Long,
        val item_name: String,
        val category: String? = null,
        val disposal_method: String? = null,
        val disposal_time: String? = null,
        val similarity: Double,
    )

    /**
     * Returns Korean item-name keywords for local SQLite grounding.
     */
    suspend fun searchTrashVector(
        jpegBytes: ByteArray,
        sigunguCode: String,
        rawLabel: String? = null,
    ): SupabaseResult<List<VectorResult>> = withContext(Dispatchers.IO) {
        if (TrashAiConfig.USE_LOCAL_VECTOR_SEARCH) {
            return@withContext localSearchVector(rawLabel)
        }

        if (!isConfigured) return@withContext SupabaseResult.NotConfigured
        if (jpegBytes.isEmpty()) return@withContext SupabaseResult.InvalidInput("이미지 데이터가 비어있습니다.")

        when (val geminiRes = gemini.classifyTrashKeywords(jpegBytes)) {
            is GeminiResult.Ok -> {
                val keywords = geminiRes.value
                if (keywords.isEmpty()) {
                    SupabaseResult.Ok(emptyList())
                } else {
                    val results = keywords.mapIndexed { idx, item ->
                        VectorResult(
                            id = idx.toLong() + 1,
                            item_name = item,
                            category = "재활용",
                            disposal_method = "",
                            disposal_time = "",
                            similarity = if (idx == 0) 1.0 else 0.8,
                        )
                    }
                    SupabaseResult.Ok(results)
                }
            }
            is GeminiResult.HttpError -> SupabaseResult.HttpError(geminiRes.code, geminiRes.body)
            is GeminiResult.NetworkError -> SupabaseResult.NetworkError(geminiRes.detail)
            is GeminiResult.ParseError -> SupabaseResult.ParseError(geminiRes.rawSnippet)
            is GeminiResult.InvalidInput -> SupabaseResult.InvalidInput(geminiRes.detail)
            is GeminiResult.NotConfigured -> SupabaseResult.NotConfigured
        }
    }

    private fun localSearchVector(rawLabel: String?): SupabaseResult<List<VectorResult>> {
        val labelLower = rawLabel?.lowercase() ?: ""
        val matchedKorean = when {
            labelLower.contains("bottle") -> "우유팩"
            labelLower.contains("container") -> "플라스틱"
            labelLower.contains("food") -> "음식물"
            labelLower.contains("paper") -> "종이"
            labelLower.contains("book") -> "책"
            labelLower.contains("glass") -> "유리병"
            labelLower.contains("appliance") -> "가전제품"
            labelLower.contains("clothing") || labelLower.contains("fashion") -> "의류"
            labelLower.contains("plastic") -> "플라스틱"
            labelLower.contains("home goods") || labelLower.contains("home") -> "플라스틱"
            labelLower.contains("plants") || labelLower.contains("plant") -> "나무"
            else -> "일반쓰레기"
        }

        val results = listOf(
            VectorResult(
                id = 1L,
                item_name = matchedKorean,
                category = "온디바이스",
                disposal_method = "로컬 매칭 완료",
                disposal_time = "",
                similarity = 1.0,
            ),
        )
        Log.d(TAG, "USE_LOCAL_VECTOR_SEARCH - ML Kit: '$rawLabel' -> '$matchedKorean'")
        return SupabaseResult.Ok(results)
    }

    private companion object {
        const val TAG = "SupabaseVectorClient"
    }
}

sealed interface SupabaseResult<out T> {
    data class Ok<T>(val value: T) : SupabaseResult<T>
    data object NotConfigured : SupabaseResult<Nothing>
    data class InvalidInput(val detail: String) : SupabaseResult<Nothing>
    data class HttpError(val code: Int, val body: String) : SupabaseResult<Nothing>
    data class ParseError(val rawSnippet: String) : SupabaseResult<Nothing>
    data class NetworkError(val detail: String) : SupabaseResult<Nothing>
}
