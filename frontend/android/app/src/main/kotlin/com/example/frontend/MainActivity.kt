package com.example.frontend

import android.view.WindowManager
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterFragmentActivity() {
  companion object {
    private const val SNAPSHOT_CHANNEL = "kerno/snapshot_protection"
  }

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, SNAPSHOT_CHANNEL)
      .setMethodCallHandler { call, result ->
        when (call.method) {
          "enableProtection" -> {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            result.success(null)
          }
          "disableProtection" -> {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            result.success(null)
          }
          else -> result.notImplemented()
        }
      }
  }
}
