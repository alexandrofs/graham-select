import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/core/theme/kerno_accessibility.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_macro_class.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_preferences.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_template.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/repositories/cerrado_diagram_repository.dart';
import 'package:frontend/src/features/cerrado_diagram/presentation/pages/cerrado_diagram_page.dart';
import 'package:frontend/src/features/cerrado_diagram/presentation/providers/cerrado_diagram_provider.dart';
import 'package:provider/provider.dart';

class _StubCerradoRepository implements CerradoDiagramRepository {
  Future<CerradoTemplatesCatalog> Function()? onGetTemplates;
  Future<CerradoPreferences?> Function()? onGetPreferences;
  Future<CerradoPreferences> Function({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  })? onSavePreferences;

  @override
  Future<CerradoTemplatesCatalog> getTemplates() => onGetTemplates!();

  @override
  Future<CerradoPreferences?> getPreferences() => onGetPreferences!();

  @override
  Future<CerradoPreferences> savePreferences({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  }) {
    return onSavePreferences!(
      targetsBps: targetsBps,
      grahamMultiplier: grahamMultiplier,
      grahamFilterEnabled: grahamFilterEnabled,
    );
  }
}

CerradoTemplatesCatalog _catalogFixture() {
  return const CerradoTemplatesCatalog(
    defaultNeutralGrade: 6,
    cvmDisclaimer:
        'Aviso CVM: conteúdo informativo sem recomendação de investimento.',
    templates: <CerradoTemplate>[
      CerradoTemplate(
        code: 'EQUILIBRADO_CERRADO',
        name: 'Modelo Equilibrado Cerrado',
        description: 'Carteira diversificada com 1% em cripto.',
        targetAcoesBrBps: 3000,
        targetFiisBps: 2500,
        targetRendaFixaBps: 2400,
        targetInternacionalBps: 2000,
        targetCriptoBps: 100,
      ),
      CerradoTemplate(
        code: 'MODERADO_PATRIMONIAL',
        name: 'Modelo Moderado Patrimonial',
        description: 'Foco defensivo com 45% em renda fixa.',
        targetAcoesBrBps: 2000,
        targetFiisBps: 2000,
        targetRendaFixaBps: 4500,
        targetInternacionalBps: 1500,
        targetCriptoBps: 0,
      ),
      CerradoTemplate(
        code: 'ACUMULACAO_ARROJADA',
        name: 'Modelo Acumulação Arrojada',
        description: 'Maior exposição a renda variável e 5% em cripto.',
        targetAcoesBrBps: 3500,
        targetFiisBps: 2000,
        targetRendaFixaBps: 1500,
        targetInternacionalBps: 2500,
        targetCriptoBps: 500,
      ),
    ],
  );
}

Widget _wrapPage({
  required CerradoDiagramProvider provider,
  ThemeData? theme,
  double textScale = 1.0,
}) {
  final effectiveTheme = theme ?? AppTheme.lightTheme;
  return ChangeNotifierProvider<CerradoDiagramProvider>.value(
    value: provider,
    child: MaterialApp(
      key: ValueKey<Brightness>(effectiveTheme.brightness),
      theme: effectiveTheme,
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: TextScaler.linear(textScale),
          ),
          child: child!,
        );
      },
      home: const CerradoDiagramPage(),
    ),
  );
}

void main() {
  testWidgets(
      'onboarding 404 exibe 3 templates sem seleção, trava carmesim e disclaimer CVM',
      (tester) async {
    final repo = _StubCerradoRepository();
    repo.onGetTemplates = () async => _catalogFixture();
    repo.onGetPreferences = () async => null;
    final provider = CerradoDiagramProvider(repository: repo);
    addTearDown(provider.dispose);

    await tester.pumpWidget(_wrapPage(provider: provider));
    await tester.pumpAndSettle();

    expect(find.text('Modelo Equilibrado Cerrado'), findsOneWidget);
    expect(find.text('Modelo Moderado Patrimonial'), findsOneWidget);
    expect(find.text('Modelo Acumulação Arrojada'), findsOneWidget);
    expect(find.text('Selecionado'), findsNothing);
    expect(find.text('Faltam 100,00%'), findsOneWidget);
    expect(
      find.text('Aviso CVM: conteúdo informativo sem recomendação de investimento.'),
      findsOneWidget,
    );

    final saveButton = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Salvar Metas'),
    );
    expect(saveButton.onPressed, isNull);
  });

  testWidgets(
      'selecionar template preenche 100,00%, exibe badge nota 6 e edição manual valida diferenças',
      (tester) async {
    final repo = _StubCerradoRepository();
    repo.onGetTemplates = () async => _catalogFixture();
    repo.onGetPreferences = () async => null;
    final provider = CerradoDiagramProvider(repository: repo);
    addTearDown(provider.dispose);

    await tester.pumpWidget(_wrapPage(provider: provider));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Modelo Equilibrado Cerrado'));
    await tester.pumpAndSettle();

    expect(find.text('Selecionado'), findsOneWidget);
    expect(
      find.text('Notas iniciais neutras = 6 (Editáveis)'),
      findsOneWidget,
    );
    expect(find.text('Soma exata: 100,00%'), findsOneWidget);

    expect(
      tester
          .widget<TextField>(
            find.byKey(const Key('cerrado_input_targetAcoesBr')),
          )
          .controller!
          .text,
      '30,00',
    );
    expect(
      tester
          .widget<TextField>(find.byKey(const Key('cerrado_input_targetFiis')))
          .controller!
          .text,
      '25,00',
    );
    expect(
      tester
          .widget<TextField>(
            find.byKey(const Key('cerrado_input_targetRendaFixa')),
          )
          .controller!
          .text,
      '24,00',
    );
    expect(
      tester
          .widget<TextField>(
            find.byKey(const Key('cerrado_input_targetInternacional')),
          )
          .controller!
          .text,
      '20,00',
    );
    expect(
      tester
          .widget<TextField>(
            find.byKey(const Key('cerrado_input_targetCripto')),
          )
          .controller!
          .text,
      '1,00',
    );

    final saveEnabled = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Salvar Metas'),
    );
    expect(saveEnabled.onPressed, isNotNull);

    // Edição manual para entrada inválida (> 2 casas decimais)
    await tester.enterText(
      find.byKey(const Key('cerrado_input_targetAcoesBr')),
      '30,123',
    );
    await tester.pumpAndSettle();

    expect(
      find.text('Entre 0,00 e 100,00 (máx. 2 decimais)'),
      findsOneWidget,
    );
    expect(
      find.text('Valor inválido — use até 2 casas decimais (0,00 a 100,00)'),
      findsOneWidget,
    );

    // Edição manual para 98,00%
    await tester.enterText(
      find.byKey(const Key('cerrado_input_targetAcoesBr')),
      '28,00',
    );
    await tester.pumpAndSettle();

    expect(find.text('Selecionado'), findsNothing);
    expect(find.text('Faltam 2,00%'), findsOneWidget);
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, 'Salvar Metas'))
          .onPressed,
      isNull,
    );

    // Edição manual para 102,50%
    await tester.enterText(
      find.byKey(const Key('cerrado_input_targetAcoesBr')),
      '32,50',
    );
    await tester.pumpAndSettle();

    expect(find.text('Excesso de 2,50%'), findsOneWidget);
  });

  testWidgets('salvamento válido exibe feedback esmeralda e erro exibe ProblemDetail',
      (tester) async {
    var shouldFail = false;
    final repo = _StubCerradoRepository();
    repo.onGetTemplates = () async => _catalogFixture();
    repo.onGetPreferences = () async => const CerradoPreferences(
          id: 1,
          userId: 5,
          targetAcoesBrBps: 3000,
          targetFiisBps: 2500,
          targetRendaFixaBps: 2400,
          targetInternacionalBps: 2000,
          targetCriptoBps: 100,
          grahamMultiplier: 1.30,
          grahamFilterEnabled: true,
        );
    repo.onSavePreferences = ({
      required targetsBps,
      grahamMultiplier,
      grahamFilterEnabled,
    }) async {
      if (shouldFail) {
        throw Exception('Soma das metas deve ser exatamente 100.00%.');
      }
      return const CerradoPreferences(
        id: 1,
        userId: 5,
        targetAcoesBrBps: 3000,
        targetFiisBps: 2500,
        targetRendaFixaBps: 2400,
        targetInternacionalBps: 2000,
        targetCriptoBps: 100,
        grahamMultiplier: 1.30,
        grahamFilterEnabled: true,
      );
    };
    final provider = CerradoDiagramProvider(repository: repo);
    addTearDown(provider.dispose);

    await tester.pumpWidget(_wrapPage(provider: provider));
    await tester.pumpAndSettle();

    expect(find.text('Soma exata: 100,00%'), findsOneWidget);

    await tester.ensureVisible(find.widgetWithText(FilledButton, 'Salvar Metas'));
    await tester.tap(find.widgetWithText(FilledButton, 'Salvar Metas'));
    await tester.pumpAndSettle();

    expect(
      find.text('Metas do Diagrama do Cerrado salvas com sucesso.'),
      findsOneWidget,
    );

    shouldFail = true;
    await tester.tap(find.widgetWithText(FilledButton, 'Salvar Metas'));
    await tester.pumpAndSettle();

    expect(
      find.text('Soma das metas deve ser exatamente 100.00%.'),
      findsOneWidget,
    );
  });

  testWidgets('erro ao carregar exibe Tentar novamente >= 48dp e recarrega ao tocar',
      (tester) async {
    var shouldFail = true;
    final repo = _StubCerradoRepository();
    repo.onGetTemplates = () async {
      if (shouldFail) {
        throw Exception('Servidor indisponível');
      }
      return _catalogFixture();
    };
    repo.onGetPreferences = () async => null;
    final provider = CerradoDiagramProvider(repository: repo);
    addTearDown(provider.dispose);

    await tester.pumpWidget(_wrapPage(provider: provider));
    await tester.pumpAndSettle();

    final retryFinder = find.widgetWithText(FilledButton, 'Tentar novamente');
    expect(retryFinder, findsOneWidget);
    expect(tester.getSize(retryFinder).height, greaterThanOrEqualTo(48.0));

    shouldFail = false;
    await tester.tap(retryFinder);
    await tester.pumpAndSettle();

    expect(find.text('Modelo Equilibrado Cerrado'), findsOneWidget);
  });

  testWidgets('suporta textScaler 2.0x sem overflow nem ellipsis em Light e Dark',
      (tester) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    for (final theme in <ThemeData>[AppTheme.lightTheme, AppTheme.darkTheme]) {
      final repo = _StubCerradoRepository();
      repo.onGetTemplates = () async => _catalogFixture();
      repo.onGetPreferences = () async => null;
      final provider = CerradoDiagramProvider(repository: repo);

      await tester.pumpWidget(
        _wrapPage(provider: provider, theme: theme, textScale: 2.0),
      );
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      final metricTexts = tester.widgetList<Text>(
        find.descendant(
          of: find.byType(KernoMetricText),
          matching: find.byType(Text),
        ),
      );
      expect(metricTexts, isNotEmpty);
      for (final text in metricTexts) {
        expect(text.overflow, isNot(TextOverflow.ellipsis));
      }

      final saveSize = tester.getSize(
        find.widgetWithText(FilledButton, 'Salvar Metas'),
      );
      expect(saveSize.height, greaterThanOrEqualTo(48.0));

      provider.dispose();
    }
  });
}
