import 'dart:io';

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

void main() {
  final root = _resolveFrontendRoot();

  test('landing.html é leve, sem Flutter/CanvasKit e com narrativa e Founders', () {
    final landing = File('${root.path}/web/landing.html').readAsStringSync();

    // Sem bundle Flutter Web / CanvasKit / dependências externas bloqueantes
    expect(landing, isNot(contains('flutter_bootstrap.js')));
    expect(landing, isNot(contains('canvaskit')));
    expect(landing, isNot(contains('fonts.googleapis.com')));
    expect(landing, isNot(contains('Graham Select')));

    // SEO e OpenGraph
    expect(landing, contains('<meta name="robots" content="index, follow">'));
    expect(landing, contains('<link rel="canonical" href="https://kerno.afsdigital.com.br/">'));
    expect(landing, contains('<title>Kerno — O núcleo inteligente do seu dinheiro</title>'));
    expect(landing, contains('og-image.png'));
    expect(landing, contains('favicon.png'));

    // Paleta Light-First Kerno e Ouro Founder
    expect(landing, contains('#F8FAFC'));
    expect(landing, contains('#FFFFFF'));
    expect(landing, contains('#0F172A'));
    expect(landing, contains('#4F46E5'));
    expect(landing, contains('#92400E'));
    expect(landing, contains('#FEF3C7'));
    expect(landing, contains('#F59E0B'));

    // Narrativa, previews e metodologia
    expect(
      landing,
      contains('Do caos das planilhas ao núcleo inteligente das suas finanças'),
    );
    expect(landing, contains('GPS de Aporte'));
    expect(landing, contains('1.30x'));
    expect(landing, contains('Margem de Graham'));
    expect(landing, contains('Benjamin Graham'));
    expect(landing, contains('Diagrama do Cerrado'));
    expect(landing, contains('Kerno AI'));

    // Programa Founders dinâmico + fallback Trial Pro 90 dias
    expect(landing, contains('/api/v1/founders/slots'));
    expect(
      landing,
      contains('Seja um Membro Founder — Acesso Pro Vitalício Gratuito'),
    );
    expect(
      landing,
      contains('Garantir Minha Vaga de Founder Gratuitamente'),
    );
    expect(landing, contains('Trial Pro de 90 dias gratuitos'));
    expect(landing, contains('href="/login"'));
    expect(landing, contains('href="/terms"'));
    expect(landing, contains('href="/privacy"'));
    expect(landing, contains('href="/docs"'));
    expect(landing, contains('Disponibilidade no login'));
    expect(landing, contains('Number.isFinite(data.remainingSlots)'));
    expect(landing, contains('id="founders-slots-value" aria-live="polite"'));
    expect(landing, contains('class="bar-track" aria-hidden="true"'));

    // Disclaimer regulatório CVM 19/20
    expect(landing, contains('CVM 19/20'));
  });

  test('terms.html e privacy.html atendem CVM 19/20, LGPD e Marco Civil Art. 15', () {
    final terms = File('${root.path}/web/terms.html').readAsStringSync();
    expect(terms, isNot(contains('flutter_bootstrap.js')));
    expect(terms, isNot(contains('Graham Select')));
    expect(terms, isNot(contains('FOUNDER_LIFETIME')));
    expect(terms, contains('<meta name="robots" content="index, follow">'));
    expect(terms, contains('CVM 19/20'));
    expect(terms, contains('ferramenta matemática parametrizada'));

    final privacy = File('${root.path}/web/privacy.html').readAsStringSync();
    expect(privacy, isNot(contains('flutter_bootstrap.js')));
    expect(privacy, isNot(contains('Graham Select')));
    expect(privacy, contains('<meta name="robots" content="index, follow">'));
    expect(privacy, contains('LGPD'));
    expect(privacy, contains('Portabilidade'));
    expect(privacy, contains('Direito ao Esquecimento'));
    expect(privacy, contains('0 bytes PII'));
    expect(privacy, contains('6 meses'));
    expect(privacy, contains('Art. 15'));
    expect(privacy, contains('12.965/2014'));
  });

  test('robots.txt e nginx.conf isolam rotas privadas e preservam healthz/SPA', () {
    final robots = File('${root.path}/web/robots.txt').readAsStringSync();
    expect(robots, contains('Allow: /'));
    expect(robots, contains('Allow: /terms'));
    expect(robots, contains('Allow: /privacy'));
    expect(robots, contains('Allow: /docs'));
    expect(robots, contains('Disallow: /app/'));
    expect(robots, contains('Disallow: /dashboard/'));
    expect(robots, contains('Disallow: /api/'));

    final nginx = File('${root.path}/nginx.conf').readAsStringSync();
    expect(nginx, contains('listen 8080 default_server;'));
    expect(nginx, contains('location = /healthz'));
    expect(nginx, contains('location = / {'));
    expect(nginx, contains('try_files /landing.html =404;'));
    expect(nginx, contains('location ~ ^/(terms|privacy)/?\$ {'));
    expect(nginx, contains('try_files /\$1.html =404;'));
    expect(nginx, contains('location = /robots.txt {'));
    expect(nginx, contains('try_files /robots.txt =404;'));
    expect(nginx, contains('location = /docs {'));
    expect(nginx, contains('add_header X-Robots-Tag "index, follow" always;'));
    expect(nginx, contains('location ^~ /app/ {'));
    expect(nginx, contains('location ^~ /dashboard/ {'));
    expect(nginx, contains('add_header X-Robots-Tag "noindex, nofollow" always;'));
    expect(nginx, contains('try_files \$uri \$uri/ /index.html;'));
  });
}
