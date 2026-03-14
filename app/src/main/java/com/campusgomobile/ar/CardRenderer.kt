package com.campusgomobile.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicReference

/**
 * Renders a 3D card (textured quad) as a billboard at the anchor.
 * Shows quest title in header, question + choices in the black box; after answer shows Correct!/Wrong!.
 * Choice bounds (uMin, uMax, vMin, vMax per choice) are published to choiceBoundsRef for touch hit-testing.
 * When showJoinButtonRef is true (stage 1, user not in), draws a "Join quest" button and publishes its bounds to joinButtonBoundsRef.
 * When scoreRef is set shows "Score: X / Y". When stageOutcomeRef is set shows proceed/eliminated/awaiting/winner.
 */
class CardRenderer(
    private val title: String,
    private var subtitle: String,
    private var question: String? = null,
    private var choices: List<String>? = null,
    private var selectedChoiceIndex: Int? = null,
    private var answerResult: Boolean? = null,
    private var answerCorrectLabel: String = "Correct",
    private var scoreCorrect: Int? = null,
    private var scoreTotal: Int? = null,
    private var stageOutcome: StageOutcomeDisplay? = null,
    private val choiceBoundsRef: AtomicReference<List<FloatArray>>? = null,
    private val showJoinButtonRef: AtomicReference<Boolean>? = null,
    private val joinButtonBoundsRef: AtomicReference<FloatArray?>? = null
) {

    /** What to show after stage ends: next location, unlock time, eliminated, awaiting ranking, winner rewards, or rejected. */
    sealed class StageOutcomeDisplay {
        data class ProceedNextLocation(val location: String) : StageOutcomeDisplay()
        data class ProceedUnlockAt(val opensAt: String) : StageOutcomeDisplay()
        data class Eliminated(val message: String? = null) : StageOutcomeDisplay()
        object AwaitingRanking : StageOutcomeDisplay()
        data class Rejected(val reason: String) : StageOutcomeDisplay()
        data class Winner(
            val pointsEarned: Int,
            val levelUp: Boolean,
            val newLevel: Int?,
            val achievements: List<String>,
            val customPrize: String?
        ) : StageOutcomeDisplay()
    }

    private var program = 0
    private var positionAttrib = 0
    private var texCoordAttrib = 0
    private var mvpUniform = 0
    private var textureUniform = 0
    private var textureId = IntArray(1)
    private val modelMatrix = FloatArray(16)
    private val modelViewProjection = FloatArray(16)

    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null

    companion object {
        private const val CARD_WIDTH_M = 0.34f
        private const val CARD_HEIGHT_M = 0.20f
        private const val CARD_OFFSET_Y_M = -0.06f

        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            uniform mat4 u_MVP;
            void main() {
                gl_Position = u_MVP * a_Position;
                v_TexCoord = a_TexCoord;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform sampler2D u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
    }

    fun createOnGlThread(context: Context) {
        val bitmap = createCardBitmap()
        loadTexture(bitmap)
        bitmap.recycle()

        val vsh = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fsh = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vsh)
        GLES20.glAttachShader(program, fsh)
        GLES20.glLinkProgram(program)
        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        texCoordAttrib = GLES20.glGetAttribLocation(program, "a_TexCoord")
        mvpUniform = GLES20.glGetUniformLocation(program, "u_MVP")
        textureUniform = GLES20.glGetUniformLocation(program, "u_Texture")

        val w = CARD_WIDTH_M / 2f
        val h = CARD_HEIGHT_M / 2f
        val vertices = floatArrayOf(
            -w, -h, 0f,  0f, 1f,
             w, -h, 0f,  1f, 1f,
             w,  h, 0f,  1f, 0f,
            -w,  h, 0f,  0f, 0f
        )
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices).apply { position(0) }
    }

    private fun createCardBitmap(): Bitmap {
        val width = 640
        val height = 384
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val corner = 24f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Campus palette: Zinc 800 #27272a, Emerald 600 #059669, Amber 500 #f59e0b, Zinc 700 #3f3f46

        // Shadow
        val shadowPaint = Paint().apply {
            color = Color.argb(70, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(5f, 6f, width + 5f, height + 6f, corner + 3f, corner + 3f, shadowPaint)

        // Main card: Zinc 800 (dark surface)
        val bgPaint = Paint().apply {
            color = Color.argb(255, 0x27, 0x27, 0x2a)
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, corner, corner, bgPaint)

        // Top header bar: Emerald 600 — quest title only
        val headerHeight = 56f
        val emeraldPaint = Paint().apply {
            color = Color.argb(255, 0x05, 0x96, 0x69)
            isAntiAlias = true
        }
        canvas.drawRoundRect(0f, 0f, width.toFloat(), headerHeight + corner, corner, corner, emeraldPaint)
        canvas.drawRect(0f, headerHeight, width.toFloat(), headerHeight + corner, emeraldPaint)

        val centerX = width / 2f
        val titleToDraw = title.ifBlank { "Quest" }
        val titlePaint = Paint().apply {
            color = Color.argb(255, 0xff, 0xff, 0xff)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(titleToDraw, centerX, headerHeight - 14f, titlePaint)

        // Thin amber stripe under header
        val amberPaint = Paint().apply {
            color = Color.argb(255, 0xf5, 0x9e, 0x0b)
            isAntiAlias = true
        }
        canvas.drawRect(0f, headerHeight + corner, width.toFloat(), headerHeight + corner + 4f, amberPaint)

        // Bottom area: black box — question text, then Correct!/Wrong! after answer
        val contentTop = headerHeight + corner + 4f
        val contentRect = RectF(0f, contentTop, width.toFloat(), height.toFloat())
        val contentBg = Paint().apply {
            color = Color.argb(255, 0x27, 0x27, 0x2a)
            isAntiAlias = true
        }
        canvas.drawRect(contentRect, contentBg)

        val padding = 24f
        val contentWidth = width - padding * 2
        val contentBottom = height - padding

        val questionPaint = Paint().apply {
            color = Color.argb(255, 0xe4, 0xe4, 0xe7) // Zinc 200
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val resultPaint = Paint().apply {
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        if (showJoinButtonRef?.get() == true) {
            choiceBoundsRef?.set(emptyList<FloatArray>())
            val btnW = contentWidth * 0.7f
            val btnH = 56f
            val left = (width - btnW) / 2f
            val top = (contentTop + contentBottom - btnH) / 2f
            val right = left + btnW
            val bottom = top + btnH
            val btnPaint = Paint().apply {
                color = Color.argb(255, 0x05, 0x96, 0x69) // Emerald 600
                isAntiAlias = true
            }
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, btnPaint)
            val lblPaint = Paint().apply {
                color = Color.argb(255, 0xff, 0xff, 0xff)
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Join quest", width / 2f, top + btnH / 2f + 10f, lblPaint)
            joinButtonBoundsRef?.set(floatArrayOf(left / width, right / width, top / height, bottom / height))
        } else if (stageOutcome != null) {
            choiceBoundsRef?.set(emptyList<FloatArray>())
            joinButtonBoundsRef?.set(null)
            drawStageOutcome(canvas, width, contentTop, contentBottom, padding, stageOutcome!!)
        } else if (scoreCorrect != null && scoreTotal != null) {
            choiceBoundsRef?.set(emptyList<FloatArray>())
            joinButtonBoundsRef?.set(null)
            val centerY = (contentTop + contentBottom) / 2f
            resultPaint.color = Color.argb(255, 0xe4, 0xe4, 0xe7)
            resultPaint.textSize = 28f
            resultPaint.typeface = Typeface.DEFAULT
            canvas.drawText("Score", width / 2f, centerY - 24f, resultPaint)
            resultPaint.color = Color.argb(255, 0xf5, 0x9e, 0x0b)
            resultPaint.textSize = 42f
            resultPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("$scoreCorrect / $scoreTotal", width / 2f, centerY + 16f, resultPaint)
        } else if (answerResult != null) {
            choiceBoundsRef?.set(emptyList<FloatArray>())
            joinButtonBoundsRef?.set(null)
            val centerY = (contentTop + contentBottom) / 2f
            val iconSize = 48f
            val iconCenterX = width / 2f
            val iconY = centerY - 28f
            if (answerResult == true) {
                resultPaint.color = Color.argb(255, 0x34, 0xd3, 0x99)
                resultPaint.textSize = 32f
                canvas.drawText(answerCorrectLabel, width / 2f, centerY + 32f, resultPaint)
                drawCheckIcon(canvas, iconCenterX, iconY, iconSize, Color.argb(255, 0x34, 0xd3, 0x99))
            } else {
                resultPaint.color = Color.argb(255, 0xf8, 0x71, 0x71)
                resultPaint.textSize = 32f
                canvas.drawText("Wrong", width / 2f, centerY + 32f, resultPaint)
                drawWrongIcon(canvas, iconCenterX, iconY, iconSize, Color.argb(255, 0xf8, 0x71, 0x71))
            }
        } else {
            joinButtonBoundsRef?.set(null)
            val hasQuestion = !question.isNullOrBlank() && !choices.isNullOrEmpty()
            if (!hasQuestion) {
                choiceBoundsRef?.set(emptyList<FloatArray>())
            } else {
            val q = question!!.trim()
            val lines = wrapText(questionPaint, q, contentWidth)
            val lineHeight = questionPaint.textSize * 1.25f
            var currentY = contentTop + lineHeight + 8f
            lines.forEachIndexed { _, line ->
                canvas.drawText(line, width / 2f, currentY, questionPaint)
                currentY += lineHeight
            }
            val choiceList = choices.orEmpty()
            val bounds = mutableListOf<FloatArray>()
            if (choiceList.size >= 2) {
                currentY += 12f
                val sel = selectedChoiceIndex?.takeIf { it in choiceList.indices }
                val choicePaint = Paint().apply {
                    color = Color.argb(255, 0xff, 0xff, 0xff)
                    textSize = 22f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val choiceHeight = 54f
                val gap = 8f
                val palette = intArrayOf(
                    Color.argb(255, 0xf5, 0x9e, 0x0b),
                    Color.argb(255, 0x3b, 0x82, 0xf6),
                    Color.argb(255, 0x22, 0xc5, 0x55),
                    Color.argb(255, 0xef, 0x44, 0x44),
                    Color.argb(255, 0x8b, 0x5c, 0xf6),
                    Color.argb(255, 0xec, 0x48, 0x9a)
                )
                choiceList.forEachIndexed { index, text ->
                    val yTop = currentY
                    val yBottom = currentY + choiceHeight
                    val bgColor = if (sel == index) Color.argb(220, 0x05, 0x96, 0x69) else palette[index % palette.size]
                    Paint().apply {
                        color = bgColor
                        isAntiAlias = true
                    }.let { canvas.drawRoundRect(padding, yTop, width - padding, yBottom, 10f, 10f, it) }
                    val wrapped = wrapText(choicePaint, text, contentWidth - 16f)
                    val lineH = choicePaint.textSize * 1.2f
                    val startY = yTop + (choiceHeight - wrapped.size * lineH) / 2f + lineH * 0.6f
                    wrapped.forEachIndexed { i, line ->
                        canvas.drawText(line, width / 2f, startY + i * lineH, choicePaint)
                    }
                    bounds.add(floatArrayOf(0f, 1f, yTop / height, yBottom / height))
                    currentY += choiceHeight + gap
                }
                choiceBoundsRef?.set(bounds)
            } else {
                choiceBoundsRef?.set(emptyList<FloatArray>())
            }
            }
        }

        // Border around full card (Zinc 700)
        val borderPaint = Paint().apply {
            color = Color.argb(255, 0x3f, 0x3f, 0x46)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(2f, 2f, width - 2f, height - 2f, corner - 2f, corner - 2f, borderPaint)

        return bitmap
    }

    private fun wrapText(paint: Paint, text: String, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(test) <= maxWidth) current = test
            else {
                if (current.isNotEmpty()) lines.add(current)
                if (paint.measureText(word) <= maxWidth) current = word
                else {
                    lines.addAll(breakWord(paint, word, maxWidth))
                    current = ""
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    private fun breakWord(paint: Paint, word: String, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            while (end > start && paint.measureText(word.substring(start, end)) > maxWidth) end--
            if (end == start) end = (start + 1).coerceAtMost(word.length)
            result.add(word.substring(start, end))
            start = end
        }
        return result
    }

    private fun drawCheckIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val p = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.15f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        val left = cx - size / 2f
        val top = cy - size / 2f
        path.moveTo(left + size * 0.2f, cy)
        path.lineTo(left + size * 0.45f, cy + size * 0.35f)
        path.lineTo(left + size * 0.85f, cy - size * 0.35f)
        canvas.drawPath(path, p)
    }

    private fun drawWrongIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val p = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.15f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        val h = size / 2f
        val left = cx - h
        val top = cy - h
        canvas.drawLine(left, top, left + size, top + size, p)
        canvas.drawLine(left + size, top, left, top + size, p)
    }

    private fun drawStageOutcome(
        canvas: Canvas,
        width: Int,
        contentTop: Float,
        contentBottom: Float,
        padding: Float,
        outcome: StageOutcomeDisplay
    ) {
        val centerX = width / 2f
        var y = contentTop + 32f
        val titlePaint = Paint().apply {
            color = Color.argb(255, 0xe4, 0xe4, 0xe7)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.argb(255, 0xa1, 0xa1, 0xaa)
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        when (outcome) {
            is StageOutcomeDisplay.ProceedNextLocation -> {
                titlePaint.color = Color.argb(255, 0x34, 0xd3, 0x99)
                canvas.drawText("You advanced!", centerX, y, titlePaint)
                y += 36f
                linePaint.textSize = 24f
                canvas.drawText("Next: ${outcome.location}", centerX, y, linePaint)
            }
            is StageOutcomeDisplay.ProceedUnlockAt -> {
                titlePaint.color = Color.argb(255, 0x34, 0xd3, 0x99)
                canvas.drawText("You advanced!", centerX, y, titlePaint)
                y += 36f
                canvas.drawText("Next stage opens at", centerX, y, linePaint)
                y += 28f
                linePaint.textSize = 24f
                canvas.drawText(outcome.opensAt, centerX, y, linePaint)
            }
            is StageOutcomeDisplay.Eliminated -> {
                titlePaint.color = Color.argb(255, 0xf8, 0x71, 0x71)
                canvas.drawText("You were eliminated", centerX, y, titlePaint)
                if (!outcome.message.isNullOrBlank()) {
                    y += 36f
                    val wrappedLines = wrapText(linePaint, outcome.message, (width - padding * 2))
                    wrappedLines.forEach { line ->
                        canvas.drawText(line, centerX, y, linePaint)
                        y += 26f
                    }
                }
            }
            is StageOutcomeDisplay.AwaitingRanking -> {
                titlePaint.color = Color.argb(255, 0xf5, 0x9e, 0x0b)
                canvas.drawText("Waiting for results", centerX, y, titlePaint)
                y += 36f
                val wrappedLines = wrapText(linePaint, "You'll be notified when results are ready.", (width - padding * 2))
                wrappedLines.forEach { line ->
                    canvas.drawText(line, centerX, y, linePaint)
                    y += 26f
                }
            }
            is StageOutcomeDisplay.Rejected -> {
                titlePaint.color = Color.argb(255, 0xf8, 0x71, 0x71)
                canvas.drawText("Cannot join", centerX, y, titlePaint)
                y += 36f
                val wrappedLines = wrapText(linePaint, outcome.reason, (width - padding * 2))
                wrappedLines.forEach { line ->
                    canvas.drawText(line, centerX, y, linePaint)
                    y += 26f
                }
            }
            is StageOutcomeDisplay.Winner -> {
                titlePaint.color = Color.argb(255, 0xf5, 0x9e, 0x0b)
                titlePaint.textSize = 34f
                canvas.drawText("Winner!", centerX, y, titlePaint)
                y += 38f
                linePaint.textSize = 24f
                linePaint.color = Color.argb(255, 0x34, 0xd3, 0x99)
                canvas.drawText("+${outcome.pointsEarned} points", centerX, y, linePaint)
                y += 32f
                linePaint.color = Color.argb(255, 0xa1, 0xa1, 0xaa)
                if (outcome.levelUp && outcome.newLevel != null) {
                    canvas.drawText("Level up! Now level ${outcome.newLevel}", centerX, y, linePaint)
                    y += 28f
                }
                outcome.achievements.firstOrNull()?.let { canvas.drawText(it, centerX, y, linePaint); y += 28f }
                outcome.achievements.drop(1).forEach { canvas.drawText(it, centerX, y, linePaint); y += 28f }
                outcome.customPrize?.takeIf { it.isNotBlank() }?.let { canvas.drawText("Prize: $it", centerX, y, linePaint) }
            }
        }
    }

    fun updateSubtitle(newSubtitle: String) {
        subtitle = newSubtitle
    }

    fun updateAnswerCorrectLabel(label: String) {
        answerCorrectLabel = label
    }

    /** Call on GL thread when content changes. Regenerates card texture. */
    fun updateContent(
        newQuestion: String?,
        newChoices: List<String>?,
        newSelectedChoiceIndex: Int?,
        newAnswerResult: Boolean?,
        newScoreCorrect: Int? = null,
        newScoreTotal: Int? = null,
        newStageOutcome: StageOutcomeDisplay? = null
    ) {
        question = newQuestion
        choices = newChoices
        selectedChoiceIndex = newSelectedChoiceIndex
        answerResult = newAnswerResult
        scoreCorrect = newScoreCorrect
        scoreTotal = newScoreTotal
        stageOutcome = newStageOutcome
        val bitmap = createCardBitmap()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap)
        bitmap.recycle()
    }

    private fun loadTexture(bitmap: Bitmap) {
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    }

    /** If modelMatrixOut is non-null, the card's model matrix (16 floats) is copied into it for hit-testing. */
    fun draw(
        anchorPose: Pose,
        camera: Camera,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        modelMatrixOut: FloatArray? = null
    ) {
        val anchorT = FloatArray(3)
        anchorPose.getTranslation(anchorT, 0)
        val cameraT = FloatArray(3)
        camera.pose.getTranslation(cameraT, 0)
        var fx = cameraT[0] - anchorT[0]
        var fy = cameraT[1] - anchorT[1]
        var fz = cameraT[2] - anchorT[2]
        val len = Math.sqrt((fx * fx + fy * fy + fz * fz).toDouble()).toFloat()
        if (len < 1e-6f) return
        fx /= len
        fy /= len
        fz /= len
        var ux = 0f
        var uy = 1f
        var uz = 0f
        var rx = uy * fz - uz * fy
        var ry = uz * fx - ux * fz
        var rz = ux * fy - uy * fx
        val rLen = Math.sqrt((rx * rx + ry * ry + rz * rz).toDouble()).toFloat()
        if (rLen > 1e-6f) {
            rx /= rLen
            ry /= rLen
            rz /= rLen
            ux = fy * rz - fz * ry
            uy = fz * rx - fx * rz
            uz = fx * ry - fy * rx
        }
        val cardY = anchorT[1] + CARD_OFFSET_Y_M
        modelMatrix[0] = rx
        modelMatrix[1] = ux
        modelMatrix[2] = -fx
        modelMatrix[3] = 0f
        modelMatrix[4] = ry
        modelMatrix[5] = uy
        modelMatrix[6] = -fy
        modelMatrix[7] = 0f
        modelMatrix[8] = rz
        modelMatrix[9] = uz
        modelMatrix[10] = -fz
        modelMatrix[11] = 0f
        modelMatrix[12] = anchorT[0]
        modelMatrix[13] = cardY
        modelMatrix[14] = anchorT[2]
        modelMatrix[15] = 1f

        Matrix.multiplyMM(modelViewProjection, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewProjection, 0)
        modelMatrixOut?.let { System.arraycopy(modelMatrix, 0, it, 0, 16) }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform1i(textureUniform, 0)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glEnableVertexAttribArray(texCoordAttrib)
        val stride = 5 * 4
        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        vertexBuffer?.position(3)
        GLES20.glVertexAttribPointer(texCoordAttrib, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        indexBuffer?.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        vertexBuffer?.position(0)
        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDisableVertexAttribArray(texCoordAttrib)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
}
