package me.vprogrammers.a11yotp

import kotlin.math.pow

/** WCAG contrast requirement that a color pair is checked against. */
enum class OtpContrastRequirement(
    val minimumRatio: Double,
    val criterion: String,
    val subject: String,
    internal val consequence: String,
) {
    TEXT(4.5, "1.4.3 Contrast (Minimum)", "text", "read this text"),
    NON_TEXT(3.0, "1.4.11 Non-text Contrast", "UI components", "see this part of the field"),
    FOCUS_INDICATOR(
        3.0,
        "2.4.13 Focus Appearance",
        "the focus indicator against the unfocused state",
        "see which cell has focus",
    ),
}

/**
 * WCAG contrast checks shared by the Android and iOS components.
 *
 * Colors are opaque sRGB values in `0xRRGGBB` form. Platforms skip translucent colors,
 * because their contrast depends on what is drawn behind them.
 */
object OtpContrast {
    /** Contrast ratio between two colors, from 1.0 to 21.0. */
    fun ratio(
        foregroundRgb: Int,
        backgroundRgb: Int,
    ): Double {
        val a = relativeLuminance(foregroundRgb)
        val b = relativeLuminance(backgroundRgb)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /**
     * Returns a warning that explains which pair fails and why, or null when the pair meets
     * [requirement]. The library logs it and still uses the colors, so brand colors remain usable.
     */
    fun warning(
        owner: String,
        foregroundName: String,
        foregroundRgb: Int,
        backgroundName: String,
        backgroundRgb: Int,
        requirement: OtpContrastRequirement,
    ): String? {
        val ratio = ratio(foregroundRgb, backgroundRgb)
        if (ratio >= requirement.minimumRatio) return null
        return "A11yOtp: $owner $foregroundName (${hex(foregroundRgb)}) on $backgroundName " +
            "(${hex(backgroundRgb)}) has contrast ${formatRatio(ratio)}:1; WCAG ${requirement.criterion} " +
            "requires at least ${requirement.minimumRatio}:1 for ${requirement.subject}. Users with low " +
            "vision may not be able to ${requirement.consequence}. Using the colors as provided."
    }

    private fun relativeLuminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((rgb shr shift) and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun hex(rgb: Int): String = "#" + (rgb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

    // Truncates instead of rounding so 4.499 is never reported as passing 4.5.
    private fun formatRatio(ratio: Double): String {
        val hundredths = (ratio * 100).toInt()
        return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
    }
}

/**
 * True when the focused-cell shadow should be white: the cell background is dark, which is when
 * white contrasts with it more than black does. Black would be invisible on a dark background.
 */
internal fun otpShadowIsLight(backgroundRgb: Int): Boolean =
    OtpContrast.ratio(0xFFFFFF, backgroundRgb) > OtpContrast.ratio(0x000000, backgroundRgb)
