import 'package:flutter/material.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';

class AppTheme {
  // Constantes legadas preservadas para compatibilidade de compilação com
  // módulos existentes que serão migrados nas Stories 2.3–2.5.
  static const primaryColor = Color(0xFF6366F1);
  static const secondaryColor = Color(0xFF8B5CF6);
  static const accentColor = Color(0xFF10B981);
  static const backgroundColor = Color(0xFF0F172A);
  static const surfaceColor = Color(0xFF1E293B);
  static const textColor = Color(0xFFF8FAFC);

  static ThemeData get lightTheme => _buildTheme(
    brightness: Brightness.light,
    tokens: KernoThemeData.light,
  );

  static ThemeData get darkTheme => _buildTheme(
    brightness: Brightness.dark,
    tokens: KernoThemeData.dark,
  );

  static ThemeData _buildTheme({
    required Brightness brightness,
    required KernoThemeData tokens,
  }) {
    final isDark = brightness == Brightness.dark;
    final baseTheme = isDark ? ThemeData.dark() : ThemeData.light();
    final colorScheme = isDark
        ? ColorScheme.dark(
            primary: tokens.brandPrimary,
            secondary: KernoColors.brandPrimaryDark,
            surface: tokens.surfaceElevated,
            onPrimary: tokens.textOnPrimary,
            onSecondary: tokens.textOnPrimary,
            onSurface: tokens.textPrimary,
            onSurfaceVariant: tokens.textSecondary,
            error: tokens.crimson,
            onError: tokens.textOnPrimary,
            outline: tokens.borderStrong,
            outlineVariant: tokens.borderSubtle,
          )
        : ColorScheme.light(
            primary: tokens.brandPrimary,
            secondary: KernoColors.brandPrimaryDeep,
            surface: tokens.surfaceElevated,
            onPrimary: tokens.textOnPrimary,
            onSecondary: tokens.textOnPrimary,
            onSurface: tokens.textPrimary,
            onSurfaceVariant: tokens.textSecondary,
            error: tokens.crimson,
            onError: tokens.textOnPrimary,
            outline: tokens.borderStrong,
            outlineVariant: tokens.borderSubtle,
          );

    const minTouchSize = Size(
      KernoSpacing.touchTargetMin,
      KernoSpacing.touchTargetMin,
    );
    final buttonShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(KernoRadius.md),
    );
    final buttonTextStyle = KernoTypography.bodyBase().copyWith(
      fontWeight: FontWeight.w600,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      primaryColor: tokens.brandPrimary,
      scaffoldBackgroundColor: tokens.surfaceBase,
      colorScheme: colorScheme,
      extensions: <ThemeExtension<dynamic>>[tokens],
      textTheme: KernoTypography.buildTextTheme(
        base: baseTheme.textTheme,
        primaryColor: tokens.textPrimary,
        secondaryColor: tokens.textSecondary,
      ),
      cardTheme: CardThemeData(
        color: tokens.surfaceElevated,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(KernoRadius.lg),
          side: BorderSide(color: tokens.borderSubtle),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: tokens.borderSubtle,
        thickness: 1.0,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: tokens.brandPrimary,
          foregroundColor: tokens.textOnPrimary,
          minimumSize: minTouchSize,
          padding: const EdgeInsets.symmetric(horizontal: KernoSpacing.s6),
          shape: buttonShape,
          textStyle: buttonTextStyle,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: tokens.brandPrimary,
          foregroundColor: tokens.textOnPrimary,
          minimumSize: minTouchSize,
          padding: const EdgeInsets.symmetric(horizontal: KernoSpacing.s6),
          shape: buttonShape,
          textStyle: buttonTextStyle,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          backgroundColor: tokens.surfaceElevated,
          foregroundColor: tokens.textPrimary,
          minimumSize: minTouchSize,
          padding: const EdgeInsets.symmetric(horizontal: KernoSpacing.s5),
          side: BorderSide(color: tokens.borderStrong, width: 1.5),
          shape: buttonShape,
          textStyle: buttonTextStyle,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: tokens.brandPrimary,
          minimumSize: minTouchSize,
          padding: const EdgeInsets.symmetric(horizontal: KernoSpacing.s4),
          shape: buttonShape,
          textStyle: buttonTextStyle,
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(
          minimumSize: minTouchSize,
          foregroundColor: tokens.textPrimary,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: tokens.surfaceElevated,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: KernoSpacing.s4,
          vertical: KernoSpacing.s3,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(KernoRadius.md),
          borderSide: BorderSide(color: tokens.borderStrong, width: 1.5),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(KernoRadius.md),
          borderSide: BorderSide(color: tokens.borderStrong, width: 1.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(KernoRadius.md),
          borderSide: BorderSide(color: tokens.borderFocus, width: 2.0),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(KernoRadius.md),
          borderSide: BorderSide(color: tokens.crimson, width: 1.5),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(KernoRadius.md),
          borderSide: BorderSide(color: tokens.crimson, width: 2.0),
        ),
      ),
    );
  }
}
