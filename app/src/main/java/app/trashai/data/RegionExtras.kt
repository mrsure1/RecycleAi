package app.trashai.data

import android.database.sqlite.SQLiteDatabase

/** 행안부 Open API 기반 지역별 배출 요일·시간 (빌드 시 SQLite [app_mois_disposal]에 적재). */
data class MoisDisposalRule(
    val sigunguCode: String,
    val sidoName: String?,
    val sigunguName: String?,
    val category: String,
    val disposalMethod: String?,
    val disposalTime: String?,
) {
    val hasSchedule: Boolean get() = !disposalTime.isNullOrBlank()
}

/** 지자체 분리수거 문의처 — 데이터가 있을 때만 UI에 표시. */
data class RegionContact(
    val regionCode: String,
    val sigunguName: String?,
    val deptName: String,
    val phone: String,
    val telUri: String?,
    val sourceName: String?,
    val sourceUrl: String?,
)

private fun android.database.Cursor.toMoisDisposalRule() = MoisDisposalRule(
    sigunguCode = getString(0),
    sidoName = getString(1),
    sigunguName = getString(2),
    category = getString(3),
    disposalMethod = getString(4),
    disposalTime = getString(5),
)

private fun android.database.Cursor.toRegionContact() = RegionContact(
    regionCode = getString(0),
    sigunguName = getString(1),
    deptName = getString(2),
    phone = getString(3),
    telUri = getString(4),
    sourceName = getString(5),
    sourceUrl = getString(6),
)

fun SQLiteDatabase.moisDisposalByRegionCode(regionCode: String): List<MoisDisposalRule> =
    rawQuery(
        """
        SELECT sigungu_code, sido_name, sigungu_name, category, disposal_method, disposal_time
        FROM app_mois_disposal
        WHERE sigungu_code = ?
        ORDER BY
          CASE category
            WHEN '재활용품' THEN 0
            WHEN '음식물쓰레기' THEN 1
            ELSE 2
          END
        """.trimIndent(),
        arrayOf(regionCode),
    ).use { c ->
        buildList {
            while (c.moveToNext()) add(c.toMoisDisposalRule())
        }
    }

fun SQLiteDatabase.regionContactByCode(regionCode: String): RegionContact? =
    rawQuery(
        """
        SELECT region_code, sigungu_name, dept_name, phone, tel_uri, source_name, source_url
        FROM app_region_contact
        WHERE region_code = ?
        LIMIT 1
        """.trimIndent(),
        arrayOf(regionCode),
    ).use { c -> if (c.moveToFirst()) c.toRegionContact() else null }
