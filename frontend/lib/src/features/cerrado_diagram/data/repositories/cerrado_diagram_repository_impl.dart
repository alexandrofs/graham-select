import '../../domain/entities/cerrado_macro_class.dart';
import '../../domain/entities/cerrado_preferences.dart';
import '../../domain/entities/cerrado_template.dart';
import '../../domain/repositories/cerrado_diagram_repository.dart';
import '../datasources/cerrado_diagram_remote_data_source.dart';

class CerradoDiagramRepositoryImpl implements CerradoDiagramRepository {
  const CerradoDiagramRepositoryImpl({required this.remoteDataSource});

  final CerradoDiagramRemoteDataSource remoteDataSource;

  @override
  Future<CerradoTemplatesCatalog> getTemplates() {
    return remoteDataSource.fetchTemplates();
  }

  @override
  Future<CerradoPreferences?> getPreferences() {
    return remoteDataSource.fetchPreferences();
  }

  @override
  Future<CerradoPreferences> savePreferences({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  }) {
    return remoteDataSource.savePreferences(
      targetsBps: targetsBps,
      grahamMultiplier: grahamMultiplier,
      grahamFilterEnabled: grahamFilterEnabled,
    );
  }
}
