import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/security/privacy_provider.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';
import 'package:frontend/src/features/portfolio/domain/entities/custody_position.dart';
import 'package:frontend/src/features/portfolio/domain/entities/monthly_evolution.dart';
import 'package:frontend/src/features/portfolio/domain/entities/portfolio_summary.dart';
import 'package:frontend/src/features/portfolio/domain/entities/trade.dart';
import 'package:frontend/src/features/portfolio/domain/repositories/portfolio_repository.dart';
import 'package:frontend/src/features/portfolio/presentation/providers/portfolio_provider.dart';
import 'package:frontend/src/features/portfolio/presentation/widgets/b3_ingestion_crud_sheet.dart';
import 'package:frontend/src/features/upload/domain/entities/app_file.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_processing_status.dart';
import 'package:frontend/src/features/upload/domain/entities/upload_result.dart';
import 'package:frontend/src/features/upload/domain/repositories/upload_repository.dart';
import 'package:frontend/src/features/upload/domain/usecases/get_b3_upload_status_usecase.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_b3_file_usecase.dart';
import 'package:frontend/src/features/upload/domain/usecases/upload_file_usecase.dart';
import 'package:frontend/src/features/upload/presentation/providers/upload_provider.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

class _FakePortfolioRepository implements PortfolioRepository {
  final List<Trade> trades = [
    Trade(
      id: 't-1',
      userId: 'u-1',
      ticker: 'IVVB11',
      side: 'COMPRA',
      tradeDate: DateTime(2026, 5, 10),
      quantity: 10,
      price: 300,
      broker: 'Manual',
      assetClass: 'Internacional',
      createdAt: DateTime(2026, 5, 10),
    ),
  ];

  bool shouldFailCreate = false;

  @override
  Future<List<Trade>> getManualTrades() async => List.of(trades);

  @override
  Future<Trade> createManualTrade({
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
    String? assetClass,
    String? profileId,
  }) async {
    if (shouldFailCreate) {
      throw Exception('Operação duplicada detectada.');
    }
    final created = Trade(
      id: 't-${trades.length + 1}',
      userId: 'u-1',
      profileId: profileId,
      ticker: ticker,
      side: side,
      tradeDate: tradeDate,
      quantity: quantity,
      price: price,
      broker: broker,
      assetClass: assetClass,
      createdAt: DateTime.now(),
    );
    trades.add(created);
    return created;
  }

  @override
  Future<Trade> updateManualTrade({
    required String id,
    required String ticker,
    required String side,
    required DateTime tradeDate,
    required double quantity,
    required double price,
    required String broker,
    String? assetClass,
    String? profileId,
  }) async {
    final idx = trades.indexWhere((t) => t.id == id);
    final updated = Trade(
      id: id,
      userId: 'u-1',
      profileId: profileId,
      ticker: ticker,
      side: side,
      tradeDate: tradeDate,
      quantity: quantity,
      price: price,
      broker: broker,
      assetClass: assetClass,
      createdAt: trades[idx].createdAt,
    );
    trades[idx] = updated;
    return updated;
  }

  @override
  Future<void> deleteManualTrade(String id) async {
    trades.removeWhere((t) => t.id == id);
  }

  @override
  Future<PortfolioSummary> getSummary({String? tagId}) async => PortfolioSummary(
        totalEquity: 3000,
        grossYieldPercentage: 5,
        accumulatedDividends: 0,
        monthlyProjection: 0,
      );

  @override
  Future<({
    List<CustodyPosition> positions,
    DateTime? metaPriceUpdatedAt,
    int untaggedAssetsCount,
    List<String> untaggedTickers,
  })> getCustodyPositions({String? tagId}) async => (
        positions: <CustodyPosition>[],
        metaPriceUpdatedAt: null,
        untaggedAssetsCount: 0,
        untaggedTickers: const <String>[],
      );

  @override
  Future<PortfolioEvolution> getPortfolioEvolution({String? tagId}) async =>
      const PortfolioEvolution(monthlyData: []);
}

class _FakeUploadRepository implements UploadRepository {
  @override
  Future<UploadResult> uploadFile(AppFile file) async =>
      UploadResult(success: true, message: 'OK');

  @override
  Future<UploadResult> uploadB3File(AppFile file, {String? profileId}) async =>
      UploadResult(success: true, message: 'OK', correlationId: 'corr-1');

  @override
  Future<UploadResult> uploadBrokerageNoteFile(AppFile file, {String? profileId}) async =>
      UploadResult(success: true, message: 'OK', correlationId: 'corr-1');

  @override
  Future<UploadProcessingStatus> fetchB3UploadStatus(String correlationId) async =>
      UploadProcessingStatus(
        correlationId: correlationId,
        fileName: 'b3.xlsx',
        status: 'COMPLETED',
        processedRows: 1,
        successfulRows: 1,
        failedRows: 0,
        duplicatedRows: 0,
        message: 'Concluído',
      );
}

Widget _buildTestHost({
  required PortfolioProvider portfolioProvider,
  required UploadProvider uploadProvider,
  required PrivacyProvider privacyProvider,
  Size size = const Size(1280, 800),
}) {
  return MultiProvider(
    providers: [
      ChangeNotifierProvider<PortfolioProvider>.value(value: portfolioProvider),
      ChangeNotifierProvider<UploadProvider>.value(value: uploadProvider),
      ChangeNotifierProvider<PrivacyProvider>.value(value: privacyProvider),
    ],
    child: MaterialApp(
      theme: ThemeData.light().copyWith(
        extensions: const <ThemeExtension<dynamic>>[KernoThemeData.light],
      ),
      home: MediaQuery(
        data: MediaQueryData(size: size),
        child: Builder(
          builder: (context) => Scaffold(
            body: Center(
              child: ElevatedButton(
                onPressed: () => B3IngestionCrudSheet.show(context),
                child: const Text('Abrir'),
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

void main() {
  setUpAll(() async {
    await initializeDateFormatting('pt_BR', null);
  });

  testWidgets('Exibe todas as 5 Macro Classes e mascara valores com PrivacyProvider', (tester) async {
    final repo = _FakePortfolioRepository();
    final portfolioProvider = PortfolioProvider(repo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    final privacyProvider = PrivacyProvider()..setPrivacyMode(true);

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();

    // Troca para aba Lançamento Manual
    await tester.tap(find.text('Lançamento Manual'));
    await tester.pumpAndSettle();

    for (final macroClass in kCanonicalMacroClasses) {
      expect(find.widgetWithText(ChoiceChip, macroClass), findsOneWidget);
    }

    // Trade listado com máscara R$ •••••
    expect(find.text('IVVB11'), findsOneWidget);
    expect(find.textContaining('R\$ •••••'), findsOneWidget);

    // Fecha com Esc no Web Drawer (480px)
    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();
    expect(find.text('Atualizar Carteira'), findsNothing);
  });

  testWidgets('Rejeita .csv inline com banner crimsonSurface sem fechar o sheet', (tester) async {
    final repo = _FakePortfolioRepository();
    final portfolioProvider = PortfolioProvider(repo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    uploadProvider.setFile(
      AppFile(name: 'extrato.csv', bytes: Uint8List.fromList([1, 2, 3]), size: 3),
    );
    final privacyProvider = PrivacyProvider();

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Enviar para Processamento'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Formato não suportado'), findsOneWidget);
    expect(find.text('Atualizar Carteira'), findsOneWidget);
  });

  testWidgets('Cria posição contínua em Renda Fixa / Tesouro em BRL', (tester) async {
    final repo = _FakePortfolioRepository();
    final portfolioProvider = PortfolioProvider(repo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    final privacyProvider = PrivacyProvider();

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lançamento Manual'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Renda Fixa / Tesouro'));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextFormField, 'Ativo / Ticker (ex: TESOURO_SELIC, BTC)'),
      'TESOURO_SELIC_2029',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Valor / Preço (R\$)'),
      '15000,50',
    );

    await tester.ensureVisible(find.text('Salvar Lançamento'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Salvar Lançamento'));
    await tester.pumpAndSettle();

    expect(repo.trades.any((t) => t.ticker == 'TESOURO_SELIC_2029'), isTrue);
  });

  testWidgets('Valida cotas inteiras para Ações BR e IVVB11', (tester) async {
    final repo = _FakePortfolioRepository();
    final portfolioProvider = PortfolioProvider(repo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    final privacyProvider = PrivacyProvider();

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lançamento Manual'));
    await tester.pumpAndSettle();

    await tester.enterText(
      find.widgetWithText(TextFormField, 'Ticker (ex: PETR4, HGLG11, IVVB11)'),
      'PETR4',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Quantidade'),
      '10,5',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Preço Unitário (R\$)'),
      '35,00',
    );

    await tester.ensureVisible(find.text('Salvar Lançamento'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Salvar Lançamento'));
    await tester.pumpAndSettle();

    expect(find.text('Cotas inteiras'), findsOneWidget);
  });

  testWidgets('Edita e exclui lançamento preservando limpeza do modo de edição', (tester) async {
    final repo = _FakePortfolioRepository();
    final portfolioProvider = PortfolioProvider(repo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    final privacyProvider = PrivacyProvider();

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lançamento Manual'));
    await tester.pumpAndSettle();

    // Inicia edição de IVVB11
    await tester.ensureVisible(find.byTooltip('Editar lançamento').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Editar lançamento').first);
    await tester.pumpAndSettle();

    expect(find.text('Atualizar Lançamento'), findsOneWidget);

    // Exclui o mesmo lançamento em edição -> deve limpar modo de edição
    await tester.ensureVisible(find.byTooltip('Excluir lançamento').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Excluir lançamento').first);
    await tester.pumpAndSettle();

    expect(find.text('Salvar Lançamento'), findsOneWidget);
    expect(repo.trades, isEmpty);
  });

  testWidgets('Exibe banner crimsonSurface quando manualTradesError está presente', (tester) async {
    final failingRepo = _FailingManualTradesRepository();
    final portfolioProvider = PortfolioProvider(failingRepo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    final privacyProvider = PrivacyProvider();

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lançamento Manual'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Falha ao carregar lançamentos'), findsOneWidget);
  });

  testWidgets('Dispara refresh do PortfolioProvider após upload B3 com sucesso', (tester) async {
    final repo = _FakePortfolioRepository();
    final portfolioProvider = PortfolioProvider(repo);
    final uploadRepo = _FakeUploadRepository();
    final uploadProvider = UploadProvider(
      uploadFileUseCase: UploadFileUseCase(uploadRepo),
      uploadB3FileUseCase: UploadB3FileUseCase(uploadRepo),
      getB3UploadStatusUseCase: GetB3UploadStatusUseCase(uploadRepo),
    );
    uploadProvider.setFile(
      AppFile(name: 'negociacao.xlsx', bytes: Uint8List.fromList([1, 2, 3]), size: 3),
    );
    final privacyProvider = PrivacyProvider();

    await tester.pumpWidget(_buildTestHost(
      portfolioProvider: portfolioProvider,
      uploadProvider: uploadProvider,
      privacyProvider: privacyProvider,
    ));

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Enviar para Processamento'));
    await tester.pumpAndSettle();

    expect(uploadProvider.state, equals(UploadState.success));
    expect(portfolioProvider.status, equals(PortfolioStatus.success));
    uploadProvider.dispose();
  });
}

class _FailingManualTradesRepository extends _FakePortfolioRepository {
  @override
  Future<List<Trade>> getManualTrades() async {
    throw Exception('Falha ao carregar lançamentos');
  }
}
