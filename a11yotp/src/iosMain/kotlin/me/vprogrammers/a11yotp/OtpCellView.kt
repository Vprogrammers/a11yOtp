@file:OptIn(ExperimentalForeignApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectInset
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.QuartzCore.CAShapeLayer
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UITraitCollection
import platform.UIKit.UIView
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setIsAccessibilityElement

/** What one decorative cell shows. Colors are resolved against the view's traits when applied. */
internal class OtpCellAppearance(
    val glyph: String?,
    val glyphColor: UIColor,
    val font: UIFont,
    val fill: UIColor,
    val border: UIColor,
    val borderWidth: Double,
    val cornerRadius: Double,
    /** Inset ring drawn inside a filled cell when focused or in error, or null. */
    val ring: UIColor?,
    val ringWidth: Double,
    /** Raised with a shadow, for the focused cell. */
    val lifted: Boolean = false,
)

/** One decorative digit cell. It is never an accessibility element; the text field is. */
internal class OtpCellView : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val label = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
    private val ringLayer = CAShapeLayer()
    private var ringInset = 0.0
    private var requestedCornerRadius = 0.0
    private var lifted = false

    init {
        setIsAccessibilityElement(false)
        label.setIsAccessibilityElement(false)
        label.textAlignment = NSTextAlignmentCenter
        label.adjustsFontSizeToFitWidth = true
        label.minimumScaleFactor = MIN_SCALE
        addSubview(label)
        ringLayer.fillColor = null
        layer.addSublayer(ringLayer)
        layer.shadowColor = UIColor.blackColor.CGColor
        layer.shadowOffset = CGSizeMake(0.0, SHADOW_OFFSET)
        layer.shadowRadius = SHADOW_RADIUS
        layer.shadowOpacity = 0f
    }

    /**
     * Positions the cell with bounds and center, which stay valid while the lift transform is set
     * (a frame does not).
     */
    fun place(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ) {
        setBounds(CGRectMake(0.0, 0.0, width, height))
        setCenter(CGPointMake(x + width / 2.0, y + height / 2.0))
    }

    private fun applyLift(lifted: Boolean) {
        if (lifted == this.lifted) return
        this.lifted = lifted
        val change = {
            transform =
                if (lifted) CGAffineTransformMakeTranslation(0.0, -LIFT) else CGAffineTransformIdentity.readValue()
            layer.shadowOpacity = if (lifted) SHADOW_OPACITY else 0f
        }
        // The thicker border already shows focus; the lift is extra, so it is not animated under Reduce Motion.
        if (window == null || UIAccessibilityIsReduceMotionEnabled()) {
            change()
        } else {
            UIView.animateWithDuration(LIFT_DURATION, animations = change)
        }
    }

    fun apply(
        appearance: OtpCellAppearance,
        traits: UITraitCollection,
    ) {
        requestedCornerRadius = appearance.cornerRadius
        layer.borderWidth = appearance.borderWidth
        layer.borderColor = appearance.border.resolvedColorWithTraitCollection(traits).CGColor
        layer.backgroundColor = appearance.fill.resolvedColorWithTraitCollection(traits).CGColor
        label.font = appearance.font
        label.text = appearance.glyph
        label.textColor = appearance.glyphColor.resolvedColorWithTraitCollection(traits)
        val ring = appearance.ring
        ringLayer.hidden = ring == null
        ringLayer.strokeColor = ring?.resolvedColorWithTraitCollection(traits)?.CGColor
        ringLayer.lineWidth = appearance.ringWidth
        ringInset = RING_GAP + appearance.ringWidth / 2.0
        applyLift(appearance.lifted)
        setNeedsLayout()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        label.setFrame(CGRectInset(bounds, INNER_PADDING, 0.0))
        ringLayer.frame = bounds
        val (width, height) = bounds.useContents { size.width to size.height }
        // Clamped so a large radius gives circles (square cells) or capsules, also after Dynamic Type growth.
        layer.cornerRadius = minOf(requestedCornerRadius, minOf(width, height) / 2.0)
        val ringWidth = width - 2 * ringInset
        val ringHeight = height - 2 * ringInset
        if (ringWidth > 0.0 && ringHeight > 0.0) {
            val ringRect = CGRectMake(ringInset, ringInset, ringWidth, ringHeight)
            val radius = (layer.cornerRadius - ringInset).coerceAtLeast(0.0)
            ringLayer.path = UIBezierPath.bezierPathWithRoundedRect(ringRect, cornerRadius = radius).CGPath
        }
        layer.shadowPath = UIBezierPath.bezierPathWithRoundedRect(bounds, cornerRadius = layer.cornerRadius).CGPath
    }
}

// Cells scale with Dynamic Type and only shrink below the minimum width as a last resort, so glyphs
// keep their size normally. 0.75 is the smallest shrink that still stops a glyph from clipping in a
// narrowed cell; the 0.5 used before would undercut large text.
private const val MIN_SCALE = 0.75
private const val RING_GAP = 3.0
private const val INNER_PADDING = 2.0
private const val LIFT = 4.0
private const val LIFT_DURATION = 0.18
private const val SHADOW_OFFSET = 6.0
private const val SHADOW_RADIUS = 8.0
private const val SHADOW_OPACITY = 0.35f
