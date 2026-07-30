# Task: City and Travel Sync Fix

- [ ] Verify `GeocodingResultDto` compatibility `[/]`
- [ ] Refactor `ThemeViewModel.kt` for City Sync `[ ]`
    - [ ] Change city storage to sub-collection `users/{uid}/cities`
    - [ ] Implement real-time `onSnapshot` listener for cities
    - [ ] Sync Firestore data to local `DataStore`
    - [ ] Harden `addCity` and `removeCity` with error handling
- [ ] Refactor `TravelViewModel.kt` for robust Travel Sync `[ ]`
    - [ ] Standardize Firestore path to `users/{uid}/trips`
    - [ ] Improve real-time sync reliability
    - [ ] Harden CRUD operations with error handling
- [ ] Verify multi-device sync and state reset `[ ]`
