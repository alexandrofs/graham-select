import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/entities/cerrado_macro_class.dart';
import 'package:frontend/src/features/cerrado_diagram/domain/services/cerrado_target_validator.dart';

void main() {
  group('CerradoTargetValidator.parsePercentageToBps', () {
    test('interpreta string vazia como 0 bps', () {
      expect(CerradoTargetValidator.parsePercentageToBps(''), 0);
      expect(CerradoTargetValidator.parsePercentageToBps('   '), 0);
    });

    test('normaliza vírgula para ponto e converte exatamente sem float drift', () {
      expect(CerradoTargetValidator.parsePercentageToBps('25'), 2500);
      expect(CerradoTargetValidator.parsePercentageToBps('25,5'), 2550);
      expect(CerradoTargetValidator.parsePercentageToBps('25,50'), 2550);
      expect(CerradoTargetValidator.parsePercentageToBps('19.99'), 1999);
      expect(CerradoTargetValidator.parsePercentageToBps('0.01'), 1);
      expect(CerradoTargetValidator.parsePercentageToBps('100.00'), 10000);
    });

    test('rejeita negativos, > 100.00, > 2 casas decimais e não numéricos', () {
      expect(CerradoTargetValidator.parsePercentageToBps('-1'), isNull);
      expect(CerradoTargetValidator.parsePercentageToBps('100.01'), isNull);
      expect(CerradoTargetValidator.parsePercentageToBps('12.345'), isNull);
      expect(CerradoTargetValidator.parsePercentageToBps('12,345'), isNull);
      expect(CerradoTargetValidator.parsePercentageToBps('abc'), isNull);
      expect(CerradoTargetValidator.parsePercentageToBps('10.2.3'), isNull);
    });
  });

  group('CerradoTargetValidator conversões e validação global', () {
    test('parseApiValueToBps aceita num e String', () {
      expect(CerradoTargetValidator.parseApiValueToBps(35), 3500);
      expect(CerradoTargetValidator.parseApiValueToBps(22.5), 2250);
      expect(CerradoTargetValidator.parseApiValueToBps('19.99'), 1999);
      expect(CerradoTargetValidator.parseApiValueToBps('1,00'), 100);
    });

    test('parseApiValueToBps rejeita NaN, Infinity e valores negativos ou > 100', () {
      expect(
        () => CerradoTargetValidator.parseApiValueToBps(double.nan),
        throwsFormatException,
      );
      expect(
        () => CerradoTargetValidator.parseApiValueToBps(double.infinity),
        throwsFormatException,
      );
      expect(
        () => CerradoTargetValidator.parseApiValueToBps(-1),
        throwsFormatException,
      );
      expect(
        () => CerradoTargetValidator.parseApiValueToBps('-5.00'),
        throwsFormatException,
      );
      expect(
        () => CerradoTargetValidator.parseApiValueToBps('101.00'),
        throwsFormatException,
      );
    });

    test('bpsToJsonDouble serializa com exatamente até 2 casas decimais', () {
      expect(CerradoTargetValidator.bpsToJsonDouble(1999), 19.99);
      expect(CerradoTargetValidator.bpsToJsonDouble(2500), 25.0);
      expect(CerradoTargetValidator.bpsToJsonDouble(1), 0.01);
      expect(CerradoTargetValidator.bpsToJsonDouble(0), 0.0);
    });

    test('formatBpsPtBr formata corretamente em pt-BR', () {
      expect(CerradoTargetValidator.formatBpsPtBr(0), '0,00');
      expect(CerradoTargetValidator.formatBpsPtBr(5), '0,05');
      expect(CerradoTargetValidator.formatBpsPtBr(250), '2,50');
      expect(CerradoTargetValidator.formatBpsPtBr(10000), '100,00');
    });

    test('validateAll valida soma exata de 10000 bps e diferenças', () {
      final exact = CerradoTargetValidator.validateAll(<CerradoMacroClass, int?>{
        CerradoMacroClass.acoesBr: 3000,
        CerradoMacroClass.fiis: 2000,
        CerradoMacroClass.rendaFixa: 2500,
        CerradoMacroClass.internacional: 2400,
        CerradoMacroClass.cripto: 100,
      });
      expect(exact.isValid, isTrue);
      expect(exact.hasInvalidField, isFalse);
      expect(exact.totalBps, 10000);
      expect(exact.remainingBps, 0);

      final missing = CerradoTargetValidator.validateAll(<CerradoMacroClass, int?>{
        CerradoMacroClass.acoesBr: 3000,
        CerradoMacroClass.fiis: 2000,
        CerradoMacroClass.rendaFixa: 2500,
        CerradoMacroClass.internacional: 2300,
        CerradoMacroClass.cripto: 0,
      });
      expect(missing.isValid, isFalse);
      expect(missing.totalBps, 9800);
      expect(missing.remainingBps, 200);

      final excess = CerradoTargetValidator.validateAll(<CerradoMacroClass, int?>{
        CerradoMacroClass.acoesBr: 3000,
        CerradoMacroClass.fiis: 2000,
        CerradoMacroClass.rendaFixa: 2750,
        CerradoMacroClass.internacional: 2500,
        CerradoMacroClass.cripto: 0,
      });
      expect(excess.isValid, isFalse);
      expect(excess.totalBps, 10250);
      expect(excess.remainingBps, -250);

      final invalid = CerradoTargetValidator.validateAll(<CerradoMacroClass, int?>{
        CerradoMacroClass.acoesBr: null,
        CerradoMacroClass.fiis: 2500,
        CerradoMacroClass.rendaFixa: 2500,
        CerradoMacroClass.internacional: 2500,
        CerradoMacroClass.cripto: 2500,
      });
      expect(invalid.isValid, isFalse);
      expect(invalid.hasInvalidField, isTrue);
    });
  });
}
