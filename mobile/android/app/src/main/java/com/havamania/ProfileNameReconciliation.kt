package com.havamania

internal fun needsProfileNameReconciliation(
    profile: UserProfile, currentUid: String?, sdkName: String?, serverConfirmed: Boolean,
    sessionReady: Boolean, saving: Boolean
): Boolean = serverConfirmed && sessionReady && !saving && profile.uid == currentUid &&
    profile.name.isNotBlank() && profile.name.length <= 50 && profile.name != sdkName
