import 'package:flutter/material.dart';
import 'package:frontend/src/core/theme/kerno_tokens.dart';

/// Link de acessibilidade (WCAG 2.4.1) que permanece oculto visualmente
/// até receber foco de teclado, permitindo saltar diretamente para o conteúdo principal.
class SkipToContentLink extends StatefulWidget {
  const SkipToContentLink({
    super.key,
    required this.contentFocusNode,
    this.focusNode,
    this.label = 'Pular para o conteúdo principal',
    this.onSkip,
  });

  final FocusNode contentFocusNode;
  final FocusNode? focusNode;
  final String label;
  final VoidCallback? onSkip;

  @override
  State<SkipToContentLink> createState() => _SkipToContentLinkState();
}

class _SkipToContentLinkState extends State<SkipToContentLink> {
  late final FocusNode _internalFocusNode;
  bool _isFocused = false;

  FocusNode get _effectiveFocusNode => widget.focusNode ?? _internalFocusNode;

  @override
  void initState() {
    super.initState();
    _internalFocusNode = FocusNode(debugLabel: 'SkipToContentLink');
  }

  @override
  void dispose() {
    _internalFocusNode.dispose();
    super.dispose();
  }

  void _activateSkip() {
    widget.contentFocusNode.requestFocus();
    widget.onSkip?.call();
  }

  @override
  Widget build(BuildContext context) {
    final tokens = KernoThemeData.of(context);

    return Semantics(
      button: true,
      label: widget.label,
      child: FocusableActionDetector(
        focusNode: _effectiveFocusNode,
        onFocusChange: (focused) {
          if (_isFocused != focused) {
            setState(() => _isFocused = focused);
          }
        },
        actions: <Type, Action<Intent>>{
          ActivateIntent: CallbackAction<ActivateIntent>(
            onInvoke: (_) {
              _activateSkip();
              return null;
            },
          ),
        },
        child: _isFocused
            ? GestureDetector(
                onTap: _activateSkip,
                child: Container(
                  constraints: const BoxConstraints(
                    minHeight: KernoSpacing.touchTargetMin,
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: KernoSpacing.s4,
                    vertical: KernoSpacing.s2,
                  ),
                  decoration: BoxDecoration(
                    color: tokens.brandPrimary,
                    borderRadius: BorderRadius.circular(KernoRadius.md),
                    border: Border.all(
                      color: tokens.borderFocus,
                      width: 2.0,
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Icon(
                        Icons.arrow_downward_rounded,
                        size: 18,
                        color: tokens.textOnPrimary,
                      ),
                      const SizedBox(width: KernoSpacing.s2),
                      Text(
                        widget.label,
                        style: KernoTypography.bodySm(
                          color: tokens.textOnPrimary,
                        ).copyWith(fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                ),
              )
            : const SizedBox(width: 1, height: 1),
      ),
    );
  }
}
