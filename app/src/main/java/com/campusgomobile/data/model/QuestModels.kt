package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

// GET /api/quests
data class QuestsResponse(
    val quests: List<Quest>
)

data class Quest(
    val id: Int,
    val title: String,
    val description: String? = null,
    @SerializedName("quest_type") val questType: String? = null,
    @SerializedName("question_type") val questionType: String? = null,
    @SerializedName("is_elimination") val isElimination: Boolean = false,
    @SerializedName("reward_points") val rewardPoints: Int = 0,
    @SerializedName("reward_custom_prize") val rewardCustomPrize: String? = null,
    @SerializedName("buy_in_points") val buyInPoints: Int = 0,
    @SerializedName("max_participants") val maxParticipants: Int = 0,
    @SerializedName("current_participants") val currentParticipants: Int = 0,
    @SerializedName("stages_count") val stagesCount: Int = 0,
    val status: String = "ongoing",
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("first_stage_id") val firstStageId: Int? = null,
    @SerializedName("first_stage_location_hint") val firstStageLocationHint: String? = null
)

// GET /api/quests/participating
data class ParticipatingResponse(
    val participations: List<Participation>
)

data class Participation(
    @SerializedName("participant_id") val participantId: Int,
    @SerializedName("quest_id") val questId: Int,
    @SerializedName("quest_title") val questTitle: String,
    @SerializedName("current_stage") val currentStage: Int,
    val status: String,
    @SerializedName("total_stages") val totalStages: Int,
    @SerializedName("quest_type") val questType: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("last_submission_at") val lastSubmissionAt: String? = null,
    val preview: ParticipationPreview? = null
)

data class ParticipationPreview(
    @SerializedName("next_location_hint") val nextLocationHint: String? = null,
    @SerializedName("next_stage_opens_at") val nextStageOpensAt: String? = null,
    @SerializedName("next_stage_number") val nextStageNumber: Int? = null
)

// GET /api/quests/history
data class QuestHistoryResponse(
    val history: List<QuestHistoryEntry>,
    val pagination: QuestHistoryPagination
)

data class QuestHistoryEntry(
    @SerializedName("participant_id") val participantId: Int,
    @SerializedName("quest_id") val questId: Int,
    @SerializedName("quest_title") val questTitle: String,
    @SerializedName("quest_type") val questType: String? = null,
    @SerializedName("current_stage") val currentStage: Int,
    val status: String,
    @SerializedName("total_stages") val totalStages: Int,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("last_submission_at") val lastSubmissionAt: String? = null
) {
    /** Map to Participation so QuestHistoryCard can be reused. */
    fun toParticipation(): Participation = Participation(
        participantId = participantId,
        questId = questId,
        questTitle = questTitle,
        currentStage = currentStage,
        status = status,
        totalStages = totalStages,
        questType = questType,
        updatedAt = updatedAt,
        lastSubmissionAt = lastSubmissionAt,
        preview = null
    )
}

data class QuestHistoryPagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    val total: Int
)

// GET /api/quests/{quest} — quest + one stage
data class QuestDetailResponse(
    val quest: Quest,
    val stage: StageDetail
)

data class StageDetail(
    val id: Int,
    @SerializedName("stage_number") val stageNumber: Int,
    @SerializedName("location_hint") val locationHint: String? = null,
    @SerializedName("stage_deadline") val stageDeadline: String? = null,
    @SerializedName("stage_start") val stageStart: String? = null,
    @SerializedName("passing_score") val passingScore: Int? = null,
    val questions: List<PlayQuestion>? = null
)

// GET /api/participants/{participant}/play — also used as submit response shape
data class PlayStateResponse(
    @SerializedName("participant_id") val participantId: Int,
    @SerializedName("quest_id") val questId: Int,
    @SerializedName("current_stage") val currentStage: Int,
    val status: String,
    @SerializedName("total_stages") val totalStages: Int,
    @SerializedName("question_type") val questionType: String? = null,
    @SerializedName("is_elimination") val isElimination: Boolean = false,
    @SerializedName("stage_locked") val stageLocked: Boolean = false,
    @SerializedName("next_stage_opens_at") val nextStageOpensAt: String? = null,
    @SerializedName("next_stage_number") val nextStageNumber: Int? = null,
    @SerializedName("can_quit") val canQuit: Boolean = true,
    @SerializedName("quit_guard_reason") val quitGuardReason: String? = null,
    val stage: PlayStageDetail? = null,
    @SerializedName("next_stage_location_hint") val nextStageLocationHint: String? = null,
    @SerializedName("next_stage_starts_at") val nextStageStartsAt: String? = null,
    @SerializedName("awaiting_ranking") val awaitingRanking: Boolean = false,
    val message: String? = null,
    val outcome: String? = null,
    val passed: Boolean? = null,
    val failed: Boolean? = null,
    @SerializedName("correct_count") val correctCount: Int? = null,
    @SerializedName("total_count") val totalCount: Int? = null,
    val rewards: PlayRewards? = null
)

data class PlayRewards(
    @SerializedName("points_earned") val pointsEarned: Int = 0,
    @SerializedName("custom_prize") val customPrize: String? = null,
    @SerializedName("level_up") val levelUp: Boolean = false,
    @SerializedName("previous_level") val previousLevel: Int? = null,
    @SerializedName("new_level") val newLevel: Int? = null,
    val achievements: List<PlayAchievement>? = null
)

data class PlayAchievement(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class PlayStageDetail(
    val id: Int,
    @SerializedName("stage_number") val stageNumber: Int,
    @SerializedName("location_hint") val locationHint: String? = null,
    @SerializedName("stage_deadline") val stageDeadline: String? = null,
    @SerializedName("stage_start") val stageStart: String? = null,
    @SerializedName("passing_score") val passingScore: Int? = null,
    @SerializedName("question_type") val questionType: String? = null,
    val questions: List<PlayQuestion>? = null
)

data class PlayQuestion(
    val id: Int,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("question_type") val questionType: String? = null,
    @SerializedName("already_answered") val alreadyAnswered: Boolean = false,
    val choices: List<PlayChoice>? = null
)

data class PlayChoice(
    val id: Int,
    @SerializedName("choice_text") val choiceText: String
)

/** Quest + play state + stages 1..currentStage for My Quest detail screen. */
data class MyQuestDetailData(
    val quest: Quest,
    val playState: PlayStateResponse,
    val stages: List<StageDetail>
)

/** Quest + all stages for Discover quest detail (no participant). */
data class DiscoverQuestDetailData(
    val quest: Quest,
    val stages: List<StageDetail>
)

// GET /api/quests/resolve — QR code resolution
data class QrResolveResponse(
    @SerializedName("quest_id") val questId: Int,
    @SerializedName("quest_title") val questTitle: String,
    @SerializedName("stage_id") val stageId: Int,
    @SerializedName("stage_number") val stageNumber: Int,
    @SerializedName("location_hint") val locationHint: String? = null,
    @SerializedName("can_join") val canJoin: Boolean = false,
    @SerializedName("can_play") val canPlay: Boolean = false,
    @SerializedName("question_type") val questionType: String? = null,
    @SerializedName("is_elimination") val isElimination: Boolean = false,
    @SerializedName("stage_deadline") val stageDeadline: String? = null,
    @SerializedName("stage_start") val stageStart: String? = null,
    val reason: String? = null,
    @SerializedName("participant_id") val participantId: Int? = null
)

// GET /api/participants/{participant}/status — lightweight poll
data class ParticipantStatusResponse(
    @SerializedName("participant_id") val participantId: Int,
    val status: String,
    @SerializedName("current_stage") val currentStage: Int,
    @SerializedName("awaiting_ranking") val awaitingRanking: Boolean = false,
    val outcome: String? = null
)

// POST /api/quests/{questId}/join
data class JoinResponse(
    @SerializedName("participant_id") val participantId: Int,
    @SerializedName("quest_id") val questId: Int,
    @SerializedName("current_stage") val currentStage: Int,
    val status: String
)
