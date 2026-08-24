package com.havamania

sealed interface AssistantResult {
    data class Success(val content: String) : AssistantResult
    data object ConfigurationError : AssistantResult
    data class HttpError(val code: Int) : AssistantResult
    data object NetworkError : AssistantResult
    data object Timeout : AssistantResult
    data object ParseError : AssistantResult
    data object EmptyResponse : AssistantResult

    /**
     * Bot "Lütfen geçerli bir soru sorunuz." döndürdü: sunucu gönderdiğimiz metni
     * temizleyip geriye anlamlı bir soru kalmadığına karar verdi (ör. satır başı
     * girintili payload'u markdown kod bloğu sayıp siliyor). Cevap değil, hatadır.
     */
    data object QuestionRejected : AssistantResult
    data class UnknownError(val type: String) : AssistantResult
}

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
