import os
import Shared
import SwiftUI
import UIKit

/// Thin adapter between SwiftUI colors and the shared `OtpContrast` checks exported through the Shared framework.
/// Colors are resolved to gamma-encoded sRGB through `UIColor`, never through `Color.Resolved`
/// (whose components are linear), and translucent colors are skipped.
enum OtpContrastAdapter {
    struct Check {
        let foregroundName: String
        let foreground: Color
        let backgroundName: String
        let background: Color
        let requirement: OtpContrastRequirement
    }

    /// Opaque sRGB `0xRRGGBB`, or nil when the color is translucent.
    static func rgb(_ color: Color, in environment: EnvironmentValues) -> Int32? {
        let traits = UITraitCollection { mutable in
            mutable.userInterfaceStyle = environment.colorScheme == .dark ? .dark : .light
            mutable.accessibilityContrast = environment.colorSchemeContrast == .increased ? .high : .normal
        }
        let resolved = UIColor(color).resolvedColor(with: traits)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        guard resolved.getRed(&red, green: &green, blue: &blue, alpha: &alpha), alpha >= 0.999 else { return nil }
        func byte(_ value: CGFloat) -> Int32 { Int32((min(max(value, 0), 1) * 255).rounded()) }
        return (byte(red) << 16) | (byte(green) << 8) | byte(blue)
    }

    static func warnings(owner: String, checks: [Check], in environment: EnvironmentValues) -> [String] {
        checks.compactMap { check in
            guard let foreground = rgb(check.foreground, in: environment),
                  let background = rgb(check.background, in: environment) else { return nil }
            return OtpContrast.shared.warning(
                owner: owner,
                foregroundName: check.foregroundName,
                foregroundRgb: foreground,
                backgroundName: check.backgroundName,
                backgroundRgb: background,
                requirement: check.requirement
            )
        }
    }
}

/// Logs each distinct contrast warning once per process.
final class OtpContrastLog: @unchecked Sendable {
    static let shared = OtpContrastLog()

    private let lock = NSLock()
    private var logged = Set<String>()
    private let logger = Logger(subsystem: "A11yOtp", category: "Contrast")

    func log(_ warnings: [String]) {
        lock.lock()
        let fresh = warnings.filter { logged.insert($0).inserted }
        lock.unlock()
        for warning in fresh {
            logger.warning("\(warning, privacy: .public)")
        }
    }
}
