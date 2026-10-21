package me.vprogrammers.a11yotp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Logs an accessibility warning on the platform log. Declared in commonMain because Android
 * Compose production code lives in sharedUI/commonMain, which cannot call android.util.Log.
 */
internal expect fun logA11yWarning(message: String)

/** Adapter to the shared contrast logic; translucent colors cannot be evaluated and are skipped. */
internal fun contrastWarning(
    owner: String,
    foregroundName: String,
    foreground: Color,
    backgroundName: String,
    background: Color,
    requirement: OtpContrastRequirement,
): String? {
    if (foreground.alpha < 1f || background.alpha < 1f) return null
    return OtpContrast.warning(
        owner,
        foregroundName,
        foreground.toArgb() and 0xFFFFFF,
        backgroundName,
        background.toArgb() and 0xFFFFFF,
        requirement,
    )
}

/** Logs each warning once. Call from a constructor so it runs once per instance. */
internal fun reportContrast(warnings: List<String?>): List<String> =
    warnings.filterNotNull().also { list -> list.forEach(::logA11yWarning) }
