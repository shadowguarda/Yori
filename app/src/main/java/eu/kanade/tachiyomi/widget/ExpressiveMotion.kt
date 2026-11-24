package eu.kanade.tachiyomi.widget

/**
 * Shared Material 3 Expressive motion constants. The default spring is slightly under-damped so UI settles with a
 * small, lively overshoot (damping ratio 0.8, stiffness 380).
 */
object ExpressiveMotion {
    const val SPRING_DAMPING_RATIO = 0.8f
    const val SPRING_STIFFNESS = 380f

    /** Fast fade/scale used for pills appearing and disappearing. */
    const val QUICK_DURATION_MS = 180L

    /** Standard enter/exit for sheets and overlays. */
    const val STANDARD_DURATION_MS = 300L
}
