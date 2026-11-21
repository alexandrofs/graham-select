import 'web_push_browser_client_stub.dart'
    if (dart.library.js_interop) 'web_push_browser_client_web.dart';

class WebPushSubscriptionPayload {
  final String endpoint;
  final String p256dhKey;
  final String authKey;
  final String? userAgent;

  const WebPushSubscriptionPayload({
    required this.endpoint,
    required this.p256dhKey,
    required this.authKey,
    this.userAgent,
  });
}

abstract class WebPushBrowserClient {
  factory WebPushBrowserClient() => createWebPushBrowserClient();

  bool get isSupported;
  Future<String> getPermissionState();
  Future<WebPushSubscriptionPayload?> subscribeToPush(String vapidPublicKeyBase64Url);
}
