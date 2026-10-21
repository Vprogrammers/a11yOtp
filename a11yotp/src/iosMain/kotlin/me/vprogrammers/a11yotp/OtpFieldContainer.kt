@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSClassFromString
import platform.Foundation.NSSelectorFromString
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.NSTextAlignmentLeft
import platform.UIKit.NSTextAlignmentRight
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontMetrics
import platform.UIKit.UIFontTextStyle
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIImageView
import platform.UIKit.UILabel
import platform.UIKit.UISemanticContentAttributeForceLeftToRight
import platform.UIKit.UITextField
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.UIVisualEffect
import platform.UIKit.UIVisualEffectView
import platform.UIKit.resolvedColorWithTraitCollection
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setAccessibilityValue
import platform.UIKit.setIsAccessibilityElement
import platform.darwin.NSObjectProtocol

/** Everything the container needs to draw one frame. */
internal class OtpFieldModel(
    val state: OtpState,
    val label: String,
    val value: String,
    val errorMessage: String,
    val style: OtpFieldStyle,
    val mask: OtpMask,
    val isEnabled: Boolean,
    val alignment: OtpAlignment,
    val isFocused: Boolean,
)

internal fun otpCellBorderWidth(
    style: OtpFieldStyle,
    isEnabled: Boolean,
    isError: Boolean,
    isActive: Boolean,
): Double = if (isEnabled && (isError || isActive)) style.focusedBorderWidth else style.borderWidth

internal fun otpCellBorderColor(
    colors: OtpFieldColors,
    isEnabled: Boolean,
    isError: Boolean,
    isActive: Boolean,
): UIColor =
    when {
        !isEnabled -> colors.disabledCellBorder
        isError -> colors.errorCellBorder
        isActive -> colors.focusedCellBorder
        else -> colors.cellBorder
    }

/**
 * The view hosted by [OtpField]. It lays out decorative cells and separators and one transparent
 * [textField] over them. Only the text field is an accessibility element. It has no strong
 * reference to the Kotlin owner; [onTraitsChanged] must capture the owner weakly.
 */
internal class OtpFieldContainer : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val textField = UITextField(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
    var onTraitsChanged: (() -> Unit)? = null

    private val cells = mutableListOf<OtpCellView>()
    private val separators = mutableListOf<UIView>()
    private val errorIcon = UIImageView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
    private val errorLabel = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
    private var model: OtpFieldModel? = null

    /** Liquid Glass backdrop behind the cells, created on first use. */
    private var glassView: UIVisualEffectView? = null

    init {
        setIsAccessibilityElement(false)
        // Cells are positioned with absolute frames, so digits read left to right also in right-to-left
        // layouts. The input itself is forced left to right for the same reason.
        textField.semanticContentAttribute = UISemanticContentAttributeForceLeftToRight
        errorIcon.setIsAccessibilityElement(false)
        errorIcon.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        errorLabel.setIsAccessibilityElement(false)
        errorLabel.numberOfLines = 0
        errorLabel.adjustsFontForContentSizeCategory = true
        errorIcon.hidden = true
        errorLabel.hidden = true
        addSubview(errorIcon)
        addSubview(errorLabel)
        addSubview(textField)
    }

    fun render(model: OtpFieldModel) {
        this.model = model
        val wantsGlass = glassInset(model) > 0.0
        if (wantsGlass && glassView == null) {
            glassView =
                UIVisualEffectView(effect = sharedGlassEffect).also {
                    it.userInteractionEnabled = false
                    it.setIsAccessibilityElement(false)
                    it.clipsToBounds = true
                    insertSubview(it, atIndex = 0)
                }
        }
        glassView?.hidden = !wantsGlass
        val style = model.style
        val colors = style.colors
        val traits = traitCollection
        val enabled = model.isEnabled
        val state = model.state
        val total = state.length.digitCount
        val planner = planner(model, traits)
        ensureCells(total, planner.separatorCount)

        val font = scaledFont(style, traits)
        val activeIndex = state.enteredDigitCount.coerceAtMost(total - 1)
        cells.forEachIndexed { index, cell ->
            val digit = state.code.getOrNull(index)
            val isActive = model.isFocused && enabled && index == activeIndex
            val borderColor = otpCellBorderColor(colors, enabled, state.isError, isActive)
            val emphasised = enabled && (state.isError || isActive)
            val filled = digit != null && model.mask == OtpMask.FilledCell
            val background = if (enabled) colors.cellBackground else colors.disabledCellBackground
            val glyphColor = if (enabled) colors.text else colors.disabledText
            val glyph =
                when {
                    digit != null && model.mask is OtpMask.Character -> model.mask.char.toString()
                    digit != null && model.mask == OtpMask.None -> digit.toString()
                    digit == null -> style.placeholder
                    else -> null
                }
            cell.apply(
                OtpCellAppearance(
                    glyph = glyph,
                    glyphColor =
                        if (digit ==
                            null
                        ) {
                            (if (enabled) colors.placeholder else colors.disabledText)
                        } else {
                            glyphColor
                        },
                    font = font,
                    fill = if (filled) borderColor else background,
                    border = borderColor,
                    borderWidth = otpCellBorderWidth(style, enabled, state.isError, isActive),
                    cornerRadius = style.cornerRadius,
                    ring = if (filled && emphasised) background else null,
                    ringWidth = style.focusedBorderWidth,
                ),
                traits,
            )
        }
        val separatorColor = (if (enabled) colors.cellBorder else colors.disabledCellBorder)
        separators.forEach { it.backgroundColor = separatorColor.resolvedColorWithTraitCollection(traits) }

        val showsError = state.isError
        errorLabel.hidden = !showsError
        errorIcon.hidden = !showsError
        if (showsError) {
            val errorColor =
                (if (enabled) colors.errorText else colors.disabledText).resolvedColorWithTraitCollection(
                    traits,
                )
            errorLabel.text = model.errorMessage
            errorLabel.font = UIFont.preferredFontForTextStyle(style.errorTextStyle)
            errorLabel.textColor = errorColor
            errorLabel.textAlignment = textAlignment(model.alignment)
            val symbol = UIImageSymbolConfiguration.configurationWithTextStyle(style.errorTextStyle)
            errorIcon.image = UIImage.systemImageNamed("exclamationmark.circle.fill", withConfiguration = symbol)
            errorIcon.tintColor = errorColor
        }

        textField.enabled = model.isEnabled
        textField.secureTextEntry = model.mask != OtpMask.None
        if (textField.text != state.code) textField.text = state.code
        textField.setAccessibilityLabel(model.label)
        textField.setAccessibilityValue(model.value)

        setNeedsLayout()
        invalidateIntrinsicContentSize()
    }

    private fun ensureCells(
        total: Int,
        separatorCount: Int,
    ) {
        while (cells.size > total) cells.removeAt(cells.lastIndex).removeFromSuperview()
        while (cells.size < total) {
            val cell = OtpCellView()
            insertSubview(cell, belowSubview = textField)
            cells += cell
        }
        while (separators.size > separatorCount) separators.removeAt(separators.lastIndex).removeFromSuperview()
        while (separators.size < separatorCount) {
            val separator = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
            separator.setIsAccessibilityElement(false)
            insertSubview(separator, belowSubview = textField)
            separators += separator
        }
    }

    private fun scaledFont(
        style: OtpFieldStyle,
        traits: UITraitCollection,
    ): UIFont =
        UIFontMetrics(forTextStyle = style.fontTextStyle).scaledFontForFont(
            style.font,
            compatibleWithTraitCollection = traits,
        )

    private fun scaled(
        value: Double,
        style: UIFontTextStyle,
        traits: UITraitCollection,
    ): Double = UIFontMetrics(forTextStyle = style).scaledValueForValue(value, compatibleWithTraitCollection = traits)

    private fun planner(
        model: OtpFieldModel,
        traits: UITraitCollection,
    ): OtpCellsPlanner {
        val style = model.style
        val count = model.state.length.digitCount
        return OtpCellsPlanner(
            count = count,
            groupSize = evenGroupSize(style.groupSize, count) ?: 0,
            cellWidth = scaled(style.minCellWidth, style.fontTextStyle, traits),
            spacing = style.cellSpacing,
            separatorWidth = scaled(SEPARATOR_WIDTH, style.fontTextStyle, traits),
        )
    }

    private fun cellHeight(
        model: OtpFieldModel,
        traits: UITraitCollection,
    ): Double {
        val style = model.style
        val minimum = scaled(style.minCellHeight, style.fontTextStyle, traits)
        return maxOf(minimum, scaledFont(style, traits).lineHeight + CELL_VERTICAL_PADDING)
    }

    private fun isRtl(): Boolean =
        effectiveUserInterfaceLayoutDirection ==
            UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft

    /** 0 = rows start at the physical left, 1 = physical right. */
    private fun fraction(alignment: OtpAlignment): Double = (alignment.absoluteBias(isRtl()) + 1.0) / 2.0

    private fun textAlignment(alignment: OtpAlignment): Long {
        val bias = alignment.absoluteBias(isRtl())
        return when {
            bias < 0f -> NSTextAlignmentLeft
            bias > 0f -> NSTextAlignmentRight
            else -> NSTextAlignmentCenter
        }
    }

    private fun errorSize(
        model: OtpFieldModel,
        width: Double,
    ): Triple<Double, Double, Double> {
        // Returns icon width, label width, height.
        if (!model.state.isError) return Triple(0.0, 0.0, 0.0)
        // `this.` is required: a bare `width` would resolve to the parameter, not CGSize.width.
        val iconWidth = errorIcon.intrinsicContentSize().useContents { this.width }
        val labelWidth = (width - iconWidth - ERROR_ICON_GAP).coerceAtLeast(1.0)
        val labelHeight = errorLabel.sizeThatFits(CGSizeMake(labelWidth, UNBOUNDED)).useContents { this.height }
        val iconHeight = errorIcon.intrinsicContentSize().useContents { this.height }
        return Triple(iconWidth, labelWidth, maxOf(labelHeight, iconHeight))
    }

    /** Padding around the cells for the Liquid Glass backdrop, or 0 when there is none. */
    private fun glassInset(model: OtpFieldModel): Double =
        if (model.style.glass && sharedGlassEffect != null) GLASS_PADDING else 0.0

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val model = model ?: return CGSizeMake(0.0, MIN_FALLBACK_HEIGHT)
        val proposed = size.useContents { width }
        val width = if (proposed.isFinite() && proposed > 0.0 && proposed < UNBOUNDED / 2) proposed else null
        val inset = glassInset(model)
        val traits = traitCollection
        val planner = planner(model, traits)
        val plan = planner.plan(width?.let { it - 2 * inset })
        val cellHeight = cellHeight(model, traits)
        val rowsWidth =
            plan.rows.maxOf { planner.rowWidth(it, plan.cellWidth, plan.showsSeparators) } + 2 * inset
        val rowsHeight = plan.rows.size * cellHeight + (plan.rows.size - 1) * model.style.cellSpacing + 2 * inset
        val errorHeight = errorSize(model, width ?: rowsWidth).third
        val total = rowsHeight + if (errorHeight > 0.0) ERROR_GAP + errorHeight else 0.0
        return CGSizeMake(width ?: rowsWidth, total)
    }

    override fun intrinsicContentSize(): CValue<CGSize> {
        val width = bounds.useContents { size.width }
        val height = sizeThatFits(CGSizeMake(if (width > 0.0) width else UNBOUNDED, UNBOUNDED)).useContents { height }
        return CGSizeMake(UIViewNoIntrinsicMetric, height)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val model = model ?: return
        val width = bounds.useContents { size.width }
        val traits = traitCollection
        val style = model.style
        val inset = glassInset(model)
        val planner = planner(model, traits)
        val plan = planner.plan(width - 2 * inset)
        val cellHeight = cellHeight(model, traits)
        val separatorHeight = maxOf(style.borderWidth, MIN_SEPARATOR_HEIGHT)
        val separatorWidth = scaled(SEPARATOR_WIDTH, style.fontTextStyle, traits)
        val fraction = fraction(model.alignment)
        val shown = mutableSetOf<Int>()
        var minX = Double.MAX_VALUE
        var maxX = 0.0
        plan.rows.forEachIndexed { rowIndex, row ->
            val rowWidth = planner.rowWidth(row, plan.cellWidth, plan.showsSeparators)
            var x = inset + (width - 2 * inset - rowWidth) * fraction
            val y = inset + rowIndex * (cellHeight + style.cellSpacing)
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x + rowWidth)
            for (cell in row.start until row.endExclusive) {
                cells[cell].setFrame(CGRectMake(x, y, plan.cellWidth, cellHeight))
                x += plan.cellWidth
                val endsGroup = (cell + 1) % planner.groupSize == 0
                if (plan.showsSeparators && cell != row.endExclusive - 1 && endsGroup) {
                    val index = (cell + 1) / planner.groupSize - 1
                    if (index < separators.size) {
                        shown += index
                        separators[index].setFrame(
                            CGRectMake(
                                x + style.cellSpacing,
                                y + (cellHeight - separatorHeight) / 2,
                                separatorWidth,
                                separatorHeight,
                            ),
                        )
                    }
                }
                x += planner.gapAfter(cell, plan.showsSeparators)
            }
        }
        separators.forEachIndexed { index, separator -> separator.hidden = index !in shown }
        val rowsHeight = plan.rows.size * cellHeight + (plan.rows.size - 1) * style.cellSpacing
        // The text field has the frame of the visible cells, so the VoiceOver focus ring, Voice Control
        // overlay, and touch target match what users see.
        if (minX <= maxX) textField.setFrame(CGRectMake(minX, inset, maxX - minX, rowsHeight))
        glassView?.let { glass ->
            if (minX <= maxX && inset > 0.0) {
                val glassHeight = rowsHeight + 2 * inset
                glass.setFrame(CGRectMake(minX - inset, 0.0, maxX - minX + 2 * inset, glassHeight))
                // Concentric with the cells, so round cells get a capsule.
                glass.layer.cornerRadius = minOf(style.cornerRadius + inset, glassHeight / 2.0)
            }
        }

        if (model.state.isError) {
            val (iconWidth, labelWidth, height) = errorSize(model, width)
            val rowWidth = iconWidth + ERROR_ICON_GAP + labelWidth
            val x = ((width - rowWidth) * fraction).coerceAtLeast(0.0)
            val y = rowsHeight + 2 * inset + ERROR_GAP
            val iconHeight = errorIcon.intrinsicContentSize().useContents { this.height }
            errorIcon.setFrame(CGRectMake(x, y + (height - iconHeight) / 2, iconWidth, iconHeight))
            errorLabel.setFrame(CGRectMake(x + iconWidth + ERROR_ICON_GAP, y, labelWidth, height))
        }
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        onTraitsChanged?.invoke()
    }
}

private const val SEPARATOR_WIDTH = 12.0
private const val MIN_SEPARATOR_HEIGHT = 2.0
private const val CELL_VERTICAL_PADDING = 16.0

/**
 * One shared UIGlassEffect (effects are immutable), or null before iOS 26. The class is looked up by
 * name: a direct reference compiles to a strong `_OBJC_CLASS_$_UIGlassEffect` symbol, which would
 * stop apps from launching on earlier iOS versions even with glass turned off. `new` returns a +1
 * reference that is never balanced; it is created once per process, so that is one small object.
 */
internal val sharedGlassEffect: UIVisualEffect? by lazy {
    if (!isLiquidGlassAvailable()) return@lazy null
    val glassClass = NSClassFromString("UIGlassEffect") as? NSObjectProtocol ?: return@lazy null
    glassClass.performSelector(NSSelectorFromString("new")) as? UIVisualEffect
}

private const val ERROR_GAP = 8.0
private const val GLASS_PADDING = 10.0
private const val ERROR_ICON_GAP = 6.0
private const val MIN_FALLBACK_HEIGHT = 56.0
private const val UNBOUNDED = 100_000.0
