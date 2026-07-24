# implementation_plan.artifact.md

This document outlines the root cause analysis and proposed fixes for the AI Assistant and Travel Planner analysis issues.

## Root Cause Analysis

### 1. AI Assistant (Merged Messages & Failures)
- **Failure**: The `AltikodChatService` call in `AiChatViewModel` is wrapped in a generic `catch (e: Exception)` block that only sets the error state. It doesn't provide granular info.
- **Merged Messages**: The `addErrorMessage` function (even after my previous refactor) might be called indirectly or the UI might be displaying a cached "fallback" message alongside the error state.
- **State Machine**: The state transitions are not atomic, allowing for race conditions.

### 2. Travel Planner (Gaziantep Analysis)
- **Gaziantep Eligibility**: The seyahat is clearly within the 10-day window. The failure is likely due to the weather API returning a null response or the AI suggestion generator returning an empty string, which is then mapped to `WEATHER_FAILED`.
- **Snackbar Logic**: Success events are emitted before data persistence is guaranteed or verified.

---

## Proposed Changes

### [Core] [AI Service Layer]

#### [NEW] [AiAssistantRepository.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AiAssistantRepository.kt)
- Centralize AI calls.
- Implement strict error mapping to `AiResult`.
- Add configuration validation (API key/Base URL check).

### [AI Assistant]

#### [MODIFY] [AiChatViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AiChatScreen.kt)
- Use `AiAssistantRepository`.
- Refactor `sendMessage` to use a strict state machine: `IDLE -> SENDING -> SUCCESS | ERROR`.
- Ensure error messages are NEVER stored in history.
- Prevent duplicate requests using an active request tracking.

#### [MODIFY] [AiChatScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AiChatScreen.kt)
- Clean up UI triggers.
- Ensure the error card is distinct from message bubbles.

### [Travel Planner]

#### [MODIFY] [TravelViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/TravelViewModel.kt)
- Differentiate between "Analysis prepared" (Success) and "Unable to prepare" (Error) in Snackbar.
- Log specific failure points (Weather API vs AI Mapping).

#### [MODIFY] [TravelPlannerScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/TravelPlannerScreen.kt)
- Redesign "10 Days Rule" card with warmer tone and `Icons.Rounded.Route`.
- Update error placeholder text.

#### [MODIFY] [TravelAiHelper.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/TravelAiHelper.kt)
- Update "10 days" warning text to be more natural and welcoming.

---

## Verification Plan

### Manual Verification
1. **Assistant**: Send a message. Verify only 1 bubble appears.
2. **Assistant Error**: Trigger a network error. Verify a standalone error card appears with a "Retry" button. Ensure no merged fallback text.
3. **Retry**: Click "Retry". Verify the *last* user message is sent again.
4. **Calendar**: Open Gaziantep trip. Click "Update". Verify Snackbar reflects actual result.
5. **10 Day Card**: View a trip > 10 days away. Verify new warm design.
