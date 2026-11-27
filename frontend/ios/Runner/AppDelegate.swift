import Flutter
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  private static let snapshotChannelName = "kerno/snapshot_protection"
  private static let privacyOverlayTag = 992026
  static var isSnapshotProtectionEnabled = false

  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)

    let channel = FlutterMethodChannel(
      name: AppDelegate.snapshotChannelName,
      binaryMessenger: engineBridge.applicationRegistrar.messenger()
    )
    channel.setMethodCallHandler { call, result in
      switch call.method {
      case "enableProtection":
        AppDelegate.isSnapshotProtectionEnabled = true
        result(nil)
      case "disableProtection":
        AppDelegate.isSnapshotProtectionEnabled = false
        DispatchQueue.main.async {
          AppDelegate.removePrivacyOverlayFromAllWindows()
        }
        result(nil)
      default:
        result(FlutterMethodNotImplemented)
      }
    }
  }

  override func applicationWillResignActive(_ application: UIApplication) {
    super.applicationWillResignActive(application)
    if AppDelegate.isSnapshotProtectionEnabled {
      AppDelegate.showPrivacyOverlay(on: application.windows.first(where: { $0.isKeyWindow }) ?? application.windows.first)
    }
  }

  override func applicationDidBecomeActive(_ application: UIApplication) {
    super.applicationDidBecomeActive(application)
    AppDelegate.removePrivacyOverlayFromAllWindows()
  }

  static func showPrivacyOverlay(on window: UIWindow?) {
    guard let window = window else { return }
    if window.viewWithTag(privacyOverlayTag) != nil { return }

    let overlay = UIView(frame: window.bounds)
    overlay.tag = privacyOverlayTag
    overlay.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    overlay.backgroundColor = UIColor(red: 15.0 / 255.0, green: 23.0 / 255.0, blue: 42.0 / 255.0, alpha: 1.0)
    window.addSubview(overlay)
  }

  static func removePrivacyOverlay(from window: UIWindow?) {
    window?.viewWithTag(privacyOverlayTag)?.removeFromSuperview()
  }

  static func removePrivacyOverlayFromAllWindows() {
    for scene in UIApplication.shared.connectedScenes {
      guard let windowScene = scene as? UIWindowScene else { continue }
      for window in windowScene.windows {
        removePrivacyOverlay(from: window)
      }
    }
  }
}
