package app.trashai.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RegionExtras(
    val moisSchedules: List<MoisDisposalRule> = emptyList(),
    val contact: RegionContact? = null,
)

/**
 * Loads region MOIS schedules and contacts from the bundled SQLite only (offline-first).
 * MOIS data is populated at build time via [scripts/import_region_extras.py].
 */
object RegionExtrasLoader {

    suspend fun load(context: Context, regionCode: String?): RegionExtras = withContext(Dispatchers.IO) {
        if (regionCode.isNullOrBlank()) return@withContext RegionExtras()
        val db = WasteGuideDb.open(context)
        val contact = runCatching { db.regionContactByCode(regionCode) }.getOrNull()
        val mois = runCatching { db.moisDisposalByRegionCode(regionCode) }.getOrNull().orEmpty()
        RegionExtras(
            moisSchedules = mois.filter { it.hasSchedule || !it.disposalMethod.isNullOrBlank() },
            contact = contact,
        )
    }
}
