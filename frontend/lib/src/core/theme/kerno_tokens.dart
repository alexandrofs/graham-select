import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Paleta oficial do Design System Intergeracional do Kerno (`DESIGN.md`).
///
/// Construída sob a filosofia Light-First respirável com paridade nativa de Dark Mode,
/// garantindo contraste WCAG 2.1 AA (>= 4.5:1), AAA (>= 7.0:1) em saldos/alertas
/// e >= 3.0:1 em bordas de componentes não-textuais (WCAG 1.4.11).
abstract final class KernoColors {
  // Superfícies Light
  static const Color surfaceBase = Color(0xFFF8FAFC);
  static const Color surfaceElevated = Color(0xFFFFFFFF);
  static const Color surfaceSubtle = Color(0xFFF1F5F9);
  static const Color surfaceOverlay = Color(0xFF0F172A);

  // Superfícies Dark
  static const Color surfaceBaseDark = Color(0xFF0B0F19);
  static const Color surfaceElevatedDark = Color(0xFF111827);
  static const Color surfaceSubtleDark = Color(0xFF1E293B);

  // Tipografia Light
  static const Color textPrimary = Color(0xFF0F172A);
  static const Color textSecondary = Color(0xFF334155);
  static const Color textMuted = Color(0xFF475569);
  static const Color textOnPrimary = Color(0xFFFFFFFF);

  // Tipografia Dark
  static const Color textPrimaryDark = Color(0xFFF8FAFC);
  static const Color textSecondaryDark = Color(0xFFCBD5E1);
  static const Color textMutedDark = Color(0xFF94A3B8);

  // Identidade Kerno (Roxo-Índigo Institucional)
  static const Color brandPrimary = Color(0xFF4F46E5);
  static const Color brandPrimaryHover = Color(0xFF4338CA);
  static const Color brandPrimaryDeep = Color(0xFF3730A3);
  static const Color brandAccentSoft = Color(0xFFEEF2FF);
  static const Color brandPrimaryDark = Color(0xFF818CF8);
  static const Color brandAccentSoftDark = Color(0xFF1E1B4B);

  // Semântica Esmeralda (Margem Graham & Aporte)
  static const Color semanticEmerald = Color(0xFF065F46);
  static const Color semanticEmeraldSurface = Color(0xFFECFDF5);
  static const Color semanticEmeraldBorder = Color(0xFFA7F3D0);
  static const Color semanticEmeraldDark = Color(0xFF6EE7B7);
  static const Color semanticEmeraldSurfaceDark = Color(0xFF064E3B);

  // Semântica Âmbar (Atenção & Quarentena)
  static const Color semanticAmber = Color(0xFF92400E);
  static const Color semanticAmberSurface = Color(0xFFFFFBEB);
  static const Color semanticAmberBorder = Color(0xFFFDE68A);
  static const Color semanticAmberDark = Color(0xFFFBBF24);
  static const Color semanticAmberSurfaceDark = Color(0xFF451A03);

  // Semântica Carmesim (Erro & Bloqueio)
  static const Color semanticCrimson = Color(0xFF991B1B);
  static const Color semanticCrimsonSurface = Color(0xFFFEF2F2);
  static const Color semanticCrimsonBorder = Color(0xFFFECACA);
  static const Color semanticCrimsonDark = Color(0xFFFCA5A5);
  static const Color semanticCrimsonSurfaceDark = Color(0xFF450A0A);

  // Ouro Founder (Selo Vitalício)
  static const Color founderGold = Color(0xFF92400E);
  static const Color founderGoldSurface = Color(0xFFFEF3C7);
  static const Color founderGoldBorder = Color(0xFFF59E0B);
  static const Color founderGoldDark = Color(0xFFFDE68A);
  static const Color founderGoldSurfaceDark = Color(0xFF78350F);

  // Bordas (WCAG 1.4.11 >= 3:1 para borderStrong)
  static const Color borderSubtle = Color(0xFFE2E8F0);
  static const Color borderStrong = Color(0xFF64748B);
  static const Color borderFocus = Color(0xFF4F46E5);
  static const Color borderSubtleDark = Color(0xFF1E293B);
  static const Color borderStrongDark = Color(0xFF64748B);
  static const Color borderFocusDark = Color(0xFF818CF8);
}

/// Escala de espaçamentos em grade 4dp/8dp (`DESIGN.md`).
abstract final class KernoSpacing {
  static const double s1 = 4.0;
  static const double s2 = 8.0;
  static const double s3 = 12.0;
  static const double s4 = 16.0;
  static const double s5 = 20.0;
  static const double s6 = 24.0;
  static const double s8 = 32.0;
  static const double s10 = 40.0;
  static const double s12 = 48.0;

  static const double cardPaddingMobile = 16.0;
  static const double cardPaddingDesktop = 24.0;
  static const double screenMarginMobile = 16.0;
  static const double screenMarginDesktop = 32.0;
  static const double sectionGap = 24.0;
  static const double touchTargetMin = 48.0;
}

/// Raios de borda do Design System Intergeracional (`DESIGN.md`).
abstract final class KernoRadius {
  static const double none = 0.0;
  static const double sm = 6.0;
  static const double md = 10.0;
  static const double lg = 16.0;
  static const double xl = 24.0;
  static const double xxl = 32.0;
  static const double full = 9999.0;
}

/// Escala tipográfica do Kerno com `Plus Jakarta Sans` (títulos/display)
/// e `Inter` com `FontFeature.tabularFigures()` obrigatório para alinhamento
/// vertical exato de cifras, tickers e percentuais (`DESIGN.md`).
abstract final class KernoTypography {
  static const List<FontFeature> tabularFeatures = <FontFeature>[
    FontFeature.tabularFigures(),
  ];

  static TextStyle displayWealth({Color? color}) =>
      GoogleFonts.plusJakartaSans(
        fontSize: 36.0,
        fontWeight: FontWeight.w700,
        height: 1.15,
        letterSpacing: -0.72,
        color: color,
      ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle heading1({Color? color}) => GoogleFonts.plusJakartaSans(
    fontSize: 28.0,
    fontWeight: FontWeight.w700,
    height: 1.2,
    letterSpacing: -0.42,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle heading2({Color? color}) => GoogleFonts.plusJakartaSans(
    fontSize: 22.0,
    fontWeight: FontWeight.w600,
    height: 1.25,
    letterSpacing: -0.22,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle heading3({Color? color}) => GoogleFonts.plusJakartaSans(
    fontSize: 18.0,
    fontWeight: FontWeight.w600,
    height: 1.3,
    letterSpacing: -0.09,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle metricHero({Color? color}) => GoogleFonts.inter(
    fontSize: 32.0,
    fontWeight: FontWeight.w700,
    height: 1.2,
    letterSpacing: -0.48,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle metricCard({Color? color}) => GoogleFonts.inter(
    fontSize: 20.0,
    fontWeight: FontWeight.w600,
    height: 1.25,
    letterSpacing: -0.20,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle tickerBadge({Color? color}) => GoogleFonts.inter(
    fontSize: 14.0,
    fontWeight: FontWeight.w700,
    height: 1.2,
    letterSpacing: 0.56,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle bodyLg({Color? color}) => GoogleFonts.inter(
    fontSize: 18.0,
    fontWeight: FontWeight.w400,
    height: 1.55,
    letterSpacing: 0.0,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle bodyBase({Color? color}) => GoogleFonts.inter(
    fontSize: 16.0,
    fontWeight: FontWeight.w400,
    height: 1.5,
    letterSpacing: 0.0,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle bodySm({Color? color}) => GoogleFonts.inter(
    fontSize: 14.0,
    fontWeight: FontWeight.w500,
    height: 1.45,
    letterSpacing: 0.07,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  static TextStyle caption({Color? color}) => GoogleFonts.inter(
    fontSize: 12.0,
    fontWeight: FontWeight.w500,
    height: 1.4,
    letterSpacing: 0.12,
    color: color,
  ).copyWith(fontFeatures: tabularFeatures);

  /// Constrói o [TextTheme] completo do Material 3 combinando `Plus Jakarta Sans`
  /// nos papéis de exibição/título e `Inter` tabular nos papéis de corpo/rótulo.
  static TextTheme buildTextTheme({
    required TextTheme base,
    required Color primaryColor,
    required Color secondaryColor,
  }) {
    return GoogleFonts.interTextTheme(base)
        .apply(
          bodyColor: primaryColor,
          displayColor: primaryColor,
        )
        .copyWith(
          displayLarge: displayWealth(color: primaryColor),
          displayMedium: heading1(color: primaryColor),
          displaySmall: heading2(color: primaryColor),
          headlineLarge: heading1(color: primaryColor),
          headlineMedium: heading2(color: primaryColor),
          headlineSmall: heading3(color: primaryColor),
          titleLarge: heading2(color: primaryColor),
          titleMedium: heading3(color: primaryColor),
          titleSmall: bodySm(color: primaryColor).copyWith(
            fontWeight: FontWeight.w600,
          ),
          bodyLarge: bodyLg(color: primaryColor),
          bodyMedium: bodyBase(color: primaryColor),
          bodySmall: bodySm(color: secondaryColor),
          labelLarge: bodyBase(color: primaryColor).copyWith(
            fontWeight: FontWeight.w600,
          ),
          labelMedium: bodySm(color: secondaryColor),
          labelSmall: caption(color: secondaryColor),
        );
  }
}

/// Extensão de tema semântica do Kerno para acesso direto aos tokens Light/Dark
/// com interpolação suave (`lerp`) e fallback seguro via [KernoThemeData.of].
@immutable
class KernoThemeData extends ThemeExtension<KernoThemeData> {
  const KernoThemeData({
    required this.surfaceBase,
    required this.surfaceElevated,
    required this.surfaceSubtle,
    required this.surfaceOverlay,
    required this.textPrimary,
    required this.textSecondary,
    required this.textMuted,
    required this.textOnPrimary,
    required this.brandPrimary,
    required this.brandPrimaryHover,
    required this.brandPrimaryDeep,
    required this.brandAccentSoft,
    required this.emerald,
    required this.emeraldSurface,
    required this.emeraldBorder,
    required this.amber,
    required this.amberSurface,
    required this.amberBorder,
    required this.crimson,
    required this.crimsonSurface,
    required this.crimsonBorder,
    required this.founderGold,
    required this.founderGoldSurface,
    required this.founderGoldBorder,
    required this.borderSubtle,
    required this.borderStrong,
    required this.borderFocus,
  });

  final Color surfaceBase;
  final Color surfaceElevated;
  final Color surfaceSubtle;
  final Color surfaceOverlay;
  final Color textPrimary;
  final Color textSecondary;
  final Color textMuted;
  final Color textOnPrimary;
  final Color brandPrimary;
  final Color brandPrimaryHover;
  final Color brandPrimaryDeep;
  final Color brandAccentSoft;
  final Color emerald;
  final Color emeraldSurface;
  final Color emeraldBorder;
  final Color amber;
  final Color amberSurface;
  final Color amberBorder;
  final Color crimson;
  final Color crimsonSurface;
  final Color crimsonBorder;
  final Color founderGold;
  final Color founderGoldSurface;
  final Color founderGoldBorder;
  final Color borderSubtle;
  final Color borderStrong;
  final Color borderFocus;

  static const KernoThemeData light = KernoThemeData(
    surfaceBase: KernoColors.surfaceBase,
    surfaceElevated: KernoColors.surfaceElevated,
    surfaceSubtle: KernoColors.surfaceSubtle,
    surfaceOverlay: KernoColors.surfaceOverlay,
    textPrimary: KernoColors.textPrimary,
    textSecondary: KernoColors.textSecondary,
    textMuted: KernoColors.textMuted,
    textOnPrimary: KernoColors.textOnPrimary,
    brandPrimary: KernoColors.brandPrimary,
    brandPrimaryHover: KernoColors.brandPrimaryHover,
    brandPrimaryDeep: KernoColors.brandPrimaryDeep,
    brandAccentSoft: KernoColors.brandAccentSoft,
    emerald: KernoColors.semanticEmerald,
    emeraldSurface: KernoColors.semanticEmeraldSurface,
    emeraldBorder: KernoColors.semanticEmeraldBorder,
    amber: KernoColors.semanticAmber,
    amberSurface: KernoColors.semanticAmberSurface,
    amberBorder: KernoColors.semanticAmberBorder,
    crimson: KernoColors.semanticCrimson,
    crimsonSurface: KernoColors.semanticCrimsonSurface,
    crimsonBorder: KernoColors.semanticCrimsonBorder,
    founderGold: KernoColors.founderGold,
    founderGoldSurface: KernoColors.founderGoldSurface,
    founderGoldBorder: KernoColors.founderGoldBorder,
    borderSubtle: KernoColors.borderSubtle,
    borderStrong: KernoColors.borderStrong,
    borderFocus: KernoColors.borderFocus,
  );

  static const KernoThemeData dark = KernoThemeData(
    surfaceBase: KernoColors.surfaceBaseDark,
    surfaceElevated: KernoColors.surfaceElevatedDark,
    surfaceSubtle: KernoColors.surfaceSubtleDark,
    surfaceOverlay: KernoColors.surfaceOverlay,
    textPrimary: KernoColors.textPrimaryDark,
    textSecondary: KernoColors.textSecondaryDark,
    textMuted: KernoColors.textMutedDark,
    textOnPrimary: KernoColors.textPrimary,
    brandPrimary: KernoColors.brandPrimaryDark,
    brandPrimaryHover: KernoColors.brandPrimary,
    brandPrimaryDeep: KernoColors.brandPrimaryDeep,
    brandAccentSoft: KernoColors.brandAccentSoftDark,
    emerald: KernoColors.semanticEmeraldDark,
    emeraldSurface: KernoColors.semanticEmeraldSurfaceDark,
    emeraldBorder: KernoColors.semanticEmeraldBorder,
    amber: KernoColors.semanticAmberDark,
    amberSurface: KernoColors.semanticAmberSurfaceDark,
    amberBorder: KernoColors.semanticAmberBorder,
    crimson: KernoColors.semanticCrimsonDark,
    crimsonSurface: KernoColors.semanticCrimsonSurfaceDark,
    crimsonBorder: KernoColors.semanticCrimsonBorder,
    founderGold: KernoColors.founderGoldDark,
    founderGoldSurface: KernoColors.founderGoldSurfaceDark,
    founderGoldBorder: KernoColors.founderGoldBorder,
    borderSubtle: KernoColors.borderSubtleDark,
    borderStrong: KernoColors.borderStrongDark,
    borderFocus: KernoColors.borderFocusDark,
  );

  /// Recupera o [KernoThemeData] do [BuildContext] com fallback seguro para
  /// [light] ou [dark] conforme o [Brightness] ativo quando a extensão não
  /// estiver anexada ao [ThemeData].
  static KernoThemeData of(BuildContext context) {
    final theme = Theme.of(context);
    return theme.extension<KernoThemeData>() ??
        (theme.brightness == Brightness.dark ? dark : light);
  }

  @override
  KernoThemeData copyWith({
    Color? surfaceBase,
    Color? surfaceElevated,
    Color? surfaceSubtle,
    Color? surfaceOverlay,
    Color? textPrimary,
    Color? textSecondary,
    Color? textMuted,
    Color? textOnPrimary,
    Color? brandPrimary,
    Color? brandPrimaryHover,
    Color? brandPrimaryDeep,
    Color? brandAccentSoft,
    Color? emerald,
    Color? emeraldSurface,
    Color? emeraldBorder,
    Color? amber,
    Color? amberSurface,
    Color? amberBorder,
    Color? crimson,
    Color? crimsonSurface,
    Color? crimsonBorder,
    Color? founderGold,
    Color? founderGoldSurface,
    Color? founderGoldBorder,
    Color? borderSubtle,
    Color? borderStrong,
    Color? borderFocus,
  }) {
    return KernoThemeData(
      surfaceBase: surfaceBase ?? this.surfaceBase,
      surfaceElevated: surfaceElevated ?? this.surfaceElevated,
      surfaceSubtle: surfaceSubtle ?? this.surfaceSubtle,
      surfaceOverlay: surfaceOverlay ?? this.surfaceOverlay,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      textMuted: textMuted ?? this.textMuted,
      textOnPrimary: textOnPrimary ?? this.textOnPrimary,
      brandPrimary: brandPrimary ?? this.brandPrimary,
      brandPrimaryHover: brandPrimaryHover ?? this.brandPrimaryHover,
      brandPrimaryDeep: brandPrimaryDeep ?? this.brandPrimaryDeep,
      brandAccentSoft: brandAccentSoft ?? this.brandAccentSoft,
      emerald: emerald ?? this.emerald,
      emeraldSurface: emeraldSurface ?? this.emeraldSurface,
      emeraldBorder: emeraldBorder ?? this.emeraldBorder,
      amber: amber ?? this.amber,
      amberSurface: amberSurface ?? this.amberSurface,
      amberBorder: amberBorder ?? this.amberBorder,
      crimson: crimson ?? this.crimson,
      crimsonSurface: crimsonSurface ?? this.crimsonSurface,
      crimsonBorder: crimsonBorder ?? this.crimsonBorder,
      founderGold: founderGold ?? this.founderGold,
      founderGoldSurface: founderGoldSurface ?? this.founderGoldSurface,
      founderGoldBorder: founderGoldBorder ?? this.founderGoldBorder,
      borderSubtle: borderSubtle ?? this.borderSubtle,
      borderStrong: borderStrong ?? this.borderStrong,
      borderFocus: borderFocus ?? this.borderFocus,
    );
  }

  @override
  KernoThemeData lerp(covariant ThemeExtension<KernoThemeData>? other, double t) {
    if (other is! KernoThemeData) {
      return this;
    }
    return KernoThemeData(
      surfaceBase: Color.lerp(surfaceBase, other.surfaceBase, t)!,
      surfaceElevated: Color.lerp(surfaceElevated, other.surfaceElevated, t)!,
      surfaceSubtle: Color.lerp(surfaceSubtle, other.surfaceSubtle, t)!,
      surfaceOverlay: Color.lerp(surfaceOverlay, other.surfaceOverlay, t)!,
      textPrimary: Color.lerp(textPrimary, other.textPrimary, t)!,
      textSecondary: Color.lerp(textSecondary, other.textSecondary, t)!,
      textMuted: Color.lerp(textMuted, other.textMuted, t)!,
      textOnPrimary: Color.lerp(textOnPrimary, other.textOnPrimary, t)!,
      brandPrimary: Color.lerp(brandPrimary, other.brandPrimary, t)!,
      brandPrimaryHover: Color.lerp(brandPrimaryHover, other.brandPrimaryHover, t)!,
      brandPrimaryDeep: Color.lerp(brandPrimaryDeep, other.brandPrimaryDeep, t)!,
      brandAccentSoft: Color.lerp(brandAccentSoft, other.brandAccentSoft, t)!,
      emerald: Color.lerp(emerald, other.emerald, t)!,
      emeraldSurface: Color.lerp(emeraldSurface, other.emeraldSurface, t)!,
      emeraldBorder: Color.lerp(emeraldBorder, other.emeraldBorder, t)!,
      amber: Color.lerp(amber, other.amber, t)!,
      amberSurface: Color.lerp(amberSurface, other.amberSurface, t)!,
      amberBorder: Color.lerp(amberBorder, other.amberBorder, t)!,
      crimson: Color.lerp(crimson, other.crimson, t)!,
      crimsonSurface: Color.lerp(crimsonSurface, other.crimsonSurface, t)!,
      crimsonBorder: Color.lerp(crimsonBorder, other.crimsonBorder, t)!,
      founderGold: Color.lerp(founderGold, other.founderGold, t)!,
      founderGoldSurface: Color.lerp(founderGoldSurface, other.founderGoldSurface, t)!,
      founderGoldBorder: Color.lerp(founderGoldBorder, other.founderGoldBorder, t)!,
      borderSubtle: Color.lerp(borderSubtle, other.borderSubtle, t)!,
      borderStrong: Color.lerp(borderStrong, other.borderStrong, t)!,
      borderFocus: Color.lerp(borderFocus, other.borderFocus, t)!,
    );
  }
}
