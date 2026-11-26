package com.havamania

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Observe edits/deletions and date changes without fetching remote data. */
internal fun observeAssistantTravels(
    plans: Flow<List<TravelPlan>>,
    dates: Flow<LocalDate>,
    owner: String
): Flow<List<TravelPlan>> = combine(plans, dates) { rows, today ->
    rows.filter { it.userId == owner && !it.isDemo && !it.isArchived && !it.endDate.isBefore(today) }
        .sortedWith(compareBy<TravelPlan> { it.startDate }.thenBy { it.id })
}.distinctUntilChanged()
