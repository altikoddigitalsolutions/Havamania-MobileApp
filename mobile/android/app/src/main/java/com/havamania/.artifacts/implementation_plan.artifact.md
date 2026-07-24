# implementation_plan.artifact.md

This document outlines the root cause and proposed fix for the HAVAMANIA ASİSTAN (AI Assistant) response issue.

## Root Cause Analysis

### 1. HTTP 422 Unprocessable Entity
The current implementation in `AiChatViewModel` uses a 24-character hex string as the `botId`: `"6724b94f6f1c48010ba457c1"`. However, the Altikod API endpoint `api/widget/{bot_id}/chat` expects a valid integer for the `{bot_id}` path parameter. Sending the hex string results in a validation error (422) on the server.

### 2. Correct Bot ID
Through a diagnostic scan of the Altikod service endpoints, I discovered that the correct integer ID for "Havamania Asistan" on the `api/widget/` path is **`6`**.

### 3. Error Logic
The ViewModel's `try-catch` block converts all network/HTTP errors into a generic `AssistantRequestState.ERROR`, hiding the specific 422 error code and preventing a successful response.

---

## Proposed Changes

### [AI Assistant]

#### [MODIFY] [AiChatScreen.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AiChatScreen.kt)
- Update `botId` from `"6724b94f6f1c48010ba457c1"` to `"6"`.
- Refactor the catch block in `sendMessage` to provide more granular logging in debug builds.
- Implement a more robust `AssistantResult` mapping (Success, ConfigurationError, HttpError, etc.).

#### [MODIFY] [AltikodChatService.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AltikodChatService.kt)
- Ensure the `botId` parameter is correctly typed (though String is technically fine for Retrofit, using a clean ID value is key).

---

## Verification Plan

### Automated Tests
- Create a unit test `AiAssistantLogicTest.kt` to verify that `sendMessage` correctly handles success and various HTTP error codes.

### Manual Verification
1. Open "Havamania Asistan" screen.
2. Send the message: "Hava durumuna göre seyahat planlamama yardımcı olur musun?".
3. Verify that the assistant responds with a text message.
4. Verify that the error card is not shown upon success.
5. Verify that "TEKRAR DENE" button is not visible.
6. Verify in Logcat that `ASSISTANT_HTTP_RESULT` shows `httpCode=200` and `successful=true`.
