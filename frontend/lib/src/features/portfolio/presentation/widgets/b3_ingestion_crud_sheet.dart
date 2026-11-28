import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../../core/security/privacy_provider.dart';
import '../../../../core/theme/kerno_tokens.dart';
import '../../../upload/presentation/providers/upload_provider.dart';
import '../../domain/entities/trade.dart';
import '../providers/portfolio_provider.dart';

/// Lista canônica das 5 Macro Classes do Kerno.
const List<String> kCanonicalMacroClasses = <String>[
  'Ações BR',
  'FIIs',
  'Renda Fixa / Tesouro',
  'Internacional',
  'Criptoativos',
];

/// Sheet/Drawer unificado para Ingestão B3 (`.xlsx`/`.pdf` SINACOR) e CRUD Manual Multi-Classe.
///
/// - Mobile (`width < 600`): Bottom Sheet modal.
/// - Web/Desktop (`width >= 600`): Drawer lateral direito de `480px` com fechamento via `Esc`.
class B3IngestionCrudSheet extends StatefulWidget {
  final int initialTabIndex;

  const B3IngestionCrudSheet({
    super.key,
    this.initialTabIndex = 0,
  });

  static Future<void> show(
    BuildContext context, {
    int initialTabIndex = 0,
  }) {
    final width = MediaQuery.sizeOf(context).width;
    if (width < 600) {
      return showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        useSafeArea: true,
        backgroundColor: Colors.transparent,
        builder: (_) => B3IngestionCrudSheet(initialTabIndex: initialTabIndex),
      );
    }

    return showGeneralDialog<void>(
      context: context,
      barrierDismissible: true,
      barrierLabel: 'Fechar painel de carteira',
      barrierColor: Colors.black54,
      transitionDuration: const Duration(milliseconds: 220),
      pageBuilder: (dialogContext, _, _) {
        return CallbackShortcuts(
          bindings: <ShortcutActivator, VoidCallback>{
            const SingleActivator(LogicalKeyboardKey.escape): () {
              Navigator.of(dialogContext).pop();
            },
          },
          child: Focus(
            autofocus: true,
            child: Align(
              alignment: Alignment.centerRight,
              child: Material(
                color: Colors.transparent,
                child: SizedBox(
                  width: 480,
                  height: double.infinity,
                  child: B3IngestionCrudSheet(initialTabIndex: initialTabIndex),
                ),
              ),
            ),
          ),
        );
      },
      transitionBuilder: (context, animation, _, child) {
        final slide = Tween<Offset>(
          begin: const Offset(1, 0),
          end: Offset.zero,
        ).animate(CurvedAnimation(parent: animation, curve: Curves.easeOutCubic));
        return SlideTransition(position: slide, child: child);
      },
    );
  }

  @override
  State<B3IngestionCrudSheet> createState() => _B3IngestionCrudSheetState();
}

class _B3IngestionCrudSheetState extends State<B3IngestionCrudSheet>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  final _formKey = GlobalKey<FormState>();

  final _tickerController = TextEditingController();
  final _quantityController = TextEditingController(text: '1');
  final _priceController = TextEditingController();
  final _brokerController = TextEditingController(text: 'Manual');

  String _selectedAssetClass = kCanonicalMacroClasses.first;
  String _selectedSide = 'COMPRA';
  DateTime _selectedDate = DateTime.now();
  String? _editingTradeId;
  String? _editingProfileId;
  bool _uploadRefreshTriggered = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTabIndex.clamp(0, 1),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final portfolioProvider = Provider.of<PortfolioProvider?>(context, listen: false);
      portfolioProvider?.resetTradeStatus();
      portfolioProvider?.loadManualTrades();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _tickerController.dispose();
    _quantityController.dispose();
    _priceController.dispose();
    _brokerController.dispose();
    super.dispose();
  }

  bool _requiresIntegerShares(String assetClass, String ticker) {
    final normalizedTicker = ticker.trim().toUpperCase();
    if (assetClass == 'Ações BR' || assetClass == 'FIIs') {
      return true;
    }
    if ((assetClass == 'Internacional' || assetClass == 'Criptoativos') &&
        normalizedTicker.length == 6 &&
        normalizedTicker.endsWith('11')) {
      return true;
    }
    return false;
  }

  void _onAssetClassChanged(String nextClass) {
    setState(() {
      _selectedAssetClass = nextClass;
      if (nextClass == 'Renda Fixa / Tesouro' &&
          _editingTradeId == null &&
          _quantityController.text.trim().isEmpty) {
        _quantityController.text = '1';
      }
    });
  }

  void _startEditing(Trade trade) {
    setState(() {
      _editingTradeId = trade.id;
      _editingProfileId = trade.profileId;
      _tickerController.text = trade.ticker;
      _selectedSide =
          (trade.side == 'COMPRA' || trade.side == 'VENDA') ? trade.side : 'COMPRA';
      _selectedDate = trade.tradeDate;
      _quantityController.text = _formatQuantityForInput(trade.quantity);
      _priceController.text = trade.price.toString().replaceAll('.', ',');
      _brokerController.text = trade.broker;
      if (trade.assetClass != null &&
          kCanonicalMacroClasses.contains(trade.assetClass)) {
        _selectedAssetClass = trade.assetClass!;
      }
      _tabController.animateTo(1);
    });
  }

  void _clearForm() {
    setState(() {
      _editingTradeId = null;
      _editingProfileId = null;
      _tickerController.clear();
      _quantityController.text =
          _selectedAssetClass == 'Renda Fixa / Tesouro' ? '1' : '';
      _priceController.clear();
      _brokerController.text = 'Manual';
      _selectedSide = 'COMPRA';
      _selectedDate = DateTime.now();
    });
    Provider.of<PortfolioProvider?>(context, listen: false)?.resetTradeStatus();
  }

  String _formatQuantityForInput(double qty) {
    if (qty == qty.truncateToDouble()) {
      return qty.toInt().toString();
    }
    return qty.toString().replaceAll('.', ',');
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final minDate = DateTime(2000);
    final clampedInitial = _selectedDate.isBefore(minDate)
        ? minDate
        : (_selectedDate.isAfter(now) ? now : _selectedDate);
    final picked = await showDatePicker(
      context: context,
      initialDate: clampedInitial,
      firstDate: minDate,
      lastDate: now,
    );
    if (picked != null) {
      setState(() => _selectedDate = picked);
    }
  }

  Future<void> _submitManualTrade() async {
    if (!_formKey.currentState!.validate()) return;
    final portfolioProvider = context.read<PortfolioProvider>();

    final ticker = _tickerController.text.trim().toUpperCase();
    final quantity =
        double.parse(_quantityController.text.trim().replaceAll(',', '.'));
    final price =
        double.parse(_priceController.text.trim().replaceAll(',', '.'));
    final broker = _brokerController.text.trim().isEmpty
        ? 'Manual'
        : _brokerController.text.trim();

    final bool ok;
    if (_editingTradeId != null) {
      ok = await portfolioProvider.updateManualTrade(
        id: _editingTradeId!,
        ticker: ticker,
        side: _selectedSide,
        tradeDate: _selectedDate,
        quantity: quantity,
        price: price,
        broker: broker,
        assetClass: _selectedAssetClass,
        profileId: _editingProfileId,
      );
    } else {
      ok = await portfolioProvider.createManualTrade(
        ticker: ticker,
        side: _selectedSide,
        tradeDate: _selectedDate,
        quantity: quantity,
        price: price,
        broker: broker,
        assetClass: _selectedAssetClass,
      );
    }

    if (ok && mounted) {
      _clearForm();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Posição atualizada na carteira.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final kerno = KernoThemeData.of(context);
    final isMobile = MediaQuery.sizeOf(context).width < 600;

    return Container(
      decoration: BoxDecoration(
        color: kerno.surfaceElevated,
        borderRadius: isMobile
            ? const BorderRadius.vertical(top: Radius.circular(KernoRadius.xl))
            : const BorderRadius.horizontal(
                left: Radius.circular(KernoRadius.lg),
              ),
        border: Border.all(color: kerno.borderSubtle),
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
              KernoSpacing.s6,
              KernoSpacing.s4,
              KernoSpacing.s3,
              KernoSpacing.s2,
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    'Atualizar Carteira',
                    style: KernoTypography.heading3(color: kerno.textPrimary),
                  ),
                ),
                SizedBox(
                  width: KernoSpacing.touchTargetMin,
                  height: KernoSpacing.touchTargetMin,
                  child: IconButton(
                    tooltip: 'Fechar',
                    onPressed: () => Navigator.of(context).pop(),
                    icon: Icon(Icons.close, color: kerno.textSecondary),
                  ),
                ),
              ],
            ),
          ),
          TabBar(
            controller: _tabController,
            labelColor: kerno.brandPrimary,
            unselectedLabelColor: kerno.textMuted,
            indicatorColor: kerno.brandPrimary,
            tabs: const [
              Tab(
                height: KernoSpacing.touchTargetMin,
                text: 'Importar B3 / Nota',
              ),
              Tab(
                height: KernoSpacing.touchTargetMin,
                text: 'Lançamento Manual',
              ),
            ],
          ),
          Divider(height: 1, color: kerno.borderSubtle),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildB3UploadTab(context, kerno),
                _buildManualCrudTab(context, kerno),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildB3UploadTab(BuildContext context, KernoThemeData kerno) {
    final uploadProvider = Provider.of<UploadProvider?>(context);
    if (uploadProvider == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(KernoSpacing.s6),
          child: Text(
            'Serviço de importação indisponível neste contexto.',
            style: KernoTypography.bodyBase(color: kerno.textSecondary),
          ),
        ),
      );
    }

    if (uploadProvider.state == UploadState.success && !_uploadRefreshTriggered) {
      _uploadRefreshTriggered = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        final portfolioProvider =
            Provider.of<PortfolioProvider?>(context, listen: false);
        portfolioProvider?.loadSummary(tagId: portfolioProvider.activeTagId);
        portfolioProvider?.loadCustodyPositions(
          tagId: portfolioProvider.activeTagId,
        );
        portfolioProvider?.loadManualTrades();
      });
    } else if (uploadProvider.state != UploadState.success) {
      _uploadRefreshTriggered = false;
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(KernoSpacing.s6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Importe sua planilha de Negociação da B3 (.xlsx) ou Nota de Corretagem SINACOR (.pdf). O arquivo bruto é descartado imediatamente após o processamento.',
            style: KernoTypography.bodySm(color: kerno.textSecondary),
          ),
          const SizedBox(height: KernoSpacing.s4),
          if (uploadProvider.state == UploadState.error &&
              uploadProvider.errorMessage != null) ...[
            _InlineAlertBanner(
              message: uploadProvider.errorMessage!,
              backgroundColor: kerno.crimsonSurface,
              borderColor: kerno.crimsonBorder,
              foregroundColor: kerno.crimson,
              icon: Icons.error_outline,
            ),
            const SizedBox(height: KernoSpacing.s4),
          ],
          if (uploadProvider.state == UploadState.success &&
              uploadProvider.successMessage != null) ...[
            _InlineAlertBanner(
              message: uploadProvider.successMessage!,
              backgroundColor: kerno.emeraldSurface,
              borderColor: kerno.emeraldBorder,
              foregroundColor: kerno.emerald,
              icon: Icons.check_circle_outline,
            ),
            const SizedBox(height: KernoSpacing.s4),
          ],
          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              minimumSize: const Size.fromHeight(KernoSpacing.touchTargetMin),
              side: BorderSide(color: kerno.borderStrong),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(KernoRadius.md),
              ),
            ),
            onPressed: uploadProvider.state == UploadState.uploading
                ? null
                : () => uploadProvider.pickFile(),
            icon: Icon(Icons.upload_file, color: kerno.brandPrimary),
            label: Text(
              uploadProvider.hasFile
                  ? uploadProvider.selectedFile!.name
                  : 'Selecionar arquivo (.xlsx ou .pdf)',
              style: KernoTypography.bodyBase(color: kerno.textPrimary),
            ),
          ),
          const SizedBox(height: KernoSpacing.s4),
          if (uploadProvider.state == UploadState.uploading) ...[
            LinearProgressIndicator(
              color: kerno.brandPrimary,
              backgroundColor: kerno.brandAccentSoft,
            ),
            const SizedBox(height: KernoSpacing.s2),
            Text(
              uploadProvider.progressMessage,
              style: KernoTypography.bodySm(color: kerno.textSecondary),
            ),
            const SizedBox(height: KernoSpacing.s4),
          ],
          FilledButton(
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(KernoSpacing.touchTargetMin),
              backgroundColor: kerno.brandPrimary,
              foregroundColor: kerno.textOnPrimary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(KernoRadius.md),
              ),
            ),
            onPressed: uploadProvider.canUpload
                ? () => uploadProvider.uploadB3File()
                : null,
            child: const Text('Enviar para Processamento'),
          ),
        ],
      ),
    );
  }

  Widget _buildManualCrudTab(BuildContext context, KernoThemeData kerno) {
    final portfolioProvider = context.watch<PortfolioProvider>();
    final privacyProvider = Provider.of<PrivacyProvider?>(context);
    final isPrivacyMode = privacyProvider?.isPrivacyMode ?? false;
    final currencyFormatter =
        NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final isSaving = portfolioProvider.tradeStatus == TradeStatus.loading;
    final isContinuousBrl = _selectedAssetClass == 'Renda Fixa / Tesouro' ||
        _selectedAssetClass == 'Criptoativos';

    return SingleChildScrollView(
      padding: const EdgeInsets.all(KernoSpacing.s6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (portfolioProvider.tradeStatus == TradeStatus.error &&
              portfolioProvider.tradeError != null) ...[
            _InlineAlertBanner(
              message: portfolioProvider.tradeError!,
              backgroundColor: kerno.crimsonSurface,
              borderColor: kerno.crimsonBorder,
              foregroundColor: kerno.crimson,
              icon: Icons.error_outline,
            ),
            const SizedBox(height: KernoSpacing.s4),
          ],
          Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Macro Classe',
                  style: KernoTypography.bodySm(color: kerno.textSecondary),
                ),
                const SizedBox(height: KernoSpacing.s2),
                Wrap(
                  spacing: KernoSpacing.s2,
                  runSpacing: KernoSpacing.s2,
                  children: kCanonicalMacroClasses.map((macroClass) {
                    final selected = _selectedAssetClass == macroClass;
                    return ChoiceChip(
                      label: Text(macroClass),
                      selected: selected,
                      onSelected: (_) => _onAssetClassChanged(macroClass),
                      selectedColor: kerno.brandAccentSoft,
                      labelStyle: KernoTypography.bodySm(
                        color: selected ? kerno.brandPrimary : kerno.textPrimary,
                      ),
                    );
                  }).toList(),
                ),
                const SizedBox(height: KernoSpacing.s4),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(value: 'COMPRA', label: Text('Compra / Posição')),
                    ButtonSegment(value: 'VENDA', label: Text('Venda / Resgate')),
                  ],
                  selected: {_selectedSide},
                  onSelectionChanged: (val) =>
                      setState(() => _selectedSide = val.first),
                ),
                const SizedBox(height: KernoSpacing.s4),
                TextFormField(
                  controller: _tickerController,
                  textCapitalization: TextCapitalization.characters,
                  decoration: InputDecoration(
                    labelText: isContinuousBrl
                        ? 'Ativo / Ticker (ex: TESOURO_SELIC, BTC)'
                        : 'Ticker (ex: PETR4, HGLG11, IVVB11)',
                    border: const OutlineInputBorder(),
                  ),
                  validator: (val) {
                    if (val == null || val.trim().isEmpty) {
                      return 'Informe o código ou nome do ativo';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: KernoSpacing.s3),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _quantityController,
                        keyboardType: TextInputType.numberWithOptions(
                          decimal: !(_selectedAssetClass == 'Ações BR' ||
                              _selectedAssetClass == 'FIIs'),
                        ),
                        decoration: InputDecoration(
                          labelText: isContinuousBrl
                              ? 'Quantidade (1 p/ BRL)'
                              : 'Quantidade',
                          border: const OutlineInputBorder(),
                        ),
                        inputFormatters: [
                          FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
                        ],
                        validator: (val) {
                          if (val == null || val.trim().isEmpty) {
                            return 'Obrigatório';
                          }
                          final normalized = val.trim().replaceAll(',', '.');
                          final parsed = double.tryParse(normalized);
                          if (parsed == null || parsed <= 0) {
                            return 'Valor inválido';
                          }
                          if (_requiresIntegerShares(
                                _selectedAssetClass,
                                _tickerController.text,
                              ) &&
                              parsed != parsed.truncateToDouble()) {
                            return 'Cotas inteiras';
                          }
                          final parts = normalized.split('.');
                          if (parts.length == 2 && parts[1].length > 4) {
                            return 'Máx. 4 casas';
                          }
                          return null;
                        },
                      ),
                    ),
                    const SizedBox(width: KernoSpacing.s3),
                    Expanded(
                      child: TextFormField(
                        controller: _priceController,
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        decoration: InputDecoration(
                          labelText: isContinuousBrl
                              ? 'Valor / Preço (R\$)'
                              : 'Preço Unitário (R\$)',
                          border: const OutlineInputBorder(),
                        ),
                        inputFormatters: [
                          FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]')),
                        ],
                        validator: (val) {
                          if (val == null || val.trim().isEmpty) {
                            return 'Obrigatório';
                          }
                          final parsed = double.tryParse(
                            val.trim().replaceAll(',', '.'),
                          );
                          if (parsed == null || parsed <= 0) {
                            return 'Valor inválido';
                          }
                          return null;
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: KernoSpacing.s3),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _brokerController,
                        decoration: const InputDecoration(
                          labelText: 'Corretora (opcional)',
                          border: OutlineInputBorder(),
                        ),
                      ),
                    ),
                    const SizedBox(width: KernoSpacing.s3),
                    Expanded(
                      child: InkWell(
                        onTap: _pickDate,
                        borderRadius: BorderRadius.circular(KernoRadius.sm),
                        child: Container(
                          height: 56,
                          padding: const EdgeInsets.symmetric(
                            horizontal: KernoSpacing.s3,
                          ),
                          decoration: BoxDecoration(
                            border: Border.all(color: kerno.borderStrong),
                            borderRadius: BorderRadius.circular(KernoRadius.sm),
                          ),
                          alignment: Alignment.centerLeft,
                          child: Row(
                            children: [
                              Icon(
                                Icons.calendar_today,
                                size: 18,
                                color: kerno.textSecondary,
                              ),
                              const SizedBox(width: KernoSpacing.s2),
                              Expanded(
                                child: Text(
                                  DateFormat('dd/MM/yyyy').format(_selectedDate),
                                  style: KernoTypography.bodyBase(
                                    color: kerno.textPrimary,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: KernoSpacing.s4),
                Row(
                  children: [
                    if (_editingTradeId != null) ...[
                      Expanded(
                        child: OutlinedButton(
                          style: OutlinedButton.styleFrom(
                            minimumSize: const Size.fromHeight(
                              KernoSpacing.touchTargetMin,
                            ),
                          ),
                          onPressed: isSaving ? null : _clearForm,
                          child: const Text('Cancelar'),
                        ),
                      ),
                      const SizedBox(width: KernoSpacing.s3),
                    ],
                    Expanded(
                      flex: 2,
                      child: FilledButton(
                        style: FilledButton.styleFrom(
                          minimumSize: const Size.fromHeight(
                            KernoSpacing.touchTargetMin,
                          ),
                          backgroundColor: kerno.brandPrimary,
                          foregroundColor: kerno.textOnPrimary,
                        ),
                        onPressed: isSaving ? null : _submitManualTrade,
                        child: isSaving
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : Text(
                                _editingTradeId != null
                                    ? 'Atualizar Lançamento'
                                    : 'Salvar Lançamento',
                              ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: KernoSpacing.s6),
          Divider(color: kerno.borderSubtle),
          const SizedBox(height: KernoSpacing.s4),
          Text(
            'Lançamentos Registrados',
            style: KernoTypography.heading3(color: kerno.textPrimary),
          ),
          const SizedBox(height: KernoSpacing.s3),
          if (portfolioProvider.manualTradesLoading)
            const Center(child: CircularProgressIndicator())
          else if (portfolioProvider.manualTradesError != null)
            _InlineAlertBanner(
              message: portfolioProvider.manualTradesError!,
              backgroundColor: kerno.crimsonSurface,
              borderColor: kerno.crimsonBorder,
              foregroundColor: kerno.crimson,
              icon: Icons.error_outline,
            )
          else if (portfolioProvider.manualTrades.isEmpty)
            Text(
              'Nenhum lançamento encontrado.',
              style: KernoTypography.bodySm(color: kerno.textMuted),
            )
          else
            ...portfolioProvider.manualTrades.map((trade) {
              final totalBrl = trade.quantity * trade.price;
              final totalText = isPrivacyMode
                  ? 'R\$ •••••'
                  : currencyFormatter.format(totalBrl);
              return Container(
                margin: const EdgeInsets.only(bottom: KernoSpacing.s2),
                padding: const EdgeInsets.all(KernoSpacing.s3),
                decoration: BoxDecoration(
                  color: kerno.surfaceSubtle,
                  borderRadius: BorderRadius.circular(KernoRadius.md),
                  border: Border.all(color: kerno.borderSubtle),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Wrap(
                            spacing: KernoSpacing.s2,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              Text(
                                trade.ticker,
                                style: KernoTypography.tickerBadge(
                                  color: kerno.textPrimary,
                                ),
                              ),
                              if (trade.assetClass != null)
                                Text(
                                  trade.assetClass!,
                                  style: KernoTypography.caption(
                                    color: kerno.brandPrimary,
                                  ),
                                ),
                            ],
                          ),
                          const SizedBox(height: KernoSpacing.s1),
                          Text(
                            '${trade.side} • ${DateFormat('dd/MM/yyyy').format(trade.tradeDate)} • $totalText',
                            style: KernoTypography.bodySm(
                              color: kerno.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    SizedBox(
                      width: KernoSpacing.touchTargetMin,
                      height: KernoSpacing.touchTargetMin,
                      child: IconButton(
                        tooltip: 'Editar lançamento',
                        onPressed: () => _startEditing(trade),
                        icon: Icon(Icons.edit_outlined, color: kerno.textPrimary),
                      ),
                    ),
                    SizedBox(
                      width: KernoSpacing.touchTargetMin,
                      height: KernoSpacing.touchTargetMin,
                      child: IconButton(
                        tooltip: 'Excluir lançamento',
                        onPressed: () async {
                          final ok = await portfolioProvider
                              .deleteManualTrade(trade.id);
                          if (ok && _editingTradeId == trade.id && mounted) {
                            _clearForm();
                          }
                        },
                        icon: Icon(Icons.delete_outline, color: kerno.crimson),
                      ),
                    ),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }
}

class _InlineAlertBanner extends StatelessWidget {
  final String message;
  final Color backgroundColor;
  final Color borderColor;
  final Color foregroundColor;
  final IconData icon;

  const _InlineAlertBanner({
    required this.message,
    required this.backgroundColor,
    required this.borderColor,
    required this.foregroundColor,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(KernoSpacing.s3),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(KernoRadius.md),
        border: Border.all(color: borderColor),
      ),
      child: Row(
        children: [
          Icon(icon, color: foregroundColor, size: 20),
          const SizedBox(width: KernoSpacing.s2),
          Expanded(
            child: Text(
              message,
              style: KernoTypography.bodySm(color: foregroundColor),
            ),
          ),
        ],
      ),
    );
  }
}
