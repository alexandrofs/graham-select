import Flutter
import UIKit

class SceneDelegate: FlutterSceneDelegate {
  override func sceneWillResignActive(_ scene: UIScene) {
    super.sceneWillResignActive(scene)
    if AppDelegate.isSnapshotProtectionEnabled,
       let windowScene = scene as? UIWindowScene {
      let targetWindow = windowScene.windows.first(where: { $0.isKeyWindow }) ?? windowScene.windows.first
      AppDelegate.showPrivacyOverlay(on: targetWindow)
    }
  }

  override func sceneDidBecomeActive(_ scene: UIScene) {
    super.sceneDidBecomeActive(scene)
    if let windowScene = scene as? UIWindowScene {
      for window in windowScene.windows {
        AppDelegate.removePrivacyOverlay(from: window)
      }
    }
  }
}
