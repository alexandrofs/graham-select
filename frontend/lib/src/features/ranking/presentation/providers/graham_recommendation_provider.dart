import 'dart:async';
import 'package:flutter/foundation.dart';
import '../../../portfolio/data/datasources/notification_service.dart';
import '../../domain/entities/graham_recommendation.dart';
import '../../domain/repositories/graham_recommendation_repository.dart';

enum RecommendationState { idle, loading, success, error }

class GrahamRecommendationProvider extends ChangeNotifier {
  final GrahamRecommendationRepository repository;
  final NotificationService? notificationService;

  RecommendationState _state = RecommendationState.idle;
  List<GrahamRecommendation> _recommendations = [];
  String? _errorMessage;
  bool _isTriggering = false;
  StreamSubscription? _notificationSubscription;
  String? _activeTagId;
  int _fetchGeneration = 0;

  GrahamRecommendationProvider({
    required this.repository,
    this.notificationService,
  });

  RecommendationState get state => _state;
  List<GrahamRecommendation> get recommendations => _recommendations;
  String? get errorMessage => _errorMessage;
  bool get isTriggering => _isTriggering;
  String? get activeTagId => _activeTagId;

  String? _normalizeTagId(String? tagId) {
    final trimmed = tagId?.trim();
    if (trimmed == null || trimmed.isEmpty) return null;
    return trimmed;
  }

  Future<void> fetchRecommendations({String? tagId}) async {
    final normalizedTagId = _normalizeTagId(tagId);
    final generation = ++_fetchGeneration;
    if (_activeTagId != normalizedTagId) {
      _recommendations = [];
      _excludedTickers = [];
    }
    _activeTagId = normalizedTagId;
    _setState(RecommendationState.loading);
    _errorMessage = null;

    try {
      final result = await repository.getRecommendations(tagId: normalizedTagId);
      if (generation != _fetchGeneration) return;
      _recommendations = result;
      _setState(RecommendationState.success);
    } catch (e) {
      if (generation != _fetchGeneration) return;
      _errorMessage = e.toString();
      _setState(RecommendationState.error);
    }
  }
List<dynamic> _excludedTickers = [];
List<dynamic> get excludedTickers => _excludedTickers;

Future<void> triggerCalculation(String currentUserId, {String? tagId}) async {
  if (_isTriggering) return;

  final targetTagId = _normalizeTagId(tagId);
  _activeTagId = targetTagId;
  _isTriggering = true;
  _errorMessage = null;
  _excludedTickers = [];
  notifyListeners();

  try {
    final completer = Completer<void>();

    // 1. Subscreve ANTES de disparar o trigger (Decision 3)
    if (notificationService != null && notificationService!.isConnected) {
      await _notificationSubscription?.cancel();
      _notificationSubscription = notificationService!.notificationStream.listen((event) {
        // 2. Filtra por userId (Achado #5) - Comparação insensível a maiúsculas/minúsculas
        if (event.userId.trim().toLowerCase() != currentUserId.trim().toLowerCase()) return;

        // Correlaciona pelo escopo da tag (GLOBAL omite tagId; visão por tag traz tagId exato)
        final eventTagId = _normalizeTagId(event.payload?['tagId']?.toString());
        if (eventTagId != targetTagId) return;

        if (event.event == 'VALUATION_COMPLETED') {
          _excludedTickers = event.payload?['excludedTickers'] ?? [];
          if (!completer.isCompleted) completer.complete();
        } else if (event.event == 'VALUATION_FAILED') {
          // 5. Extrai mensagem do payload (Acceptance Auditor finding)
          final msg = event.payload?['message'] ?? 'O recálculo do valuation falhou. Tente novamente mais tarde.';
          if (!completer.isCompleted) completer.completeError(msg);
        }
      });
    }

    // 2. Dispara o cálculo
    await repository.triggerCalculation(tagId: targetTagId);

    if (notificationService != null && notificationService!.isConnected) {
      // 3. Aguarda evento via SSE com timeout de 30s
      await completer.future.timeout(
        const Duration(seconds: 30),
        onTimeout: () {
          _notificationSubscription?.cancel();
          throw TimeoutException('O processamento está demorando mais que o esperado. As recomendações serão atualizadas automaticamente em breve.');
        },
      );
    } else {
      // 4. Fallback caso SSE não esteja disponível (Achado #6)
      await Future.delayed(const Duration(seconds: 3)); // Fix to 3s
    }

    if (_activeTagId == targetTagId) {
      await fetchRecommendations(tagId: targetTagId);
    }
  } catch (e) {
    if (_activeTagId == targetTagId) {
      _errorMessage = e.toString().replaceFirst('Exception: ', '');
      _setState(RecommendationState.error);
    }
  } finally {
    _isTriggering = false;
    _notificationSubscription?.cancel();
    _notificationSubscription = null;
    notifyListeners();
  }
}

  void reset() {
    _state = RecommendationState.idle;
    _recommendations = [];
    _errorMessage = null;
    _isTriggering = false;
    _activeTagId = null;
    _excludedTickers = [];
    _notificationSubscription?.cancel();
    _notificationSubscription = null;
    notifyListeners();
  }
 
  void _setState(RecommendationState newState) {
    _state = newState;
    notifyListeners();
  }

  @override
  void dispose() {
    _notificationSubscription?.cancel();
    super.dispose();
  }
}
