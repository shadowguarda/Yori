package eu.kanade.tachiyomi.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors

/**
 * A floating "liquid glass" bottom navigation bar.
 *
 * The view keeps its normal [BottomNavigationView] bounds (so insets, hide-on-scroll translation, snackbar anchoring
 * and every `bottomNav.height` / `bottomNav.y` calculation in the app keep working) but draws a rounded, floating
 * glass capsule inside its padding box instead of a flat bar:
 *
 * - API 31+: the content behind the bar is captured, blurred and saturation-boosted using a [RenderNode] with a
 *   [RenderEffect], then tinted and lit with a specular rim.
 * - Older devices: a more opaque tinted capsule with the same rim and highlights (no blur).
 *
 * The padding box of this view is the capsule. Horizontal/top padding come from the style and the bottom padding is
 * set by `MainActivity` (system inset + [R.dimen.glass_nav_margin_bottom]).
 *
 * The approach of recording the backdrop into a layer, blurring it, then tinting it and adding a lens-like rim is
 * inspired by Kyant0's AndroidLiquidGlass (Apache-2.0, https://github.com/Kyant0/AndroidLiquidGlass), adapted here
 * to the View system since that library is Compose only.
 */
class GlassBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle,
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val pillRect = RectF()
    private val pillPath = Path()

    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val topSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.25f * density
    }

    // Only ever created on API 31+, kept in its own class so older devices never load RenderNode/RenderEffect
    private var backdrop: Backdrop? = null
    private var selfInvalidate = false

    private val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private val drawListener = ViewTreeObserver.OnDrawListener {
        // Something in the window was drawn. If it wasn't us, the content behind the bar may have changed
        // (animation, image load, ...) so redraw the bar once to refresh the backdrop.
        if (selfInvalidate) {
            selfInvalidate = false
        } else if (blurSupported && isShown && alpha > 0f) {
            selfInvalidate = true
            postInvalidateOnAnimation()
        }
    }

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        if (blurSupported && isShown && alpha > 0f) invalidate()
    }

    init {
        // Material would otherwise fill the whole view with a flat bar background
        setBackgroundColor(Color.TRANSPARENT)
        elevation = 0f
        outlineProvider = ViewOutlineProvider.NONE
        clipToPadding = false
        clipChildren = false
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnDrawListener(drawListener)
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnDrawListener(drawListener)
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        updatePill()
        if (pillRect.width() > 0f && pillRect.height() > 0f) {
            drawGlass(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun updatePill() {
        pillRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat(),
        )
        val radius = pillRect.height() / 2f
        pillPath.rewind()
        pillPath.addRoundRect(pillRect, radius, radius, Path.Direction.CW)
    }

    private fun drawGlass(canvas: Canvas) {
        val surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
        val isLight = ColorUtils.calculateLuminance(surface) > 0.5
        val blurred = blurSupported && canvas.isHardwareAccelerated

        canvas.save()
        canvas.clipPath(pillPath)

        if (blurred && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val layer = backdrop ?: Backdrop(22f * density).also { backdrop = it }
            layer.draw(this, canvas)
        }

        // Body tint: light and see-through over a blurred backdrop, much denser when there's nothing to blur
        tintPaint.color = ColorUtils.setAlphaComponent(
            surface,
            if (blurred) (if (isLight) 120 else 110) else 235,
        )
        canvas.drawPath(pillPath, tintPaint)

        // Soft sheen over the top half, like light catching a curved glass surface
        topSheenPaint.shader = LinearGradient(
            0f,
            pillRect.top,
            0f,
            pillRect.bottom,
            intArrayOf(
                Color.argb(if (isLight) 120 else 46, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.55f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(pillPath, topSheenPaint)
        canvas.restore()

        // Specular rim: bright on the top-left and bottom-right edges, clear in between
        rimPaint.shader = LinearGradient(
            pillRect.left,
            pillRect.top,
            pillRect.right,
            pillRect.bottom,
            intArrayOf(
                Color.argb(if (isLight) 230 else 150, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Color.argb(if (isLight) 170 else 95, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.35f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
        )
        val half = rimPaint.strokeWidth / 2f
        val rimRect = RectF(pillRect).apply { inset(half, half) }
        val rimRadius = rimRect.height() / 2f
        canvas.drawRoundRect(rimRect, rimRadius, rimRadius, rimPaint)
    }

    /**
     * Records everything drawn behind [view] into a [RenderNode] carrying a blur + saturation [RenderEffect] and
     * draws that node into the (already capsule-clipped) canvas.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private class Backdrop(blurRadiusPx: Float) {
        private val node = RenderNode("glass_nav_backdrop").apply {
            val blur = RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.MIRROR)
            val saturation = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.6f) })
            setRenderEffect(RenderEffect.createColorFilterEffect(saturation, blur))
        }

        fun draw(view: View, canvas: Canvas) {
            val container = view.parent as? ViewGroup ?: return
            val width = view.width
            val height = view.height

            node.setPosition(0, 0, width, height)
            val recordingCanvas = node.beginRecording(width, height)
            try {
                val myX = view.left + view.translationX
                val myY = view.top + view.translationY
                val myIndex = container.indexOfChild(view)
                for (i in 0 until myIndex) {
                    val sibling = container.getChildAt(i)
                    if (sibling.visibility != View.VISIBLE || sibling.alpha <= 0f) continue
                    val dx = sibling.left + sibling.translationX - myX
                    val dy = sibling.top + sibling.translationY - myY
                    // Skip siblings that don't overlap the bar at all (e.g. the app bar at the top)
                    if (dx > width || dy > height || dx + sibling.width < 0 || dy + sibling.height < 0) continue
                    recordingCanvas.save()
                    recordingCanvas.translate(dx, dy)
                    recordingCanvas.clipRect(0, 0, sibling.width, sibling.height)
                    sibling.draw(recordingCanvas)
                    recordingCanvas.restore()
                }
            } finally {
                node.endRecording()
            }
            canvas.drawRenderNode(node)
        }
    }
}
