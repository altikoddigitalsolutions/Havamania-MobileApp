# Task: Final Crash Fix and Robust Synchronization

- [x] Harden Data Models for Firestore Compatibility
    - [x] Clean up `GeocodingDto.kt` (imports and default values)
    - [x] Update `TravelModels.kt` with `@Keep` and correct imports
    - [x] Update `WeatherCache.kt` with `@Keep` and `@IgnoreExtraProperties`
- [x] Fix Navigation Redirection Crash in `WeatherPremiumActivity.kt`
- [x] Robustify Firestore Listeners in ViewModels
    - [x] Improve city parsing in `ThemeViewModel.kt`
    - [x] Safe collection handling in `TravelViewModel.kt`
- [x] Create walkthrough artifact
- [x] Verify Build and Stability
