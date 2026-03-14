package com.campusgomobile.ar

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Draws a simple colored cube (~0.3 m size) at the given pose. */
class CubeRenderer {

    private var program = 0
    private var positionAttrib = 0
    private var colorUniform = 0
    private var mvpUniform = 0
    private val modelViewProjection = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: java.nio.ShortBuffer? = null

    fun createOnGlThread() {
        val vsh = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fsh = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vsh)
        GLES20.glAttachShader(program, fsh)
        GLES20.glLinkProgram(program)
        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
        mvpUniform = GLES20.glGetUniformLocation(program, "u_MVP")

        // Cube ~0.15 m half-extent (0.3 m size) so it's clearly visible on the QR
        val s = 0.15f
        val vertices = floatArrayOf(
            -s, -s, -s,  s, -s, -s,  s, s, -s, -s, s, -s,
            -s, -s,  s,  s, -s,  s,  s, s,  s, -s, s,  s,
            -s, -s, -s, -s, -s,  s, -s, s,  s, -s, s, -s,
             s, -s, -s,  s, -s,  s,  s, s,  s,  s, s, -s,
            -s, -s, -s,  s, -s, -s,  s, -s, s, -s, -s, s,
            -s,  s, -s,  s,  s, -s,  s,  s, s, -s,  s, s
        )
        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,
            4, 6, 5, 4, 7, 6,
            8, 9, 10, 8, 10, 11,
            12, 14, 13, 12, 15, 14,
            16, 18, 17, 16, 19, 18,
            20, 21, 22, 20, 22, 23
        )
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices).apply { position(0) }
    }

    fun draw(pose: Pose, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        poseToMatrix(pose, modelMatrix)
        Matrix.multiplyMM(modelViewProjection, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewProjection, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, modelViewProjection, 0)
        GLES20.glUniform4f(colorUniform, 0f, 0.8f, 0.4f, 1f)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        GLES20.glDisableVertexAttribArray(positionAttrib)
    }

    private fun poseToMatrix(pose: Pose, out: FloatArray) {
        val t = FloatArray(3)
        val q = FloatArray(4)
        pose.getTranslation(t, 0)
        pose.getRotationQuaternion(q, 0)
        val qx = q[0]; val qy = q[1]; val qz = q[2]; val qw = q[3]
        out[0] = 1f - 2f * (qy * qy + qz * qz)
        out[1] = 2f * (qx * qy - qz * qw)
        out[2] = 2f * (qx * qz + qy * qw)
        out[3] = 0f
        out[4] = 2f * (qx * qy + qz * qw)
        out[5] = 1f - 2f * (qx * qx + qz * qz)
        out[6] = 2f * (qy * qz - qx * qw)
        out[7] = 0f
        out[8] = 2f * (qx * qz - qy * qw)
        out[9] = 2f * (qy * qz + qx * qw)
        out[10] = 1f - 2f * (qx * qx + qy * qy)
        out[11] = 0f
        out[12] = t[0]
        out[13] = t[1]
        out[14] = t[2]
        out[15] = 1f
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MVP;
            attribute vec4 a_Position;
            void main() { gl_Position = u_MVP * a_Position; }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() { gl_FragColor = u_Color; }
        """
    }
}
