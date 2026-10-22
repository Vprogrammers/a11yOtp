@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.resolvedColorWithTraitCollection

/**
 * Opaque gamma-encoded sRGB `0xRRGGBB` of this color for [traits], or null when the color is
 * translucent (its contrast depends on what is behind it) or cannot be converted to RGB.
 * Dynamic colors are resolved first, so dark mode and Increase Contrast variants are checked.
 */
internal fun UIColor.opaqueRgb(traits: UITraitCollection): Int? =
    memScoped {
        val red = alloc<DoubleVar>()
        val green = alloc<DoubleVar>()
        val blue = alloc<DoubleVar>()
        val alpha = alloc<DoubleVar>()
        val resolved = resolvedColorWithTraitCollection(traits)
        if (!resolved.getRed(red.ptr, green.ptr, blue.ptr, alpha.ptr) || alpha.value < OPAQUE_ALPHA) {
            null
        } else {
            (toByte(red.value) shl 16) or (toByte(green.value) shl 8) or toByte(blue.value)
        }
    }

private const val OPAQUE_ALPHA = 0.999

private fun toByte(component: Double): Int = (component.coerceIn(0.0, 1.0) * 255.0 + 0.5).toInt()

/** Adapter to the shared contrast logic. Translucent colors cannot be evaluated and are skipped. */
internal fun contrastWarning(
    owner: String,
    foregroundName: String,
    foreground: UIColor,
    backgroundName: String,
    background: UIColor,
    requirement: OtpContrastRequirement,
    traits: UITraitCollection,
): String? {
    val foregroundRgb = foreground.opaqueRgb(traits) ?: return null
    val backgroundRgb = background.opaqueRgb(traits) ?: return null
    return OtpContrast.warning(owner, foregroundName, foregroundRgb, backgroundName, backgroundRgb, requirement)
}

/** Logs each distinct contrast warning once per process, so trait changes and re-renders do not repeat it. */
internal object OtpContrastLog {
    private val logged = mutableSetOf<String>()

    fun log(warnings: List<String>) {
        for (warning in warnings) {
            if (logged.add(warning)) logA11yWarning(warning)
        }
    }

    /** Forgets what was logged. Only for tests. */
    fun reset() {
        logged.clear()
    }
}
