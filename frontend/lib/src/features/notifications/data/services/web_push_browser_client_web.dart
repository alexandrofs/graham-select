import 'dart:convert';
import 'dart:js_interop';
import 'dart:js_interop_unsafe';
import 'dart:typed_data';
import 'web_push_browser_client.dart';

WebPushBrowserClient createWebPushBrowserClient() => WebPushBrowserClientWeb();

@JS('Notification.permission')
external JSString? get _notificationPermission;

@JS('Notification.requestPermission')
external JSPromise<JSString> _requestNotificationPermission();

@JS('navigator.userAgent')
external JSString? get _navigatorUserAgent;

class WebPushBrowserClientWeb implements WebPushBrowserClient {
  @override
  bool get isSupported {
    try {
      final hasSw = globalContext.has('navigator') &&
          (globalContext.getProperty<JSObject>('navigator'.toJS)).has('serviceWorker');
      final hasNotification = globalContext.has('Notification');
      return hasSw && hasNotification;
    } catch (_) {
      return false;
    }
  }

  @override
  Future<String> getPermissionState() async {
    if (!isSupported) return 'unsupported';
    try {
      final perm = _notificationPermission?.toDart;
      return perm ?? 'default';
    } catch (_) {
      return 'unsupported';
    }
  }

  @override
  Future<WebPushSubscriptionPayload?> subscribeToPush(String vapidPublicKeyBase64Url) async {
    if (!isSupported) return null;
    try {
      final permPromise = _requestNotificationPermission();
      final permJs = await permPromise.toDart;
      final perm = permJs.toDart;
      if (perm != 'granted') {
        return null;
      }

      // Converte Base64URL VAPID key para Uint8List
      final convertedKey = _urlBase64ToUint8Array(vapidPublicKeyBase64Url);

      final nav = globalContext.getProperty<JSObject>('navigator'.toJS);
      final sw = nav.getProperty<JSObject>('serviceWorker'.toJS);

      // Registrar push-sw.js com escopo /push/
      final regPromise = sw.callMethod<JSPromise<JSObject>>(
        'register'.toJS,
        '/push-sw.js'.toJS,
        {'scope': '/push/'}.jsify()!,
      );
      final registration = await regPromise.toDart;

      final pushManager = registration.getProperty<JSObject>('pushManager'.toJS);

      // pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: convertedKey })
      final subscribePromise = pushManager.callMethod<JSPromise<JSObject>>(
        'subscribe'.toJS,
        {
          'userVisibleOnly': true,
          'applicationServerKey': convertedKey.toJS,
        }.jsify()!,
      );
      final subscription = await subscribePromise.toDart;

      final endpoint = subscription.getProperty<JSString>('endpoint'.toJS).toDart;

      // sub.getKey('p256dh') e sub.getKey('auth')
      final p256dhBuffer = subscription.callMethod<JSArrayBuffer?>('getKey'.toJS, 'p256dh'.toJS);
      final authBuffer = subscription.callMethod<JSArrayBuffer?>('getKey'.toJS, 'auth'.toJS);

      if (p256dhBuffer == null || authBuffer == null) {
        return null;
      }

      final p256dhBytes = p256dhBuffer.toDart.asUint8List();
      final authBytes = authBuffer.toDart.asUint8List();

      final p256dhBase64 = base64UrlEncode(p256dhBytes).replaceAll('=', '');
      final authBase64 = base64UrlEncode(authBytes).replaceAll('=', '');

      final userAgent = _navigatorUserAgent?.toDart;

      return WebPushSubscriptionPayload(
        endpoint: endpoint,
        p256dhKey: p256dhBase64,
        authKey: authBase64,
        userAgent: userAgent,
      );
    } catch (e) {
      return null;
    }
  }

  Uint8List _urlBase64ToUint8Array(String base64String) {
    String padding = '=' * ((4 - base64String.length % 4) % 4);
    String base64 = (base64String + padding)
        .replaceAll('-', '+')
        .replaceAll('_', '/');
    return Uint8List.fromList(base64Decode(base64));
  }
}
