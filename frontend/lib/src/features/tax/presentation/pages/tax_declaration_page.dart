import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../family_holding/presentation/providers/family_profile_provider.dart';
import '../providers/tax_provider.dart';
import '../widgets/tax_item_card.dart';
import '../widgets/tax_summary_card.dart';

class TaxDeclarationPage extends StatefulWidget {
  const TaxDeclarationPage({super.key});

  @override
  State<TaxDeclarationPage> createState() => _TaxDeclarationPageState();
}

class _TaxDeclarationPageState extends State<TaxDeclarationPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<TaxProvider>().init();
        try {
          context.read<FamilyProfileProvider>().fetchProfiles();
        } catch (_) {}
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    FamilyProfileProvider? familyProvider;
    try {
      familyProvider = context.watch<FamilyProfileProvider>();
    } catch (_) {
      familyProvider = null;
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Consumer<TaxProvider>(
        builder: (context, provider, child) {
          final profiles = familyProvider?.profiles ?? const [];
          final report = provider.report;

          return SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Header
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Informe de Bens e Direitos',
                            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Declaração de Ajuste Anual do IRPF • Custo de Aquisição em 31/12 (IN RFB 1.585/2015)',
                            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  color: Colors.white60,
                                ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 16),
                    ElevatedButton.icon(
                      key: const Key('export_csv_button'),
                      onPressed: (provider.isLoading || provider.isExporting || report == null || report.items.isEmpty)
                          ? null
                          : () async {
                              final csv = await provider.exportCsv();
                              if (context.mounted) {
                                if (csv != null) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text('Arquivo CSV exportado com sucesso!'),
                                      backgroundColor: AppTheme.accentColor,
                                    ),
                                  );
                                } else {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(
                                      content: Text('Falha ao exportar CSV. Tente novamente.'),
                                      backgroundColor: Colors.redAccent,
                                    ),
                                  );
                                }
                              }
                            },
                      icon: provider.isExporting
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                            )
                          : const Icon(Icons.download_rounded, size: 20),
                      label: const Text('Exportar CSV'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryColor,
                        foregroundColor: Colors.white,
                        minimumSize: const Size(160, 48),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),

                // Filtros (Ano e Perfil Familiar)
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Wrap(
                      spacing: 16,
                      runSpacing: 16,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        // Seletor de Ano
                        SizedBox(
                          width: 180,
                          child: DropdownButtonFormField<int>(
                            key: const Key('tax_year_selector'),
                            value: (provider.availableYears.contains(provider.selectedYear))
                                ? provider.selectedYear
                                : (provider.availableYears.isNotEmpty
                                    ? provider.availableYears.first
                                    : DateTime.now().year - 1),
                            decoration: const InputDecoration(
                              labelText: 'Ano-Calendário',
                              border: OutlineInputBorder(),
                              contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                            ),
                            dropdownColor: AppTheme.surfaceColor,
                            items: (provider.availableYears.isNotEmpty
                                    ? provider.availableYears
                                    : [DateTime.now().year - 1])
                                .map((year) => DropdownMenuItem<int>(
                                      value: year,
                                      child: Text('$year'),
                                    ))
                                .toList(),
                            onChanged: provider.isLoading ? null : (year) {
                              if (year != null) {
                                provider.setYear(year);
                              }
                            },
                          ),
                        ),

                        // Seletor de Perfil Familiar (se houver dependentes)
                        if (profiles.isNotEmpty)
                          SizedBox(
                            width: 280,
                            child: DropdownButtonFormField<String?>(
                              key: const Key('tax_profile_selector'),
                              initialValue: provider.selectedProfileId,
                              decoration: const InputDecoration(
                                labelText: 'Titular / Dependente',
                                border: OutlineInputBorder(),
                                contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                              ),
                              dropdownColor: AppTheme.surfaceColor,
                              items: [
                                const DropdownMenuItem<String?>(
                                  value: null,
                                  child: Text('Conta Titular (Principal)'),
                                ),
                                ...profiles.map(
                                  (profile) => DropdownMenuItem<String?>(
                                    value: profile.id,
                                    child: Text('${profile.name} (${profile.cpfMasked})'),
                                  ),
                                ),
                              ],
                              onChanged: provider.isLoading ? null : (value) {
                                provider.setProfileId(value);
                              },
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 20),

                // Conteúdo Principal
                if (provider.isLoading) ...[
                  const SizedBox(height: 64),
                  const Center(child: CircularProgressIndicator()),
                ] else if (provider.errorMessage != null) ...[
                  Card(
                    color: Colors.red.withValues(alpha: 0.1),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                      side: BorderSide(color: Colors.red.withValues(alpha: 0.3)),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(24.0),
                      child: Column(
                        children: [
                          const Icon(Icons.error_outline, color: Colors.redAccent, size: 40),
                          const SizedBox(height: 12),
                          Text(
                            provider.errorMessage!,
                            textAlign: TextAlign.center,
                            style: const TextStyle(color: Colors.white70),
                          ),
                          const SizedBox(height: 16),
                          ElevatedButton.icon(
                            onPressed: () => provider.loadReport(),
                            icon: const Icon(Icons.refresh),
                            label: const Text('Tentar novamente'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppTheme.primaryColor,
                              minimumSize: const Size(160, 44),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ] else if (report == null || report.items.isEmpty) ...[
                  Card(
                    key: const Key('tax_empty_state'),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 48.0, horizontal: 24.0),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.inventory_2_outlined, size: 56, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 16),
                          Text(
                            'Nenhum bem ou direito registrado para o ano ${provider.selectedYear ?? DateTime.now().year - 1}',
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                  color: Colors.white70,
                                  fontWeight: FontWeight.bold,
                                ),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Faça a importação de extratos ou notas de negociação para apurar as posições.',
                            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                  color: Colors.white54,
                                ),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
                  ),
                ] else ...[
                  // Resumo dos Totais
                  TaxSummaryCard(report: report),
                  const SizedBox(height: 20),

                  // Título da listagem
                  Row(
                    children: [
                      Text(
                        'Itens a Declarar (${report.items.length})',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                            ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '• Ordenados por Grupo e Ticker',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: Colors.white54,
                            ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // Listagem dos Ativos com Subtotais por Grupo
                  ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: report.items.length,
                    itemBuilder: (context, index) {
                      final item = report.items[index];
                      final isFirstOfGroup = index == 0 || report.items[index - 1].groupCode != item.groupCode;
                      final groupSubtotal = report.subtotalsByGroup[item.groupCode] ?? item.currentYearValue;

                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (isFirstOfGroup) ...[
                            if (index > 0) const SizedBox(height: 16),
                            Container(
                              key: Key('tax_group_header_${item.groupCode}'),
                              margin: const EdgeInsets.only(bottom: 8),
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                              decoration: BoxDecoration(
                                color: AppTheme.surfaceColor,
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: Colors.white12),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: Text(
                                      'Grupo ${item.groupCode} • ${item.groupDescription}',
                                      style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                            fontWeight: FontWeight.bold,
                                            color: AppTheme.primaryColor,
                                          ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Subtotal: ${NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$ ').format(groupSubtotal)}',
                                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                          fontWeight: FontWeight.bold,
                                          color: Colors.white,
                                        ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                          TaxItemCard(
                            key: Key('tax_item_${item.ticker}'),
                            item: item,
                            previousYear: report.previousYear,
                            reportYear: report.year,
                          ),
                        ],
                      );
                    },
                  ),
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}
