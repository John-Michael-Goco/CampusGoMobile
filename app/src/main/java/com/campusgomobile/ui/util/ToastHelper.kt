package com.campusgomobile.ui.util

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.campusgomobile.R

/**
 * Shows a styled toast above the bottom nav / FAB so it stays visible.
 * Uses a rounded card-like design with theme colors.
 */
fun showStyledToast(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
    val toast = Toast(context)
    toast.duration = duration

    val dp = context.resources.displayMetrics.density
    val cornerRadiusPx = (20 * dp).toInt()
    val paddingH = (24 * dp).toInt()
    val paddingV = (16 * dp).toInt()
    val textSizeSp = 15f

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(paddingH, paddingV, paddingH, paddingV)
        background = GradientDrawable().apply {
            setColor(context.getColor(R.color.toast_background))
            cornerRadius = cornerRadiusPx.toFloat()
        }
        elevation = 12f * dp
    }

    val textView = TextView(context).apply {
        text = message
        setTextColor(context.getColor(R.color.toast_text))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }
    container.addView(textView)

    toast.view = container
    toast.setGravity(
        Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        0,
        (80 * dp).toInt() // Below status bar, well above bottom nav and FAB
    )
    toast.show()
}
