import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../data/auth_repository.dart';

class LoginScreen extends StatefulWidget {
  final Future<bool> Function(Uri uri)? onLaunchUrl;

  const LoginScreen({super.key, this.onLaunchUrl});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _isLoading = false;

  Uri _resolveLegalUri(String path) {
    final base = Uri.base;
    if (base.scheme == 'http' || base.scheme == 'https') {
      return base.resolve(path);
    }
    return Uri.https('kerno.afsdigital.com.br', path);
  }

  Future<void> _openLegalDocument(BuildContext context, String path) async {
    final uri = _resolveLegalUri(path);
    final launcher =
        widget.onLaunchUrl ??
        (u) => launchUrl(u, mode: LaunchMode.externalApplication);
    var launched = false;
    try {
      launched = await launcher(uri);
    } catch (_) {
      launched = false;
    }
    if (!launched && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Não foi possível abrir $path no navegador.'),
        ),
      );
    }
  }

  Future<void> _signInWithGoogle(BuildContext context) async {
    setState(() {
      _isLoading = true;
    });

    try {
      final authRepository = context.read<AuthRepository>();
      final userCredential = await authRepository.signInWithGoogle();

      if (userCredential != null && context.mounted) {
        context.go('/dashboard');
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Falha ao entrar com Google: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    } finally {
      if (context.mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final bodyStyle = theme.textTheme.bodySmall?.copyWith(
      color: const Color(0xFFCBD5E1),
    );
    final linkStyle = theme.textTheme.bodySmall?.copyWith(
      color: const Color(0xFFA5B4FC),
      fontWeight: FontWeight.w600,
      decoration: TextDecoration.underline,
      decorationColor: const Color(0xFFA5B4FC),
    );

    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 400),
            padding: const EdgeInsets.all(32.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Center(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(14),
                    child: Image.asset(
                      'assets/images/logo.png',
                      width: 56,
                      height: 56,
                      excludeFromSemantics: true,
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  'Kerno',
                  style: theme.textTheme.displaySmall?.copyWith(
                    color: colorScheme.primary,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  'O núcleo inteligente do seu dinheiro.',
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: colorScheme.onSurfaceVariant,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 48),
                if (_isLoading)
                  const Center(child: CircularProgressIndicator())
                else
                  ElevatedButton(
                    onPressed: () => _signInWithGoogle(context),
                    style: ElevatedButton.styleFrom(
                      elevation: 2,
                      minimumSize: const Size(0, 48),
                      padding: const EdgeInsets.symmetric(
                        vertical: 16,
                        horizontal: 16,
                      ),
                      backgroundColor: Colors.white,
                      foregroundColor: Colors.black87,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      side: BorderSide(color: Colors.grey.shade300),
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.account_circle, color: Colors.blue),
                        SizedBox(width: 12),
                        Flexible(
                          child: Text(
                            'Continuar com Google',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontWeight: FontWeight.w600,
                              fontSize: 16,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                const SizedBox(height: 24),
                Wrap(
                  alignment: WrapAlignment.center,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Text(
                      'Ao continuar, você concorda com nossos ',
                      style: bodyStyle,
                      textAlign: TextAlign.center,
                    ),
                    Semantics(
                      link: true,
                      child: TextButton(
                        key: const Key('login_terms_link'),
                        onPressed: () => _openLegalDocument(context, '/terms'),
                        style: TextButton.styleFrom(
                          minimumSize: const Size(48, 48),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 4,
                          ),
                          tapTargetSize: MaterialTapTargetSize.padded,
                        ),
                        child: Text('Termos de Uso', style: linkStyle),
                      ),
                    ),
                    Text(' e ', style: bodyStyle),
                    Semantics(
                      link: true,
                      child: TextButton(
                        key: const Key('login_privacy_link'),
                        onPressed: () =>
                            _openLegalDocument(context, '/privacy'),
                        style: TextButton.styleFrom(
                          minimumSize: const Size(48, 48),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 4,
                          ),
                          tapTargetSize: MaterialTapTargetSize.padded,
                        ),
                        child: Text(
                          'Política de Privacidade',
                          style: linkStyle,
                        ),
                      ),
                    ),
                    Text('.', style: bodyStyle),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
