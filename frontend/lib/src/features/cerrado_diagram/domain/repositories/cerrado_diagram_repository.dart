import '../entities/cerrado_macro_class.dart';
import '../entities/cerrado_preferences.dart';
import '../entities/cerrado_template.dart';

/// Contrato de repositório do Diagrama do Cerrado.
abstract class CerradoDiagramRepository {
  Future<CerradoTemplatesCatalog> getTemplates();

  Future<CerradoPreferences?> getPreferences();

  Future<CerradoPreferences> savePreferences({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  });
}
