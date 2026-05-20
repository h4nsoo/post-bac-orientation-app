package com.example.orientation_app.data.db

import android.content.Context
import android.util.Log
import com.example.orientation_app.data.entity.FiliereMaster
import com.example.orientation_app.data.entity.Governorate
import com.example.orientation_app.data.entity.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

private const val TAG = "DatabasePrepopulator"

/**
 * Seeds the Room database from bundled JSON assets on first install.
 *
 * Currently populates:
 *  - [Governorate] from the unique governorate names found in programs.json
 *  - [FiliereMaster] from programs.json (one row per programme × section)
 *  - A singleton [UserProfile] row (id = 1) so UPDATE queries never silently no-op
 *
 * programs.json shape:
 * ```json
 * [{ "code", "namear", "namefr", "universityar", "universityfr",
 *    "lastyearscore", "capacity", "governoratename", "section",
 *    "formulacalcform" }, …]
 * ```
 *
 * NOTE: [formulacalcform] (values like "FG+AR", "FG") is stored but not yet
 * acted upon — it specifies which additional exam score formula a programme uses.
 * TODO: confirm intended usage and propagate to filtering/display logic.
 */
object DatabasePrepopulator {

    suspend fun prepopulate(context: Context, database: UniCompassDatabase) =
        withContext(Dispatchers.IO) {
            try {
                seedUserProfile(database)
                seedFromProgramsJson(context, database)
            } catch (e: Exception) {
                Log.e(TAG, "Prepopulation failed", e)
            }
        }

    // ── User profile ─────────────────────────────────────────────────────────

    private suspend fun seedUserProfile(database: UniCompassDatabase) {
        database.userProfileDao().insertOrUpdate(UserProfile(id = 1))
        Log.d(TAG, "UserProfile (id=1) ensured.")
    }

    // ── Programs + governorates ───────────────────────────────────────────────

    private suspend fun seedFromProgramsJson(context: Context, database: UniCompassDatabase) {
        val inputStream = context.assets.open("programs.json")
        val jsonString = InputStreamReader(inputStream).readText()
        val jsonArray = JSONArray(jsonString)

        // Collect all unique governorate names first, then assign stable IDs.
        val governorateNameToId = mutableMapOf<String, Int>()
        for (i in 0 until jsonArray.length()) {
            val name = jsonArray.getJSONObject(i).optString("governoratename", "").trim()
            if (name.isNotBlank() && name !in governorateNameToId) {
                governorateNameToId[name] = governorateNameToId.size + 1
            }
        }

        // Insert governorates.
        val governorates = governorateNameToId.map { (name, id) ->
            Governorate(id = id, nameAr = name, regionCode = name)
        }
        database.governorateDao().insertAll(governorates)
        Log.d(TAG, "Inserted ${governorates.size} governorate records.")

        // Insert filière records.
        val filieres = mutableListOf<FiliereMaster>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val governorateName = obj.optString("governoratename", "").trim()
            val governorateId = governorateNameToId[governorateName]

            filieres.add(
                FiliereMaster(
                    code          = obj.optString("code", ""),
                    nameAr        = obj.optString("namear", ""),
                    nameFr        = obj.optString("namefr", ""),
                    universityAr  = obj.optString("universityar", ""),
                    universityFr  = obj.optString("universityfr", ""),
                    lastYearScore = if (obj.isNull("lastyearscore")) 0.0
                                   else obj.optDouble("lastyearscore", 0.0),
                    capacity         = obj.optInt("capacity", 0),
                    governorateId    = governorateId,
                    sectionEligibility = parseSectionEligibility(obj.optString("section", ""))
                )
            )
        }

        database.filiereDao().insertAll(filieres)
        Log.d(TAG, "Inserted ${filieres.size} FiliereMaster records.")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps a comma-separated list of English section names from programs.json to
     * a bitmask compatible with [FiliereMaster.sectionEligibility]:
     *
     * | Bit | Section                    | Value |
     * |-----|----------------------------|-------|
     * |  0  | Mathematics                |   1   |
     * |  1  | Experimental sciences      |   2   |
     * |  2  | Technical sciences         |   4   |
     * |  3  | Computer science           |   8   |
     * |  4  | Literatures                |  16   |
     * |  5  | Economics and management   |  32   |
     * |  6  | Sports                     |  64   |
     */
    private fun parseSectionEligibility(raw: String): Int {
        var mask = 0
        raw.split(",").map { it.trim().lowercase() }.forEach { section ->
            when (section) {
                "mathematics"            -> mask = mask or 1
                "experimental sciences"  -> mask = mask or 2
                "technical sciences"     -> mask = mask or 4
                "computer science"       -> mask = mask or 8
                "literatures"            -> mask = mask or 16
                "economics and management" -> mask = mask or 32
                "sports"                 -> mask = mask or 64
            }
        }
        return mask
    }
}
