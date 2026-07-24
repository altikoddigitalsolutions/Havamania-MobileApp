# Walkthrough - Legal Pages & Global Branding Fix

This update addresses the temporary redirection of legal pages to a unified URL and a global correction of the brand name and assistant terminology.

## 1. Legal Pages Redirection
All legal pages (KVKK, Privacy Policy, Terms of Use) are now redirected to `https://www.havamania.com/` as a temporary measure.

### key Improvements:
- **Centralized URL**: Managed via `LegalUrls.LEGAL_TEMP_URL` for easy future updates.
- **Enhanced WebView**:
    - **Loading States**: Added a `CircularProgressIndicator` during page load.
    - **Error Handling**: Custom error screen appears if the internet is disconnected or an HTTP error occurs.
    - **Retry Logic**: "TEKRAR DENE" button allows users to attempt reloading the page.
    - **Privacy**: JavaScript is disabled by default for these static legal pages.

## 2. Global Branding Fix
Corrected the Turkish character usage in brand names and UI headers.

### Terminology Updates:
- **HAVAMANIA** $\rightarrow$ **HAVAMANİA**
- **ASISTAN** $\rightarrow$ **ASİSTAN**

> [!TIP]
> These changes were applied to all user-facing strings (Titles, Buttons, Messages) while preserving code-level identifiers like package names and variable names.

## 3. Navigation Integrity
- Legal screens remain in the **Auth Flow** for unauthenticated users, ensuring **NO Bottom Navigation Bar** is visible.
- Navigation back stack correctly returns the user to the previous Auth screen (Login/Register).

---

### Verification Summary
- [x] **URL Check**: Verified KVKK, Privacy Policy, and Terms of Use point to the new URL.
- [x] **Connectivity Test**: Verified that disabling internet triggers the custom "Sayfa şu anda açılamıyor" screen.
- [x] **Branding Audit**: Verified "HAVAMANİA ASİSTAN" and other headers are correctly spelled across all screens.
