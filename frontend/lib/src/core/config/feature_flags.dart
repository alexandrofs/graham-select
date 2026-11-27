import 'package:flutter/foundation.dart';

/// Feature flags modulares da plataforma Kerno.
///
/// Controla a exposição de módulos em desenvolvimento sem acoplar
/// suas rotas ou árvores de widgets ao shell principal.
class FeatureFlags {
  FeatureFlags._();

  static const bool _envEnableExpensesModule = bool.fromEnvironment(
    'ENABLE_EXPENSES_MODULE',
    defaultValue: false,
  );

  static bool? _enableExpensesModuleOverride;

  /// Habilita o 5º módulo de Gastos (`/expenses`, Ciclo 2.1).
  static bool get enableExpensesModule =>
      _enableExpensesModuleOverride ?? _envEnableExpensesModule;

  /// Permite sobrescrever a flag em testes unitários e de widget.
  @visibleForTesting
  static void setEnableExpensesModuleForTest(bool? value) {
    _enableExpensesModuleOverride = value;
  }

  /// Restaura todas as flags para os valores de compilação.
  @visibleForTesting
  static void resetForTest() {
    _enableExpensesModuleOverride = null;
  }
}
