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
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A floating "liquid glass" bottom navigation bar.
 *
 * The view keeps its normal [BottomNavigationView] bounds (so insets, hide-on-scroll translation, snackbar anchoring
 * and every `bottomNav.height` / `bottomNav.y` calculation in the app keep working) but draws a rounded, floating
 * glass capsule inside its padding box instead of a flat bar. The padding box of this view is the capsule: horizontal
 * and top padding come from the style, the bottom padding is set by `MainActivity` (system inset + margin).
 *
 * - **Bar glass** (API 31+): the content behind the bar is captured, blurred and saturation-boosted using a
 *   [RenderNode] with a [RenderEffect], then tinted and lit with a specular rim. Older devices get a denser tinted
 *   capsule without blur.
 * - **Lens**: a glass lens sits behind the selected tab. It moves between tabs on a spring, stretches and squashes
 *   with its speed like a blob of liquid, swells while touched, and can be dragged across the tabs. On API 31+ it
 *   shows a clearer, magnified view of whatever is behind it.
 *
 * The idea of recording the backdrop into a layer, blurring it and adding a lens with a rim is inspired by Kyant0's
 * AndroidLiquidGlass (Apache-2.0, https://github.com/Kyant0/AndroidLiquidGlass), adapted to the View system since
 * that library is Compose only.
 */
class GlassBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle,
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val pillRect = RectF()
    private val pillPath = Path()
    private val lensRect = RectF()
    private val lensPath = Path()

    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.25f * density
    }
    private val lensRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val lensEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }

    // Only ever created on API 31+, kept in its own class so older devices never load RenderNode/RenderEffect
    private var backdrop: Backdrop? = null
    private var selfInvalidate = false

    private val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Lens spring state (x is the lens centre in this view's coordinates)
    private var lensInit = false
    private var lensX = 0f
    private var lensV = 0f
    private var lensTarget = 0f
    private var lensItemWidth = 0f
    private var press = 0f
    private var pressTarget = 0f
    private var lastSelectedId = View.NO_ID
    private var dragging = false
    private var downX = 0f
    private var frameScheduled = false
    private var lastFrameNanos = 0L

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        frameScheduled = false
        val dt = if (lastFrameNanos == 0L) {
            1f / 60f
        } else {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.032f)
        }
        lastFrameNanos = frameTimeNanos
        stepSpring(dt)
        invalidate()
        if (isLensSettled()) {
            lastFrameNanos = 0L
        } else {
            scheduleFrame()
        }
    }

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
        // Material would otherwise fill the whole view with a flat bar background and its own active indicator
        setBackgroundColor(Color.TRANSPARENT)
        elevation = 0f
        outlineProvider = null
        clipToPadding = false
        clipChildren = false
        setWillNotDraw(false)
        isItemActiveIndicatorEnabled = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnDrawListener(drawListener)
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnDrawListener(drawListener)
        viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        frameScheduled = false
        lastFrameNanos = 0L
        super.onDetachedFromWindow()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!dragging) retarget(snap = changed || !lensInit)
    }

    // region Touch: press swell + drag the lens across tabs

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (pillRect.contains(ev.x, ev.y)) {
                    pressTarget = 1f
                    scheduleFrame()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressTarget = 0f
                scheduleFrame()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && abs(ev.x - downX) > touchSlop && pillRect.contains(ev.x, ev.y)) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragging) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val centres = itemViews().map { centreOf(it) }
                if (centres.isNotEmpty()) {
                    lensTarget = ev.x.coerceIn(centres.first(), centres.last())
                    scheduleFrame()
                }
            }
            MotionEvent.ACTION_UP -> {
                dragging = false
                val nearest = itemViews().minByOrNull { abs(centreOf(it) - ev.x) }
                if (nearest != null && nearest.id != selectedItemId) {
                    // Same path as a tap, so listeners and badges behave exactly as before
                    nearest.performClick()
                }
                retarget(snap = false)
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                retarget(snap = false)
            }
        }
        return true
    }

    // endregion

    // region Lens spring

    private fun itemViews(): List<View> {
        val menu = getChildAt(0) as? ViewGroup ?: return emptyList()
        return (0 until menu.childCount).map { menu.getChildAt(it) }.filter { it.visibility == View.VISIBLE }
    }

    private fun centreOf(item: View): Float {
        val menuLeft = (item.parent as? View)?.left ?: 0
        return menuLeft + item.left + item.width / 2f
    }

    private fun retarget(snap: Boolean) {
        val item = findViewById<View>(selectedItemId) ?: return
        if (item.width == 0) return
        lensTarget = centreOf(item)
        lensItemWidth = item.width.toFloat()
        lastSelectedId = selectedItemId
        if (snap || !lensInit) {
            lensX = lensTarget
            lensV = 0f
            lensInit = true
            invalidate()
        } else {
            scheduleFrame()
        }
    }

    private fun scheduleFrame() {
        if (frameScheduled) return
        frameScheduled = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stepSpring(dt: Float) {
        // Slightly under-damped spring (damping ratio ~0.58) so the lens overshoots a little and settles, liquid style
        val stiffness = 520f
        val damping = 2f * 0.58f * sqrt(stiffness)
        val acceleration = -stiffness * (lensX - lensTarget) - damping * lensV
        lensV += acceleration * dt
        lensX += lensV * dt
        press += (pressTarget - press) * (1f - exp(-18f * dt))
    }

    private fun isLensSettled(): Boolean {
        val settled = abs(lensX - lensTarget) < 0.5f && abs(lensV) < 5f && abs(press - pressTarget) < 0.003f
        if (settled) {
            lensX = lensTarget
            lensV = 0f
            press = pressTarget
        }
        return settled
    }

    // endregion

    override fun dispatchDraw(canvas: Canvas) {
        if (selectedItemId != lastSelectedId && !dragging) retarget(snap = false)
        updatePill()
        if (pillRect.width() > 0f && pillRect.height() > 0f) {
            val surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
            val isLight = ColorUtils.calculateLuminance(surface) > 0.5
            val blurred = blurSupported && canvas.isHardwareAccelerated
            drawBar(canvas, surface, isLight, blurred)
            drawLens(canvas, isLight, blurred)
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

    private fun drawBar(canvas: Canvas, surface: Int, isLight: Boolean, blurred: Boolean) {
        canvas.save()
        canvas.clipPath(pillPath)

        if (blurred && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val layer = backdrop ?: Backdrop(density).also { backdrop = it }
            layer.record(this)
            layer.drawBar(this, canvas)
        }

        // Body tint: light and see-through over a blurred backdrop, much denser when there's nothing to blur
        tintPaint.color = ColorUtils.setAlphaComponent(
            surface,
            if (blurred) (if (isLight) 120 else 110) else 235,
        )
        canvas.drawPath(pillPath, tintPaint)

        // Soft sheen over the top half, like light catching a curved glass surface
        sheenPaint.shader = LinearGradient(
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
        canvas.drawPath(pillPath, sheenPaint)
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

    private fun drawLens(canvas: Canvas, isLight: Boolean, blurred: Boolean) {
        if (!lensInit || lensItemWidth <= 0f) return

        // Squash and stretch: the faster the lens moves, the longer and flatter it gets. Touching it makes it swell.
        val speed = (abs(lensV) / (max(lensItemWidth, 1f) * 7f)).coerceIn(0f, 1f)
        val width = (lensItemWidth - 10f * density) * (1f + 0.32f * speed) * (1f + 0.10f * press)
        val height = (pillRect.height() - 10f * density) * (1f - 0.10f * speed) * (1f + 0.14f * press)
        val radius = height / 2f
        lensRect.set(
            lensX - width / 2f,
            pillRect.centerY() - height / 2f,
            lensX + width / 2f,
            pillRect.centerY() + height / 2f,
        )
        lensPath.rewind()
        lensPath.addRoundRect(lensRect, radius, radius, Path.Direction.CW)

        // Faint dark halo so the lens lifts off the bar, especially on light themes
        lensEdgePaint.color = Color.argb(if (isLight) 22 else 40, 0, 0, 0)
        canvas.drawPath(lensPath, lensEdgePaint)

        canvas.save()
        canvas.clipPath(lensPath)

        if (blurred && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            backdrop?.drawLens(this, canvas, lensRect.centerX(), lensRect.centerY())
        }

        // Body: brighter than the bar around it
        tintPaint.color = Color.argb(
            when {
                blurred && isLight -> 80
                blurred -> 46
                isLight -> 140
                else -> 60
            },
            255,
            255,
            255,
        )
        canvas.drawPath(lensPath, tintPaint)

        sheenPaint.shader = LinearGradient(
            0f,
            lensRect.top,
            0f,
            lensRect.bottom,
            intArrayOf(
                Color.argb(if (isLight) 150 else 70, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.6f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(lensPath, sheenPaint)
        canvas.restore()

        lensRimPaint.shader = LinearGradient(
            lensRect.left,
            lensRect.top,
            lensRect.right,
            lensRect.bottom,
            intArrayOf(
                Color.argb(255, 255, 255, 255),
                Color.argb(40, 255, 255, 255),
                Color.argb(40, 255, 255, 255),
                Color.argb(200, 255, 255, 255),
            ),
            floatArrayOf(0f, 0.4f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        val half = lensRimPaint.strokeWidth / 2f
        val rimRect = RectF(lensRect).apply { inset(half, half) }
        val rimRadius = rimRect.height() / 2f
        canvas.drawRoundRect(rimRect, rimRadius, rimRadius, lensRimPaint)
    }

    /**
     * Everything drawn behind the bar is recorded once per frame into [content]. The bar and the lens then replay it
     * through their own [RenderEffect]: a heavy blur for the bar, a light blur plus magnification for the lens.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private class Backdrop(density: Float) {
        private val content = RenderNode("glass_nav_content")

        private val bar = RenderNode("glass_nav_bar").apply {
            val blur = RenderEffect.createBlurEffect(22f * density, 22f * density, Shader.TileMode.MIRROR)
            setRenderEffect(RenderEffect.createColorFilterEffect(saturation(1.6f), blur))
        }

        private val lens = RenderNode("glass_nav_lens").apply {
            val blur = RenderEffect.createBlurEffect(3f * density, 3f * density, Shader.TileMode.MIRROR)
            setRenderEffect(RenderEffect.createColorFilterEffect(saturation(1.9f), blur))
        }

        private fun saturation(value: Float) = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(value) })

        fun record(view: View) {
            val container = view.parent as? ViewGroup ?: return
            val width = view.width
            val height = view.height

            content.setPosition(0, 0, width, height)
            val recordingCanvas = content.beginRecording(width, height)
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
                content.endRecording()
            }
        }

        fun drawBar(view: View, canvas: Canvas) {
            bar.setPosition(0, 0, view.width, view.height)
            val recordingCanvas = bar.beginRecording(view.width, view.height)
            try {
                recordingCanvas.drawRenderNode(content)
            } finally {
                bar.endRecording()
            }
            canvas.drawRenderNode(bar)
        }

        fun drawLens(view: View, canvas: Canvas, centerX: Float, centerY: Float) {
            lens.setPosition(0, 0, view.width, view.height)
            val recordingCanvas = lens.beginRecording(view.width, view.height)
            try {
                // Magnify what's under the lens around its centre
                recordingCanvas.save()
                recordingCanvas.scale(LENS_ZOOM, LENS_ZOOM, centerX, centerY)
                recordingCanvas.drawRenderNode(content)
                recordingCanvas.restore()
            } finally {
                lens.endRecording()
            }
            canvas.drawRenderNode(lens)
        }

        private companion object {
            const val LENS_ZOOM = 1.18f
        }
    }
}
