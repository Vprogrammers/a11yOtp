@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalNativeApi::class)

package me.vprogrammers.a11yotp

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

/** Target of the button's touch-up action. UIKit does not retain targets, so the owner keeps it. */
internal class OtpTapHandler(
    private val onTap: () -> Unit,
) : NSObject() {
    @ObjCAction
    fun tapped() {
        onTap()
    }
}

private fun OtpButtonContainer.attach(handler: OtpTapHandler) {
    control.addTarget(handler, action = NSSelectorFromString("tapped"), forControlEvents = UIControlEventTouchUpInside)
}

/**
 * Button that verifies a complete code. It is enabled only while [state] is complete and
 * [isEnabled] is true. The host owns verification: [onVerify] receives the code and should not
 * also be triggered by [OtpField.onComplete] for the same code unless it de-duplicates. Use the
 * same [alignment] as [OtpField] to keep them aligned. The visible text is also the accessible name.
 * Embed [view] in SwiftUI with a `UIViewRepresentable` and keep this object alive.
 */
class OtpVerifyButton(
    accessibility: OtpVerifyAccessibility,
) {
    private val container = OtpButtonContainer()

    /** The UIKit view to embed. Size it with `sizeThatFits(_:)`; it spans the proposed width. */
    val view: UIView = container

    /** Set this to the field's state. The initial value is an empty, incomplete code. */
    var state: OtpState = OtpState(OtpLength.SIX)
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var accessibility: OtpVerifyAccessibility = accessibility
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

    var style: OtpButtonStyle = OtpButtonDefaults.style()
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var onVerify: ((String) -> Unit)? = null

    private val handler = OtpTapHandler { onVerify?.invoke(state.code) }

    init {
        container.attach(handler)
        val weakSelf = WeakReference(this)
        container.onTraitsChanged = { weakSelf.get()?.render() }
        render()
    }

    /** True while the button can be activated: enabled and the code is complete. */
    internal val isActionEnabled: Boolean get() = isEnabled && state.isComplete

    private fun render() {
        OtpContrastLog.log(style.colors.contrastWarningsIn(container.traitCollection))
        container.render(OtpButtonModel(accessibility.label, isActionEnabled, alignment, style))
    }
}

/**
 * "Resend code" button with a countdown. The host owns the timer and sets [secondsRemaining]; the
 * button is disabled while it is above zero. The countdown text is the visible text and therefore also
 * the accessible name (WCAG 2.5.3 Label in Name). There is no per-second announcement; verify the
 * VoiceOver behavior on a device.
 *
 * Hosts should not make resend permanently unavailable and should let users request more time or
 * another code (WCAG 2.2.1 Timing Adjustable). Use the same [alignment] as [OtpField].
 */
class OtpResendButton(
    accessibility: OtpResendAccessibility,
) {
    private val container = OtpButtonContainer()

    /** The UIKit view to embed. Size it with `sizeThatFits(_:)`; it spans the proposed width. */
    val view: UIView = container

    var secondsRemaining: Int = 0
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var accessibility: OtpResendAccessibility = accessibility
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

    var style: OtpButtonStyle = OtpButtonDefaults.style()
        set(value) {
            if (field == value) return
            field = value
            render()
        }

    var onResend: (() -> Unit)? = null

    private val handler = OtpTapHandler { onResend?.invoke() }

    init {
        container.attach(handler)
        val weakSelf = WeakReference(this)
        container.onTraitsChanged = { weakSelf.get()?.render() }
        render()
    }

    /** Visible text and accessible name for the current countdown. */
    internal val text: String
        get() =
            if (secondsRemaining > 0) {
                accessibility.countdownDescription(OtpCountdown(secondsRemaining))
            } else {
                accessibility.label
            }

    internal val isActionEnabled: Boolean get() = secondsRemaining <= 0

    private fun render() {
        OtpContrastLog.log(style.colors.contrastWarningsIn(container.traitCollection))
        container.render(OtpButtonModel(text, isActionEnabled, alignment, style))
    }
}
