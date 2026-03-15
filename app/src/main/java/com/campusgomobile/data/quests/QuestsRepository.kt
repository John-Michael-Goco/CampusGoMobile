package com.campusgomobile.data.quests

import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.model.DiscoverQuestDetailData
import com.campusgomobile.data.model.JoinResponse
import com.campusgomobile.data.model.MyQuestDetailData
import com.campusgomobile.data.model.ParticipantStatusResponse
import com.campusgomobile.data.model.ParticipatingResponse
import com.campusgomobile.data.model.PlayStateResponse
import com.campusgomobile.data.model.QrResolveResponse
import com.campusgomobile.data.model.QuestDetailResponse
import com.campusgomobile.data.model.QuestHistoryResponse
import com.campusgomobile.data.model.QuestsResponse
import com.campusgomobile.data.model.StageDetail
import com.campusgomobile.data.network.NetworkModule
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

sealed class QuestsResult<out T> {
    data class Success<T>(val data: T) : QuestsResult<T>()
    data class Error(val message: String) : QuestsResult<Nothing>()
    data class NetworkError(val cause: Throwable) : QuestsResult<Nothing>()
}

class QuestsRepository(private val tokenStorage: TokenStorage) {

    private suspend fun questsApi() = with(NetworkModule) {
        val token = tokenStorage.token.first() ?: throw IOException("Not logged in")
        createQuestsApi(createAuthenticatedClient(token))
    }

    private suspend fun participantsApi() = with(NetworkModule) {
        val token = tokenStorage.token.first() ?: throw IOException("Not logged in")
        createParticipantsApi(createAuthenticatedClient(token))
    }

    suspend fun getQuests(page: Int = 1, perPage: Int = 15): QuestsResult<QuestsResponse> {
        repeat(2) { attempt ->
            val result = try {
                val api = questsApi()
                val response = api.getQuests(page = page, perPage = perPage)
                if (response.isSuccessful) {
                    response.body()?.let { QuestsResult.Success(it) }
                        ?: QuestsResult.Error("Empty response")
                } else {
                    val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                        ?: "Failed to load quests (${response.code()})"
                    QuestsResult.Error(msg)
                }
            } catch (e: HttpException) {
                QuestsResult.Error(e.message() ?: "Request failed")
            } catch (e: IOException) {
                QuestsResult.NetworkError(e)
            } catch (e: JsonSyntaxException) {
                null
            }
            if (result != null) return result
            if (attempt == 0) delay(400L)
        }
        return QuestsResult.Error(
            "Quest list response was incomplete or invalid. Pull to refresh to try again."
        )
    }

    suspend fun getParticipating(): QuestsResult<ParticipatingResponse> {
        return try {
            val api = questsApi()
            val response = api.getParticipating()
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load your quests (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun getQuestHistory(
        search: String? = null,
        questType: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        page: Int = 1,
        perPage: Int = 20
    ): QuestsResult<QuestHistoryResponse> {
        return try {
            val api = questsApi()
            val response = api.getHistory(
                search = search?.takeIf { it.isNotBlank() },
                questType = questType,
                dateFrom = dateFrom,
                dateTo = dateTo,
                page = page,
                perPage = perPage
            )
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load quest history (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun getQuestDetail(questId: Int, stage: Int? = null, includeQuestions: Boolean = false): QuestsResult<QuestDetailResponse> {
        return try {
            val api = questsApi()
            val response = api.getQuestDetail(questId = questId, stage = stage, includeQuestions = includeQuestions.takeIf { it })
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load quest (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun getPlayState(participantId: Int): QuestsResult<PlayStateResponse> {
        return try {
            val api = participantsApi()
            val response = api.getPlayState(participantId)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load play state (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun getStatus(participantId: Int): QuestsResult<ParticipantStatusResponse> {
        return try {
            val api = participantsApi()
            val response = api.getStatus(participantId)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to load status (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun quitQuest(participantId: Int): QuestsResult<Unit> {
        return try {
            val api = participantsApi()
            val response = api.quit(participantId)
            if (response.isSuccessful) {
                QuestsResult.Success(Unit)
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to leave quest (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun submitAnswers(
        participantId: Int,
        answers: List<Map<String, Int>>
    ): QuestsResult<PlayStateResponse> {
        return try {
            val api = participantsApi()
            val body = mapOf<String, Any>("answers" to answers)
            val response = api.submit(participantId, body)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to submit (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun submitStageCompleted(participantId: Int): QuestsResult<PlayStateResponse> {
        return try {
            val api = participantsApi()
            val body = mapOf<String, Any>("stage_completed" to true)
            val response = api.submit(participantId, body)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to submit (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun resolveQr(qr: String): QuestsResult<QrResolveResponse> {
        return try {
            val api = questsApi()
            val response = api.resolve(qr = qr)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to resolve QR (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    /** Resolve by quest ID and stage ID (e.g. from parsed QR payload "questId:stageId"). */
    suspend fun resolveByIds(questId: Int, stageId: Int): QuestsResult<QrResolveResponse> {
        return try {
            val api = questsApi()
            val response = api.resolve(questId = questId, stageId = stageId)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to resolve QR (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    suspend fun joinQuest(questId: Int, stageId: Int): QuestsResult<JoinResponse> {
        return try {
            val api = questsApi()
            val body = mapOf("quest_id" to questId, "stage_id" to stageId)
            val response = api.join(body)
            if (response.isSuccessful) {
                response.body()?.let { QuestsResult.Success(it) }
                    ?: QuestsResult.Error("Empty response")
            } else {
                val msg = response.errorBody()?.string()?.let { parseMessage(it) }
                    ?: "Failed to join quest (${response.code()})"
                QuestsResult.Error(msg)
            }
        } catch (e: HttpException) {
            QuestsResult.Error(e.message() ?: "Request failed")
        } catch (e: IOException) {
            QuestsResult.NetworkError(e)
        }
    }

    /** Load quest metadata and stages 1..currentStage for My Quest detail. */
    suspend fun loadMyQuestDetail(participantId: Int, questId: Int): QuestsResult<MyQuestDetailData> {
        val playResult = getPlayState(participantId)
        val playState = when (playResult) {
            is QuestsResult.Success -> playResult.data
            is QuestsResult.Error -> return playResult
            is QuestsResult.NetworkError -> return playResult
        }
        val firstDetail = getQuestDetail(questId, stage = 1)
        val quest = when (firstDetail) {
            is QuestsResult.Success -> firstDetail.data.quest
            is QuestsResult.Error -> return firstDetail
            is QuestsResult.NetworkError -> return firstDetail
        }
        val stages = mutableListOf<StageDetail>()
        for (n in 1..playState.currentStage) {
            when (val r = getQuestDetail(questId, stage = n)) {
                is QuestsResult.Success -> stages.add(r.data.stage)
                is QuestsResult.Error -> return r
                is QuestsResult.NetworkError -> return r
            }
        }
        return QuestsResult.Success(MyQuestDetailData(quest = quest, playState = playState, stages = stages))
    }

    /** Winner/completed = show all stages (1..totalStages). Eliminated/quit = show stages before quit (1..currentStage). */
    suspend fun loadQuestHistoryDetail(
        participantId: Int,
        questId: Int,
        status: String,
        currentStage: Int,
        totalStages: Int
    ): QuestsResult<MyQuestDetailData> {
        val lastStage = when (status.lowercase()) {
            "completed", "winner", "won" -> totalStages
            else -> currentStage // eliminated, quit, or other: show stages up to and including current
        }
        val firstDetail = getQuestDetail(questId, stage = 1)
        val quest = when (firstDetail) {
            is QuestsResult.Success -> firstDetail.data.quest
            is QuestsResult.Error -> return firstDetail
            is QuestsResult.NetworkError -> return firstDetail
        }
        val stages = mutableListOf<StageDetail>()
        for (n in 1..lastStage) {
            when (val r = getQuestDetail(questId, stage = n)) {
                is QuestsResult.Success -> stages.add(r.data.stage)
                is QuestsResult.Error -> return r
                is QuestsResult.NetworkError -> return r
            }
        }
        val playState = PlayStateResponse(
            participantId = participantId,
            questId = questId,
            currentStage = currentStage,
            status = status,
            totalStages = totalStages,
            questionType = quest.questionType,
            isElimination = quest.isElimination,
            stageLocked = false,
            nextStageOpensAt = null,
            nextStageNumber = null,
            stage = null
        )
        return QuestsResult.Success(MyQuestDetailData(quest = quest, playState = playState, stages = stages))
    }

    /** Load quest + all stages for Discover detail (view before join). */
    suspend fun loadDiscoverQuestDetail(questId: Int): QuestsResult<DiscoverQuestDetailData> {
        val firstDetail = getQuestDetail(questId, stage = 1)
        when (firstDetail) {
            is QuestsResult.Success -> {
                val quest = firstDetail.data.quest
                val stages = mutableListOf(firstDetail.data.stage)
                for (n in 2..quest.stagesCount) {
                    when (val r = getQuestDetail(questId, stage = n)) {
                        is QuestsResult.Success -> stages.add(r.data.stage)
                        is QuestsResult.Error -> return r
                        is QuestsResult.NetworkError -> return r
                    }
                }
                return QuestsResult.Success(DiscoverQuestDetailData(quest = quest, stages = stages))
            }
            is QuestsResult.Error -> return firstDetail
            is QuestsResult.NetworkError -> return firstDetail
        }
    }

    private fun parseMessage(body: String): String? {
        return try {
            val json = org.json.JSONObject(body)
            json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optJSONObject("errors")?.let { err ->
                    err.keys().asSequence().flatMap { key ->
                        (err.get(key) as? org.json.JSONArray)?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                    }.joinToString(" ")
                }?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
