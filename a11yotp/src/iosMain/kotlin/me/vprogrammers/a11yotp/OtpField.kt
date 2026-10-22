@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalNativeApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSRange
import platform.UIKit.UIAccessibilityAnnouncementNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentTypeOneTimeCode
import platform.UIKit.UITextField
import platform.UIKit.UITextFieldDelegateProtocol
import platform.UIKit.UITextSpellCheckingType
import platform.UIKit.UIView
import platform.UIKit.setIsAccessibilityElement
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

/**
 * One-time-code input built on UIKit. Visually it is a row of cells; for VoiceOver and Voice Control
 * it is one text field (the cells, separators, placeholder, and error text are not accessibility
 * elements). Embed [view] in SwiftUI with a `UIViewRepresentable` and keep this object alive, for
 * example in the coordinator.
 *
 * The host owns [state] and verification: call [OtpState.withError] or [OtpState.cleared] after a
 * failed verification and assign the result to [state]. Typing, deletion, and paste arrive as
 * [onStateChange] with the new state, which the component renders immediately. [onComplete]
 * fires once per incomplete-to-complete transition. [accessibility] and [doneButtonTitle] must be
 * localized by the host. With a [mask] other than [OtpMask.None] the field is a secure text field
 * and VoiceOver gets the label and progress only, never the digits.
 *
 * The field uses the one-time-code content type, so the system can offer codes from messages.
 * Setting [state] to an error state announces [OtpFieldAccessibility.errorMessage] once without moving
 * focus, and vibrates once when [hapticFeedbackOnError] is true. [alignment] only positions the
 * cells and error text; digits inside the row always read left to right. No animation is used, so
 * Reduce Motion needs no special handling.
 *
 * No `accessibilityHint` is set by default, intentionally: progress and error state are already part
 * of the accessibility value, and a hint would be read on every focus.
 */
class OtpField(
    state: OtpState,
    accessibility: OtpFieldAccessibility,
    doneButtonTitle: String,
) {
    init {
        require(doneButtonTitle.isNotBlank()) { "doneButtonTitle must not be blank" }
    }

    private val container = OtpFieldContainer()

    /** The UIKit view to embed. Size it with `sizeThatFits(_:)`; it spans the proposed width. */
    val view: UIView = container

    var state: OtpState = state
        set(value) {
            if (field == value) return
            val wasError = field.isError
            field = value
            // A new host state ends the transient rejection message.
            rejection = null
            rejectionGeneration++
            render()
            if (value.isError && !wasError) {
                errorEntered()
            } else if (!value.isError) {
                errorGeneration++
            }
        }

    /** Title of the keyboard Done button. Localized by the host; may change at runtime. Must not be blank. */
    var doneButtonTitle: String = doneButtonTitle
        set(value) {
            require(value.isNotBlank()) { "doneButtonTitle must not be blank" }
            if (field == value) return
            field = value
            applyDoneTitle()
        }

    var accessibility: OtpFieldAccessibility = accessibility
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var style: OtpFieldStyle = OtpFieldDefaults.style()
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var mask: OtpMask = OtpMask.None
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var isEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var alignment: OtpAlignment = OtpAlignment.CENTER
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    /** Vibrates once when [state] enters the error state. */
    var hapticFeedbackOnError: Boolean = true

    var onStateChange: ((OtpState) -> Unit)? = null
    var onComplete: ((String) -> Unit)? = null

    /**
     * Called once per rejected multi-digit input (paste, autofill, keyboard suggestion) whose digit
     * count differs from the field length. The state is unchanged. The counts usually mean the
     * configured [OtpLength] differs from the host's backend. The digits are never passed.
     */
    var onInputRejected: ((OtpInputRejection) -> Unit)? = null

    private var isFocused = false
    private var isPinningCaret = false
    private val handler = Handler()
    private var doneBar: OtpKeyboardAccessory? = null
    private var errorGeneration = 0
    private var rejection: OtpInputRejection? = null
    private var rejectionGeneration = 0

    private inner class Handler :
        NSObject(),
        UITextFieldDelegateProtocol {
        override fun textField(
            textField: UITextField,
            shouldChangeCharactersInRange: CValue<NSRange>,
            replacementString: String,
        ): Boolean {
            handleReplacement(replacementString)
            return false
        }

        override fun textFieldDidBeginEditing(textField: UITextField) {
            isFocused = true
            render()
        }

        override fun textFieldDidEndEditing(textField: UITextField) {
            isFocused = false
            render()
        }

        override fun textFieldDidChangeSelection(textField: UITextField) {
            pinCaretToEnd()
        }

        override fun textFieldShouldReturn(textField: UITextField): Boolean {
            textField.resignFirstResponder()
            return true
        }

        @ObjCAction
        fun donePressed() {
            blur()
        }
    }

    init {
        val textField = container.textField
        textField.delegate = handler
        textField.keyboardType = UIKeyboardTypeNumberPad
        textField.textContentType = UITextContentTypeOneTimeCode
        textField.autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeNo
        textField.autocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
        textField.spellCheckingType = UITextSpellCheckingType.UITextSpellCheckingTypeNo
        // The text is drawn by the cells; keep the real input invisible but fully laid out.
        textField.textColor = platform.UIKit.UIColor.clearColor
        textField.tintColor = platform.UIKit.UIColor.clearColor
        textField.backgroundColor = platform.UIKit.UIColor.clearColor
        textField.setIsAccessibilityElement(true)
        // Done bar detached for now: it gets in the way of the one-time-code suggestion (#10).
        // textField.inputAccessoryView = doneBar()

        val weakSelf = WeakReference(this)
        container.onTraitsChanged = { weakSelf.get()?.render() }
        render()
    }

    /** Moves keyboard focus to the field. Never call this just to announce something. */
    fun focus() {
        container.textField.becomeFirstResponder()
    }

    /** Dismisses the keyboard. */
    fun blur() {
        container.textField.resignFirstResponder()
    }

    /**
     * Applies text the user typed, pasted, or autofilled at the end of the field. Because the
     * caret is pinned to the end, a non-empty [replacement] is appended and an empty one deletes the
     * last digit. The rules of [OtpState.withInput] apply: a multi-digit insertion is accepted only
     * with exactly the field's digit count (it then replaces the input); otherwise the state stays
     * unchanged, [onInputRejected] is called, and the rejection message is shown and announced.
     * Unsupported characters leave the state unchanged.
     */
    internal fun handleReplacement(replacement: String) {
        val current = state
        val newText = if (replacement.isEmpty()) current.code.dropLast(1) else current.code + replacement
        val result = current.withInput(newText)
        val rejected = result.rejection
        if (result.state != current) {
            // The state setter ends any earlier rejection message.
            state = result.state
            onStateChange?.invoke(result.state)
        } else if (rejected != null) {
            showRejection(rejected)
        } else {
            // No-op input: write the committed code back.
            render()
        }
        if (result.didComplete) onComplete?.invoke(result.state.code)
    }

    private fun showRejection(rejected: OtpInputRejection) {
        rejection = rejected
        val generation = ++rejectionGeneration
        render()
        onInputRejected?.invoke(rejected)
        // While in error the error text occupies the slot and is already part of the value.
        if (state.isError) return
        val message = accessibility.inputRejectedMessage(rejected)
        val weakSelf = WeakReference(this)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, ANNOUNCEMENT_DELAY_NANOS), dispatch_get_main_queue()) {
            val self = weakSelf.get()
            // Skip if the message was cleared or replaced in the meantime.
            if (self != null && self.rejectionGeneration == generation && self.rejection != null &&
                !self.state.isError
            ) {
                UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, message)
            }
        }
    }

    private fun pinCaretToEnd() {
        if (isPinningCaret) return
        val textField = container.textField
        val end = textField.endOfDocument
        val selected = textField.selectedTextRange
        // Positions are compared by offset; UITextPosition instances are not reliably equal.
        val atEnd =
            selected != null && selected.empty && textField.offsetFromPosition(end, toPosition = selected.start) == 0L
        if (!atEnd) {
            isPinningCaret = true
            textField.selectedTextRange = textField.textRangeFromPosition(end, end)
            isPinningCaret = false
        }
    }

    private fun render() {
        OtpContrastLog.log(style.colors.contrastWarningsIn(container.traitCollection))
        container.render(
            OtpFieldModel(
                state = state,
                label = accessibility.label,
                value = otpAccessibilityValue(state, accessibility, mask != OtpMask.None),
                errorMessage = accessibility.errorMessage,
                rejectedMessage = rejection?.let { accessibility.inputRejectedMessage(it) },
                style = style,
                mask = mask,
                isEnabled = isEnabled,
                alignment = alignment,
                isFocused = isFocused,
            ),
        )
    }

    private fun errorEntered() {
        val generation = ++errorGeneration
        val message = accessibility.errorMessage
        // Posted shortly after the state change so the announcement is not cut off by the speech
        // for the digit that just caused the error.
        val weakSelf = WeakReference(this)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, ANNOUNCEMENT_DELAY_NANOS), dispatch_get_main_queue()) {
            val self = weakSelf.get()
            // Only the latest error transition posts, and only while still in error.
            if (self != null && self.errorGeneration == generation && self.state.isError) {
                UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, message)
            }
        }
        if (hapticFeedbackOnError) {
            UINotificationFeedbackGenerator().notificationOccurred(
                UINotificationFeedbackType.UINotificationFeedbackTypeError,
            )
        }
    }

    // The number pad has no Return key, so the keyboard gets a Done button.
    private fun doneBar(): OtpKeyboardAccessory =
        OtpKeyboardAccessory(handler, "donePressed").also {
            it.anchorView = container
            it.setTitle(doneButtonTitle)
            doneBar = it
        }

    private fun applyDoneTitle() {
        doneBar?.setTitle(doneButtonTitle)
    }

    private companion object {
        const val ANNOUNCEMENT_DELAY_NANOS = 300_000_000L
    }
}
