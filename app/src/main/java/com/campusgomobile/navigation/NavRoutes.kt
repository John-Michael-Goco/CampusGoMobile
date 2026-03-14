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
    /** Quest history (search + filter by type). */
    const val QUEST_HISTORY = "quest_history"
    /** My quest detail: participantId, questId. */
    const val MY_QUEST_DETAIL = "my_quest_detail/{participantId}/{questId}"

    fun myQuestDetail(participantId: Int, questId: Int) = "my_quest_detail/$participantId/$questId"

    /** Quest history detail: participantId, questId, status, currentStage, totalStages. */
    const val QUEST_HISTORY_DETAIL = "quest_history_detail/{participantId}/{questId}/{status}/{currentStage}/{totalStages}"

    fun questHistoryDetail(participantId: Int, questId: Int, status: String, currentStage: Int, totalStages: Int) =
        "quest_history_detail/$participantId/$questId/$status/$currentStage/$totalStages"

    /** Play screen: MCQ questions after QR scan. */
    const val PLAY = "play/{participantId}"

    fun play(participantId: Int) = "play/$participantId"

    /** Discover quest detail (view before join). */
    const val DISCOVER_QUEST_DETAIL = "discover_quest_detail/{questId}"

    fun discoverQuestDetail(questId: Int) = "discover_quest_detail/$questId"

    /** Profile & account sub-screens (navigate from Profile tab). */
    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE_TRANSACTIONS = "profile_transactions"
    const val PROFILE_ACTIVITY = "profile_activity"
    const val PROFILE_LEADERBOARD = "profile_leaderboard"
    const val PROFILE_ACHIEVEMENTS = "profile_achievements"
    const val PROFILE_INVENTORY = "profile_inventory"
    const val PROFILE_INVENTORY_HISTORY = "profile_inventory_history"
    const val PROFILE_TRANSFER_POINTS = "profile_transfer_points"
}
