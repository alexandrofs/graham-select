import 'cerrado_macro_class.dart';

/// Preferências persistidas de alocação por Macro Classe do Diagrama do Cerrado.
class CerradoPreferences {
  const CerradoPreferences({
    required this.id,
    required this.userId,
    required this.targetAcoesBrBps,
    required this.targetFiisBps,
    required this.targetRendaFixaBps,
    required this.targetInternacionalBps,
    required this.targetCriptoBps,
    required this.grahamMultiplier,
    required this.grahamFilterEnabled,
    this.updatedAt,
  });

  final int id;
  final int userId;
  final int targetAcoesBrBps;
  final int targetFiisBps;
  final int targetRendaFixaBps;
  final int targetInternacionalBps;
  final int targetCriptoBps;
  final double grahamMultiplier;
  final bool grahamFilterEnabled;
  final DateTime? updatedAt;

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
