package com.example.orientation_app.data.repository

import com.example.orientation_app.data.entity.FiliereMaster
import com.example.orientation_app.domain.model.ScoredProgram

/**
 * Extension functions that convert Room entities → domain models and vice versa.
 * Keeping them in one file makes it easy to audit every cross-layer translation.
 */

internal fun FiliereMaster.toDomain(): ScoredProgram = ScoredProgram(
    id                = id,
    code              = code,
    nameAr            = nameAr,
    nameFr            = nameFr,
    universityAr      = universityAr,
    universityFr      = universityFr,
    lastYearScore     = lastYearScore,
    capacity          = capacity,
    sectionEligibility = sectionEligibility
)
