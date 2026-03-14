package com.campusgomobile.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders a 3D star as "quest taken" indicator, with a label billboard above showing quest name and "Stage N".
 */
class QuestTakenRenderer(
    private val questTitle: String,
    private val stageLabel: String
) {

    private var solidProgram = 0
    private var positionAttrib = 0
    private var colorUniform = 0
    private var solidMvpUniform = 0
    private var starVertexBuffer: FloatBuffer? = null
    private var starIndexBuffer: ShortBuffer? = null

    private var labelProgram = 0
    private var labelPositionAttrib = 0
    private var labelTexCoordAttrib = 0
    private var labelTextureUniform = 0
    private var labelMvpUniform = 0
    private var labelTextureId = IntArray(1)
    private var labelVertexBuffer: FloatBuffer? = null
    private var labelIndexBuffer: ShortBuffer? = null

    private val modelMatrix = FloatArray(16)
    private val modelViewProjection = FloatArray(16)

    companion object {
        private const val STAR_SIZE_M = 0.065f
        private const val OFFSET_Y_M = 0.14f
        private const val LABEL_OFFSET_ABOVE_STAR_M = 0.12f
        private const val LABEL_WIDTH_M = 0.22f
        private const val LABEL_HEIGHT_M = 0.08f

        private const val SOLID_VERTEX_SHADER = """
            uniform mat4 u_MVP;
            attribute vec4 a_Position;
            void main() { gl_Position = u_MVP * a_Position; }
        """
        private const val SOLID_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() { gl_FragColor = u_Color; }
        """
        private const val LABEL_VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            uniform mat4 u_MVP;
            void main() {
                gl_Position = u_MVP * a_Position;
                v_TexCoord = a_TexCoord;
            }
        """
        private const val LABEL_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform sampler2D u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
    }

    fun createOnGlThread(context: Context) {
        createStarGeometry()
        createLabelTexture(context)
        createLabelGeometry()
    }

    private fun createStarGeometry() {
        val vsh = loadShader(GLES20.GL_VERTEX_SHADER, SOLID_VERTEX_SHADER)
        val fsh = loadShader(GLES20.GL_FRAGMENT_SHADER, SOLID_FRAGMENT_SHADER)
        solidProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(solidProgram, vsh)
        GLES20.glAttachShader(solidProgram, fsh)
        GLES20.glLinkProgram(solidProgram)
        positionAttrib = GLES20.glGetAttribLocation(solidProgram, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(solidProgram, "u_Color")
        solidMvpUniform = GLES20.glGetUniformLocation(solidProgram, "u_MVP")

        val s = STAR_SIZE_M
        val deg = Math.PI / 180.0
        val outerR = s * 0.9
        val innerR = s * 0.35
        val top = floatArrayOf(0f, s, 0f)
        val bottom = floatArrayOf(0f, -s, 0f)
        val outer = (0 until 5).map { i ->
            val a = 90.0 + i * 72.0
            floatArrayOf(
                (outerR * cos(a * deg)).toFloat(),
                0f,
                (outerR * sin(a * deg)).toFloat()
            )
        }.flatMap { it.toList() }.toFloatArray()
        val inner = (0 until 5).map { i ->
            val a = 90.0 + 36.0 + i * 72.0
            floatArrayOf(
                (innerR * cos(a * deg)).toFloat(),
                0f,
                (innerR * sin(a * deg)).toFloat()
            )
        }.flatMap { it.toList() }.toFloatArray()
        val vertices = floatArrayOf(
            top[0], top[1], top[2],
            bottom[0], bottom[1], bottom[2],
            outer[0], outer[1], outer[2],
            outer[3], outer[4], outer[5],
            outer[6], outer[7], outer[8],
            outer[9], outer[10], outer[11],
            outer[12], outer[13], outer[14],
            inner[0], inner[1], inner[2],
            inner[3], inner[4], inner[5],
            inner[6], inner[7], inner[8],
            inner[9], inner[10], inner[11],
            inner[12], inner[13], inner[14]
        )
        // Top star: 0, 2,7, 0,7,3, 0,3,8, 0,8,4, 0,4,9, 0,9,5, 0,5,10, 0,10,6, 0,6,11, 0,11,2
        // Bottom: 1, 7,2, 1,3,7, 1,8,3, 1,4,8, 1,9,4, 1,5,9, 1,10,5, 1,6,10, 1,11,6, 1,2,11
        // Sides: 2,7,8,3 / 3,8,9,4 / 4,9,10,5 / 5,10,11,6 / 6,11,7,2
        val indices = shortArrayOf(
            0, 2, 7,  0, 7, 3,  0, 3, 8,  0, 8, 4,  0, 4, 9,  0, 9, 5,  0, 5, 10,  0, 10, 6,  0, 6, 11,  0, 11, 2,
            1, 7, 2,  1, 3, 7,  1, 8, 3,  1, 4, 8,  1, 9, 4,  1, 5, 9,  1, 10, 5,  1, 6, 10,  1, 11, 6,  1, 2, 11,
            2, 7, 8,  2, 8, 3,  3, 8, 9,  3, 9, 4,  4, 9, 10,  4, 10, 5,  5, 10, 11,  5, 11, 6,  6, 11, 7,  6, 7, 2
        )
        starVertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        starIndexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices).apply { position(0) }
    }

    private fun createLabelTexture(context: Context) {
        val width = 256
        val height = 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply {
            color = Color.argb(220, 30, 32, 36)
            isAntiAlias = true
        }
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 12f, 12f, bgPaint)
        val borderPaint = Paint().apply {
            color = Color.argb(255, 52, 168, 83)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(1f, 1f, width - 1f, height - 1f, 11f, 11f, borderPaint)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val stagePaint = Paint().apply {
            color = Color.argb(255, 180, 220, 120)
            textSize = 22f
            isAntiAlias = true
        }
        val titleToDraw = questTitle.ifBlank { "Quest" }
        canvas.drawText(titleToDraw, 16f, 52f, titlePaint)
        if (stageLabel.isNotBlank()) {
            canvas.drawText(stageLabel, 16f, 88f, stagePaint)
        }
        GLES20.glGenTextures(1, labelTextureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, labelTextureId[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        val vsh = loadShader(GLES20.GL_VERTEX_SHADER, LABEL_VERTEX_SHADER)
        val fsh = loadShader(GLES20.GL_FRAGMENT_SHADER, LABEL_FRAGMENT_SHADER)
        labelProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(labelProgram, vsh)
        GLES20.glAttachShader(labelProgram, fsh)
        GLES20.glLinkProgram(labelProgram)
        labelPositionAttrib = GLES20.glGetAttribLocation(labelProgram, "a_Position")
        labelTexCoordAttrib = GLES20.glGetAttribLocation(labelProgram, "a_TexCoord")
        labelTextureUniform = GLES20.glGetUniformLocation(labelProgram, "u_Texture")
        labelMvpUniform = GLES20.glGetUniformLocation(labelProgram, "u_MVP")
    }

    private fun createLabelGeometry() {
        val w = LABEL_WIDTH_M / 2f
        val h = LABEL_HEIGHT_M / 2f
        val vertices = floatArrayOf(
            -w, -h, 0f,  0f, 1f,
             w, -h, 0f,  1f, 1f,
             w,  h, 0f,  1f, 0f,
            -w,  h, 0f,  0f, 0f
        )
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        labelVertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        labelIndexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices).apply { position(0) }
    }

    fun draw(anchorPose: Pose, camera: Camera, viewMatrix: FloatArray, projectionMatrix: FloatArray, showLabel: Boolean = true) {
        val t = FloatArray(3)
        anchorPose.getTranslation(t, 0)
        val starY = t[1] + OFFSET_Y_M
        t[1] = starY

        val spinDeg = (SystemClock.elapsedRealtime() % 4000L) / 4000f * 360f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, t[0], t[1], t[2])
        Matrix.rotateM(modelMatrix, 0, spinDeg, 0f, 1f, 0f)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // Edge highlight: slightly larger darker star behind so edges are more visible
        val edgeMatrix = FloatArray(16)
        Matrix.setIdentityM(edgeMatrix, 0)
        Matrix.translateM(edgeMatrix, 0, t[0], t[1], t[2])
        Matrix.rotateM(edgeMatrix, 0, spinDeg, 0f, 1f, 0f)
        Matrix.scaleM(edgeMatrix, 0, 1.06f, 1.06f, 1.06f)
        Matrix.multiplyMM(modelViewProjection, 0, viewMatrix, 0, edgeMatrix, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewProjection, 0)
        GLES20.glUseProgram(solidProgram)
        GLES20.glUniformMatrix4fv(solidMvpUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform4f(colorUniform, 0.5f, 0.35f, 0.05f, 1f)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        starVertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, starVertexBuffer)
        starIndexBuffer?.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 90, GLES20.GL_UNSIGNED_SHORT, starIndexBuffer)

        // Star glow (scaled, with spin)
        val glowMatrix = FloatArray(16)
        Matrix.setIdentityM(glowMatrix, 0)
        Matrix.translateM(glowMatrix, 0, t[0], t[1], t[2])
        Matrix.rotateM(glowMatrix, 0, spinDeg, 0f, 1f, 0f)
        Matrix.scaleM(glowMatrix, 0, 1.2f, 1.2f, 1.2f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        Matrix.multiplyMM(modelViewProjection, 0, viewMatrix, 0, glowMatrix, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewProjection, 0)
        GLES20.glUseProgram(solidProgram)
        GLES20.glUniformMatrix4fv(solidMvpUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform4f(colorUniform, 0.9f, 0.55f, 0.1f, 0.98f)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        starVertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, starVertexBuffer)
        starIndexBuffer?.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 90, GLES20.GL_UNSIGNED_SHORT, starIndexBuffer)
        GLES20.glDisable(GLES20.GL_BLEND)

        // Main star (with spin)
        Matrix.multiplyMM(modelViewProjection, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewProjection, 0)
        GLES20.glUniformMatrix4fv(solidMvpUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform4f(colorUniform, 1f, 0.82f, 0.28f, 1f)
        starIndexBuffer?.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 90, GLES20.GL_UNSIGNED_SHORT, starIndexBuffer)
        GLES20.glDisableVertexAttribArray(positionAttrib)

        // Label billboard above star (only during first 4 sec)
        if (!showLabel) {
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            return
        }
        val labelY = starY + LABEL_OFFSET_ABOVE_STAR_M
        val cameraT = FloatArray(3)
        camera.pose.getTranslation(cameraT, 0)
        var fx = cameraT[0] - t[0]
        var fy = cameraT[1] - labelY
        var fz = cameraT[2] - t[2]
        val len = sqrt(fx * fx + fy * fy + fz * fz)
        if (len > 1e-6f) {
            fx /= len
            fy /= len
            fz /= len
            var ux = 0f
            var uy = 1f
            var uz = 0f
            var rx = uy * fz - uz * fy
            var ry = uz * fx - ux * fz
            var rz = ux * fy - uy * fx
            val rLen = sqrt(rx * rx + ry * ry + rz * rz)
            if (rLen > 1e-6f) {
                rx /= rLen
                ry /= rLen
                rz /= rLen
                ux = fy * rz - fz * ry
                uy = fz * rx - fx * rz
                uz = fx * ry - fy * rx
            }
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
            modelMatrix[12] = t[0]
            modelMatrix[13] = labelY
            modelMatrix[14] = t[2]
            modelMatrix[15] = 1f
            Matrix.multiplyMM(modelViewProjection, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewProjection, 0)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, labelTextureId[0])
            GLES20.glUseProgram(labelProgram)
            GLES20.glUniformMatrix4fv(labelMvpUniform, 1, false, modelViewProjection, 0)
            GLES20.glUniform1i(labelTextureUniform, 0)
            GLES20.glEnableVertexAttribArray(labelPositionAttrib)
            GLES20.glEnableVertexAttribArray(labelTexCoordAttrib)
            val stride = 5 * 4
            labelVertexBuffer?.position(0)
            GLES20.glVertexAttribPointer(labelPositionAttrib, 3, GLES20.GL_FLOAT, false, stride, labelVertexBuffer)
            labelVertexBuffer?.position(3)
            GLES20.glVertexAttribPointer(labelTexCoordAttrib, 2, GLES20.GL_FLOAT, false, stride, labelVertexBuffer)
            labelIndexBuffer?.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, labelIndexBuffer)
            GLES20.glDisableVertexAttribArray(labelPositionAttrib)
            GLES20.glDisableVertexAttribArray(labelTexCoordAttrib)
            GLES20.glDisable(GLES20.GL_BLEND)
        }

        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
}
