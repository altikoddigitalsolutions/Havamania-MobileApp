# Implementation Plan - UI & Functional Fixes

This plan details the changes for the legal links, assistant title, and travel planner button responsiveness.

## User Review Required

> [!IMPORTANT]
> The legal links will now open the system browser directly to `https://www.havamania.com/` instead of navigating within the app.
> The "Ayrıntıları Göster" button will be made responsive to prevent text overflow on small screens.

## Proposed Changes

### [Auth & Legal]
Update legal links to open in the system browser.

#### [MODIFY] [LegalUrls.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/LegalUrls.kt)
- Update all constants to `https://www.havamania.com/`.

#### [MODIFY] [AuthScreens.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AuthScreens.kt)
- Remove `onNavigateToLegal` from `AuthWelcomeScreen` and `RegisterScreen` if possible, or just change the implementation.
- Use `LocalUriHandler.current.openUri()` to open the URL.
- Add error handling with a `Toast` message if the browser cannot be opened.

#### [MODIFY] [SettingsView.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/SettingsView.kt)
- Update the legal links here as well to open the system browser for consistency.

---

### [Assistant]
Fix the title text characters.

#### [MODIFY] [AiChatScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AiChatScreen.kt)
- Hardcode the title to `"HAVAMANİA ASİSTAN"` to ensure correct Turkish characters are displayed regardless of remote config.

---

### [Travel Planner]
Fix button responsiveness.

#### [MODIFY] [HavamaniaCommonComponents.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/ui/theme/HavamaniaCommonComponents.kt)
- Refactor `HavamaniaPrimaryButton` to be more flexible:
    - Allow overriding default `height` and `fillMaxWidth` via parameters.
    - Set `maxLines = 1` and use a slightly smaller default font size or adaptive scaling if feasible without breaking the design.

#### [MODIFY] [TravelPlannerScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/TravelPlannerScreen.kt)
- Remove the fixed `width(160.dp)` from the "AYRINTILARI GÖSTER" button.
- Use `widthIn(min = 120.dp)` to allow it to grow responsively on small screens.

---

## Verification Plan

### Manual Verification
1.  **Legal Links**:
    *   Click "KVKK", "Gizlilik Politikası", and "Kullanım Koşulları" on the Login/Register screens.
    *   Verify they open in the external system browser.
    *   Verify no internal app screen is opened.
2.  **Assistant Title**:
    *   Open the Assistant screen.
    *   Verify the title is "HAVAMANİA ASİSTAN".
3.  **Travel Planner Button**:
    *   Navigate to the Travel Planner.
    *   Verify the "AYRINTILARI GÖSTER" button text does not overflow.
    *   Test on a small screen emulator if possible, or verify the layout logic.

### Automated Tests
- None required for these UI tweaks.
