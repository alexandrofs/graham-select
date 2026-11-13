import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/admin_ticker.dart';
import '../providers/admin_ticker_provider.dart';
import '../widgets/admin_ticker_form_dialog.dart';

class AdminTickersPage extends StatefulWidget {
  const AdminTickersPage({super.key});

  @override
  State<AdminTickersPage> createState() => _AdminTickersPageState();
}

class _AdminTickersPageState extends State<AdminTickersPage> {
  final TextEditingController _searchController = TextEditingController();

  static const List<String> _classFilters = [
    'Todos',
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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final provider = Provider.of<AdminTickerProvider?>(context, listen: false);
      if (provider != null && provider.tickers.isEmpty && !provider.isLoading && !provider.isAccessDenied) {
        provider.loadTickers();
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _openCreateDialog(BuildContext context, AdminTickerProvider provider) async {
    final created = await showDialog<bool>(
      context: context,
      builder: (_) => AdminTickerFormDialog(
        errorMessageBuilder: () => provider.errorMessage,
        onSave: ({
          required String ticker,
          required String name,
          required String assetClass,
          String? sector,
        }) {
          return provider.createTicker(
            ticker: ticker,
            name: name,
            assetClass: assetClass,
            sector: sector,
          );
        },
      ),
    );

    if (created == true && context.mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          const SnackBar(
            content: Text('Ticker cadastrado com sucesso!'),
            backgroundColor: Color(0xFF10B981),
          ),
        );
    }
  }

  Future<void> _openEditDialog(
    BuildContext context,
    AdminTickerProvider provider,
    AdminTicker item,
  ) async {
    final updated = await showDialog<bool>(
      context: context,
      builder: (_) => AdminTickerFormDialog(
        ticker: item,
        errorMessageBuilder: () => provider.errorMessage,
        onSave: ({
          required String ticker,
          required String name,
          required String assetClass,
          String? sector,
        }) {
          return provider.updateTicker(
            id: item.id,
            name: name,
            assetClass: assetClass,
            sector: sector,
          );
        },
      ),
    );

    if (updated == true && context.mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text('Ticker ${item.ticker} atualizado com sucesso!'),
            backgroundColor: const Color(0xFF10B981),
          ),
        );
    }
  }

  Future<void> _handleClearCache(BuildContext context, AdminTickerProvider provider) async {
    final success = await provider.clearCache();
    if (!context.mounted) return;

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            success
                ? 'Cache de Market Data limpo com sucesso!'
                : (provider.errorMessage ?? 'Falha ao limpar cache de Market Data.'),
          ),
          backgroundColor: success ? const Color(0xFF10B981) : const Color(0xFFEF4444),
        ),
      );
  }

  Future<void> _handleEvictTicker(
    BuildContext context,
    AdminTickerProvider provider,
    String ticker,
  ) async {
    final success = await provider.evictTicker(ticker);
    if (!context.mounted) return;

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            success
                ? 'Cache do ativo $ticker invalidado com sucesso!'
                : (provider.errorMessage ?? 'Falha ao invalidar cache de $ticker.'),
          ),
          backgroundColor: success ? const Color(0xFF10B981) : const Color(0xFFEF4444),
        ),
      );
  }

  Future<void> _handleDeleteTicker(
    BuildContext context,
    AdminTickerProvider provider,
    AdminTicker item,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        key: const Key('admin_ticker_delete_confirm_dialog'),
        title: Text('Remover ${item.ticker}?'),
        content: Text(
          'Deseja remover o ativo ${item.ticker} (${item.name}) do dicionário mestre? Esta ação será bloqueada caso haja cotações ou transações vinculadas.',
        ),
        actions: [
          TextButton(
            key: const Key('admin_ticker_delete_cancel_button'),
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            key: const Key('admin_ticker_delete_confirm_button'),
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFEF4444)),
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Remover'),
          ),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    final success = await provider.deleteTicker(item.id);
    if (!context.mounted) return;

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(
            success
                ? 'Ticker ${item.ticker} removido com sucesso!'
                : (provider.errorMessage ?? 'Falha ao remover ticker ${item.ticker}.'),
          ),
          backgroundColor: success ? const Color(0xFF10B981) : const Color(0xFFEF4444),
        ),
      );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AdminTickerProvider>(
      builder: (context, provider, _) {
        if (provider.isLoading) {
          return const _AdminTickersSkeleton();
        }
        if (provider.isAccessDenied) {
          return const _AdminTickersAccessDeniedView();
        }
        if (provider.errorMessage != null && provider.tickers.isEmpty) {
          return _AdminTickersErrorState(
            message: provider.errorMessage!,
            onRetry: provider.loadTickers,
          );
        }

        final items = provider.filteredTickers;

        return SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Barra Superior / Ações
              Wrap(
                spacing: 12,
                runSpacing: 12,
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      IconButton(
                        key: const Key('admin_tickers_back_button'),
                        icon: const Icon(Icons.arrow_back),
                        tooltip: 'Voltar ao Painel Admin',
                        onPressed: () => context.go('/admin/dashboard'),
                      ),
                      const SizedBox(width: 8),
                      const Flexible(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Gestão de Ativos e Tickers (Mestre de Dados)',
                              style: TextStyle(
                                fontSize: 22,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            SizedBox(height: 4),
                            Text(
                              'Cadastre novos papéis (IPOs), gerencie classes de ativos e invalide o cache de cotações.',
                              style: TextStyle(
                                fontSize: 13,
                                color: Colors.white70,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      OutlinedButton.icon(
                        key: const Key('admin_tickers_clear_cache_button'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.amberAccent,
                          side: BorderSide(color: Colors.amberAccent.withValues(alpha: 0.5)),
                          minimumSize: const Size(0, 40),
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        ),
                        onPressed: provider.isActionLoading
                            ? null
                            : () => _handleClearCache(context, provider),
                        icon: const Icon(Icons.cleaning_services_outlined, size: 18),
                        label: const Text('Limpar Cache'),
                      ),
                      ElevatedButton.icon(
                        key: const Key('admin_tickers_create_button'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primaryColor,
                          foregroundColor: Colors.white,
                          minimumSize: const Size(0, 40),
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        ),
                        onPressed: provider.isActionLoading
                            ? null
                            : () => _openCreateDialog(context, provider),
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Novo Ticker'),
                      ),
                      IconButton(
                        key: const Key('admin_tickers_refresh_button'),
                        icon: const Icon(Icons.refresh),
                        tooltip: 'Atualizar lista',
                        onPressed: provider.isActionLoading ? null : provider.loadTickers,
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 24),

              // Filtros e Busca
              Card(
                color: AppTheme.surfaceColor,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      TextField(
                        key: const Key('admin_tickers_search_field'),
                        controller: _searchController,
                        onChanged: provider.setSearchQuery,
                        decoration: InputDecoration(
                          hintText: 'Buscar por ticker ou nome corporativo (ex: PETR4, Vale)...',
                          prefixIcon: const Icon(Icons.search),
                          suffixIcon: _searchController.text.isNotEmpty
                              ? IconButton(
                                  icon: const Icon(Icons.clear),
                                  onPressed: () {
                                    _searchController.clear();
                                    provider.setSearchQuery('');
                                  },
                                )
                              : null,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                          isDense: true,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Wrap(
                        key: const Key('admin_tickers_class_filter'),
                        spacing: 8,
                        runSpacing: 8,
                        children: _classFilters.map((assetClass) {
                          final isSelected =
                              provider.selectedAssetClassFilter.toLowerCase() ==
                                  assetClass.toLowerCase();
                          return ChoiceChip(
                            label: Text(assetClass),
                            selected: isSelected,
                            onSelected: (_) => provider.setAssetClassFilter(assetClass),
                          );
                        }).toList(),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),

              if (items.isEmpty)
                const SizedBox(
                  height: 320,
                  child: _AdminTickersEmptyState(),
                )
              else
                Card(
                  key: const Key('admin_tickers_table'),
                  color: AppTheme.surfaceColor,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          child: Row(
                            children: const [
                              SizedBox(
                                width: 110,
                                child: Text(
                                  'Ticker',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white70,
                                  ),
                                ),
                              ),
                              Expanded(
                                flex: 3,
                                child: Text(
                                  'Nome',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white70,
                                  ),
                                ),
                              ),
                              SizedBox(
                                width: 130,
                                child: Text(
                                  'Classe de Ativo',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white70,
                                  ),
                                ),
                              ),
                              Expanded(
                                flex: 2,
                                child: Text(
                                  'Setor',
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white70,
                                  ),
                                ),
                              ),
                              SizedBox(
                                width: 148,
                                child: Text(
                                  'Ações',
                                  textAlign: TextAlign.right,
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white70,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const Divider(height: 16),
                        ListView.separated(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          itemCount: items.length,
                          separatorBuilder: (_, _) => Divider(
                            height: 1,
                            color: Colors.white.withValues(alpha: 0.06),
                          ),
                          itemBuilder: (context, index) {
                            final item = items[index];
                            return Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                              child: Row(
                                children: [
                                  SizedBox(
                                    width: 110,
                                    child: Text(
                                      item.ticker,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 15,
                                        fontFamily: 'monospace',
                                      ),
                                    ),
                                  ),
                                  Expanded(
                                    flex: 3,
                                    child: Text(
                                      item.name,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  SizedBox(
                                    width: 130,
                                    child: Align(
                                      alignment: Alignment.centerLeft,
                                      child: _AssetClassBadge(assetClass: item.assetClass),
                                    ),
                                  ),
                                  Expanded(
                                    flex: 2,
                                    child: Text(
                                      item.sector ?? '-',
                                      style: TextStyle(
                                        color: Colors.white.withValues(alpha: 0.7),
                                        fontSize: 13,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  SizedBox(
                                    width: 148,
                                    child: Row(
                                      mainAxisAlignment: MainAxisAlignment.end,
                                      children: [
                                        IconButton(
                                          key: Key('admin_ticker_edit_button_${item.id}'),
                                          icon: const Icon(Icons.edit_outlined, size: 20),
                                          tooltip: 'Editar Ticker',
                                          onPressed: provider.isActionLoading
                                              ? null
                                              : () => _openEditDialog(context, provider, item),
                                        ),
                                        IconButton(
                                          key: Key('admin_ticker_evict_cache_button_${item.id}'),
                                          icon: const Icon(
                                            Icons.cached_outlined,
                                            size: 20,
                                            color: Colors.amberAccent,
                                          ),
                                          tooltip: 'Invalidar Cache de Cotação',
                                          onPressed: provider.isActionLoading
                                              ? null
                                              : () => _handleEvictTicker(context, provider, item.ticker),
                                        ),
                                        IconButton(
                                          key: Key('admin_ticker_delete_button_${item.id}'),
                                          icon: const Icon(
                                            Icons.delete_outline,
                                            size: 20,
                                            color: Color(0xFFEF4444),
                                          ),
                                          tooltip: 'Remover Ticker',
                                          onPressed: provider.isActionLoading
                                              ? null
                                              : () => _handleDeleteTicker(context, provider, item),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}

class _AssetClassBadge extends StatelessWidget {
  final String assetClass;

  const _AssetClassBadge({required this.assetClass});

  @override
  Widget build(BuildContext context) {
    Color color;
    switch (assetClass.toLowerCase()) {
      case 'ações':
      case 'acoes':
        color = const Color(0xFF3B82F6);
      case 'fiis':
        color = const Color(0xFF10B981);
      case 'bdrs':
        color = const Color(0xFF8B5CF6);
      case 'etfs':
        color = const Color(0xFFF59E0B);
      case 'renda fixa':
        color = const Color(0xFF06B6D4);
      default:
        color = const Color(0xFF94A3B8);
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.18),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Text(
        assetClass,
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

class _AdminTickersSkeleton extends StatelessWidget {
  const _AdminTickersSkeleton() : super(key: const Key('admin_tickers_skeleton'));

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildBox(width: 340, height: 32),
          const SizedBox(height: 12),
          _buildBox(width: 480, height: 16),
          const SizedBox(height: 24),
          _buildBox(width: double.infinity, height: 96),
          const SizedBox(height: 16),
          _buildBox(width: double.infinity, height: 64),
          const SizedBox(height: 12),
          _buildBox(width: double.infinity, height: 64),
          const SizedBox(height: 12),
          _buildBox(width: double.infinity, height: 64),
        ],
      ),
    );
  }

  Widget _buildBox({required double width, required double height}) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(8),
      ),
    );
  }
}

class _AdminTickersAccessDeniedView extends StatelessWidget {
  const _AdminTickersAccessDeniedView()
      : super(key: const Key('admin_tickers_access_denied_state'));

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Card(
            color: AppTheme.surfaceColor,
            elevation: 4,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFEF4444).withValues(alpha: 0.15),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.lock_person_outlined,
                      size: 40,
                      color: Color(0xFFEF4444),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Acesso Restrito a Administradores',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Você não possui privilégios de administrador para gerenciar o dicionário mestre de ativos e tickers.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.white.withValues(alpha: 0.75),
                    ),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(minimumSize: const Size(0, 44)),
                    onPressed: () => context.go('/dashboard'),
                    icon: const Icon(Icons.arrow_back),
                    label: const Text('Voltar ao Dashboard'),
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

class _AdminTickersEmptyState extends StatelessWidget {
  const _AdminTickersEmptyState() : super(key: const Key('admin_tickers_empty_state'));

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppTheme.primaryColor.withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.candlestick_chart_outlined,
                size: 52,
                color: AppTheme.primaryColor,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Nenhum ticker encontrado',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              'Não há ativos correspondentes aos filtros aplicados ou cadastrados no dicionário mestre.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: Colors.white.withValues(alpha: 0.70),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AdminTickersErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _AdminTickersErrorState({
    required this.message,
    required this.onRetry,
  }) : super(key: const Key('admin_tickers_error_state'));

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Card(
            color: AppTheme.surfaceColor,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.error_outline,
                    size: 48,
                    color: Color(0xFFEF4444),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Erro ao carregar tickers',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    message,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.white.withValues(alpha: 0.75),
                    ),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton.icon(
                    key: const Key('admin_tickers_retry_button'),
                    onPressed: onRetry,
                    icon: const Icon(Icons.refresh),
                    label: const Text('Tentar Novamente'),
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
