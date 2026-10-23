@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UITraitAccessibilityContrast
import platform.UIKit.UITraitChangeObservableProtocol
import platform.UIKit.UITraitCollection
import platform.UIKit.UITraitEnvironmentProtocol
import platform.UIKit.UITraitLayoutDirection
import platform.UIKit.UITraitLegibilityWeight
import platform.UIKit.UITraitPreferredContentSizeCategory
import platform.UIKit.UITraitUserInterfaceStyle
import platform.UIKit.UIView

/** Dynamic Type: fonts and the sizes scaled from them. */
internal fun contentSizeTrait(): Any = UITraitPreferredContentSizeCategory

/** Bold Text changes the weight of the preferred fonts. */
internal fun legibilityWeightTrait(): Any = UITraitLegibilityWeight

/** Dark mode, Increase Contrast, and layout direction: dynamic colors and cell placement. */
internal fun appearanceTraits(): List<Any> =
    listOf(UITraitUserInterfaceStyle, UITraitAccessibilityContrast, UITraitLayoutDirection)

/**
 * Registers [onChange] for exactly [traits] (iOS 17+, replacing `traitCollectionDidChange`). The
 * handler receives the view, so [onChange] must not capture it
 * (UIKit retains the handler, so that would be a cycle).
 */
internal inline fun <reified V : UIView> V.observeTraits(
    traits: List<Any>,
    crossinline onChange: (V) -> Unit,
) {
    (this as UITraitChangeObservableProtocol).registerForTraitChanges(
        traits,
        withHandler = { environment: UITraitEnvironmentProtocol?, _: UITraitCollection? ->
            (environment as? V)?.let(onChange)
        },
    )
}
