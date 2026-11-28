package com.havamania

import java.time.LocalDate

internal const val ASSISTANT_TRIP_CHANGED_MESSAGE = "Seyahat bilgisi değişti veya kayıt artık kullanılabilir değil. Güncel yer ve tarihle yeni bir soru sorabilirsin."

/** Only resolve a current, owned record; never silently substitute another trip. */
internal fun assistantTripWeatherQuestion(plans: List<TravelPlan>, owner: String, id: String,
    today: LocalDate): String? {
    val trip = plans.singleOrNull { it.id == id && it.userId == owner } ?: return null
    if (trip.isDemo || trip.isArchived || trip.endDate.isBefore(today) || trip.endDate.isBefore(trip.startDate)) return null
    val period = if (trip.startDate == trip.endDate) "${trip.startDate} tarihinde"
        else "${trip.startDate}–${trip.endDate} tarihleri arasında"
    return "${trip.displayName} seyahatim için $period hava durumu nasıl? Tahmin kapsamı dışındaki günler için kesin hava tahmini verme."
}

/** Re-check the selected record before releasing a question that waited for consent. */
internal fun assistantTripQuestionStillCurrent(plans: List<TravelPlan>, owner: String, id: String,
    originalQuestion: String, today: LocalDate): Boolean {
    val current = assistantTripWeatherQuestion(plans, owner, id, today) ?: return false
    return current == originalQuestion
}
