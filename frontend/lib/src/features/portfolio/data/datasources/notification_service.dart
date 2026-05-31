import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../../domain/entities/portfolio_notification_event.dart';

class NotificationService {
  final ApiClient _apiClient;
  StreamController<PortfolioNotificationEvent>? _controller;
  StreamSubscription<List<int>>? _subscription;
  bool _isConnected = false;
  int _reconnectDelay = 2; // Inicia com 2 segundos
  bool _isDisposed = false;

  NotificationService(this._apiClient);

  bool get isConnected => _isConnected;

  Stream<PortfolioNotificationEvent> get notificationStream {
    _controller ??= StreamController<PortfolioNotificationEvent>.broadcast();
    return _controller!.stream;
  }

  Future<void> connect() async {
    if (_isDisposed) return;
    
    // Cancela inscrição anterior se houver
    await _subscription?.cancel();
    _subscription = null;

    try {
      final response = await _apiClient.dio.get<ResponseBody>(
        '/api/v1/notifications/stream',
        options: Options(
          responseType: ResponseType.stream,
          headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
          },
          // Sem timeout para streams
          receiveTimeout: Duration.zero,
        ),
      );

      _isConnected = true;
      _reconnectDelay = 2; // Reseta o delay de reconexão ao conectar com sucesso

      _subscription = response.data?.stream.listen(
        (data) {
          final text = utf8.decode(data);
          // O formato SSE do Spring SseEmitter envia eventos linha por linha:
          // event:PORTFOLIO_UPDATED
          // data:{"userId":"...","timestamp":"..."}
          // Vamos verificar se a mensagem contém PORTFOLIO_UPDATED
          if (text.contains('PORTFOLIO_UPDATED')) {
            String userId = '';
            try {
              final lines = text.split('\n');
              for (final line in lines) {
                if (line.startsWith('data:')) {
                  final dataJson = line.substring(5).trim();
                  final parsed = jsonDecode(dataJson);
                  userId = parsed['userId'] as String? ?? '';
                  break;
                }
              }
            } catch (_) {
              // Fallback
            }

            _controller?.add(PortfolioNotificationEvent(
              userId: userId,
              event: 'PORTFOLIO_UPDATED',
              timestamp: DateTime.now(),
            ));
          }
        },
        onError: (err) {
          _isConnected = false;
          _handleReconnect();
        },
        onDone: () {
          _isConnected = false;
          _handleReconnect();
        },
      );
    } catch (_) {
      _isConnected = false;
      _handleReconnect();
    }
  }

  void _handleReconnect() {
    if (_isDisposed) return;
    _subscription?.cancel();
    _subscription = null;
    
    // Reconexão com backoff exponencial com teto de 30s
    Future.delayed(Duration(seconds: _reconnectDelay), () {
      if (!_isConnected && !_isDisposed) {
        connect();
      }
    });

    _reconnectDelay = (_reconnectDelay * 2).clamp(2, 30);
  }

  void dispose() {
    _isDisposed = true;
    _isConnected = false;
    _subscription?.cancel();
    _subscription = null;
    _controller?.close();
    _controller = null;
  }
}
