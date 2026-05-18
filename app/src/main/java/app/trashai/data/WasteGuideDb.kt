package app.trashai.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

object WasteGuideDb {
    private const val ASSET_NAME = "wasteguide.sqlite3"
    private const val DB_FILENAME = "wasteguide.sqlite3"

    @Volatile private var db: SQLiteDatabase? = null

    fun open(context: Context): SQLiteDatabase {
        db?.let { return it }
        synchronized(this) {
            db?.let { return it }
            val file = File(context.filesDir, DB_FILENAME)
            var needsCopy = !file.exists()
            if (file.exists()) {
                runCatching {
                    val assetSize = context.assets.open(ASSET_NAME).use { it.available().toLong() }
                    if (file.length() != assetSize) {
                        needsCopy = true
                    }
                }
            }
            if (needsCopy) {
                runCatching {
                    context.assets.open(ASSET_NAME).use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
            val opened = SQLiteDatabase.openDatabase(
                file.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            db = opened
            return opened
        }
    }
}

data class ItemRule(
    val itemId: String,
    val itemName: String,
    val primaryCategory: String?,
    val dischargeMethod: String?,
    val featureText: String?,
    val cautionText: String?,
    val appSummary: String?,
    val sourceName: String,
    val sourceUrl: String,
)

data class KeywordHit(
    val itemId: String,
    val itemName: String,
    val matchedKeyword: String,
    val weight: Int,
)

data class CommonGuide(
    val guideId: String,
    val title: String,
    val subtitle: String?,
    val description: String,
    val tableHeaders: List<String>?,
    val tableRows: List<List<String>>?,
    val ctaLabel: String?,
    val ctaAction: String?,
)

private const val SELECT_RULE = """
    SELECT item_id, item_name, primary_category, discharge_method,
           feature_text, caution_text, app_summary, source_name, source_url
    FROM app_item_rule
"""

private fun android.database.Cursor.toItemRule() = ItemRule(
    itemId = getString(0),
    itemName = getString(1),
    primaryCategory = getString(2),
    dischargeMethod = getString(3),
    featureText = getString(4),
    cautionText = getString(5),
    appSummary = getString(6),
    sourceName = getString(7),
    sourceUrl = getString(8),
)

private fun android.database.Cursor.toCommonGuide(): CommonGuide {
    val headersJson = getString(4)
    val rowsJson = getString(5)

    val headers = runCatching {
        if (headersJson != null) {
            val arr = org.json.JSONArray(headersJson)
            List(arr.length()) { arr.getString(it) }
        } else null
    }.getOrNull()

    val rows = runCatching {
        if (rowsJson != null) {
            val arr = org.json.JSONArray(rowsJson)
            List(arr.length()) { i ->
                val sub = arr.getJSONArray(i)
                List(sub.length()) { j -> sub.getString(j) }
            }
        } else null
    }.getOrNull()

    return CommonGuide(
        guideId = getString(0),
        title = getString(1),
        subtitle = getString(2),
        description = getString(3),
        tableHeaders = headers,
        tableRows = rows,
        ctaLabel = getString(6),
        ctaAction = getString(7),
    )
}

fun SQLiteDatabase.firstItemRule(): ItemRule? =
    rawQuery("$SELECT_RULE ORDER BY item_name LIMIT 1", null).use { c ->
        if (c.moveToFirst()) c.toItemRule() else null
    }

fun SQLiteDatabase.itemById(itemId: String): ItemRule? =
    rawQuery("$SELECT_RULE WHERE item_id = ? LIMIT 1", arrayOf(itemId)).use { c ->
        if (c.moveToFirst()) c.toItemRule() else null
    }

fun SQLiteDatabase.commonGuideById(guideId: String): CommonGuide? =
    runCatching {
        rawQuery(
            "SELECT guide_id, title, subtitle, description, table_headers, table_rows, cta_label, cta_action FROM app_common_guide WHERE guide_id = ? LIMIT 1",
            arrayOf(guideId)
        ).use { c ->
            if (c.moveToFirst()) c.toCommonGuide() else null
        }
    }.getOrNull()

data class RegionOrdinance(
    val regionId: String,
    val sidoName: String,
    val sigunguName: String,
    val ordinanceTitle: String,
    val ordinanceText: String,
    val appSummary: String?,
    val sourceName: String,
    val sourceUrl: String,
)

private fun android.database.Cursor.toRegionOrdinance() = RegionOrdinance(
    regionId = getString(0),
    sidoName = getString(1),
    sigunguName = getString(2),
    ordinanceTitle = getString(3),
    ordinanceText = getString(4),
    appSummary = getString(5),
    sourceName = getString(6),
    sourceUrl = getString(7),
)

fun SQLiteDatabase.ordinanceByRegion(sido: String, sigungu: String): RegionOrdinance? {
    val cleanSigungu = sigungu.replace("특별시", "").replace("광역시", "").replace("특례시", "").trim()
    val parts = cleanSigungu.split(" ").filter { it.isNotBlank() }
    val candidates = parts.reversed() + cleanSigungu + sido
    
    for (cand in candidates) {
        if (cand.isBlank()) continue
        val res = runCatching {
            rawQuery(
                """
                SELECT region_id, sido_name, sigungu_name, ordinance_title, ordinance_text, app_summary, source_name, source_url
                FROM app_region_ordinance
                WHERE sigungu_name LIKE ? OR sido_name LIKE ?
                LIMIT 1
                """,
                arrayOf("%$cand%", "%$cand%")
            ).use { c ->
                if (c.moveToFirst()) c.toRegionOrdinance() else null
            }
        }.getOrNull()
        if (res != null) return res
    }
    return null
}

/**
 * Search app_search_keyword by exact or LIKE match (Korean strings expected).
 * Returns top hits ordered by weight desc, then keyword length asc (more specific first).
 */
fun SQLiteDatabase.searchByKeywords(needles: List<String>, limit: Int = 8): List<KeywordHit> {
    if (needles.isEmpty()) return emptyList()
    val results = mutableListOf<KeywordHit>()
    val seen = HashSet<String>()
    for (n in needles.distinct()) {
        if (n.isBlank()) continue
        val pattern = "%$n%"
        rawQuery(
            """
            SELECT k.target_id, r.item_name, k.keyword, k.weight
            FROM app_search_keyword k
            JOIN app_item_rule r ON r.item_id = k.target_id
            WHERE k.target_type = 'item' AND (k.keyword = ? OR k.keyword LIKE ?)
            ORDER BY (k.keyword = ?) DESC, k.weight DESC, length(k.keyword) ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(n, pattern, n, limit.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                if (seen.add(id)) {
                    results.add(
                        KeywordHit(
                            itemId = id,
                            itemName = c.getString(1),
                            matchedKeyword = c.getString(2),
                            weight = c.getInt(3),
                        )
                    )
                }
            }
        }
    }
    return results.take(limit)
}
