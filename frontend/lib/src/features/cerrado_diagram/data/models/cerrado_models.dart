import '../../domain/entities/cerrado_macro_class.dart';
import '../../domain/entities/cerrado_preferences.dart';
import '../../domain/entities/cerrado_template.dart';
import '../../domain/services/cerrado_target_validator.dart';

class CerradoTemplateModel extends CerradoTemplate {
  const CerradoTemplateModel({
    required super.code,
    required super.name,
    required super.description,
    required super.targetAcoesBrBps,
    required super.targetFiisBps,
    required super.targetRendaFixaBps,
    required super.targetInternacionalBps,
    required super.targetCriptoBps,
  });

  factory CerradoTemplateModel.fromJson(Map<String, dynamic> json) {
    return CerradoTemplateModel(
      code: (json['code'] ?? '').toString(),
      name: (json['name'] ?? '').toString(),
      description: (json['description'] ?? '').toString(),
      targetAcoesBrBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetAcoesBr'],
      ),
      targetFiisBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetFiis'],
      ),
      targetRendaFixaBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetRendaFixa'],
      ),
      targetInternacionalBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetInternacional'],
      ),
      targetCriptoBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetCripto'],
      ),
    );
  }
}

class CerradoTemplatesCatalogModel extends CerradoTemplatesCatalog {
  const CerradoTemplatesCatalogModel({
    required super.defaultNeutralGrade,
    required super.cvmDisclaimer,
    required super.templates,
  });

  factory CerradoTemplatesCatalogModel.fromJson(Map<String, dynamic> json) {
    final rawTemplates = json['templates'];
    if (rawTemplates is! List) {
      throw const FormatException(
        'Resposta inválida: lista de templates ausente.',
      );
    }

    final templates = rawTemplates
        .whereType<Map>()
        .map(
          (item) => CerradoTemplateModel.fromJson(
            Map<String, dynamic>.from(item),
          ),
        )
        .toList(growable: false);

    final rawGrade = json['defaultNeutralGrade'];
    final defaultNeutralGrade = rawGrade is int
        ? rawGrade
        : int.tryParse(rawGrade?.toString() ?? '') ?? 6;

    return CerradoTemplatesCatalogModel(
      defaultNeutralGrade: defaultNeutralGrade,
      cvmDisclaimer: (json['cvmDisclaimer'] ?? '').toString(),
      templates: templates,
    );
  }
}

class CerradoPreferencesModel extends CerradoPreferences {
  const CerradoPreferencesModel({
    required super.id,
    required super.userId,
    required super.targetAcoesBrBps,
    required super.targetFiisBps,
    required super.targetRendaFixaBps,
    required super.targetInternacionalBps,
    required super.targetCriptoBps,
    required super.grahamMultiplier,
    required super.grahamFilterEnabled,
    super.updatedAt,
  });

  factory CerradoPreferencesModel.fromJson(Map<String, dynamic> json) {
    final rawId = json['id'];
    final rawUserId = json['userId'];
    final rawMultiplier = json['grahamMultiplier'];
    final rawUpdatedAt = json['updatedAt']?.toString();

    final grahamMultiplier = rawMultiplier is num
        ? rawMultiplier.toDouble()
        : double.tryParse(rawMultiplier?.toString() ?? '') ?? 1.30;

    return CerradoPreferencesModel(
      id: rawId is int ? rawId : int.tryParse(rawId?.toString() ?? '') ?? 0,
      userId: rawUserId is int
          ? rawUserId
          : int.tryParse(rawUserId?.toString() ?? '') ?? 0,
      targetAcoesBrBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetAcoesBr'],
      ),
      targetFiisBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetFiis'],
      ),
      targetRendaFixaBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetRendaFixa'],
      ),
      targetInternacionalBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetInternacional'],
      ),
      targetCriptoBps: CerradoTargetValidator.parseApiValueToBps(
        json['targetCripto'],
      ),
      grahamMultiplier: grahamMultiplier,
      grahamFilterEnabled: json['grahamFilterEnabled'] as bool? ?? true,
      updatedAt:
          rawUpdatedAt != null && rawUpdatedAt.isNotEmpty
              ? DateTime.tryParse(rawUpdatedAt)
              : null,
    );
  }

  static Map<String, dynamic> toUpdatePayload({
    required Map<CerradoMacroClass, int> targetsBps,
    double? grahamMultiplier,
    bool? grahamFilterEnabled,
  }) {
    final payload = <String, dynamic>{
      for (final macroClass in CerradoMacroClass.values)
        macroClass.jsonKey: CerradoTargetValidator.bpsToJsonDouble(
          targetsBps[macroClass] ?? 0,
        ),
    };
    if (grahamMultiplier != null && grahamMultiplier.isFinite) {
      payload['grahamMultiplier'] = double.parse(
        grahamMultiplier.toStringAsFixed(2),
      );
    }
    if (grahamFilterEnabled != null) {
      payload['grahamFilterEnabled'] = grahamFilterEnabled;
    }
    return payload;
  }
}
