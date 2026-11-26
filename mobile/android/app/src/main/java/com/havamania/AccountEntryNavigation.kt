package com.havamania

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

internal val accountEntryRoutes = setOf(Routes.AUTH_WELCOME, Routes.LOGIN, Routes.REGISTER,
    Routes.FORGOT_PASSWORD, Routes.ACCOUNT_LOADING)

internal fun accountEntryTarget(owner: String?, profile: UserProfile?): String = when {
    owner == null -> Routes.AUTH_WELCOME
    profile == null || profile.uid != owner -> Routes.ACCOUNT_LOADING
    !profile.onboardingCompleted -> Routes.PERSONALIZATION
    else -> Routes.WEATHER_ROOT
}

/** Redirect bootstrap routes only; editing personalization later must remain possible. */
@Composable
internal fun AccountEntryRedirect(nav: NavHostController, owner: String?, state: ProfileState, enabled: Boolean) {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val profile = (state as? ProfileState.Success)?.profile
    LaunchedEffect(owner, profile, route, enabled) {
        if (!enabled || route == null) return@LaunchedEffect
        val target = when {
            owner == null && (route == Routes.ACCOUNT_LOADING || route !in accountEntryRoutes) -> Routes.AUTH_WELCOME
            owner != null && route in accountEntryRoutes -> accountEntryTarget(owner, profile)
            else -> return@LaunchedEffect
        }
        if (target != route) nav.navigate(target) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}

@Composable
internal fun AccountProfileLoadingScreen(owner: String?, state: ProfileState, onRetry: () -> Unit, onExit: () -> Unit) {
    var attempt by remember(owner) { mutableStateOf(0) }
    BackHandler(onBack = onExit)
    ProfileUnavailableContent(owner, state, attempt,
        onRetry = { attempt++; onRetry() }, onBack = onExit)
}
/** The graph begins at a transient gate; tab stacks must use the real weather root. */
internal fun androidx.navigation.NavOptionsBuilder.accountTabOptions(targetRoute: String) {
    popUpTo(Routes.WEATHER_ROOT) { saveState = true }
    launchSingleTop = true
    // Weather is the retained root: restoring its saved alias can restore the
    // assistant pushed by a recommendation instead of returning to weather.
    restoreState = targetRoute.substringBefore('?') != Routes.WEATHER_ROOT
}

internal fun NavHostController.leavePersonalization(source: androidx.navigation.NavBackStackEntry, onExit: () -> Unit) {
    if (currentBackStackEntry != source) return
    if (previousBackStackEntry == null) onExit() else popBackStack()
}