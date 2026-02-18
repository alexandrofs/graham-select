import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/ranking_provider.dart';
import '../../domain/entities/ranked_company.dart';

class RankingPage extends StatefulWidget {
  const RankingPage({super.key});

  @override
  State<RankingPage> createState() => _RankingPageState();
}

class _RankingPageState extends State<RankingPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<RankingProvider>().fetchRanking();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Top 20 Graham'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<RankingProvider>().fetchRanking(),
          ),
        ],
      ),
      body: Consumer<RankingProvider>(
        builder: (context, provider, child) {
          switch (provider.state) {
            case RankingState.loading:
              return const Center(child: CircularProgressIndicator());
            case RankingState.error:
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(24.0),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.error_outline, size: 60, color: Colors.red),
                      const SizedBox(height: 16),
                      Text(
                        'Erro ao carregar ranking:',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        provider.errorMessage ?? 'Erro desconhecido',
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton.icon(
                        onPressed: () => provider.fetchRanking(),
                        icon: const Icon(Icons.refresh),
                        label: const Text('Tentar Novamente'),
                      ),
                    ],
                  ),
                ),
              );
            case RankingState.success:
              if (provider.companies.isEmpty) {
                 return const Center(child: Text('Nenhuma empresa encontrada com os critérios.'));
              }
              return _buildRankingTable(context, provider.companies);
            case RankingState.idle:
              return const SizedBox.shrink();
          }
        },
      ),
    );
  }

  Widget _buildRankingTable(BuildContext context, List<RankedCompany> companies) {
    return SingleChildScrollView(
      scrollDirection: Axis.vertical,
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: DataTable(
          columns: const [
            DataColumn(label: Text('Ticker', style: TextStyle(fontWeight: FontWeight.bold))),
            DataColumn(label: Text('Nome', style: TextStyle(fontWeight: FontWeight.bold))),
            DataColumn(label: Text('Preço Atual', style: TextStyle(fontWeight: FontWeight.bold)), numeric: true),
            DataColumn(label: Text('Valor Intrínseco', style: TextStyle(fontWeight: FontWeight.bold)), numeric: true),
            DataColumn(label: Text('Margem Seg.', style: TextStyle(fontWeight: FontWeight.bold)), numeric: true),
          ],
          rows: companies.map((company) {
            return DataRow(cells: [
              DataCell(Text(company.symbol, style: const TextStyle(fontWeight: FontWeight.bold))),
              DataCell(Text(company.name)),
              DataCell(Text('R\$ ${company.currentPrice.toStringAsFixed(2)}')),
              DataCell(Text('R\$ ${company.intrinsicValue.toStringAsFixed(2)}')),
              DataCell(Text('${(company.marginOfSafety * 100).toStringAsFixed(1)}%')),
            ]);
          }).toList(),
        ),
      ),
    );
  }
}
