import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../../domain/entities/portfolio_notification_event.dart';

class NotificationService {
  final ApiClient _apiClient;
  StreamController<PortfolioNotificationEvent>? _controller;
  StreamSubscription? _subscription;
  bool _isConnected = false;
  int _reconnectDelay = 2; // Inicia com 2 segundos
  bool _isDisposed = false;

  NotificationService(this._apiClient);

  bool get isConnected => _isConnected;

  Stream<PortfolioNotificationEvent> get notificationStream {
    _controller ??= StreamController<PortfolioNotificationEvent>.broadcast();
    return _controller!.stream;
  }

  String _buffer = '';

  Future<void> connect() async {
    if (_isDisposed) return;
    
    // Cancela inscrição anterior se houver
    await _subscription?.cancel();
    _subscription = null;
    _buffer = '';

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

      _subscription = response.data?.stream
          .cast<List<int>>()
          .transform(utf8.decoder)
          .listen(
        (text) {
          _buffer += text;
          
          // Processa eventos completos separados por duas quebras de linha
          while (_buffer.contains('\n\n')) {
            final eventEnd = _buffer.indexOf('\n\n');
            final eventBlock = _buffer.substring(0, eventEnd);
            _buffer = _buffer.substring(eventEnd + 2);
            
            _processEventBlock(eventBlock);
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

  void _processEventBlock(String block) {
    final lines = block.split('\n');
    String? currentEvent;
    
    for (final line in lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        final dataJson = line.substring(5).trim();
        try {
          final parsed = jsonDecode(dataJson);
          final userId = parsed['userId'] as String? ?? '';
          
          if (currentEvent != null) {
            _controller?.add(PortfolioNotificationEvent(
              userId: userId,
              event: currentEvent,
              timestamp: DateTime.now(),
              payload: parsed,
            ));
          }
        } catch (_) {
          // Log or ignore malformed JSON
        }
      }
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
