package consumer

import me.vprogrammers.a11yotp.OtpField
import me.vprogrammers.a11yotp.OtpFieldAccessibility
import me.vprogrammers.a11yotp.OtpLength
import me.vprogrammers.a11yotp.OtpState

fun makeField(): OtpField =
    OtpField(
        state = OtpState(OtpLength.SIX),
        accessibility = OtpFieldAccessibility("Code", "Wrong code") { "${it.entered} of ${it.total}" },
        doneButtonTitle = "Done",
    )
