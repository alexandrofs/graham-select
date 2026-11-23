import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

Directory _resolveFrontendRoot() {
  final cwd = Directory.current;
  if (File('${cwd.path}/pubspec.yaml').existsSync() &&
      Directory('${cwd.path}/web').existsSync()) {
    return cwd;
  }
  final nested = Directory('${cwd.path}/frontend');
  if (File('${nested.path}/pubspec.yaml').existsSync() &&
      Directory('${nested.path}/web').existsSync()) {
    return nested;
  }
  throw StateError('Não foi possível localizar o diretório raiz frontend/.');
}

({int width, int height, int colorType}) _readPngHeader(File file) {
  final bytes = file.readAsBytesSync();
  const pngSignature = [137, 80, 78, 71, 13, 10, 26, 10];
  expect(bytes.length, greaterThanOrEqualTo(26), reason: file.path);
  expect(bytes.sublist(0, 8), equals(pngSignature), reason: file.path);
  expect(
    String.fromCharCodes(bytes.sublist(12, 16)),
    equals('IHDR'),
    reason: file.path,
  );
  final data = ByteData.sublistView(Uint8List.fromList(bytes));
  return (
    width: data.getUint32(16, Endian.big),
    height: data.getUint32(20, Endian.big),
    colorType: bytes[25],
  );
}

void main() {
  final root = _resolveFrontendRoot();

  test('Metadados Web/PWA/OpenGraph refletem Kerno e kerno.afsdigital.com.br', () {
    final indexHtml = File('${root.path}/web/index.html').readAsStringSync();
    expect(indexHtml, contains('<html lang="pt-BR">'));
    expect(indexHtml, contains('<base href="\$FLUTTER_BASE_HREF">'));
    expect(
      indexHtml,
      contains('<title>Kerno — O núcleo inteligente do seu dinheiro</title>'),
    );
    expect(
      indexHtml,
      contains('content="Kerno — O núcleo inteligente do seu dinheiro"'),
    );
    expect(
      indexHtml,
      contains('<link rel="canonical" href="https://kerno.afsdigital.com.br/">'),
    );
    expect(
      indexHtml,
      contains('property="og:site_name" content="Kerno"'),
    );
    expect(
      indexHtml,
      contains('property="og:title" content="Kerno — O núcleo inteligente do seu dinheiro"'),
    );
    expect(
      indexHtml,
      contains('property="og:description" content="'),
    );
    expect(
      indexHtml,
      contains('property="og:url" content="https://kerno.afsdigital.com.br/"'),
    );
    expect(
      indexHtml,
      contains(
        'property="og:image" content="https://kerno.afsdigital.com.br/og-image.png"',
      ),
    );
    expect(
      indexHtml,
      contains('<meta name="twitter:card" content="summary_large_image">'),
    );
    expect(
      indexHtml,
      contains(
        '<meta name="twitter:title" content="Kerno — O núcleo inteligente do seu dinheiro">',
      ),
    );
    expect(
      indexHtml,
      contains('<meta name="twitter:description" content="'),
    );
    expect(
      indexHtml,
      contains(
        '<meta name="twitter:image" content="https://kerno.afsdigital.com.br/og-image.png">',
      ),
    );
    expect(indexHtml, contains('<meta name="theme-color" content="#4F46E5">'));
    expect(
      indexHtml,
      contains('<meta name="apple-mobile-web-app-title" content="Kerno">'),
    );
    expect(
      indexHtml,
      contains('<link rel="apple-touch-icon" href="icons/Icon-maskable-192.png">'),
    );
    expect(indexHtml, isNot(contains('Graham Select')));
    expect(indexHtml, isNot(contains('content="frontend"')));

    final manifest = jsonDecode(
      File('${root.path}/web/manifest.json').readAsStringSync(),
    ) as Map<String, dynamic>;
    expect(manifest['name'], equals('Kerno'));
    expect(manifest['short_name'], equals('Kerno'));
    expect(manifest['theme_color'], equals('#4F46E5'));
    expect(manifest['background_color'], equals('#F8FAFC'));

    final pushSw = File('${root.path}/web/push-sw.js').readAsStringSync();
    expect(pushSw, contains('// Web Push Service Worker for Kerno'));
    expect(pushSw, contains("'Zona de Compra Graham'"));
    expect(pushSw, contains("'/graham-recommendations'"));
  });

  test('Ativos visuais SVG e PNGs possuem dimensões e modos exatos', () {
    final svg = File('${root.path}/assets/images/logo.svg').readAsStringSync();
    expect(svg, contains('<svg'));
    expect(svg, contains('#4F46E5'));
    expect(svg, contains('#FFFFFF'));

    const expectedSizes = <String, (int, int)>{
      'assets/images/logo.png': (640, 640),
      'web/favicon.png': (32, 32),
      'web/icons/Icon-192.png': (192, 192),
      'web/icons/Icon-512.png': (512, 512),
      'web/icons/Icon-maskable-192.png': (192, 192),
      'web/icons/Icon-maskable-512.png': (512, 512),
      'web/og-image.png': (1200, 630),
      'android/app/src/main/res/mipmap-mdpi/ic_launcher.png': (48, 48),
      'android/app/src/main/res/mipmap-hdpi/ic_launcher.png': (72, 72),
      'android/app/src/main/res/mipmap-xhdpi/ic_launcher.png': (96, 96),
      'android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png': (144, 144),
      'android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png': (192, 192),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_16.png': (16, 16),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_32.png': (32, 32),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_64.png': (64, 64),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_128.png': (128, 128),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_256.png': (256, 256),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_512.png': (512, 512),
      'macos/Runner/Assets.xcassets/AppIcon.appiconset/app_icon_1024.png': (1024, 1024),
    };

    for (final entry in expectedSizes.entries) {
      final info = _readPngHeader(File('${root.path}/${entry.key}'));
      expect((info.width, info.height), equals(entry.value), reason: entry.key);
    }

    final iosContents = jsonDecode(
      File(
        '${root.path}/ios/Runner/Assets.xcassets/AppIcon.appiconset/Contents.json',
      ).readAsStringSync(),
    ) as Map<String, dynamic>;
    final iosImages = (iosContents['images'] as List<dynamic>)
        .cast<Map<String, dynamic>>();
    for (final img in iosImages) {
      final filename = img['filename'] as String;
      final sizePart = (img['size'] as String).split('x').first;
      final scalePart = (img['scale'] as String).replaceAll('x', '');
      final expectedPx =
          (double.parse(sizePart) * int.parse(scalePart)).round();
      final info = _readPngHeader(
        File(
          '${root.path}/ios/Runner/Assets.xcassets/AppIcon.appiconset/$filename',
        ),
      );
      expect(
        (info.width, info.height),
        equals((expectedPx, expectedPx)),
        reason: filename,
      );
      // App Store requer PNGs de AppIcon iOS sem canal alpha (colorType == 2: RGB).
      expect(info.colorType, equals(2), reason: '$filename deve ser RGB opaco');
    }
  });

  test('Aplicativos nativos Android/iOS/macOS e pubspec preservam IDs com marca Kerno', () {
    final androidManifest = File(
      '${root.path}/android/app/src/main/AndroidManifest.xml',
    ).readAsStringSync();
    expect(androidManifest, contains('android:label="Kerno"'));

    final androidGradle = File(
      '${root.path}/android/app/build.gradle.kts',
    ).readAsStringSync();
    expect(androidGradle, contains('applicationId = "com.example.frontend"'));

    final iosPlist = File('${root.path}/ios/Runner/Info.plist')
        .readAsStringSync()
        .replaceAll('\r\n', '\n');
    expect(
      iosPlist,
      contains('<key>CFBundleDisplayName</key>\n\t<string>Kerno</string>'),
    );
    expect(
      iosPlist,
      contains('<key>CFBundleName</key>\n\t<string>Kerno</string>'),
    );
    expect(iosPlist, contains('CFBundleURLSchemes'));
    expect(iosPlist, contains(r'$(PRODUCT_BUNDLE_IDENTIFIER)'));

    final macosConfig = File(
      '${root.path}/macos/Runner/Configs/AppInfo.xcconfig',
    ).readAsStringSync();
    expect(macosConfig, contains('PRODUCT_NAME = Kerno'));
    expect(
      macosConfig,
      contains('PRODUCT_BUNDLE_IDENTIFIER = com.example.frontend'),
    );

    final macosPbxproj = File(
      '${root.path}/macos/Runner.xcodeproj/project.pbxproj',
    ).readAsStringSync();
    expect(macosPbxproj, contains('Kerno.app'));
    expect(
      macosPbxproj,
      contains(
        r'$(BUILT_PRODUCTS_DIR)/Kerno.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/Kerno',
      ),
    );
    expect(
      macosPbxproj,
      contains('PRODUCT_BUNDLE_IDENTIFIER = com.example.frontend.RunnerTests;'),
    );
    expect(macosPbxproj, isNot(contains('frontend.app')));
    expect(macosPbxproj, isNot(contains('/frontend"')));

    final macosScheme = File(
      '${root.path}/macos/Runner.xcodeproj/xcshareddata/xcschemes/Runner.xcscheme',
    ).readAsStringSync();
    expect(macosScheme, contains('BuildableName = "Kerno.app"'));
    expect(macosScheme, isNot(contains('frontend.app')));

    final pubspec = File('${root.path}/pubspec.yaml').readAsStringSync();
    expect(pubspec, contains('name: frontend'));
    expect(pubspec, contains('Kerno'));

    final mainDart = File('${root.path}/lib/main.dart').readAsStringSync();
    expect(
      mainDart,
      contains("title: 'Kerno — O núcleo inteligente do seu dinheiro'"),
    );
  });
}
