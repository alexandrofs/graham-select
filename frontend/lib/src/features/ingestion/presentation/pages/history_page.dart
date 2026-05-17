import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/ingestion_history_provider.dart';
import '../widgets/ingestion_history_table.dart';

class IngestionHistoryPage extends StatefulWidget {
  const IngestionHistoryPage({super.key});

  @override
  State<IngestionHistoryPage> createState() => _IngestionHistoryPageState();
}

class _IngestionHistoryPageState extends State<IngestionHistoryPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<IngestionHistoryProvider>().fetchHistory();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Histórico de Importação'),
      ),
      body: Consumer<IngestionHistoryProvider>(
        builder: (context, provider, child) {
          if (provider.state == IngestionHistoryState.loading) {
            return _buildSkeleton(context);
          }

          if (provider.state == IngestionHistoryState.error) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: Colors.red),
                  const SizedBox(height: 16),
                  Text(provider.errorMessage ?? 'Erro ao carregar histórico'),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: provider.fetchHistory,
                    child: const Text('Tentar Novamente'),
                  ),
                ],
              ),
            );
          }

          if (provider.history.isEmpty) {
            return const Center(
              child: Text('Nenhuma importação encontrada'),
            );
          }

          return RefreshIndicator(
            onRefresh: provider.fetchHistory,
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              physics: const AlwaysScrollableScrollPhysics(),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Meus Arquivos',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: 16),
                  IngestionHistoryTable(audits: provider.history),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildSkeleton(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 150,
            height: 24,
            decoration: BoxDecoration(
              color: Colors.grey[300],
              borderRadius: BorderRadius.circular(4),
            ),
          ),
          const SizedBox(height: 24),
          ...List.generate(
            5,
            (index) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Container(
                width: double.infinity,
                height: 60,
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
