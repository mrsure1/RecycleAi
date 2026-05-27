package app.trashai.supabase

import android.util.Log
import app.trashai.BuildConfig
import app.trashai.data.MoisDisposalRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Supabase PostgREST — [waste_disposal_rules] by sigungu_code.
 * Used when [app_mois_disposal] has no rows for the current region (hybrid fallback).
 */
class MoisDisposalClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    @Serializable
    private data class Row(
        val sigungu_code: String? = null,
        val sido_name: String? = null,
        val sigungu_name: String? = null,
        val category: String? = null,
        val disposal_method: String? = null,
        val disposal_time: String? = null,
    )

    suspend fun fetchBySigunguCode(sigunguCode: String): List<MoisDisposalRule> = withContext(Dispatchers.IO) {
        if (!isConfigured || sigunguCode.isBlank()) return@withContext emptyList()
        val base = supabaseUrl.trimEnd('/')
        val encoded = URLEncoder.encode(sigunguCode, StandardCharsets.UTF_8.name())
        val url = "$base/rest/v1/waste_disposal_rules" +
            "?sigungu_code=eq.$encoded" +
            "&select=sigungu_code,sido_name,sigungu_name,category,disposal_method,disposal_time" +
            "&order=category.asc"
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Accept", "application/json")
            .build()
        runCatching {
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.w(TAG, "MOIS fetch HTTP ${resp.code}: ${body.take(200)}")
                    return@use emptyList()
                }
                json.decodeFromString<List<Row>>(body).mapNotNull { row ->
                    val cat = row.category?.trim().orEmpty()
                    val time = row.disposal_time?.trim()
                    val method = row.disposal_method?.trim()
                    if (time.isNullOrBlank() && method.isNullOrBlank()) return@mapNotNull null
                    MoisDisposalRule(
                        sigunguCode = row.sigungu_code ?: sigunguCode,
                        sidoName = row.sido_name,
                        sigunguName = row.sigungu_name,
                        category = cat.ifBlank { "생활폐기물" },
                        disposalMethod = method,
                        disposalTime = time,
                    )
                }
            }
        }.getOrElse {
            Log.w(TAG, "MOIS fetch failed: ${it.message}")
            emptyList()
        }
    }

    private companion object {
        const val TAG = "MoisDisposalClient"
    }
}
