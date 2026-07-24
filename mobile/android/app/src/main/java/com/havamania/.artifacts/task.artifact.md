# Havamania Bug Fixes & Design Improvements Task List

- `[ ]` Splash Screen stabilization (2500ms min duration)
- `[ ]` AI Assistant state machine & duplicate message fix
    - `[ ]` `AiAssistantState` implementation in ViewModel
    - `[ ]` Request concurrency prevention
    - `[ ]` UI trigger synchronization (`LaunchedEffect` fixes)
- `[ ]` Travel Planner UX & Logic Improvements
    - `[ ]` Fix fake success Snackbar in `TravelViewModel`
    - `[ ]` Redesign "10 Day Rule" card with warm design
    - `[ ]` Map "No Data" to user-friendly text
    - `[ ]` Refactor `RecommendationEngine` for natural Turkish sentences
