package app.trashai.supabase

import android.util.Log
import app.trashai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Supabase Edge Functions의 벡터 검색 엔드포인트(search-trash-vector)와 통신하여
 * 이미지 임베딩 코사인 유사도 기반의 의미론적 분리수거 항목 검색 결과를 가져옵니다.
 */
class SupabaseVectorClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    @Serializable
    data class VectorResult(
        val id: Long,
        val item_name: String,
        val category: String? = null,
        val disposal_method: String? = null,
        val disposal_time: String? = null,
        val similarity: Double
    )

    @Serializable
    data class VectorResponse(
        val results: List<VectorResult> = emptyList()
    )

    /**
     * 크롭된 이미지 바이트(JPEG)와 거주지 시군구 코드를 넘겨
     * pgvector 기반 의미론적 코사인 유사도 검색 상위 결과를 획득합니다.
     */
    suspend fun searchTrashVector(
        jpegBytes: ByteArray,
        sigunguCode: String
    ): SupabaseResult<List<VectorResult>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext SupabaseResult.NotConfigured
        if (jpegBytes.isEmpty()) return@withContext SupabaseResult.InvalidInput("이미지 데이터가 비어있습니다.")

        val url = "$supabaseUrl/functions/v1/search-trash-vector"
        val requestBody = jpegBytes.toRequestBody("application/octet-stream".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("sigungu-code", sigunguCode)
            .build()

        runCatching {
            http.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Supabase Vector API HTTP Error ${resp.code}: $bodyStr")
                    return@use SupabaseResult.HttpError(resp.code, bodyStr.take(400))
                }

                val parsed = runCatching { json.decodeFromString<VectorResponse>(bodyStr) }.getOrNull()
                if (parsed == null) {
                    Log.w(TAG, "Parsing JSON failed. response=$bodyStr")
                    SupabaseResult.ParseError(bodyStr.take(400))
                } else {
                    SupabaseResult.Ok(parsed.results)
                }
            }
        }.getOrElse {
            Log.w(TAG, "Network exception: ${it.message}")
            SupabaseResult.NetworkError(it.message ?: it::class.java.simpleName)
        }
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
