import 'package:flutter/foundation.dart'
    show TargetPlatform, defaultTargetPlatform, kIsWeb;
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/app_theme.dart';
import 'biometric_auth_service.dart';

/// Gate biométrico nativo que protege rotas autenticadas em dispositivos móveis,
/// exigindo desbloqueio na abertura e após retorno de background (> 2 minutos).
/// Não renderiza [child] enquanto estiver bloqueado.
class BiometricLockGate extends StatefulWidget {
  final Widget child;
  final BiometricAuthService biometricAuthService;
  final Future<void> Function() onSignOut;
  final bool isAuthenticated;
  final bool? isWebOverride;
  final bool autoTriggerOnInit;

  const BiometricLockGate({
    super.key,
    required this.child,
    required this.biometricAuthService,
    required this.onSignOut,
    this.isAuthenticated = true,
    this.isWebOverride,
    this.autoTriggerOnInit = true,
  });

  @override
  State<BiometricLockGate> createState() => _BiometricLockGateState();
}

class _BiometricLockGateState extends State<BiometricLockGate>
    with WidgetsBindingObserver {
  late bool _isLocked;
  bool _isAuthenticating = false;
  bool _isSigningOut = false;
  bool _authFailed = false;

  bool get _shouldEnforceLock {
    if (widget.isWebOverride != null) {
      return !widget.isWebOverride!;
    }
    if (kIsWeb) return false;
    return defaultTargetPlatform == TargetPlatform.android ||
        defaultTargetPlatform == TargetPlatform.iOS;
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    final skipInitial =
        widget.biometricAuthService.consumeSkipNextInitialLock();
    _isLocked = _shouldEnforceLock && widget.isAuthenticated && !skipInitial;
    if (_isLocked && widget.autoTriggerOnInit) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _triggerUnlock();
      });
    }
  }

  @override
  void didUpdateWidget(covariant BiometricLockGate oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!widget.isAuthenticated || !_shouldEnforceLock) {
      if (_isLocked || _authFailed) {
        setState(() {
          _isLocked = false;
          _authFailed = false;
        });
      }
      return;
    }

    if (!oldWidget.isAuthenticated && widget.isAuthenticated) {
      final skipInitial =
          widget.biometricAuthService.consumeSkipNextInitialLock();
      if (!skipInitial) {
        setState(() {
          _isLocked = true;
          _authFailed = false;
        });
        if (widget.autoTriggerOnInit) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            _triggerUnlock();
          });
        }
      }
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_shouldEnforceLock || !widget.isAuthenticated) return;

    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden) {
      widget.biometricAuthService.markBackgrounded();
      return;
    }

    if (state == AppLifecycleState.resumed) {
      if (_isLocked) return;
      if (widget.biometricAuthService.shouldRequireReauth()) {
        setState(() {
          _isLocked = true;
          _authFailed = false;
        });
        _triggerUnlock();
      } else {
        widget.biometricAuthService.clearBackgroundTimestamp();
      }
    }
  }

  Future<void> _triggerUnlock() async {
    if (_isAuthenticating || _isSigningOut || !mounted) return;
    setState(() {
      _isAuthenticating = true;
      _authFailed = false;
    });

    final authenticated = await widget.biometricAuthService.authenticate();
    if (!mounted) return;

    setState(() {
      _isAuthenticating = false;
      _isLocked = !authenticated;
      _authFailed = !authenticated;
    });
  }

  Future<void> _handleSignOut() async {
    if (_isSigningOut || !mounted) return;
    setState(() {
      _isSigningOut = true;
    });
    try {
      await widget.onSignOut();
      if (!mounted) return;
      final router = GoRouter.maybeOf(context);
      router?.go('/login');
    } finally {
      if (mounted) {
        setState(() {
          _isSigningOut = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_isLocked) {
      return widget.child;
    }

    return Scaffold(
      key: const Key('biometric_lock_screen'),
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 360),
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryColor.withValues(alpha: 0.14),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.lock_outline_rounded,
                      size: 48,
                      color: AppTheme.primaryColor,
                    ),
                  ),
                  const SizedBox(height: 24),
                  Text(
                    'Kerno bloqueado',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Confirme sua identidade para acessar seus dados patrimoniais.',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Colors.white70,
                        ),
                    textAlign: TextAlign.center,
                  ),
                  if (_authFailed) ...[
                    const SizedBox(height: 16),
                    Text(
                      'Autenticação não concluída. Tente novamente ou saia da conta.',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Colors.redAccent,
                          ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                  const SizedBox(height: 28),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      key: const Key('biometric_retry_button'),
                      onPressed: (_isAuthenticating || _isSigningOut) ? null : _triggerUnlock,
                      icon: const Icon(Icons.fingerprint),
                      label: Text(
                        _isAuthenticating
                            ? 'Verificando...'
                            : 'Tentar novamente',
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      key: const Key('biometric_logout_button'),
                      onPressed: (_isAuthenticating || _isSigningOut) ? null : _handleSignOut,
                      icon: const Icon(Icons.logout, color: Colors.redAccent),
                      label: const Text(
                        'Sair',
                        style: TextStyle(color: Colors.redAccent),
                      ),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: Colors.redAccent),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
