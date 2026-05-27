package app.trashai.data

import android.content.Context
import app.trashai.supabase.MoisDisposalClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RegionExtras(
    val moisSchedules: List<MoisDisposalRule> = emptyList(),
    val contact: RegionContact? = null,
)

object RegionExtrasLoader {

    suspend fun load(
        context: Context,
        regionCode: String?,
        moisClient: MoisDisposalClient = MoisDisposalClient(),
    ): RegionExtras = withContext(Dispatchers.IO) {
        if (regionCode.isNullOrBlank()) return@withContext RegionExtras()
        val db = WasteGuideDb.open(context)
        val contact = runCatching { db.regionContactByCode(regionCode) }.getOrNull()
        var mois = runCatching { db.moisDisposalByRegionCode(regionCode) }.getOrNull().orEmpty()
        if (mois.isEmpty() && moisClient.isConfigured) {
            mois = moisClient.fetchBySigunguCode(regionCode)
        }
        RegionExtras(
            moisSchedules = mois.filter { it.hasSchedule || !it.disposalMethod.isNullOrBlank() },
            contact = contact,
        )
    }
}
