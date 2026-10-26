@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UITraitChangeObservableProtocol
import platform.UIKit.UITraitCollection
import platform.UIKit.UITraitEnvironmentProtocol
import platform.UIKit.UITraitLayoutDirection
import platform.UIKit.UITraitLegibilityWeight
import platform.UIKit.UITraitPreferredContentSizeCategory
import platform.UIKit.UIView
import platform.UIKit.systemTraitsAffectingColorAppearance

/** Dynamic Type: fonts and the sizes scaled from them. */
internal fun contentSizeTrait(): Any = UITraitPreferredContentSizeCategory

/** Bold Text changes the weight of the preferred fonts. */
internal fun legibilityWeightTrait(): Any = UITraitLegibilityWeight

/**
 * Every trait that can change how a dynamic color resolves (dark mode, Increase Contrast, the base or
 * elevated interface level of sheets and popovers, and others), plus the layout direction for cell
 * placement. Colors are resolved into fixed CGColors when drawn, so any of them must re-render.
 */
internal fun appearanceTraits(): List<Any> =
    UITraitCollection.systemTraitsAffectingColorAppearance.filterNotNull() + UITraitLayoutDirection

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
