import 'web_push_browser_client.dart';

WebPushBrowserClient createWebPushBrowserClient() => WebPushBrowserClientStub();

class WebPushBrowserClientStub implements WebPushBrowserClient {
  @override
  bool get isSupported => false;

  @override
  Future<String> getPermissionState() async {
    return 'unsupported';
  }

  @override
  Future<WebPushSubscriptionPayload?> subscribeToPush(String vapidPublicKeyBase64Url) async {
    return null;
  }
}
