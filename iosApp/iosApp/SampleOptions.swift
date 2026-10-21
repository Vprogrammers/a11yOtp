import Shared
import SwiftUI

/// Demo options below the sample flow.
struct SampleOptions {
    static let lengths: [OtpLength] = [.four, .six, .eight]
    static let masks: [(name: String, mask: OtpMask)] = [
        ("None", OtpMaskNone.shared),
        ("Character", OtpMaskCharacter(char: 0x2022)),
        ("Filled cell", OtpMaskFilledCell.shared)
    ]

    var length = OtpLength.six
    var maskIndex = 0
    var roundCells = false
    var isDisabled = false

    var mask: OtpMask { Self.masks[maskIndex].mask }
}

struct SampleOptionsView: View {
    @Binding var options: SampleOptions

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Options").font(.headline).accessibilityAddTraits(.isHeader)
            // Visible labels above segmented pickers, which do not show their own label.
            Text("Code length")
            Picker("Code length", selection: $options.length) {
                ForEach(SampleOptions.lengths, id: \.self) { Text("\($0.digitCount)").tag($0) }
            }
            .pickerStyle(.segmented)
            Text("Masking")
            Picker("Masking", selection: $options.maskIndex) {
                ForEach(SampleOptions.masks.indices, id: \.self) { Text(SampleOptions.masks[$0].name).tag($0) }
            }
            .pickerStyle(.segmented)
            Toggle("Round cells", isOn: $options.roundCells)
            Toggle("Disable field", isOn: $options.isDisabled)
        }
    }
}
