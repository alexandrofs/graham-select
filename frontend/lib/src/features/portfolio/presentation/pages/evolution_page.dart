import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/monthly_evolution.dart';
import '../providers/portfolio_provider.dart';

class EvolutionPage extends StatefulWidget {
  const EvolutionPage({super.key});

  @override
  State<EvolutionPage> createState() => _EvolutionPageState();
}

class _EvolutionPageState extends State<EvolutionPage> {
  bool _isGraphView = true;
  DateTime? _startDate;
  DateTime? _endDate;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<PortfolioProvider>().loadEvolutionData();
    });
  }

  // Função para formatar o mês "yyyy-MM" para algo amigável "MM/yy"
  String _formatMonth(String monthStr) {
    try {
      final parts = monthStr.split('-');
      if (parts.length == 2) {
        final year = parts[0].substring(2); // Pegar os últimos 2 dígitos do ano
        final monthNum = int.parse(parts[1]);
        const months = [
          'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
          'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'
        ];
        return '${months[monthNum - 1]}/$year';
      }
    } catch (_) {}
    return monthStr;
  }

  // Função para formatar o mês completo para a tabela "Mês de yyyy"
  String _formatMonthFull(String monthStr) {
    try {
      final parts = monthStr.split('-');
      if (parts.length == 2) {
        final year = parts[0];
        final monthNum = int.parse(parts[1]);
        const months = [
          'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
          'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
        ];
        return '${months[monthNum - 1]} de $year';
      }
    } catch (_) {}
    return monthStr;
  }

  // Helper para formatar moeda R$
  String _formatCurrency(double value) {
    return 'R\$ ${value.toStringAsFixed(2).replaceAll('.', ',').replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (Match m) => '${m[1]}.',
        )}';
  }

  // Filtra os dados com base nas datas selecionadas
  List<MonthlyEvolution> _getFilteredData(List<MonthlyEvolution> data) {
    if (_startDate == null && _endDate == null) return data;

    return data.where((item) {
      try {
        final parts = item.month.split('-');
        if (parts.length == 2) {
          final year = int.parse(parts[0]);
          final month = int.parse(parts[1]);
          final itemDate = DateTime(year, month);

          if (_startDate != null && itemDate.isBefore(DateTime(_startDate!.year, _startDate!.month))) {
            return false;
          }
          if (_endDate != null && itemDate.isAfter(DateTime(_endDate!.year, _endDate!.month))) {
            return false;
          }
        }
      } catch (_) {}
      return true;
    }).toList();
  }

  Future<void> _selectStartDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _startDate ?? DateTime.now().minusMonths(11),
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
      helpText: 'Selecione o mês inicial',
    );
    if (picked != null) {
      setState(() {
        _startDate = picked;
      });
    }
  }

  Future<void> _selectEndDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _endDate ?? DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
      helpText: 'Selecione o mês final',
    );
    if (picked != null) {
      setState(() {
        _endDate = picked;
      });
    }
  }

  void _clearFilters() {
    setState(() {
      _startDate = null;
      _endDate = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Cabeçalho da página
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Evolução da Carteira',
                          style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Acompanhe a evolução de seus aportes e dividendos para visualizar o efeito bola de neve.',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: Colors.white60,
                              ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 16),
                  // Alternador de Visualização (Gráfico / Tabela)
                  Container(
                    decoration: BoxDecoration(
                      color: AppTheme.surfaceColor,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.white10),
                    ),
                    padding: const EdgeInsets.all(4),
                    child: Row(
                      children: [
                        _ViewTabButton(
                          label: 'Gráfico',
                          icon: Icons.bar_chart_rounded,
                          isSelected: _isGraphView,
                          onTap: () => setState(() => _isGraphView = true),
                        ),
                        _ViewTabButton(
                          label: 'Tabela',
                          icon: Icons.table_chart_outlined,
                          isSelected: !_isGraphView,
                          onTap: () => setState(() => _isGraphView = false),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),

              // Conteúdo Dinâmico com base no Provider
              Expanded(
                child: Consumer<PortfolioProvider>(
                  builder: (context, provider, child) {
                    if (provider.evolutionStatus == PortfolioStatus.loading) {
                      return const Center(
                        child: CircularProgressIndicator(color: AppTheme.primaryColor),
                      );
                    }

                    if (provider.evolutionStatus == PortfolioStatus.error) {
                      return Center(
                        child: Card(
                          color: AppTheme.surfaceColor,
                          child: Padding(
                            padding: const EdgeInsets.all(24.0),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(Icons.error_outline_rounded, size: 48, color: Colors.redAccent),
                                const SizedBox(height: 16),
                                Text(
                                  'Erro ao carregar dados',
                                  style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                                ),
                                const SizedBox(height: 8),
                                Text(
                                  provider.evolutionError ?? 'Ocorreu um erro desconhecido.',
                                  textAlign: TextAlign.center,
                                  style: const TextStyle(color: Colors.white60),
                                ),
                                const SizedBox(height: 16),
                                ElevatedButton(
                                  onPressed: () => provider.loadEvolutionData(),
                                  style: ElevatedButton.styleFrom(
                                    minimumSize: const Size(150, 44),
                                    backgroundColor: AppTheme.primaryColor,
                                  ),
                                  child: const Text('Tentar Novamente'),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    }

                    final evolution = provider.evolutionData;
                    if (evolution == null || evolution.monthlyData.isEmpty) {
                      return Center(
                        child: Text(
                          'Nenhum histórico disponível ainda.',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(color: Colors.white60),
                        ),
                      );
                    }

                    final data = evolution.monthlyData;
                    final filteredData = _getFilteredData(data);

                    if (_isGraphView) {
                      return _buildGraphView(data);
                    } else {
                      return _buildTableView(filteredData);
                    }
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // Desenha o gráfico de barras customizado, responsivo e esteticamente premium
  Widget _buildGraphView(List<MonthlyEvolution> data) {
    // Apenas últimos 12 meses na visualização de gráfico, como pede a especificação
    final displayData = data.length > 12 ? data.sublist(data.length - 12) : data;

    // Encontra o maior valor absoluto para escala proporcional das barras
    double maxVal = 0.0;
    for (var item in displayData) {
      if (item.totalContributions > maxVal) maxVal = item.totalContributions;
      if (item.totalDividends > maxVal) maxVal = item.totalDividends;
    }
    // Adiciona margem de folga no topo do gráfico
    if (maxVal == 0.0) maxVal = 1000.0;
    final graphMaxHeight = maxVal * 1.1;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Legenda moderna no topo do gráfico
            Row(
              children: [
                _LegendItem(color: AppTheme.primaryColor, label: 'Aportes (Compras)'),
                const SizedBox(width: 24),
                _LegendItem(color: AppTheme.accentColor, label: 'Dividendos (Proventos)'),
              ],
            ),
            const SizedBox(height: 32),

            // Área Principal das Barras
            Expanded(
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final barWidth = (constraints.maxWidth / displayData.length) * 0.35;
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: displayData.map((item) {
                      double contributionHeight = constraints.maxHeight * 0.75 * (item.totalContributions / graphMaxHeight);
                      if (item.totalContributions > 0 && contributionHeight < 4.0) {
                        contributionHeight = 4.0;
                      }

                      double dividendsHeight = constraints.maxHeight * 0.75 * (item.totalDividends / graphMaxHeight);
                      if (item.totalDividends > 0 && dividendsHeight < 4.0) {
                        dividendsHeight = 4.0;
                      }

                      return Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          // As duas barras emparelhadas
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              // Barra de Aporte
                              Tooltip(
                                message: 'Aportado: ${_formatCurrency(item.totalContributions)}',
                                child: AnimatedContainer(
                                  duration: const Duration(milliseconds: 500),
                                  curve: Curves.easeOutCubic,
                                  width: barWidth,
                                  height: contributionHeight,
                                  decoration: BoxDecoration(
                                    gradient: LinearGradient(
                                      colors: [
                                        AppTheme.primaryColor,
                                        AppTheme.primaryColor.withValues(alpha: 0.7),
                                      ],
                                      begin: Alignment.topCenter,
                                      end: Alignment.bottomCenter,
                                    ),
                                    borderRadius: const BorderRadius.only(
                                      topLeft: Radius.circular(6),
                                      topRight: Radius.circular(6),
                                    ),
                                    boxShadow: [
                                      BoxShadow(
                                        color: AppTheme.primaryColor.withValues(alpha: 0.2),
                                        blurRadius: 8,
                                        offset: const Offset(0, 4),
                                      )
                                    ],
                                  ),
                                ),
                              ),
                              const SizedBox(width: 4),
                              // Barra de Dividendo
                              Tooltip(
                                message: 'Dividendos: ${_formatCurrency(item.totalDividends)}',
                                child: AnimatedContainer(
                                  duration: const Duration(milliseconds: 500),
                                  curve: Curves.easeOutCubic,
                                  width: barWidth,
                                  height: dividendsHeight,
                                  decoration: BoxDecoration(
                                    gradient: LinearGradient(
                                      colors: [
                                        AppTheme.accentColor,
                                        AppTheme.accentColor.withValues(alpha: 0.7),
                                      ],
                                      begin: Alignment.topCenter,
                                      end: Alignment.bottomCenter,
                                    ),
                                    borderRadius: const BorderRadius.only(
                                      topLeft: Radius.circular(6),
                                      topRight: Radius.circular(6),
                                    ),
                                    boxShadow: [
                                      BoxShadow(
                                        color: AppTheme.accentColor.withValues(alpha: 0.2),
                                        blurRadius: 8,
                                        offset: const Offset(0, 4),
                                      )
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 12),
                          // Rótulo do Mês abaixo das barras
                          Text(
                            _formatMonth(item.month),
                            style: const TextStyle(
                              fontSize: 12,
                              color: Colors.white54,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      );
                    }).toList(),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  // Desenha a tabela analítica com cabeçalho de filtros de período
  Widget _buildTableView(List<MonthlyEvolution> data) {
    return Column(
      children: [
        // Seção de Filtros de Período
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppTheme.surfaceColor,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
          ),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                const Icon(Icons.filter_alt_outlined, color: AppTheme.primaryColor),
                const SizedBox(width: 12),
                const Text(
                  'Filtrar Período:',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                ),
                const SizedBox(width: 24),
                // Botão Mês Inicial
                InkWell(
                  onTap: () => _selectStartDate(context),
                  borderRadius: BorderRadius.circular(8),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.05),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.calendar_today_rounded, size: 14, color: Colors.white60),
                        const SizedBox(width: 8),
                        Text(
                          _startDate == null
                              ? 'Mês Início'
                              : '${_startDate!.month.toString().padLeft(2, '0')}/${_startDate!.year}',
                          style: const TextStyle(fontSize: 14, color: Colors.white),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                const Text('até', style: TextStyle(color: Colors.white38)),
                const SizedBox(width: 12),
                // Botão Mês Final
                InkWell(
                  onTap: () => _selectEndDate(context),
                  borderRadius: BorderRadius.circular(8),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.05),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.white10),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.calendar_today_rounded, size: 14, color: Colors.white60),
                        const SizedBox(width: 8),
                        Text(
                          _endDate == null
                              ? 'Mês Fim'
                              : '${_endDate!.month.toString().padLeft(2, '0')}/${_endDate!.year}',
                          style: const TextStyle(fontSize: 14, color: Colors.white),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                // Limpar filtros se houver algum selecionado
                if (_startDate != null || _endDate != null)
                  TextButton.icon(
                    onPressed: _clearFilters,
                    icon: const Icon(Icons.clear, size: 16, color: Colors.redAccent),
                    label: const Text(
                      'Limpar',
                      style: TextStyle(color: Colors.redAccent, fontSize: 14),
                    ),
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

        // Tabela de Dados Real
        Expanded(
          child: Card(
            child: data.isEmpty
                ? const Center(
                    child: Text(
                      'Nenhum registro encontrado no período selecionado.',
                      style: TextStyle(color: Colors.white54),
                    ),
                  )
                : SingleChildScrollView(
                    child: Table(
                      columnWidths: const {
                        0: FlexColumnWidth(2),
                        1: FlexColumnWidth(2),
                        2: FlexColumnWidth(2),
                        3: FlexColumnWidth(2),
                      },
                      children: [
                        // Cabeçalho da Tabela
                        TableRow(
                          decoration: BoxDecoration(
                            border: Border(
                              bottom: BorderSide(color: Colors.white.withValues(alpha: 0.05)),
                            ),
                          ),
                          children: const [
                            _TableHeaderCell(label: 'Mês de Referência'),
                            _TableHeaderCell(label: 'Total Aportado'),
                            _TableHeaderCell(label: 'Proventos Recebidos'),
                            _TableHeaderCell(label: 'Resultado Líquido'),
                          ],
                        ),
                        // Linhas de dados
                        ...data.map((item) {
                          final netResult = item.totalContributions + item.totalDividends;
                          return TableRow(
                            decoration: BoxDecoration(
                              border: Border(
                                bottom: BorderSide(color: Colors.white.withValues(alpha: 0.02)),
                              ),
                            ),
                            children: [
                              _TableCell(
                                child: Text(
                                  _formatMonthFull(item.month),
                                  style: const TextStyle(fontWeight: FontWeight.bold),
                                ),
                              ),
                              _TableCell(
                                child: Text(
                                  _formatCurrency(item.totalContributions),
                                  style: const TextStyle(color: AppTheme.primaryColor),
                                ),
                              ),
                              _TableCell(
                                child: Text(
                                  _formatCurrency(item.totalDividends),
                                  style: const TextStyle(color: AppTheme.accentColor),
                                ),
                              ),
                              _TableCell(
                                child: Text(
                                  _formatCurrency(netResult),
                                  style: const TextStyle(fontWeight: FontWeight.bold),
                                ),
                              ),
                            ],
                          );
                        }),
                      ],
                    ),
                  ),
          ),
        ),
      ],
    );
  }
}

// Botões modernos tipo Pílula para alternar a tela
class _ViewTabButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onTap;

  const _ViewTabButton({
    required this.label,
    required this.icon,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? AppTheme.primaryColor : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(icon, size: 16, color: isSelected ? Colors.white : Colors.white60),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                fontSize: 14,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                color: isSelected ? Colors.white : Colors.white60,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// Legenda estilizada para o gráfico
class _LegendItem extends StatelessWidget {
  final Color color;
  final String label;

  const _LegendItem({required this.color, required this.label});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 14,
          height: 14,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(4),
          ),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(fontSize: 13, color: Colors.white70),
        ),
      ],
    );
  }
}

// Células customizadas da Tabela
class _TableHeaderCell extends StatelessWidget {
  final String label;

  const _TableHeaderCell({required this.label});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Text(
        label,
        style: const TextStyle(
          fontWeight: FontWeight.bold,
          color: Colors.white54,
          fontSize: 13,
        ),
      ),
    );
  }
}

class _TableCell extends StatelessWidget {
  final Widget child;

  const _TableCell({required this.child});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 14.0),
      child: Align(
        alignment: Alignment.centerLeft,
        child: child,
      ),
    );
  }
}

// Helper Extension para DateTime no Dart
extension DateTimeExtension on DateTime {
  DateTime minusMonths(int months) {
    var year = this.year;
    var month = this.month - months;
    while (month <= 0) {
      month += 12;
      year -= 1;
    }
    return DateTime(year, month, day);
  }
}
