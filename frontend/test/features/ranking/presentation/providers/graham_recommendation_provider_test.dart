import 'dart:async';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/portfolio/data/datasources/notification_service.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_notification_event.dart';
import 'package:frontend/src/features/ranking/domain/entities/graham_recommendation.dart';
import 'package:frontend/src/features/ranking/domain/repositories/graham_recommendation_repository.dart';
import 'package:frontend/src/features/ranking/presentation/providers/graham_recommendation_provider.dart';

class FakeNotificationService implements NotificationService {
  final StreamController<PortfolioNotificationEvent> _controller =
      StreamController<PortfolioNotificationEvent>.broadcast();

  @override
  bool get isConnected => true;

  @override
  Stream<PortfolioNotificationEvent> get notificationStream => _controller.stream;

  @override
  Future<void> connect() async {}

  @override
  void dispose() {
    _controller.close();
  }

  void emit(PortfolioNotificationEvent event) => _controller.add(event);
}

class FakeGrahamRepository implements GrahamRecommendationRepository {
  Future<List<GrahamRecommendation>> Function(String? tagId)? fetchHandler;
  final List<String?> fetchedTags = [];
  final List<String?> triggeredTags = [];
  Future<void> Function(String? tagId)? onTrigger;

  @override
  Future<List<GrahamRecommendation>> getRecommendations({String? tagId}) async {
    fetchedTags.add(tagId);
    if (fetchHandler != null) return fetchHandler!(tagId);
    return const [];
  }

  @override
  Future<void> triggerCalculation({String? tagId}) async {
    triggeredTags.add(tagId);
    if (onTrigger != null) await onTrigger!(tagId);
  }
}

void main() {
  late FakeGrahamRepository repo;
  late FakeNotificationService sse;
  late GrahamRecommendationProvider provider;

  setUp(() {
    repo = FakeGrahamRepository();
    sse = FakeNotificationService();
    provider = GrahamRecommendationProvider(repository: repo, notificationService: sse);
  });

  tearDown(() {
    provider.dispose();
  });

  test('deve isolar fetch por tagId e ignorar respostas obsoletas', () async {
    final slowTagA = Completer<List<GrahamRecommendation>>();
    repo.fetchHandler = (tagId) {
      if (tagId == 'tag-a') return slowTagA.future;
      return Future.value(const [
        GrahamRecommendation(
          ticker: 'BBAS3',
          currentPrice: 25.0,
          intrinsicValue: 40.0,
          marginOfSafety: 0.6,
          currentAllocationPct: 5.0,
          targetAllocationPct: 20.0,
          allocationGap: 15.0,
          recommendationScore: 5.0,
        ),
      ]);
    };

    final futureA = provider.fetchRecommendations(tagId: 'tag-a');
    final futureB = provider.fetchRecommendations(tagId: 'tag-b');
    await futureB;

    expect(provider.activeTagId, 'tag-b');
    expect(provider.recommendations.single.ticker, 'BBAS3');

    slowTagA.complete(const [
      GrahamRecommendation(
        ticker: 'OBSOLETO',
        currentPrice: 10.0,
        intrinsicValue: 20.0,
        marginOfSafety: 1.0,
        currentAllocationPct: 1.0,
        targetAllocationPct: 10.0,
        allocationGap: 9.0,
        recommendationScore: 2.0,
      ),
    ]);
    await futureA;

    expect(provider.activeTagId, 'tag-b');
    expect(provider.recommendations.single.ticker, 'BBAS3');
  });

  test('triggerCalculation deve correlacionar SSE pelo mesmo userId e tagId ignorando outros escopos', () async {
    repo.onTrigger = (tagId) async {
      scheduleMicrotask(() {
        // Evento de outra tag deve ser ignorado
        sse.emit(PortfolioNotificationEvent(
          userId: 'user-1',
          event: 'VALUATION_COMPLETED',
          timestamp: DateTime.utc(2026, 11, 14),
          payload: const {'userId': 'user-1', 'event': 'VALUATION_COMPLETED', 'tagId': 'other-tag'},
        ));
        // Evento global também deve ser ignorado quando trigger foi para tag-1
        sse.emit(PortfolioNotificationEvent(
          userId: 'user-1',
          event: 'VALUATION_COMPLETED',
          timestamp: DateTime.utc(2026, 11, 14),
          payload: const {'userId': 'user-1', 'event': 'VALUATION_COMPLETED'},
        ));
        // Evento correspondente ao tag-1 deve completar o fluxo
        sse.emit(PortfolioNotificationEvent(
          userId: 'user-1',
          event: 'VALUATION_COMPLETED',
          timestamp: DateTime.utc(2026, 11, 14),
          payload: {
            'userId': 'user-1',
            'event': 'VALUATION_COMPLETED',
            'tagId': 'tag-1',
            'excludedTickers': [
              {'ticker': 'MGLU3', 'reason': 'STALE', 'detail': 'old'}
            ],
          },
        ));
      });
    };

    repo.fetchHandler = (tagId) async => const [
          GrahamRecommendation(
            ticker: 'VALE3',
            currentPrice: 70.0,
            intrinsicValue: 95.0,
            marginOfSafety: 0.35,
            currentAllocationPct: 10.0,
            targetAllocationPct: 25.0,
            allocationGap: 15.0,
            recommendationScore: 4.1,
          ),
        ];

    await provider.triggerCalculation('user-1', tagId: 'tag-1');

    expect(repo.triggeredTags, ['tag-1']);
    expect(repo.fetchedTags, ['tag-1']);
    expect(provider.state, RecommendationState.success);
    expect(provider.recommendations.single.ticker, 'VALE3');
    expect(provider.excludedTickers.length, 1);
  });
}
