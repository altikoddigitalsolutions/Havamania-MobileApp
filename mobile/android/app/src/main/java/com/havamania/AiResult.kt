package com.havamania

sealed interface AiResult {
    data class Success(val content: String) : AiResult
    data class HttpError(val code: Int) : AiResult
    data object NetworkError : AiResult
    data object Timeout : AiResult
    data object EmptyResponse : AiResult
    data object ParseError : AiResult
    data class UnknownError(val throwable: Throwable) : AiResult
}

sealed interface TripAnalysisResult {
    data class Success(val plan: TravelPlan) : TripAnalysisResult
    data object TooEarly : TripAnalysisResult
    data object WeatherUnavailable : TripAnalysisResult
    data object AiUnavailable : TripAnalysisResult
    data object SaveFailed : TripAnalysisResult
    data object NetworkError : TripAnalysisResult
    data class UnknownError(val throwable: Throwable) : TripAnalysisResult
}
