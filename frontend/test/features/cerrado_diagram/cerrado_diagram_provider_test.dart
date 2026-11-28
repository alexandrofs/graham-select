import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_macro_class.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_preferences.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_template.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/repositories/cerrado_diagram_repository.dart';
import 'package:frontend/src/features/cerrado_diagram/presentation/providers/cerrado_diagram_provider.dart';

class _FakeCerradoDiagramRepository implements CerradoDiagramRepository {
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

CerradoTemplatesCatalog _sampleCatalog() {
  return const CerradoTemplatesCatalog(
    defaultNeutralGrade: 6,
    cvmDisclaimer: 'Sem recomendação de investimento.',
    templates: <CerradoTemplate>[
      CerradoTemplate(
        code: 'EQUILIBRADO_CERRADO',
        name: 'Modelo Equilibrado Cerrado',
        description: 'Equilibrado',
        targetAcoesBrBps: 3000,
        targetFiisBps: 2500,
        targetRendaFixaBps: 2400,
        targetInternacionalBps: 2000,
        targetCriptoBps: 100,
      ),
    ],
  );
}

void main() {
  late _FakeCerradoDiagramRepository repository;
  late CerradoDiagramProvider provider;

  setUp(() {
    repository = _FakeCerradoDiagramRepository();
    provider = CerradoDiagramProvider(repository: repository);
  });

  tearDown(() {
    provider.dispose();
  });

  test('onboarding 404 carrega catálogo sem pré-seleção e soma em 0,00%', () async {
    repository.onGetTemplates = () async => _sampleCatalog();
    repository.onGetPreferences = () async => null;

    await provider.load();

    expect(provider.status, CerradoDiagramStatus.loaded);
    expect(provider.selectedTemplateCode, isNull);
    expect(provider.validation.totalBps, 0);
    expect(provider.validation.remainingBps, 10000);
    expect(provider.canSave, isFalse);
    for (final macroClass in CerradoMacroClass.values) {
      expect(provider.rawInputs[macroClass], '0,00');
    }
  });

  test('selecionar template preenche metas e edição manual limpa seleção', () async {
    repository.onGetTemplates = () async => _sampleCatalog();
    repository.onGetPreferences = () async => null;

    await provider.load();
    provider.selectTemplate(provider.catalog!.templates.first);

    expect(provider.selectedTemplateCode, 'EQUILIBRADO_CERRADO');
    expect(provider.validation.totalBps, 10000);
    expect(provider.canSave, isTrue);

    provider.updateTargetInput(CerradoMacroClass.acoesBr, '28,00');
    expect(provider.selectedTemplateCode, isNull);
    expect(provider.validation.totalBps, 9800);
    expect(provider.validation.remainingBps, 200);
    expect(provider.canSave, isFalse);
  });

  test('savePreferences preserva graham* existentes e trata erro mantendo edição', () async {
    repository.onGetTemplates = () async => _sampleCatalog();
    repository.onGetPreferences = () async => const CerradoPreferences(
          id: 1,
          userId: 7,
          targetAcoesBrBps: 3000,
          targetFiisBps: 2500,
          targetRendaFixaBps: 2400,
          targetInternacionalBps: 2000,
          targetCriptoBps: 100,
          grahamMultiplier: 1.45,
          grahamFilterEnabled: false,
        );

    double? sentMultiplier;
    bool? sentFilter;
    repository.onSavePreferences = ({
      required targetsBps,
      grahamMultiplier,
      grahamFilterEnabled,
    }) async {
      sentMultiplier = grahamMultiplier;
      sentFilter = grahamFilterEnabled;
      return const CerradoPreferences(
        id: 1,
        userId: 7,
        targetAcoesBrBps: 3000,
        targetFiisBps: 2500,
        targetRendaFixaBps: 2400,
        targetInternacionalBps: 2000,
        targetCriptoBps: 100,
        grahamMultiplier: 1.45,
        grahamFilterEnabled: false,
      );
    };

    await provider.load();
    final ok = await provider.savePreferences();

    expect(ok, isTrue);
    expect(sentMultiplier, 1.45);
    expect(sentFilter, isFalse);
    expect(provider.saveSuccessMessage, isNotNull);
    expect(provider.saveErrorMessage, isNull);
  });

  test('reset descarta resposta obsoleta de load em andamento', () async {
    final completer = Completer<CerradoTemplatesCatalog>();
    repository.onGetTemplates = () => completer.future;
    repository.onGetPreferences = () async => null;

    final loadFuture = provider.load();
    provider.reset();
    completer.complete(_sampleCatalog());
    await loadFuture;

    expect(provider.status, CerradoDiagramStatus.initial);
    expect(provider.catalog, isNull);
  });

  test('savePreferences em erro preserva inputs e preenche saveErrorMessage', () async {
    repository.onGetTemplates = () async => _sampleCatalog();
    repository.onGetPreferences = () async => null;
    repository.onSavePreferences = ({
      required targetsBps,
      grahamMultiplier,
      grahamFilterEnabled,
    }) async {
      throw Exception('Soma inválida no servidor.');
    };

    await provider.load();
    provider.selectTemplate(provider.catalog!.templates.first);

    final ok = await provider.savePreferences();
    expect(ok, isFalse);
    expect(provider.isSaving, isFalse);
    expect(provider.saveSuccessMessage, isNull);
    expect(provider.saveErrorMessage, 'Soma inválida no servidor.');
    expect(provider.rawInputs[CerradoMacroClass.acoesBr], '30,00');
  });

  test('reset durante savePreferences em voo descarta resposta obsoleta', () async {
    final saveCompleter = Completer<CerradoPreferences>();
    repository.onGetTemplates = () async => _sampleCatalog();
    repository.onGetPreferences = () async => null;
    repository.onSavePreferences = ({
      required targetsBps,
      grahamMultiplier,
      grahamFilterEnabled,
    }) => saveCompleter.future;

    await provider.load();
    provider.selectTemplate(provider.catalog!.templates.first);

    final saveFuture = provider.savePreferences();
    provider.reset();
    saveCompleter.complete(
      const CerradoPreferences(
        id: 1,
        userId: 7,
        targetAcoesBrBps: 3000,
        targetFiisBps: 2500,
        targetRendaFixaBps: 2400,
        targetInternacionalBps: 2000,
        targetCriptoBps: 100,
        grahamMultiplier: 1.30,
        grahamFilterEnabled: true,
      ),
    );
    final ok = await saveFuture;

    expect(ok, isFalse);
    expect(provider.status, CerradoDiagramStatus.initial);
    expect(provider.savedPreferences, isNull);
    expect(provider.saveSuccessMessage, isNull);
  });

  test('load(silent: true) em erro mantém status loaded anterior', () async {
    repository.onGetTemplates = () async => _sampleCatalog();
    repository.onGetPreferences = () async => null;
    await provider.load();
    expect(provider.status, CerradoDiagramStatus.loaded);

    repository.onGetTemplates = () async => throw Exception('Falha silenciosa');
    await provider.load(silent: true);

    expect(provider.status, CerradoDiagramStatus.loaded);
    expect(provider.errorMessage, 'Falha silenciosa');
  });
}
