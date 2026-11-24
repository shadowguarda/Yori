package eu.kanade.tachiyomi.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

/**
 * Lightweight, blur-free "liquid glass" surface: translucent tint, soft top sheen and a specular hairline rim.
 *
 * Same look as the capsule drawn by [GlassBottomNavigationView] but as a plain [Drawable], so any pill, chip or
 * sheet can reuse it for free (no RenderNode / RenderEffect, smooth on budget devices).
 *
 * @param cornerRadiusPx corner radius in pixels, or a negative value for a full pill (height / 2)
 */
class GlassSurfaceDrawable(
    surfaceColor: Int,
    private val density: Float,
    private val cornerRadiusPx: Float = -1f,
) : Drawable() {

    private val isLight = ColorUtils.calculateLuminance(surfaceColor) > 0.5
    private val rect = RectF()
    private var drawAlpha = 255

    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(surfaceColor, if (isLight) 215 else 205)
    }
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = HAIRLINE_DP * density
    }

    private fun radiusFor(r: RectF): Float = if (cornerRadiusPx < 0f) r.height() / 2f else cornerRadiusPx

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rect.set(bounds)
        sheenPaint.shader = LinearGradient(
            0f,
            rect.top,
            0f,
            rect.bottom,
            intArrayOf(Color.argb(if (isLight) 110 else 42, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 0.55f),
            Shader.TileMode.CLAMP,
        )
        rimPaint.shader = LinearGradient(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            intArrayOf(
                Color.argb(if (isLight) 220 else 140, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Color.argb(if (isLight) 160 else 90, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.35f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        if (rect.isEmpty) return
        val radius = radiusFor(rect)
        tintPaint.alpha = (tintPaint.alpha * drawAlpha) / 255
        canvas.drawRoundRect(rect, radius, radius, tintPaint)
        canvas.drawRoundRect(rect, radius, radius, sheenPaint)
        val half = rimPaint.strokeWidth / 2f
        val rimRect = RectF(rect).apply { inset(half, half) }
        val rimRadius = if (cornerRadiusPx < 0f) rimRect.height() / 2f else (cornerRadiusPx - half).coerceAtLeast(0f)
        canvas.drawRoundRect(rimRect, rimRadius, rimRadius, rimPaint)
    }

    override fun setAlpha(alpha: Int) {
        drawAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        tintPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private const val HAIRLINE_DP = 1f

        /** Builds a glass drawable tinted with the theme's `colorSurface`. Pass a negative radius for a full pill. */
        fun create(context: Context, cornerRadiusDp: Float = -1f): GlassSurfaceDrawable {
            val density = context.resources.displayMetrics.density
            val surface = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurface,
                Color.WHITE,
            )
            val radius = if (cornerRadiusDp < 0f) -1f else cornerRadiusDp * density
            return GlassSurfaceDrawable(surface, density, radius)
        }
    }
}

/**
 * Gives this view the shared liquid glass look (tinted capsule, sheen, rim) and clips it to the same shape.
 * Pass a negative [cornerRadiusDp] for a full pill.
 */
fun View.applyLiquidGlassSurface(cornerRadiusDp: Float = -1f) {
    background = GlassSurfaceDrawable.create(context, cornerRadiusDp)
    val radiusPx = cornerRadiusDp * resources.displayMetrics.density
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: android.graphics.Outline) {
            val r = if (cornerRadiusDp < 0f) view.height / 2f else radiusPx
            outline.setRoundRect(0, 0, view.width, view.height, r)
        }
    }
    clipToOutline = true
}
