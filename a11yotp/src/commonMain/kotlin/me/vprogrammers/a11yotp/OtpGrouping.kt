package me.vprogrammers.a11yotp

/**
 * Group size for the decorative separators, or null when [groupSize] does not split [digitCount]
 * into at least two equal groups. A separator in an uneven split (such as "123 - 4") suggests a
 * structure the code does not have, so it is not drawn.
 */
internal fun evenGroupSize(
    groupSize: Int?,
    digitCount: Int,
): Int? = groupSize?.takeIf { it in 1 until digitCount && digitCount % it == 0 }
