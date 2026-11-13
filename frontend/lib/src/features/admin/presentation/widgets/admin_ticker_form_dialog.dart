import 'package:flutter/material.dart';
import '../../domain/entities/admin_ticker.dart';

class AdminTickerFormDialog extends StatefulWidget {
  final AdminTicker? ticker;
  final String? Function()? errorMessageBuilder;
  final Future<bool> Function({
    required String ticker,
    required String name,
    required String assetClass,
    String? sector,
  }) onSave;

  const AdminTickerFormDialog({
    super.key,
    this.ticker,
    this.errorMessageBuilder,
    required this.onSave,
  });

  @override
  State<AdminTickerFormDialog> createState() => _AdminTickerFormDialogState();
}

class _AdminTickerFormDialogState extends State<AdminTickerFormDialog> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _tickerController;
  late final TextEditingController _nameController;
  late final TextEditingController _sectorController;
  late final List<String> _availableClasses;
  String _selectedClass = 'Ações';
  bool _isSaving = false;
  String? _errorMessage;

  static const List<String> assetClasses = [
    'Ações',
    'FIIs',
    'BDRs',
    'ETFs',
    'Renda Fixa',
    'Outros',
  ];

  @override
  void initState() {
    super.initState();
    _tickerController = TextEditingController(text: widget.ticker?.ticker ?? '');
    _nameController = TextEditingController(text: widget.ticker?.name ?? '');
    _sectorController = TextEditingController(text: widget.ticker?.sector ?? '');
    _availableClasses = List<String>.from(assetClasses);

    if (widget.ticker != null && widget.ticker!.assetClass.trim().isNotEmpty) {
      final rawClass = widget.ticker!.assetClass.trim();
      final matched = _availableClasses.firstWhere(
        (c) => c.toLowerCase() == rawClass.toLowerCase(),
        orElse: () {
          _availableClasses.add(rawClass);
          return rawClass;
        },
      );
      _selectedClass = matched;
    }
  }

  @override
  void dispose() {
    _tickerController.dispose();
    _nameController.dispose();
    _sectorController.dispose();
    super.dispose();
  }

  Future<void> _handleSave() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSaving = true;
      _errorMessage = null;
    });

    final success = await widget.onSave(
      ticker: _tickerController.text.trim().toUpperCase(),
      name: _nameController.text.trim(),
      assetClass: _selectedClass,
      sector: _sectorController.text.trim().isNotEmpty ? _sectorController.text.trim() : null,
    );

    if (!mounted) return;

    if (success) {
      Navigator.of(context).pop(true);
    } else {
      final providerError = widget.errorMessageBuilder?.call();
      setState(() {
        _isSaving = false;
        _errorMessage = (providerError != null && providerError.trim().isNotEmpty)
            ? providerError
            : (widget.ticker != null
                ? 'Falha ao atualizar ticker. Tente novamente.'
                : 'Falha ao cadastrar ticker. Verifique os dados ou tente novamente.');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.ticker != null;

    return Dialog(
      key: const Key('admin_ticker_form_dialog'),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      backgroundColor: const Color(0xFF1E293B),
      child: Container(
        width: 480,
        padding: const EdgeInsets.all(24),
        child: SingleChildScrollView(
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                isEditing ? 'Editar Ticker ${widget.ticker!.ticker}' : 'Novo Ticker no Mestre de Dados',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                isEditing
                    ? 'Atualize os dados cadastrais e classificação do ativo.'
                    : 'Cadastre um novo papel (IPO/ativo) no dicionário mestre global.',
                style: const TextStyle(fontSize: 14, color: Colors.white70),
              ),
              const SizedBox(height: 20),
              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.red.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.red.withValues(alpha: 0.5)),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.error_outline, color: Colors.red, size: 20),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(color: Colors.white, fontSize: 13),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
              ],
              TextFormField(
                key: const Key('admin_ticker_input_ticker'),
                controller: _tickerController,
                enabled: !isEditing && !_isSaving,
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(
                  labelText: 'Ticker (Ex: PETR4, BOVA11)',
                  hintText: 'Código do ativo na B3',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.tag),
                ),
                validator: (val) {
                  if (val == null || val.trim().isEmpty) {
                    return 'Informe o ticker.';
                  }
                  final clean = val.trim();
                  if (clean.length < 2 || clean.length > 10) {
                    return 'O ticker deve conter entre 2 e 10 caracteres.';
                  }
                  if (!RegExp(r'^[a-zA-Z0-9]+$').hasMatch(clean)) {
                    return 'O ticker deve conter apenas letras e números.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('admin_ticker_input_name'),
                controller: _nameController,
                enabled: !_isSaving,
                decoration: const InputDecoration(
                  labelText: 'Nome Corporativo / Descritivo',
                  hintText: 'Ex: Petróleo Brasileiro S.A.',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.business),
                ),
                validator: (val) {
                  if (val == null || val.trim().isEmpty) {
                    return 'Informe o nome corporativo.';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                key: const Key('admin_ticker_input_class'),
                value: _selectedClass,
                decoration: const InputDecoration(
                  labelText: 'Classe de Ativo',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.category),
                ),
                items: _availableClasses
                    .map((c) => DropdownMenuItem(value: c, child: Text(c)))
                    .toList(),
                onChanged: _isSaving
                    ? null
                    : (val) {
                        if (val != null) {
                          setState(() => _selectedClass = val);
                        }
                      },
              ),
              const SizedBox(height: 16),
              TextFormField(
                key: const Key('admin_ticker_input_sector'),
                controller: _sectorController,
                enabled: !_isSaving,
                decoration: const InputDecoration(
                  labelText: 'Setor (Opcional)',
                  hintText: 'Ex: Petróleo e Gás, Financeiro, Logística',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.pie_chart_outline),
                ),
              ),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    key: const Key('admin_ticker_cancel_button'),
                    onPressed: _isSaving ? null : () => Navigator.of(context).pop(false),
                    child: const Text('Cancelar'),
                  ),
                  const SizedBox(width: 12),
                  ElevatedButton(
                    key: const Key('admin_ticker_save_button'),
                    onPressed: _isSaving ? null : _handleSave,
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(120, 48),
                    ),
                    child: _isSaving
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : const Text('Salvar'),
                  ),
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
