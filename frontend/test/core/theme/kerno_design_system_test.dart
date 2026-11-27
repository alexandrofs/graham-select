import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/main.dart';
import 'package:frontend/src/core/theme/app_theme.dart';
import 'package:frontend/src/core/theme/kerno_accessibility.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';
import 'package:mockito/mockito.dart';

import '../../home_page_test.mocks.dart';

/// Calcula a razão de contraste WCAG 2.1 entre duas cores opacas.
double _wcagContrastRatio(Color foreground, Color background) {
  final l1 = _relativeLuminance(foreground);
  final l2 = _relativeLuminance(background);
  final lighter = math.max(l1, l2);
  final darker = math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

double _relativeLuminance(Color color) {
  double linearize(double channel) {
    return channel <= 0.03928
        ? channel / 12.92
        : math.pow((channel + 0.055) / 1.055, 2.4).toDouble();
  }

  final r = linearize(color.r);
  final g = linearize(color.g);
  final b = linearize(color.b);
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

void main() {
  group('KernoColors — Contraste WCAG 2.1 AA / AAA / 1.4.11', () {
    test('Light mode entrega >= 7.0:1 (AAA) em saldos primários e alertas', () {
      expect(
        _wcagContrastRatio(KernoColors.textPrimary, KernoColors.surfaceElevated),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(KernoColors.textPrimary, KernoColors.surfaceBase),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticEmerald,
          KernoColors.semanticEmeraldSurface,
        ),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticAmber,
          KernoColors.surfaceElevated,
        ),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticAmber,
          KernoColors.semanticAmberSurface,
        ),
        greaterThanOrEqualTo(6.8),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticCrimson,
          KernoColors.semanticCrimsonSurface,
        ),
        greaterThanOrEqualTo(7.0),
      );
    });

    test('Light mode entrega >= 4.5:1 (AA) em textos secundários, atenuados, brand e founder', () {
      expect(
        _wcagContrastRatio(
          KernoColors.textSecondary,
          KernoColors.surfaceElevated,
        ),
        greaterThanOrEqualTo(4.5),
      );
      expect(
        _wcagContrastRatio(KernoColors.textMuted, KernoColors.surfaceElevated),
        greaterThanOrEqualTo(4.5),
      );
      expect(
        _wcagContrastRatio(KernoColors.textOnPrimary, KernoColors.brandPrimary),
        greaterThanOrEqualTo(4.5),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.founderGold,
          KernoColors.founderGoldSurface,
        ),
        greaterThanOrEqualTo(4.5),
      );
    });

    test('Dark mode entrega >= 7.0:1 (AAA) em saldos primários e alertas e >= 4.5:1 no restante', () {
      expect(
        _wcagContrastRatio(
          KernoColors.textPrimaryDark,
          KernoColors.surfaceElevatedDark,
        ),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.textPrimaryDark,
          KernoColors.surfaceBaseDark,
        ),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.textSecondaryDark,
          KernoColors.surfaceElevatedDark,
        ),
        greaterThanOrEqualTo(4.5),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.textMutedDark,
          KernoColors.surfaceElevatedDark,
        ),
        greaterThanOrEqualTo(4.5),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticEmeraldDark,
          KernoColors.surfaceElevatedDark,
        ),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticEmeraldDark,
          KernoColors.semanticEmeraldSurfaceDark,
        ),
        greaterThanOrEqualTo(6.3),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticAmberDark,
          KernoColors.semanticAmberSurfaceDark,
        ),
        greaterThanOrEqualTo(7.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.semanticCrimsonDark,
          KernoColors.semanticCrimsonSurfaceDark,
        ),
        greaterThanOrEqualTo(6.3),
      );
      expect(
        _wcagContrastRatio(
          KernoThemeData.dark.textOnPrimary,
          KernoThemeData.dark.brandPrimary,
        ),
        greaterThanOrEqualTo(4.5),
      );
    });

    test('Bordas fortes atendem WCAG 1.4.11 (>= 3.0:1) em Light e Dark', () {
      expect(
        _wcagContrastRatio(
          KernoColors.borderStrong,
          KernoColors.surfaceElevated,
        ),
        greaterThanOrEqualTo(3.0),
      );
      expect(
        _wcagContrastRatio(
          KernoColors.borderStrongDark,
          KernoColors.surfaceElevatedDark,
        ),
        greaterThanOrEqualTo(3.0),
      );
    });
  });

  group('KernoTypography — Numerais tabulares obrigatórios', () {
    testWidgets('Todos os estilos numéricos e de corpo incluem FontFeature.tabularFigures()', (
      tester,
    ) async {
      final styles = <String, TextStyle>{
        'displayWealth': KernoTypography.displayWealth(),
        'heading1': KernoTypography.heading1(),
        'heading2': KernoTypography.heading2(),
        'heading3': KernoTypography.heading3(),
        'metricHero': KernoTypography.metricHero(),
        'metricCard': KernoTypography.metricCard(),
        'tickerBadge': KernoTypography.tickerBadge(),
        'bodyLg': KernoTypography.bodyLg(),
        'bodyBase': KernoTypography.bodyBase(),
        'bodySm': KernoTypography.bodySm(),
        'caption': KernoTypography.caption(),
      };

      for (final entry in styles.entries) {
        expect(
          entry.value.fontFeatures,
          contains(const FontFeature.tabularFigures()),
          reason: '${entry.key} deve incluir FontFeature.tabularFigures()',
        );
      }
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
    });
  });

  group('AppTheme — Light-First, Dark Mode, extensão e compatibilidade legada', () {
    test('Constantes estáticas legadas continuam preservadas', () {
      expect(AppTheme.primaryColor, const Color(0xFF6366F1));
      expect(AppTheme.secondaryColor, const Color(0xFF8B5CF6));
      expect(AppTheme.accentColor, const Color(0xFF10B981));
      expect(AppTheme.backgroundColor, const Color(0xFF0F172A));
      expect(AppTheme.surfaceColor, const Color(0xFF1E293B));
      expect(AppTheme.textColor, const Color(0xFFF8FAFC));
    });

    testWidgets('lightTheme e darkTheme aplicam superfícies e KernoThemeData', (
      tester,
    ) async {
      final light = AppTheme.lightTheme;
      final dark = AppTheme.darkTheme;

      expect(light.brightness, Brightness.light);
      expect(light.scaffoldBackgroundColor, KernoColors.surfaceBase);
      expect(light.colorScheme.surface, KernoColors.surfaceElevated);
      expect(light.extension<KernoThemeData>(), KernoThemeData.light);

      expect(dark.brightness, Brightness.dark);
      expect(dark.scaffoldBackgroundColor, KernoColors.surfaceBaseDark);
      expect(dark.colorScheme.surface, KernoColors.surfaceElevatedDark);
      expect(dark.extension<KernoThemeData>(), KernoThemeData.dark);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pumpAndSettle();
    });

    testWidgets('KernoThemeData.of faz fallback seguro quando extensão não está anexada', (
      tester,
    ) async {
      late KernoThemeData resolvedLight;
      late KernoThemeData resolvedDark;

      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData.light(),
          home: Builder(
            builder: (context) {
              resolvedLight = KernoThemeData.of(context);
              return const SizedBox.shrink();
            },
          ),
        ),
      );
      expect(resolvedLight, KernoThemeData.light);

      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData.dark(),
          home: Builder(
            builder: (context) {
              resolvedDark = KernoThemeData.of(context);
              return const SizedBox.shrink();
            },
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(resolvedDark, KernoThemeData.dark);
    });
  });

  group('Ergonomia & Acessibilidade — Alvos >= 48x48dp e textScaler 1.0x–2.0x', () {
    for (final entry in <String, ThemeData Function()>{
      'Light': () => AppTheme.lightTheme,
      'Dark': () => AppTheme.darkTheme,
    }.entries) {
      testWidgets('Botões sob ${entry.key} theme possuem alvo mínimo >= 48x48dp', (
        tester,
      ) async {
        await tester.pumpWidget(
          MaterialApp(
            theme: entry.value(),
            home: Scaffold(
              body: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  ElevatedButton(onPressed: () {}, child: const Text('A')),
                  FilledButton(onPressed: () {}, child: const Text('F')),
                  OutlinedButton(onPressed: () {}, child: const Text('B')),
                  TextButton(onPressed: () {}, child: const Text('C')),
                  IconButton(onPressed: () {}, icon: const Icon(Icons.visibility)),
                ],
              ),
            ),
          ),
        );
        await tester.pumpAndSettle();

        for (final type in <Type>[
          ElevatedButton,
          FilledButton,
          OutlinedButton,
          TextButton,
          IconButton,
        ]) {
          final size = tester.getSize(find.byType(type));
          expect(size.width, greaterThanOrEqualTo(48.0), reason: '$type width');
          expect(size.height, greaterThanOrEqualTo(48.0), reason: '$type height');
        }
      });
    }

    for (final scale in <double>[1.0, 1.5, 2.0]) {
      testWidgets(
        'KernoMetricText, ResponsiveMetricRow e TriadStatusBadge renderizam sem overflow ou ellipsis em ${scale}x',
        (tester) async {
          tester.view.physicalSize = const Size(360, 800);
          tester.view.devicePixelRatio = 1.0;
          addTearDown(tester.view.resetPhysicalSize);
          addTearDown(tester.view.resetDevicePixelRatio);

          await tester.pumpWidget(
            MaterialApp(
              theme: AppTheme.lightTheme,
              darkTheme: AppTheme.darkTheme,
              home: MediaQuery(
                data: MediaQueryData(textScaler: TextScaler.linear(scale)),
                child: const Scaffold(
                  body: SingleChildScrollView(
                    padding: EdgeInsets.all(KernoSpacing.s4),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        KernoMetricText('R\$ 1.482.930,45'),
                        SizedBox(height: KernoSpacing.s3),
                        ResponsiveMetricRow(
                          leading: KernoMetricText('ITUB4 — Itaú Unibanco'),
                          trailing: KernoMetricText('R\$ 4.250,00 (36,0%)'),
                        ),
                        SizedBox(height: KernoSpacing.s3),
                        TriadStatusBadge(
                          label: 'Margem Graham Positiva (1.30x)',
                          icon: Icons.trending_up,
                          tone: TriadStatusTone.emerald,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          );
          await tester.pumpAndSettle();

          expect(tester.takeException(), isNull);

          final textWidgets = tester.widgetList<Text>(find.byType(Text));
          for (final text in textWidgets) {
            expect(
              text.overflow,
              isNot(TextOverflow.ellipsis),
              reason: 'Nenhum texto financeiro ou badge pode usar ellipsis',
            );
          }

          if (scale < 1.3) {
            expect(
              find.descendant(
                of: find.byType(ResponsiveMetricRow),
                matching: find.byType(Row),
              ),
              findsOneWidget,
            );
          } else {
            expect(
              find.descendant(
                of: find.byType(ResponsiveMetricRow),
                matching: find.byType(Column),
              ),
              findsOneWidget,
            );
          }
        },
      );
    }

    testWidgets('KernoMetricText remove ellipsis do caller e deduplica tabularFigures', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.lightTheme,
          home: Scaffold(
            body: KernoMetricText(
              'R\$ 99.999,99',
              style: KernoTypography.metricCard().copyWith(
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final text = tester.widget<Text>(find.byType(Text));
      expect(text.overflow, TextOverflow.visible);
      expect(text.style?.overflow, TextOverflow.visible);
      expect(
        text.style?.fontFeatures
            ?.where((f) => f == const FontFeature.tabularFigures())
            .length,
        1,
      );
    });

    testWidgets('ResponsiveMetricRow e TriadStatusBadge funcionam sob largura horizontal ilimitada', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.lightTheme,
          home: const Scaffold(
            body: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  ResponsiveMetricRow(
                    leading: KernoMetricText('WEGE3'),
                    trailing: KernoMetricText('R\$ 1.200,00'),
                  ),
                  SizedBox(height: KernoSpacing.s2),
                  TriadStatusBadge(
                    label: 'Margem Graham Positiva',
                    icon: Icons.trending_up,
                    tone: TriadStatusTone.emerald,
                  ),
                ],
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(
        find.descendant(
          of: find.byType(ResponsiveMetricRow),
          matching: find.byType(Column),
        ),
        findsOneWidget,
      );
      final semantics = tester.widget<Semantics>(
        find.descendant(
          of: find.byType(TriadStatusBadge),
          matching: find.byWidgetPredicate(
            (w) => w is Semantics && w.excludeSemantics,
          ),
        ),
      );
      expect(semantics.properties.label, 'Margem Graham Positiva');
    });

    testWidgets('GrahamSelectApp conecta lightTheme, darkTheme e ThemeMode.system', (
      tester,
    ) async {
      final mockAuth = MockAuthRepository();
      final mockApi = MockApiClient();
      final mockStorage = MockFlutterSecureStorage();
      when(mockAuth.currentUser).thenReturn(null);
      when(mockAuth.getPersistedToken()).thenAnswer((_) async => null);
      when(mockAuth.authStateChanges).thenAnswer((_) => const Stream.empty());

      await tester.pumpWidget(
        GrahamSelectApp(
          authRepository: mockAuth,
          apiClient: mockApi,
          storage: mockStorage,
        ),
      );
      await tester.pumpAndSettle(const Duration(milliseconds: 500));

      final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
      expect(app.theme?.brightness, Brightness.light);
      expect(app.darkTheme?.brightness, Brightness.dark);
      expect(app.themeMode, ThemeMode.system);
    });
  });
}
