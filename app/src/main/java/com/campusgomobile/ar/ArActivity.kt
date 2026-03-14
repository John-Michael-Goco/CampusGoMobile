package com.campusgomobile.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.model.PlayQuestion
import com.campusgomobile.data.quests.QuestsRepository
import com.campusgomobile.data.quests.QuestsResult
import com.campusgomobile.util.QuestTimeUtils
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * AR screen: camera background and a 3D card at the QR anchor showing quest title, question in the black box,
 * and Correct!/Wrong! after the user answers. Launched after scanning a quest QR (e.g. from Scanner "View in AR").
 */
class ArActivity : ComponentActivity() {

    private var session: Session? = null
    private var surfaceView: GLSurfaceView? = null
    private var arRenderer: ArRenderer? = null
    private var sessionResumedByRenderer = false
    private var pendingOverlayTitle: String? = null
    private var pendingOverlaySubtitle: String? = null

    private val cardQuestionRef = AtomicReference<String?>(null)
    private val cardChoicesRef = AtomicReference<List<String>?>(null)
    private val selectedChoiceIndexRef = AtomicReference<Int?>(null)
    private val cardAnswerResultRef = AtomicReference<Boolean?>(null)
    private val cardScoreCorrectRef = AtomicReference<Int?>(null)
    private val cardScoreTotalRef = AtomicReference<Int?>(null)
    private val cardStageOutcomeRef = AtomicReference<CardRenderer.StageOutcomeDisplay?>(null)
    private val cardSubtitleRef = AtomicReference<String?>(null)
    private val choiceBoundsRef = AtomicReference<List<FloatArray>>(emptyList())
    private val cardHitTestDataRef = AtomicReference<CardHitTestData?>(null)
    private val showJoinButtonRef = AtomicReference(false)
    private val joinButtonBoundsRef = AtomicReference<FloatArray?>(null)
    private var arStageQuestions: List<PlayQuestion>? = null
    private var currentQuestionIndex: Int = 0
    private var lastCorrectCountAfterSubmit: Int = 0
    private var participantIdForSubmit: Int = 0
    private var questIdForJoin: Int = 0
    private var stageIdForJoin: Int = 0
    private var questionType: String? = null
    private val submittedAnswers = mutableListOf<Map<String, Int>>()

    /** Published by ArRenderer each frame when the card is drawn; used for touch hit-testing. */
    data class CardHitTestData(
        val modelMatrix: FloatArray,
        val viewMatrix: FloatArray,
        val projectionMatrix: FloatArray,
        val viewWidth: Int,
        val viewHeight: Int,
        val cardHalfW: Float,
        val cardHalfH: Float
    ) {
        override fun equals(other: Any?) = (other as? CardHitTestData)?.modelMatrix === modelMatrix
        override fun hashCode() = modelMatrix.hashCode()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) initAr(pendingOverlayTitle, pendingOverlaySubtitle)
        else {
            Toast.makeText(this, "Camera permission needed for AR", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val questTitle = intent.getStringExtra(EXTRA_QUEST_TITLE)
        val stageNumber = intent.getIntExtra(EXTRA_STAGE_NUMBER, 0)
        val locationHint = intent.getStringExtra(EXTRA_LOCATION_HINT)
        questIdForJoin = intent.getIntExtra(EXTRA_QUEST_ID, 0)
        stageIdForJoin = intent.getIntExtra(EXTRA_STAGE_ID, 0)
        participantIdForSubmit = intent.getIntExtra(EXTRA_PARTICIPANT_ID, 0)
        questionType = intent.getStringExtra(EXTRA_QUESTION_TYPE)
        val showJoinOnCard = intent.getBooleanExtra(EXTRA_SHOW_JOIN_ON_CARD, false)
        val stageStart = intent.getStringExtra(EXTRA_STAGE_START)
        val rejectReason = intent.getStringExtra(EXTRA_REJECT_REASON)
        val isUpcoming = QuestTimeUtils.isStageStartInFuture(stageStart)
        val actuallyShowJoin = showJoinOnCard && participantIdForSubmit <= 0 && !isUpcoming
        showJoinButtonRef.set(actuallyShowJoin)

        if (rejectReason != null) {
            cardStageOutcomeRef.set(CardRenderer.StageOutcomeDisplay.Rejected(rejectReason))
            cardQuestionRef.set(null)
            cardChoicesRef.set(emptyList())
        } else if (showJoinOnCard && isUpcoming && !stageStart.isNullOrBlank()) {
            cardQuestionRef.set("Quest starts at $stageStart")
            cardChoicesRef.set(emptyList())
        }

        val subtitleParts = mutableListOf<String>()
        if (stageNumber > 0) subtitleParts.add("Stage $stageNumber")
        if (!locationHint.isNullOrBlank()) subtitleParts.add(locationHint)
        pendingOverlayTitle = questTitle
        pendingOverlaySubtitle = subtitleParts.joinToString(" · ")

        surfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setZOrderMediaOverlay(true)
        }
        val container = FrameLayout(this).apply {
            addView(surfaceView)
            addView(View(this@ArActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        hitTestCardAndSelectChoice(event.x, event.y)
                    }
                    true
                }
            })
        }
        setContentView(container)

        if (rejectReason == null && questIdForJoin > 0 && stageIdForJoin > 0) {
            lifecycleScope.launch {
                var pid = participantIdForSubmit
                if (pid <= 0) pid = resolveParticipantIdForQuest(questIdForJoin)
                if (pid > 0) {
                    participantIdForSubmit = pid
                    showJoinButtonRef.set(false)
                    if (questionType == "qr_scan") {
                        submitQrStageCompleted()
                    } else {
                        fetchStageQuestion(questIdForJoin, stageNumber, pid)
                    }
                }
            }
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                initAr(pendingOverlayTitle, pendingOverlaySubtitle)
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /** If resolve didn't return participant_id, look it up from GET /api/quests/participating. */
    private suspend fun resolveParticipantIdForQuest(questId: Int): Int {
        val tokenStorage = TokenStorage(applicationContext)
        val repo = QuestsRepository(tokenStorage)
        return when (val r = repo.getParticipating()) {
            is QuestsResult.Success -> r.data.participations
                .firstOrNull { it.questId == questId }
                ?.participantId ?: 0
            else -> 0
        }
    }

    private suspend fun fetchStageQuestion(questId: Int, stageNumber: Int, participantId: Int) {
        submittedAnswers.clear()
        lastCorrectCountAfterSubmit = 0
        val tokenStorage = TokenStorage(applicationContext)
        val repo = QuestsRepository(tokenStorage)
        if (participantId > 0) {
            when (val r = repo.getPlayState(participantId)) {
                is QuestsResult.Success -> {
                    updateSubtitleFromPlayState(r.data)
                    lastCorrectCountAfterSubmit = r.data.correctCount ?: 0
                    val questions = r.data.stage?.questions
                    arStageQuestions = questions
                    if (!questions.isNullOrEmpty()) {
                        val firstUnanswered = questions.indexOfFirst { !it.alreadyAnswered }
                        currentQuestionIndex = if (firstUnanswered >= 0) firstUnanswered else questions.size
                        if (currentQuestionIndex < questions.size) {
                            val q = questions[currentQuestionIndex]
                            cardQuestionRef.set(q.questionText)
                            cardChoicesRef.set(q.choices?.map { it.choiceText })
                            cardAnswerResultRef.set(null)
                            selectedChoiceIndexRef.set(null)
                        } else {
                            cardQuestionRef.set("Stage complete")
                            cardChoicesRef.set(emptyList())
                            cardAnswerResultRef.set(null)
                        }
                    }
                }
                else -> { /* ignore */ }
            }
        } else {
            when (val r = repo.getQuestDetail(questId, stageNumber, includeQuestions = true)) {
                is QuestsResult.Success -> {
                    val questions = r.data.stage.questions
                    arStageQuestions = questions
                    currentQuestionIndex = 0
                    questions?.firstOrNull()?.let { q ->
                        cardQuestionRef.set(q.questionText)
                        cardChoicesRef.set(q.choices?.map { it.choiceText })
                    }
                }
                else -> { /* ignore */ }
            }
        }
    }

    /** Touch hit-test: unproject to ray, intersect with card plane; if join mode and hit join button, join; else find choice and set selection. */
    private fun hitTestCardAndSelectChoice(screenX: Float, screenY: Float) {
        val data = cardHitTestDataRef.get() ?: return
        if (showJoinButtonRef.get()) {
            val joinBounds = joinButtonBoundsRef.get() ?: return
            if (joinBounds.size < 4) return
            val projView = FloatArray(16)
            Matrix.multiplyMM(projView, 0, data.projectionMatrix, 0, data.viewMatrix, 0)
            val invProjView = FloatArray(16)
            if (!Matrix.invertM(invProjView, 0, projView, 0)) return
            val ndcX = (screenX / data.viewWidth) * 2f - 1f
            val ndcY = 1f - (screenY / data.viewHeight) * 2f
            val near = FloatArray(4)
            val far = FloatArray(4)
            unproject(invProjView, ndcX, ndcY, 0f, near)
            unproject(invProjView, ndcX, ndcY, 1f, far)
            val ox = near[0] / near[3]; val oy = near[1] / near[3]; val oz = near[2] / near[3]
            val dx = (far[0] / far[3]) - ox; val dy = (far[1] / far[3]) - oy; val dz = (far[2] / far[3]) - oz
            val cx = data.modelMatrix[12]; val cy = data.modelMatrix[13]; val cz = data.modelMatrix[14]
            val nx = data.modelMatrix[2]; val ny = data.modelMatrix[6]; val nz = data.modelMatrix[10]
            val denom = nx * dx + ny * dy + nz * dz
            if (kotlin.math.abs(denom) < 1e-6f) return
            val t = (nx * (cx - ox) + ny * (cy - oy) + nz * (cz - oz)) / denom
            if (t < 0f) return
            val hx = ox + t * dx; val hy = oy + t * dy; val hz = oz + t * dz
            val invModel = FloatArray(16)
            if (!Matrix.invertM(invModel, 0, data.modelMatrix, 0)) return
            val localX = invModel[0] * hx + invModel[4] * hy + invModel[8] * hz + invModel[12]
            val localY = invModel[1] * hx + invModel[5] * hy + invModel[9] * hz + invModel[13]
            if (kotlin.math.abs(localX) > data.cardHalfW || kotlin.math.abs(localY) > data.cardHalfH) return
            val texU = (localX + data.cardHalfW) / (2f * data.cardHalfW)
            val texV = (data.cardHalfH - localY) / (2f * data.cardHalfH)
            if (texU >= joinBounds[0] && texU <= joinBounds[1] && texV >= joinBounds[2] && texV <= joinBounds[3]) {
                lifecycleScope.launch { joinQuestFromCard() }
                return
            }
            return
        }
        if (cardAnswerResultRef.get() != null) return
        if (cardStageOutcomeRef.get() != null) return
        if (cardScoreCorrectRef.get() != null) return
        val bounds = choiceBoundsRef.get().ifEmpty { return }
        val projView = FloatArray(16)
        Matrix.multiplyMM(projView, 0, data.projectionMatrix, 0, data.viewMatrix, 0)
        val invProjView = FloatArray(16)
        if (!Matrix.invertM(invProjView, 0, projView, 0)) return
        val ndcX = (screenX / data.viewWidth) * 2f - 1f
        val ndcY = 1f - (screenY / data.viewHeight) * 2f
        val near = FloatArray(4)
        val far = FloatArray(4)
        unproject(invProjView, ndcX, ndcY, 0f, near)
        unproject(invProjView, ndcX, ndcY, 1f, far)
        val ox = near[0] / near[3]; val oy = near[1] / near[3]; val oz = near[2] / near[3]
        val dx = (far[0] / far[3]) - ox; val dy = (far[1] / far[3]) - oy; val dz = (far[2] / far[3]) - oz
        val cx = data.modelMatrix[12]; val cy = data.modelMatrix[13]; val cz = data.modelMatrix[14]
        val nx = data.modelMatrix[2]; val ny = data.modelMatrix[6]; val nz = data.modelMatrix[10]
        val denom = nx * dx + ny * dy + nz * dz
        if (kotlin.math.abs(denom) < 1e-6f) return
        val t = (nx * (cx - ox) + ny * (cy - oy) + nz * (cz - oz)) / denom
        if (t < 0f) return
        val hx = ox + t * dx; val hy = oy + t * dy; val hz = oz + t * dz
        val invModel = FloatArray(16)
        if (!Matrix.invertM(invModel, 0, data.modelMatrix, 0)) return
        val localX = invModel[0] * hx + invModel[4] * hy + invModel[8] * hz + invModel[12]
        val localY = invModel[1] * hx + invModel[5] * hy + invModel[9] * hz + invModel[13]
        if (kotlin.math.abs(localX) > data.cardHalfW || kotlin.math.abs(localY) > data.cardHalfH) return
        val texU = (localX + data.cardHalfW) / (2f * data.cardHalfW)
        val texV = (data.cardHalfH - localY) / (2f * data.cardHalfH)
        val idx = bounds.indexOfFirst { rect ->
            rect.size >= 4 && texU >= rect[0] && texU <= rect[1] && texV >= rect[2] && texV <= rect[3]
        }
        if (idx >= 0) {
            selectedChoiceIndexRef.set(idx)
            lifecycleScope.launch { submitChoiceImmediate(idx) }
        }
    }

    private fun unproject(invProjView: FloatArray, ndcX: Float, ndcY: Float, ndcZ: Float, out: FloatArray) {
        out[0] = invProjView[0] * ndcX + invProjView[4] * ndcY + invProjView[8] * ndcZ + invProjView[12]
        out[1] = invProjView[1] * ndcX + invProjView[5] * ndcY + invProjView[9] * ndcZ + invProjView[13]
        out[2] = invProjView[2] * ndcX + invProjView[6] * ndcY + invProjView[10] * ndcZ + invProjView[14]
        out[3] = invProjView[3] * ndcX + invProjView[7] * ndcY + invProjView[11] * ndcZ + invProjView[15]
    }

    private suspend fun joinQuestFromCard() {
        if (questIdForJoin <= 0 || stageIdForJoin <= 0) return
        val tokenStorage = TokenStorage(applicationContext)
        val repo = QuestsRepository(tokenStorage)
        when (val r = repo.joinQuest(questIdForJoin, stageIdForJoin)) {
            is QuestsResult.Success -> {
                participantIdForSubmit = r.data.participantId
                showJoinButtonRef.set(false)
                if (questionType == "qr_scan") {
                    submitQrStageCompleted()
                } else {
                    fetchStageQuestion(questIdForJoin, 1, participantIdForSubmit)
                }
                runOnUiThread { Toast.makeText(this, "You joined the quest!", Toast.LENGTH_SHORT).show() }
            }
            is QuestsResult.Error -> runOnUiThread { Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show() }
            is QuestsResult.NetworkError -> runOnUiThread { Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun submitChoiceImmediate(choiceIndex: Int) {
        val questions = arStageQuestions ?: return
        if (currentQuestionIndex !in questions.indices) return
        val question = questions[currentQuestionIndex]
        val choices = question.choices ?: return
        if (choiceIndex !in choices.indices) return
        var pid = participantIdForSubmit
        if (pid <= 0 && questIdForJoin > 0) {
            pid = resolveParticipantIdForQuest(questIdForJoin)
            if (pid > 0) participantIdForSubmit = pid
        }
        if (pid <= 0) {
            runOnUiThread { Toast.makeText(this, "Join the quest first to submit", Toast.LENGTH_SHORT).show() }
            return
        }
        val answer = mapOf("question_id" to question.id, "choice_id" to choices[choiceIndex].id)
        submittedAnswers.add(answer)
        submitAndShowResultThenNextQuestion(listOf(answer))
    }

    /**
     * Submit one answer and show Correct/Wrong, then score and outcome.
     * Expected backend contract: see CAMPUSGO DOCS/api-docs/submit-response-contract.md
     * (correct_count/total_count semantics and outcome "completed" for last stage).
     */
    private suspend fun submitAndShowResultThenNextQuestion(answers: List<Map<String, Int>>) {
        val tokenStorage = TokenStorage(applicationContext)
        val repo = QuestsRepository(tokenStorage)
        when (val r = repo.submitAnswers(participantIdForSubmit, answers)) {
            is QuestsResult.Success -> {
                val data = r.data
                val correct = data.correctCount ?: 0
                val total = data.totalCount?.takeIf { it > 0 } ?: 1
                val outcome = data.outcome
                val stageEndedByBackend = outcome in listOf("advanced", "completed", "eliminated", "awaiting_ranking")

                val isCorrect = when {
                    total == 1 && correct <= 1 -> correct == 1
                    else -> correct > lastCorrectCountAfterSubmit
                }
                lastCorrectCountAfterSubmit = when {
                    total == 1 && correct <= 1 -> lastCorrectCountAfterSubmit + correct
                    else -> correct
                }

                currentQuestionIndex++
                val questions = arStageQuestions
                val noMoreQuestions = questions != null && currentQuestionIndex >= questions.size
                val stageEnded = stageEndedByBackend || noMoreQuestions
                cardStageOutcomeRef.set(null)
                cardScoreCorrectRef.set(null)
                cardScoreTotalRef.set(null)
                cardAnswerResultRef.set(isCorrect)
                delay(2000L)

                if (stageEnded) {
                    var stageData = data
                    if (!stageEndedByBackend && submittedAnswers.size > 1) {
                        val batchResult = repo.submitAnswers(participantIdForSubmit, submittedAnswers.toList())
                        if (batchResult is QuestsResult.Success) stageData = batchResult.data
                    }

                    val scoreCorrect = stageData.correctCount
                        ?: (if (total == 1) lastCorrectCountAfterSubmit else correct)
                    val scoreTotal = stageData.totalCount
                        ?: (if (total == 1) (questions?.size ?: total) else total)
                    cardScoreCorrectRef.set(scoreCorrect)
                    cardScoreTotalRef.set(scoreTotal)
                    delay(4000L)

                    resolveAndShowOutcome(repo, stageData)
                } else {
                    if (questions != null && currentQuestionIndex < questions.size) {
                        val next = questions[currentQuestionIndex]
                        cardQuestionRef.set(next.questionText)
                        cardChoicesRef.set(next.choices?.map { it.choiceText })
                        cardAnswerResultRef.set(null)
                        selectedChoiceIndexRef.set(null)
                    }
                }
            }
            is QuestsResult.Error -> runOnUiThread { Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show() }
            is QuestsResult.NetworkError -> runOnUiThread { Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show() }
        }
    }

    /**
     * QR-scan quest: submit stage_completed=true, show checkmark, then outcome
     * (Winner / next location / unlock time) — same flow as MCQ but no questions.
     */
    private suspend fun submitQrStageCompleted() {
        val pid = participantIdForSubmit
        if (pid <= 0) {
            cardQuestionRef.set("Join the quest first")
            cardChoicesRef.set(emptyList())
            return
        }
        cardQuestionRef.set("Submitting…")
        cardChoicesRef.set(emptyList())

        val tokenStorage = TokenStorage(applicationContext)
        val repo = QuestsRepository(tokenStorage)
        when (val r = repo.submitStageCompleted(pid)) {
            is QuestsResult.Success -> {
                val data = r.data

                cardQuestionRef.set(null)
                cardChoicesRef.set(emptyList())
                cardAnswerResultRef.set(true)
                delay(2000L)

                resolveAndShowOutcome(repo, data)
            }
            is QuestsResult.Error -> {
                cardQuestionRef.set(r.message)
                cardChoicesRef.set(emptyList())
            }
            is QuestsResult.NetworkError -> {
                cardQuestionRef.set("Network error")
                cardChoicesRef.set(emptyList())
            }
        }
    }

    /**
     * Shared outcome resolution: re-fetch play state, determine outcome, show on card.
     * If awaiting_ranking, shows the waiting card and stops — the backend will send a
     * push notification when ranking is resolved, so the user can leave AR.
     */
    private suspend fun resolveAndShowOutcome(
        repo: QuestsRepository,
        submitData: com.campusgomobile.data.model.PlayStateResponse
    ) {
        val pid = participantIdForSubmit
        val playData = when (val ps = repo.getPlayState(pid)) {
            is QuestsResult.Success -> { updateSubtitleFromPlayState(ps.data); ps.data }
            else -> null
        }

        val finalOutcome = playData?.outcome ?: submitData.outcome
        val finalStatus = playData?.status ?: submitData.status
        val isAwaiting = finalOutcome == "awaiting_ranking"
                || (playData?.awaitingRanking ?: submitData.awaitingRanking)

        if (isAwaiting) {
            cardStageOutcomeRef.set(CardRenderer.StageOutcomeDisplay.AwaitingRanking)
            cardAnswerResultRef.set(null)
            cardScoreCorrectRef.set(null)
            cardScoreTotalRef.set(null)
            return
        }

        val stageSource = playData ?: submitData
        val finalRewards = playData?.rewards ?: submitData.rewards
        val stageLocked = playData?.stageLocked ?: submitData.stageLocked

        val totalStages = stageSource.totalStages
        val currentStage = stageSource.currentStage
        val isLastStage = totalStages > 0 && currentStage >= totalStages

        val backendMessage = submitData.message ?: playData?.message

        val display = when {
            finalOutcome == "eliminated" || finalStatus == "eliminated"
                    || stageSource.failed == true -> {
                val msg = backendMessage ?: when {
                    isLastStage -> "A winner has already been determined."
                    else -> "The max survivors for this stage has been reached."
                }
                CardRenderer.StageOutcomeDisplay.Eliminated(msg)
            }

            finalOutcome == "completed"
                    || finalStatus in listOf("completed", "winner", "won") -> {
                CardRenderer.StageOutcomeDisplay.Winner(
                    pointsEarned = finalRewards?.pointsEarned ?: 0,
                    levelUp = finalRewards?.levelUp ?: false,
                    newLevel = finalRewards?.newLevel,
                    achievements = finalRewards?.achievements?.map { it.name } ?: emptyList(),
                    customPrize = finalRewards?.customPrize
                )
            }

            else -> buildNextStageDisplay(stageSource, stageLocked)
        }

        cardStageOutcomeRef.set(display)
        cardAnswerResultRef.set(null)
        cardScoreCorrectRef.set(null)
        cardScoreTotalRef.set(null)
    }

    private fun updateSubtitleFromPlayState(data: com.campusgomobile.data.model.PlayStateResponse) {
        val stage = data.currentStage
        val total = data.totalStages
        if (stage > 0 && total > 0) {
            val location = data.stage?.locationHint
            val parts = mutableListOf("Stage $stage of $total")
            if (!location.isNullOrBlank()) parts.add(location)
            cardSubtitleRef.set(parts.joinToString(" · "))
        }
    }

    /**
     * Build the "advanced" outcome display, respecting stage lock status.
     * After advancing, the re-fetched play state has already moved to the new stage,
     * so next_stage_location_hint is for the stage AFTER that. The current stage's
     * location_hint (stage.location_hint) is the one the user needs to go to.
     */
    private fun buildNextStageDisplay(
        data: com.campusgomobile.data.model.PlayStateResponse,
        stageLocked: Boolean
    ): CardRenderer.StageOutcomeDisplay {
        val locationHint = data.nextStageLocationHint
            ?: data.stage?.locationHint
        return when {
            stageLocked || !data.nextStageStartsAt.isNullOrBlank() -> {
                val time = data.nextStageOpensAt ?: data.nextStageStartsAt
                CardRenderer.StageOutcomeDisplay.ProceedUnlockAt(time ?: "Locked")
            }
            !locationHint.isNullOrBlank() ->
                CardRenderer.StageOutcomeDisplay.ProceedNextLocation(locationHint)
            !data.nextStageOpensAt.isNullOrBlank() ->
                CardRenderer.StageOutcomeDisplay.ProceedUnlockAt(data.nextStageOpensAt.orEmpty())
            else ->
                CardRenderer.StageOutcomeDisplay.ProceedNextLocation("Stage complete")
        }
    }

    private fun initAr(overlayTitle: String? = null, overlaySubtitle: String? = null) {
        when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> createSessionAndRenderer(pendingOverlayTitle, pendingOverlaySubtitle)
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                ArCoreApk.getInstance().requestInstall(this, true)
                finish()
            }
            else -> {
                Toast.makeText(this, "ARCore is not available on this device", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun createSessionAndRenderer(overlayTitle: String?, overlaySubtitle: String?) {
        try {
            session = Session(this).also { s ->
                s.configure(
                    Config(s).apply {
                        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create AR session: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val s = session ?: return
        val resumed = AtomicBoolean(false)
        arRenderer = ArRenderer(
            context = this,
            session = s,
            sessionResumed = resumed,
            onSessionReady = {
                runOnUiThread {
                    try {
                        s.resume()
                        resumed.set(true)
                        sessionResumedByRenderer = true
                    } catch (e: Exception) {
                        android.util.Log.e("ArActivity", "Session resume failed", e)
                    }
                }
            },
            overlayTitle = overlayTitle,
            overlaySubtitle = overlaySubtitle ?: "",
            cardQuestionRef = cardQuestionRef,
            cardChoicesRef = cardChoicesRef,
            selectedChoiceIndexRef = selectedChoiceIndexRef,
            cardAnswerResultRef = cardAnswerResultRef,
            cardScoreCorrectRef = cardScoreCorrectRef,
            cardScoreTotalRef = cardScoreTotalRef,
            cardStageOutcomeRef = cardStageOutcomeRef,
            cardSubtitleRef = cardSubtitleRef,
            choiceBoundsRef = choiceBoundsRef,
            cardHitTestDataRef = cardHitTestDataRef,
            showJoinButtonRef = showJoinButtonRef,
            joinButtonBoundsRef = joinButtonBoundsRef
        )
        surfaceView?.setRenderer(arRenderer)
    }

    override fun onResume() {
        super.onResume()
        if (sessionResumedByRenderer) session?.resume()
    }

    override fun onPause() {
        super.onPause()
        session?.pause()
    }

    override fun onDestroy() {
        arRenderer?.detachAnchor()
        arRenderer = null
        session?.close()
        session = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_QUEST_TITLE = "ar_quest_title"
        private const val EXTRA_STAGE_NUMBER = "ar_stage_number"
        private const val EXTRA_LOCATION_HINT = "ar_location_hint"
        private const val EXTRA_QUEST_ID = "ar_quest_id"
        private const val EXTRA_STAGE_ID = "ar_stage_id"
        private const val EXTRA_PARTICIPANT_ID = "ar_participant_id"
        private const val EXTRA_SHOW_JOIN_ON_CARD = "ar_show_join_on_card"
        private const val EXTRA_STAGE_START = "ar_stage_start"
        private const val EXTRA_QUESTION_TYPE = "ar_question_type"
        private const val EXTRA_REJECT_REASON = "ar_reject_reason"

        fun launch(
            activity: ComponentActivity,
            questTitle: String? = null,
            stageNumber: Int = 0,
            locationHint: String? = null,
            questId: Int = 0,
            stageId: Int = 0,
            participantId: Int = 0,
            showJoinOnCard: Boolean = false,
            stageStart: String? = null,
            questionType: String? = null,
            rejectReason: String? = null
        ) {
            activity.startActivity(Intent(activity, ArActivity::class.java).apply {
                questTitle?.let { putExtra(EXTRA_QUEST_TITLE, it) }
                if (stageNumber > 0) putExtra(EXTRA_STAGE_NUMBER, stageNumber)
                locationHint?.let { putExtra(EXTRA_LOCATION_HINT, it) }
                if (questId > 0) putExtra(EXTRA_QUEST_ID, questId)
                if (stageId > 0) putExtra(EXTRA_STAGE_ID, stageId)
                if (participantId > 0) putExtra(EXTRA_PARTICIPANT_ID, participantId)
                if (showJoinOnCard) putExtra(EXTRA_SHOW_JOIN_ON_CARD, true)
                stageStart?.let { putExtra(EXTRA_STAGE_START, it) }
                questionType?.let { putExtra(EXTRA_QUESTION_TYPE, it) }
                rejectReason?.let { putExtra(EXTRA_REJECT_REASON, it) }
            })
        }
    }
}
