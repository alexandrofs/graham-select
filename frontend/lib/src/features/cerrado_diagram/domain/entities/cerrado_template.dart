import 'cerrado_macro_class.dart';

/// Template oficial de onboarding do Diagrama do Cerrado.
class CerradoTemplate {
  const CerradoTemplate({
    required this.code,
    required this.name,
    required this.description,
    required this.targetAcoesBrBps,
    required this.targetFiisBps,
    required this.targetRendaFixaBps,
    required this.targetInternacionalBps,
    required this.targetCriptoBps,
  });

  final String code;
  final String name;
  final String description;
  final int targetAcoesBrBps;
  final int targetFiisBps;
  final int targetRendaFixaBps;
  final int targetInternacionalBps;
  final int targetCriptoBps;

  int bpsFor(CerradoMacroClass macroClass) {
    return switch (macroClass) {
      CerradoMacroClass.acoesBr => targetAcoesBrBps,
      CerradoMacroClass.fiis => targetFiisBps,
      CerradoMacroClass.rendaFixa => targetRendaFixaBps,
      CerradoMacroClass.internacional => targetInternacionalBps,
      CerradoMacroClass.cripto => targetCriptoBps,
    };
  }

  Map<CerradoMacroClass, int> get targetsBps => <CerradoMacroClass, int>{
        CerradoMacroClass.acoesBr: targetAcoesBrBps,
        CerradoMacroClass.fiis: targetFiisBps,
        CerradoMacroClass.rendaFixa: targetRendaFixaBps,
        CerradoMacroClass.internacional: targetInternacionalBps,
        CerradoMacroClass.cripto: targetCriptoBps,
      };
}

/// Catálogo de templates oficiais e metadados regulatórios retornados pelo backend.
class CerradoTemplatesCatalog {
  const CerradoTemplatesCatalog({
    required this.defaultNeutralGrade,
    required this.cvmDisclaimer,
    required this.templates,
  });

  final int defaultNeutralGrade;
  final String cvmDisclaimer;
  final List<CerradoTemplate> templates;
}
