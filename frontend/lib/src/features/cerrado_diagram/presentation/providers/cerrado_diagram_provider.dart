import 'package:flutter/foundation.dart';

import '../../domain/entities/cerrado_macro_class.dart';
import '../../domain/entities/cerrado_preferences.dart';
import '../../domain/entities/cerrado_template.dart';
import '../../domain/repositories/cerrado_diagram_repository.dart';
import '../../domain/services/cerrado_target_validator.dart';

enum CerradoDiagramStatus { initial, loading, loaded, error }

class CerradoDiagramProvider extends ChangeNotifier {
  CerradoDiagramProvider({required this.repository});

  final CerradoDiagramRepository repository;

  CerradoDiagramStatus _status = CerradoDiagramStatus.initial;
  CerradoTemplatesCatalog? _catalog;
  CerradoPreferences? _savedPreferences;
  String? _selectedTemplateCode;
  Map<CerradoMacroClass, String> _rawInputs = _emptyRawInputs();
  Map<CerradoMacroClass, int?> _parsedBps = _zeroParsedBps();
  bool _isSaving = false;
  bool _disposed = false;
  String? _errorMessage;
  String? _saveErrorMessage;
  String? _saveSuccessMessage;
  int _loadGeneration = 0;
  int _inputSyncVersion = 0;

  CerradoDiagramStatus get status => _status;
  CerradoTemplatesCatalog? get catalog => _catalog;
  CerradoPreferences? get savedPreferences => _savedPreferences;
  String? get selectedTemplateCode => _selectedTemplateCode;
  Map<CerradoMacroClass, String> get rawInputs => Map.unmodifiable(_rawInputs);
  Map<CerradoMacroClass, int?> get parsedBps => Map.unmodifiable(_parsedBps);
  bool get isSaving => _isSaving;
  String? get errorMessage => _errorMessage;
  String? get saveErrorMessage => _saveErrorMessage;
  String? get saveSuccessMessage => _saveSuccessMessage;
  int get inputSyncVersion => _inputSyncVersion;

  CerradoTargetValidationResult get validation =>
      CerradoTargetValidator.validateAll(_parsedBps);

  bool get canSave =>
      _status == CerradoDiagramStatus.loaded &&
      !_isSaving &&
      validation.isValid;

  static Map<CerradoMacroClass, String> _emptyRawInputs() {
    return <CerradoMacroClass, String>{
      for (final macroClass in CerradoMacroClass.values) macroClass: '0,00',
    };
  }

  static Map<CerradoMacroClass, int?> _zeroParsedBps() {
    return <CerradoMacroClass, int?>{
      for (final macroClass in CerradoMacroClass.values) macroClass: 0,
    };
  }

  Future<void> load({bool silent = false}) async {
    final requestGeneration = ++_loadGeneration;
    if (!silent) {
      _status = CerradoDiagramStatus.loading;
      _errorMessage = null;
      _safeNotify();
    }

    try {
      final results = await Future.wait<Object?>(<Future<Object?>>[
        repository.getTemplates(),
        repository.getPreferences(),
      ]);

      if (_disposed || requestGeneration != _loadGeneration) {
        return;
      }

      final catalog = results[0]! as CerradoTemplatesCatalog;
      final preferences = results[1] as CerradoPreferences?;

      _catalog = catalog;
      _savedPreferences = preferences;
      _selectedTemplateCode = null;
      _saveErrorMessage = null;
      _saveSuccessMessage = null;
      _errorMessage = null;

      if (preferences != null) {
        _applyTargetsBps(preferences.targetsBps);
      } else {
        _rawInputs = _emptyRawInputs();
        _parsedBps = _zeroParsedBps();
        _inputSyncVersion++;
      }

      _status = CerradoDiagramStatus.loaded;
      _safeNotify();
    } catch (e, stackTrace) {
      if (_disposed || requestGeneration != _loadGeneration) {
        return;
      }
      debugPrint('[CerradoDiagramProvider] ERROR on load: $e\n$stackTrace');
      _errorMessage = _formatExceptionMessage(e);
      if (!silent) {
        _status = CerradoDiagramStatus.error;
      }
      _safeNotify();
    }
  }

  void selectTemplate(CerradoTemplate template) {
    _selectedTemplateCode = template.code;
    _saveErrorMessage = null;
    _saveSuccessMessage = null;
    _applyTargetsBps(template.targetsBps);
    _safeNotify();
  }

  void updateTargetInput(CerradoMacroClass macroClass, String value) {
    _selectedTemplateCode = null;
    _saveErrorMessage = null;
    _saveSuccessMessage = null;
    _rawInputs = Map<CerradoMacroClass, String>.from(_rawInputs)
      ..[macroClass] = value;
    _parsedBps = Map<CerradoMacroClass, int?>.from(_parsedBps)
      ..[macroClass] = CerradoTargetValidator.parsePercentageToBps(value);
    _safeNotify();
  }

  Future<bool> savePreferences() async {
    if (!canSave) {
      return false;
    }

    final requestGeneration = _loadGeneration;
    _isSaving = true;
    _saveErrorMessage = null;
    _saveSuccessMessage = null;
    _safeNotify();

    try {
      final targetsToSave = <CerradoMacroClass, int>{
        for (final macroClass in CerradoMacroClass.values)
          macroClass: _parsedBps[macroClass] ?? 0,
      };

      final saved = await repository.savePreferences(
        targetsBps: targetsToSave,
        grahamMultiplier: _savedPreferences?.grahamMultiplier,
        grahamFilterEnabled: _savedPreferences?.grahamFilterEnabled,
      );

      if (_disposed || requestGeneration != _loadGeneration) {
        return false;
      }

      _savedPreferences = saved;
      _applyTargetsBps(saved.targetsBps);
      _isSaving = false;
      _saveSuccessMessage = 'Metas do Diagrama do Cerrado salvas com sucesso.';
      _safeNotify();
      return true;
    } catch (e, stackTrace) {
      if (_disposed || requestGeneration != _loadGeneration) {
        return false;
      }
      debugPrint(
        '[CerradoDiagramProvider] ERROR on savePreferences: $e\n$stackTrace',
      );
      _isSaving = false;
      _saveErrorMessage = _formatExceptionMessage(e);
      _safeNotify();
      return false;
    }
  }

  void reset() {
    _loadGeneration++;
    final hadState =
        _status != CerradoDiagramStatus.initial ||
        _catalog != null ||
        _savedPreferences != null ||
        _selectedTemplateCode != null ||
        _isSaving ||
        _errorMessage != null ||
        _saveErrorMessage != null ||
        _saveSuccessMessage != null;

    _status = CerradoDiagramStatus.initial;
    _catalog = null;
    _savedPreferences = null;
    _selectedTemplateCode = null;
    _rawInputs = _emptyRawInputs();
    _parsedBps = _zeroParsedBps();
    _isSaving = false;
    _errorMessage = null;
    _saveErrorMessage = null;
    _saveSuccessMessage = null;
    _inputSyncVersion++;

    if (hadState) {
      _safeNotify();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }

  void _safeNotify() {
    if (!_disposed) {
      notifyListeners();
    }
  }

  void _applyTargetsBps(Map<CerradoMacroClass, int> targetsBps) {
    _rawInputs = <CerradoMacroClass, String>{
      for (final macroClass in CerradoMacroClass.values)
        macroClass: CerradoTargetValidator.formatBpsPtBr(
          targetsBps[macroClass] ?? 0,
        ),
    };
    _parsedBps = <CerradoMacroClass, int?>{
      for (final macroClass in CerradoMacroClass.values)
        macroClass: targetsBps[macroClass] ?? 0,
    };
    _inputSyncVersion++;
  }

  String _formatExceptionMessage(Object error) {
    final text = error.toString();
    const prefix = 'Exception: ';
    if (text.startsWith(prefix)) {
      return text.substring(prefix.length);
    }
    return text;
  }
}
