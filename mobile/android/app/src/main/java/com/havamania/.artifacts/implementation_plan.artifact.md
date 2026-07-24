# implementation_plan.artifact.md

This plan details the migration of legal pages to a unified WebView implementation and the global branding fix for Havamania.

## Root Cause Analysis

### 1. Legal Pages (404 & Navigation)
- **Cause**: Legal pages currently point to specific URLs that might be 404. Also, the user wants a unified temp URL (`https://www.havamania.com/`).
- **Cause (Bottom Bar)**: Although I split Auth/Main graphs, the legal screens were recently changed to local Compose screens. The user wants to revert to WebView but keep them isolated from the Main navigation's bottom bar.

### 2. Branding (Havamania vs Havamania)
- **Issue**: The brand name "HAVAMANIA" and "ASISTAN" should use correct Turkish characters: "HAVAMANİA" and "ASİSTAN" in UI contexts.

---

## Proposed Changes

### [Legal Content]

#### [MODIFY] [LegalUrls.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/LegalUrls.kt)
- Add `LEGAL_TEMP_URL = "https://www.havamania.com/"`.
- Update `KVKK`, `PRIVACY_POLICY`, and `TERMS_OF_USE` constants to use `LEGAL_TEMP_URL`.

#### [MODIFY] [LegalScreens.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/LegalScreens.kt)
- Replace local Compose implementations of `KVKKScreen`, `PrivacyPolicyScreen`, and `TermsOfUseScreen` with calls to `LegalWebViewScreen`.
- Ensure each screen still has its specific title: "KVKK AYDINLATMA METNİ", "GİZLİLİK POLİTİKASI", "KULLANIM KOŞULLARI".

#### [MODIFY] [LegalWebViewScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/LegalWebViewScreen.kt)
- Enhance error handling to show: "Sayfa şu anda açılamıyor. İnternet bağlantınızı kontrol edip tekrar deneyin." with a "TEKRAR DENE" button.
- Add a loading indicator.
- Disable JavaScript if not strictly required (per user request).
- Handle SSL errors.

### [Branding Fixes]

#### [MODIFY] [strings.xml](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/res/values/strings.xml)
- Update `app_name` (if needed in large caps contexts, but usually "Havamania" is fine).
- Update all capitalized strings: "HAVAMANIA" -> "HAVAMANİA", "ASISTAN" -> "ASİSTAN".

#### [MODIFY] [AiChatScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AiChatScreen.kt)
- Update default top bar title: "HAVAMANİA ASİSTAN".
- Update welcome messages and other UI strings.

#### [MODIFY] [Other UI Files]
- Search and replace "HAVAMANIA" -> "HAVAMANİA" and "ASISTAN" -> "ASİSTAN" in all `Composable` titles and labels.

---

## Verification Plan

### Manual Verification
1.  **Legal Pages**:
    *   Open KVKK/Privacy/Terms from Login/Register. Verify they load `https://www.havamania.com/`.
    *   Verify NO Bottom Bar is visible.
    *   Verify Back button returns to the correct Auth screen.
    *   Disable internet. Verify the "Sayfa şu anda açılamıyor..." error message and "TEKRAR DENE" button appear.
2.  **Branding**:
    *   Check Assistant screen title. Verify "HAVAMANİA ASİSTAN".
    *   Check all other large-caps headers.
    *   Verify no code/package/variable names were broken.

### Automated Tests
- `BrandingTest.kt`: Verify that key UI constants/strings contain "İ" instead of "I" in specific keywords.
