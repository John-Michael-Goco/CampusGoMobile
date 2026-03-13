package com.campusgomobile.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    /** Post-login app shell (bottom nav). Use this when navigating after sign-in. */
    const val APP = "app"

    /** Bottom nav tab routes (used inside APP). */
    const val TAB_HOME = "home"
    const val TAB_QUESTS = "quests"
    const val TAB_STORE = "store"
    const val TAB_PROFILE = "profile"
    /** QR Scanner (opened from center FAB in bottom nav). */
    const val SCANNER = "scanner"

    /** Profile & account sub-screens (navigate from Profile tab). */
    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE_TRANSACTIONS = "profile_transactions"
    const val PROFILE_ACTIVITY = "profile_activity"
    const val PROFILE_LEADERBOARD = "profile_leaderboard"
    const val PROFILE_ACHIEVEMENTS = "profile_achievements"
    const val PROFILE_INVENTORY = "profile_inventory"
    const val PROFILE_INVENTORY_HISTORY = "profile_inventory_history"
}
