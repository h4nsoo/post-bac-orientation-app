package com.example.orientation_app.data.db

import android.content.Context
import android.util.Log
import com.example.orientation_app.data.entity.FiliereMaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader

object DatabasePrepopulator {

    suspend fun prepopulateFiliereMaster(context: Context, database: UniCompassDatabase) = withContext(Dispatchers.IO) {
        try {
            val filiereDao = database.filiereDao()
            
            // Read JSON from assets
            val inputStream = context.assets.open("programs.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonArray = JSONArray(jsonString)
            
            val filieresToInsert = mutableListOf<FiliereMaster>()
            
            // Parse each object and insert
            for (i in 0 until jsonArray.length()) {
                val element = jsonArray.getJSONObject(i)
                
                val sectionString = element.optString("section", "")
                val sectionEligibility = parseSectionEligibility(sectionString)

                val code = element.optString("code", "")
                val nameAr = element.optString("namear", "")
                val nameFr = element.optString("namefr", "")
                val universityAr = element.optString("universityar", "")
                val universityFr = element.optString("universityfr", "")
                val lastYearScore = if (element.isNull("lastyearscore")) 0.0 else element.optDouble("lastyearscore", 0.0)
                val capacity = element.optInt("capacity", 0)

                val governorateId: Int? = null
                
                filieresToInsert.add(
                    FiliereMaster(
                        code = code,
                        nameAr = nameAr,
                        nameFr = nameFr,
                        universityAr = universityAr,
                        universityFr = universityFr,
                        lastYearScore = lastYearScore,
                        capacity = capacity,
                        governorateId = governorateId,
                        sectionEligibility = sectionEligibility
                    )
                )
            }
            
            filiereDao.insertAll(filieresToInsert)
            Log.d("UniCompassDatabase", "Successfully prepopulated ${filieresToInsert.size} FiliereMaster records.")
        } catch (e: Exception) {
            Log.e("UniCompassDatabase", "Error prepopulating database", e)
        }
    }

    private fun parseSectionEligibility(sectionString: String): Int {
        var eligibility = 0
        val sections = sectionString.split(",").map { it.trim().lowercase() }
        
        if (sections.contains("mathematics")) eligibility = eligibility or 1
        if (sections.contains("experimental sciences")) eligibility = eligibility or 2
        if (sections.contains("technical sciences")) eligibility = eligibility or 4
        if (sections.contains("computer science")) eligibility = eligibility or 8
        if (sections.contains("literatures")) eligibility = eligibility or 16
        if (sections.contains("economics and management")) eligibility = eligibility or 32
        if (sections.contains("sports")) eligibility = eligibility or 64
        
        return eligibility
    }
}
