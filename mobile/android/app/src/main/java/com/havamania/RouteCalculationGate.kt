package com.havamania

private data class RouteCalculationInput(val owner: String, val id: String, val destination: GeoPoint,
    val origin: Pair<Double, Double>?, val start: java.time.LocalDate, val end: java.time.LocalDate,
    val departure: String?, val city: String, val district: String?, val originCity: String?, val originDistrict: String?)

internal class RouteCalculationGate {
    private var previous: RouteCalculationInput? = null
    fun shouldRecalculate(plan: TravelPlan): Boolean {
        val input = RouteCalculationInput(plan.userId, plan.id, GeoPoint(plan.latitude, plan.longitude), plan.originPoint,
            plan.startDate, plan.endDate, plan.departureTime, plan.city, plan.district, plan.originCity, plan.originDistrict)
        if (previous == input) return false
        previous = input
        return true
    }
    fun reset() { previous = null }
}
