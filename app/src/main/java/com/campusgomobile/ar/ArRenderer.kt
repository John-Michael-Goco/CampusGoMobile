package com.campusgomobile.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.campusgomobile.scanner.QrDecoder
import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renders AR background (camera) and a 3D card (billboard) at the anchor showing backend quest data.
 * Detects QR in the camera frame, hit-tests there and places the anchor.
 * Publishes card hit-test data each frame so the Activity can detect taps on choices.
 */
class ArRenderer(
    private val context: Context,
    private val session: Session,
    private val sessionResumed: AtomicBoolean,
    private val onSessionReady: () -> Unit,
    private val overlayTitle: String?,
    private val overlaySubtitle: String?,
    private val cardQuestionRef: AtomicReference<String?>,
    private val cardChoicesRef: AtomicReference<List<String>?>,
    private val selectedChoiceIndexRef: AtomicReference<Int?>,
    private val cardAnswerResultRef: AtomicReference<Boolean?>,
    private val cardScoreCorrectRef: AtomicReference<Int?>,
    private val cardScoreTotalRef: AtomicReference<Int?>,
    private val cardStageOutcomeRef: AtomicReference<CardRenderer.StageOutcomeDisplay?>,
    private val cardSubtitleRef: AtomicReference<String?>,
    private val choiceBoundsRef: AtomicReference<List<FloatArray>>,
    private val cardHitTestDataRef: AtomicReference<ArActivity.CardHitTestData?>,
    private val showJoinButtonRef: AtomicReference<Boolean>,
    private val joinButtonBoundsRef: AtomicReference<FloatArray?>
) : GLSurfaceView.Renderer {

    private lateinit var backgroundRenderer: BackgroundRenderer
    private var cardRenderer: CardRenderer? = null
    private var questTakenRenderer: QuestTakenRenderer? = null

    private var anchor: Anchor? = null
    private var anchorPlacedAtTimeMs: Long = -1L
    private var viewWidth = 0
    private var viewHeight = 0
    private var frameCount = 0

    private var lastCardQuestion: String? = null
    private var lastCardChoices: List<String>? = null
    private var lastCardSelectedIndex: Int? = null
    private var lastCardAnswerResult: Boolean? = null
    private var lastCardScoreCorrect: Int? = null
    private var lastCardScoreTotal: Int? = null
    private var lastCardStageOutcome: CardRenderer.StageOutcomeDisplay? = null
    private var lastCardSubtitle: String? = null
    private var lastShowJoinButton: Boolean = false

    private val cardModelMatrixCopy = FloatArray(16)

    private val indicatorDurationMs = 4000L
    private val qrDecodeExecutor = Executors.newSingleThreadExecutor()
    private val pendingQrCenterInImage = AtomicReference<FloatArray?>(null)
    private val viewCoordsFromQr = FloatArray(2)

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1f)
            backgroundRenderer = BackgroundRenderer()
            backgroundRenderer.createOnGlThread(context)
            session.setCameraTextureName(backgroundRenderer.getTextureId())
            val title = overlayTitle?.takeIf { it.isNotBlank() } ?: "Quest"
            val subtitle = overlaySubtitle?.takeIf { it.isNotBlank() } ?: ""
            val initialQuestion = cardQuestionRef.get()
            val initialChoices = cardChoicesRef.get()
            val initialSelected = selectedChoiceIndexRef.get()
            val initialResult = cardAnswerResultRef.get()
            val initialScoreCorrect = cardScoreCorrectRef.get()
            val initialScoreTotal = cardScoreTotalRef.get()
            val initialStageOutcome = cardStageOutcomeRef.get()
            try {
                cardRenderer = CardRenderer(
                    title, subtitle,
                    initialQuestion, initialChoices, initialSelected, initialResult,
                    initialScoreCorrect, initialScoreTotal, initialStageOutcome,
                    choiceBoundsRef, showJoinButtonRef, joinButtonBoundsRef
                ).also { it.createOnGlThread(context) }
            } catch (e: Exception) {
                android.util.Log.e("ArRenderer", "CardRenderer init failed", e)
            }
            try {
                questTakenRenderer = QuestTakenRenderer(title, subtitle).also { it.createOnGlThread(context) }
            } catch (e: Exception) {
                android.util.Log.e("ArRenderer", "QuestTakenRenderer init failed", e)
            }
            (context as? android.app.Activity)?.runOnUiThread { onSessionReady() }
        } catch (e: Exception) {
            android.util.Log.e("ArRenderer", "onSurfaceCreated failed", e)
            (context as? android.app.Activity)?.runOnUiThread { onSessionReady() }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        val rotation = when (context) {
            is android.app.Activity -> try {
                @Suppress("DEPRECATION")
                (context as android.app.Activity).windowManager.defaultDisplay.rotation
            } catch (_: Exception) { 0 }
            else -> 0
        }
        session.setDisplayGeometry(rotation, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!sessionResumed.get()) return
        val frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            return
        } catch (e: Exception) {
            return
        } ?: return
        val camera = frame.camera ?: return

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        try {
            backgroundRenderer.draw(frame)
        } catch (_: Exception) {
            return
        }

        if (camera.trackingState != TrackingState.TRACKING) return

        if (anchor == null) {
            val pending = pendingQrCenterInImage.getAndSet(null)
            if (pending != null && pending.size >= 2) {
                frame.transformCoordinates2d(
                    Coordinates2d.IMAGE_PIXELS,
                    pending,
                    Coordinates2d.VIEW,
                    viewCoordsFromQr
                )
                placeAnchorAtViewPosition(frame, camera, viewCoordsFromQr[0], viewCoordsFromQr[1], fromQr = true)
            } else {
                frameCount++
                if (frameCount <= 4 || frameCount % 5 == 0) tryCaptureQrAndSetPending(frame)
                if (frameCount > 150) placeAnchorAtViewPosition(frame, camera, viewWidth / 2f, viewHeight / 2f, fromQr = false)
            }
        }

        anchor?.let { anchor ->
            if (anchor.trackingState != TrackingState.TRACKING) return@let
            if (anchorPlacedAtTimeMs < 0L) anchorPlacedAtTimeMs = SystemClock.elapsedRealtime()
            camera.getViewMatrix(viewMatrix, 0)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
            questTakenRenderer?.draw(anchor.pose, camera, viewMatrix, projectionMatrix, showLabel = false)
            // Card shows immediately (no delay)
            val newSubtitle = cardSubtitleRef.get()
            val subtitleChanged = newSubtitle != null && newSubtitle != lastCardSubtitle
            if (subtitleChanged) {
                lastCardSubtitle = newSubtitle
                cardRenderer?.updateSubtitle(newSubtitle!!)
            }
            val currentQuestion = cardQuestionRef.get()
            val currentChoices = cardChoicesRef.get()
            val currentSelected = selectedChoiceIndexRef.get()
            val currentResult = cardAnswerResultRef.get()
            val currentScoreCorrect = cardScoreCorrectRef.get()
            val currentScoreTotal = cardScoreTotalRef.get()
            val currentStageOutcome = cardStageOutcomeRef.get()
            val currentShowJoin = showJoinButtonRef.get()
            if (subtitleChanged ||
                currentQuestion != lastCardQuestion || currentChoices != lastCardChoices ||
                currentSelected != lastCardSelectedIndex || currentResult != lastCardAnswerResult ||
                currentScoreCorrect != lastCardScoreCorrect || currentScoreTotal != lastCardScoreTotal ||
                currentStageOutcome != lastCardStageOutcome || currentShowJoin != lastShowJoinButton) {
                lastCardQuestion = currentQuestion
                lastCardChoices = currentChoices
                lastCardSelectedIndex = currentSelected
                lastCardAnswerResult = currentResult
                lastCardScoreCorrect = currentScoreCorrect
                lastCardScoreTotal = currentScoreTotal
                lastCardStageOutcome = currentStageOutcome
                lastShowJoinButton = currentShowJoin
                cardRenderer?.updateContent(
                    currentQuestion, currentChoices, currentSelected, currentResult,
                    currentScoreCorrect, currentScoreTotal, currentStageOutcome
                )
            }
            cardRenderer?.draw(anchor.pose, camera, viewMatrix, projectionMatrix, cardModelMatrixCopy)
            cardHitTestDataRef.set(ArActivity.CardHitTestData(
                modelMatrix = cardModelMatrixCopy.clone(),
                viewMatrix = viewMatrix.clone(),
                projectionMatrix = projectionMatrix.clone(),
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                cardHalfW = 0.17f,
                cardHalfH = 0.1f
            ))
        }
    }

    private fun tryCaptureQrAndSetPending(frame: Frame) {
        val image = try {
            frame.acquireCameraImage()
        } catch (_: Exception) {
            return
        }
        try {
            if (image.format != ImageFormat.YUV_420_888) return
            val w = image.width
            val h = image.height
            val yPlane = image.planes[0]
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val yBuffer = yPlane.buffer
            val packed = ByteArray(w * h)
            var srcIdx = 0
            var dstIdx = 0
            for (_y in 0 until h) {
                for (x in 0 until w) {
                    packed[dstIdx++] = yBuffer.get(srcIdx)
                    srcIdx += pixelStride
                }
                srcIdx += rowStride - w * pixelStride
            }
            qrDecodeExecutor.execute {
                try {
                    val pixels = IntArray(w * h)
                    for (i in packed.indices) {
                        val v = packed[i].toInt() and 0xFF
                        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                    }
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
                    val result = QrDecoder.decodeFromBitmapWithCenter(bitmap)
                    result?.second?.let { center -> pendingQrCenterInImage.set(center) }
                } catch (_: Exception) { }
            }
        } finally {
            image.close()
        }
    }

    /**
     * Place anchor at view (x,y). When fromQr is true, only create anchor on a real hit (plane or first hit);
     * never use "in front of camera" so the object stays fixed on the QR surface.
     */
    private fun placeAnchorAtViewPosition(frame: Frame, camera: Camera, x: Float, y: Float, fromQr: Boolean) {
        val hitResults = frame.hitTest(x, y)
        for (hit in hitResults) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                anchor = hit.createAnchor()
                return
            }
        }
        hitResults.firstOrNull()?.let { anchor = it.createAnchor(); return }
        if (!fromQr) {
            val poseInFront = camera.pose.compose(
                com.google.ar.core.Pose.makeTranslation(0f, 0f, -0.5f)
            )
            anchor = session.createAnchor(poseInFront)
        }
    }

    fun detachAnchor() {
        anchor?.detach()
        anchor = null
        anchorPlacedAtTimeMs = -1L
    }
}
