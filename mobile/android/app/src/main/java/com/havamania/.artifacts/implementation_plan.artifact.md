# Implementation Plan - Fix Navigation Crash and Unify NavHost

This plan addresses the crash occurring after the splash screen by unifying the navigation graph and ensuring all routes are registered within a single `NavHost`.

## Root Cause Analysis
The application currently attempts to switch between two different `NavHost` compositions using the same `NavController`.
- When the user is null, the app enters the "Main" block because the default `currentRoute` isn't an "Auth" route yet.
- The Main block's `NavHost` tries to start at `Routes.AUTH_WELCOME`, but this route is missing from its graph.
- This causes an `IllegalArgumentException`.
- Swapping `NavHost` components with a shared `NavController` is also unstable and can lead to internal state corruption.

## Proposed Changes

### [Navigation]

#### [MODIFY] [WeatherPremiumActivity.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/WeatherPremiumActivity.kt)
- Remove the `if (isAuthRoute)` conditional block that creates multiple `NavHost` instances.
- Implement a **single unified `NavHost`** containing all Auth, Main, and Legal routes.
- Wrap the unified `NavHost` in a single `Scaffold`.
- Dynamically control the visibility of the `bottomBar` based on `currentRoute`.
    - Only show `bottomBar` for `WEATHER_ROOT`, `CALENDAR_ROOT`, `AI_ROOT`, and `PROFILE_ROOT`.
    - Hide it for all Auth and Legal routes.
- Ensure the `padding` applied to the content correctly handles the presence/absence of the bottom bar.

### [UI / Screens]

#### [MODIFY] [AuthScreens.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AuthScreens.kt)
- Update `AuthWelcomeScreen` and `RegisterScreen` callback signatures to pass the route name directly (e.g., `onNavigateToLegal(Routes.KVKK)`).
- Update the UI to use these routes.

#### [MODIFY] [SettingsView.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/SettingsView.kt)
- Update the `onNavigateToLegal` call to pass the route name instead of title/url strings.

---

## Verification Plan

### Manual Verification
1.  **Splash Screen**: Verify "Seyahatini akıllıca planla" screen displays and transitions without crashing.
2.  **Unauthenticated Access**:
    - App should start and stay on `AuthWelcomeScreen`.
    - Verify NO bottom navigation bar is visible.
    - Click "KVKK". Verify the legal screen opens with NO bottom bar.
    - Click "Back". Verify it returns to the welcome screen.
3.  **Authenticated Access**:
    - Log in. Verify Home Screen.
    - Verify bottom navigation bar is visible.
    - Navigate to Settings -> KVKK. Verify it opens and the bottom bar is hidden.
    - Click "Back". Verify it returns to Settings.

### Automated Tests
- `NavigationGraphTest.kt`: Verify that all 15+ routes defined in `Routes.kt` are registered in the unified `NavHost`.
