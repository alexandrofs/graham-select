import '../entities/cerrado_macro_class.dart';

/// Resultado imutável da validação determinística da trava de soma 100,00%.
class CerradoTargetValidationResult {
  const CerradoTargetValidationResult({
    required this.totalBps,
    required this.remainingBps,
    required this.hasInvalidField,
    required this.isValid,
  });

  /// Soma atual em basis points (apenas campos válidos somados).
  final int totalBps;

  /// Diferença para `10000` bps (`> 0` faltam; `< 0` excesso; `0` exato).
  final int remainingBps;

  /// Indica se ao menos uma Macro Classe possui entrada inválida.
  final bool hasInvalidField;

  /// Verdadeiro apenas quando todos os campos são válidos e `totalBps == 10000`.
  final bool isValid;
}

/// Utilitário de conversão e validação exata em basis points (`10000 bps == 100.00%`).
///
/// Evita imprecisões de ponto flutuante (IEEE 754) na soma e serialização
/// das 5 Macro Classes do Diagrama do Cerrado.
abstract final class CerradoTargetValidator {
  static const int totalRequiredBps = 10000;
  static const int maxClassBps = 10000;

  static final RegExp _validInputPattern = RegExp(r'^\d+(?:\.\d{1,2})?$');

  /// Converte entrada textual do usuário em basis points inteiros `[0, 10000]`.
  ///
  /// - Campo vazio `""` (ou só espaços) é tratado como `0` bps.
  /// - Normaliza `,` para `.`.
  /// - Rejeita (`null`) entradas não numéricas, negativas, `> 100.00` ou com `> 2` casas decimais.
  static int? parsePercentageToBps(String raw) {
    final trimmed = raw.trim();
    if (trimmed.isEmpty) {
      return 0;
    }

    final normalized = trimmed.replaceAll(',', '.');
    if (!_validInputPattern.hasMatch(normalized)) {
      return null;
    }

    final parts = normalized.split('.');
    final whole = int.tryParse(parts[0]);
    if (whole == null || whole < 0 || whole > 100) {
      return null;
    }

    final fracStr = parts.length == 2 ? parts[1].padRight(2, '0') : '00';
    final frac = int.tryParse(fracStr);
    if (frac == null) {
      return null;
    }

    final bps = (whole * 100) + frac;
    if (bps < 0 || bps > maxClassBps) {
      return null;
    }
    return bps;
  }

  /// Converte valor JSON (`num` ou `String`) vindo da API para basis points.
  static int parseApiValueToBps(Object? value) {
    if (value is int) {
      if (value < 0 || value > 100) {
        throw FormatException('Valor percentual fora do intervalo [0, 100]: $value');
      }
      return value * 100;
    }
    if (value is num) {
      final asDouble = value.toDouble();
      if (!asDouble.isFinite || asDouble < 0 || asDouble > 100) {
        throw FormatException('Valor percentual inválido recebido da API: $value');
      }
      return (asDouble * 100).round().clamp(0, maxClassBps);
    }
    if (value is String) {
      final parsed = parsePercentageToBps(value);
      if (parsed != null) {
        return parsed;
      }
      final fallbackDouble = double.tryParse(value.trim().replaceAll(',', '.'));
      if (fallbackDouble != null &&
          fallbackDouble.isFinite &&
          fallbackDouble >= 0 &&
          fallbackDouble <= 100) {
        return (fallbackDouble * 100).round().clamp(0, maxClassBps);
      }
    }
    throw FormatException('Valor percentual inválido recebido da API: $value');
  }

  /// Serializa basis points para `double` com exatamente 2 casas decimais
  /// compatível com `BigDecimal` escala `<= 2` do backend.
  static double bpsToJsonDouble(int bps) {
    return double.parse((bps / 100).toStringAsFixed(2));
  }

  /// Formata basis points no padrão pt-BR com 2 casas decimais (ex.: `2500` -> `'25,00'`).
  static String formatBpsPtBr(int bps) {
    final absBps = bps.abs();
    final whole = absBps ~/ 100;
    final frac = (absBps % 100).toString().padLeft(2, '0');
    final prefix = bps < 0 ? '-' : '';
    return '$prefix$whole,$frac';
  }

  /// Valida o conjunto completo das 5 Macro Classes.
  static CerradoTargetValidationResult validateAll(
    Map<CerradoMacroClass, int?> parsedBps,
  ) {
    var hasInvalidField = false;
    var totalBps = 0;

    for (final macroClass in CerradoMacroClass.values) {
      final bps = parsedBps[macroClass];
      if (bps == null || bps < 0 || bps > maxClassBps) {
        hasInvalidField = true;
      } else {
        totalBps += bps;
      }
    }

    final remainingBps = totalRequiredBps - totalBps;
    final isValid = !hasInvalidField && totalBps == totalRequiredBps;

    return CerradoTargetValidationResult(
      totalBps: totalBps,
      remainingBps: remainingBps,
      hasInvalidField: hasInvalidField,
      isValid: isValid,
    );
  }
}
