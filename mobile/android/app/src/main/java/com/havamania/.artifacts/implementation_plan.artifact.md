# Implementation Plan - City and Travel Sync Fix

This plan establishes Firestore as the primary source of truth for registered cities and travel plans, ensuring real-time synchronization across all devices logged into the same account.

## User Review Required

> [!IMPORTANT]
> - Registered cities will be moved from an array in the user document to a sub-collection: `users/{uid}/cities/{cityId}`. This aligns with the structure of travel plans and improves scalability.
> - `DataStore` (for cities) and `Room DB` (for trips) will now act strictly as local caches.
> - Real-time listeners will be established globally within ViewModels to ensure changes on Device A reflect on Device B within seconds.

## Proposed Changes

### [Data Mapping]
Ensure all models are compatible with Firestore sub-collection operations.

#### [MODIFY] [GeocodingDto.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/GeocodingDto.kt)
- Ensure `GeocodingResultDto` remains compatible with Firestore's `toObject`. (Verify default values).

---

### [City Management]
Refactor city storage and synchronization.

#### [MODIFY] [ThemeViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/ui/theme/ThemeViewModel.kt)
- **Data Source Change**: Stop writing cities to the `users/{uid}` document array.
- **New Path**: Use `users/{uid}/cities/{cityId}` for all city CRUD operations.
- **Real-time Sync**: Replace `observeFirestoreUserDoc` with `observeFirestoreCities`.
    - Use `addSnapshotListener` on the `cities` sub-collection.
    - Sync incoming data to local `DataStore` and update the `_registeredCities` StateFlow.
- **Safe Operations**: Wrap `addCity` and `removeCity` in `try-catch` with `await()`.

---

### [Travel Management]
Harden travel plan synchronization.

#### [MODIFY] [TravelViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/TravelViewModel.kt)
- **Consistent Paths**: Verify and enforce the `users/{uid}/trips/{tripId}` path.
- **Real-time Sync**: Enhance `observeFirestoreTrips`.
    - Ensure it clears local `Room` state and reloads when UID changes.
    - Handle `onSnapshot` events to keep `Room` in sync with Firestore.
- **Safe Operations**: Ensure `savePlan`, `deletePlan`, `archiveTrip`, etc., all use `await()` and robust `try-catch`.

---

### [Auth & State Reset]
Ensure clean transitions between accounts.

#### [MODIFY] [AuthViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AuthViewModel.kt)
- Ensure `signOut` triggers state clearing in other ViewModels (handled by `AuthStateListener` in those ViewModels).

---

## Verification Plan

### Manual Verification (Two Devices)
1. **Login**: Log in with the same account on Phone A and Phone B.
2. **City Sync**:
    - Add "Şanlıurfa" on Phone A.
    - Verify it appears on Phone B within ~2 seconds.
    - Delete a city on Phone B. Verify it disappears from Phone A.
3. **Travel Sync**:
    - Create a trip to "Şanlıurfa" on Phone A.
    - Verify it appears on Phone B.
    - Update the trip note on Phone A. Verify Phone B shows the updated note.
4. **Offline Resilience**:
    - Turn off internet on Phone A.
    - Add a city.
    - Turn on internet.
    - Verify Phone B eventually receives the new city.

### Technical Check
- Check `adb logcat` for "Firestore snapshot received" logs on both devices.
- Verify Firestore document structure in the Firebase Console matches the new paths.
