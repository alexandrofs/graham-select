import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../domain/entities/custody_position.dart';
import '../providers/portfolio_provider.dart';

class AssetAllocationDonutChart extends StatefulWidget {
  const AssetAllocationDonutChart({super.key});

  @override
  State<AssetAllocationDonutChart> createState() => _AssetAllocationDonutChartState();
}

class _AssetAllocationDonutChartState extends State<AssetAllocationDonutChart> with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _animation;

  // Cores premium para o design system
  final Map<String, Color> _classColors = {
    'Ações': const Color(0xFF10B981),      // Emerald Green
    'FIIs': const Color(0xFF1E3A8A),       // Navy/Indigo Blue
    'Renda Fixa': const Color(0xFFF59E0B),  // Amber Gold
    'BDRs': const Color(0xFF8B5CF6),        // Premium Purple
    'Outros': const Color(0xFF64748B),      // Slate Grey
  };

  Color _getColorForClass(String assetClass) {
    return _classColors[assetClass] ?? const Color(0xFF64748B);
  }

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );
    _animation = CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeOutQuart,
    );
    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final currencyFormatter = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    final percentFormatter = NumberFormat.decimalPercentPattern(locale: 'pt_BR', decimalDigits: 1);

    return Consumer<PortfolioProvider>(
      builder: (context, provider, child) {
        if (provider.custodyStatus == PortfolioStatus.loading) {
          return const SizedBox(
            height: 200,
            child: Center(child: CircularProgressIndicator()),
          );
        }

        final List<CustodyPosition> rawPositions = provider.rawCustodyPositions;
        if (rawPositions.isEmpty) {
          return const SizedBox.shrink();
        }

        // Agrupamento por classe
        final Map<String, double> allocationMap = {};
        double totalMarketValue = 0;

        for (var pos in rawPositions) {
          final assetClass = pos.assetClass;
          final value = pos.marketValue;
          allocationMap[assetClass] = (allocationMap[assetClass] ?? 0.0) + value;
          totalMarketValue += value;
        }

        if (totalMarketValue <= 0) {
          return const SizedBox.shrink();
        }

        // Criar fatias (slices) ordenadas por valor decrescente
        final sortedKeys = allocationMap.keys.toList()
          ..sort((a, b) => (allocationMap[b] ?? 0.0).compareTo(allocationMap[a] ?? 0.0));

        final List<DonutSlice> slices = [];
        double startAngle = -pi / 2; // Início no topo (-90 graus)

        for (var key in sortedKeys) {
          final value = allocationMap[key] ?? 0.0;
          final percentage = value / totalMarketValue;
          final sweepAngle = percentage * 2 * pi;

          slices.add(DonutSlice(
            name: key,
            value: value,
            percentage: percentage,
            color: _getColorForClass(key),
            startAngle: startAngle,
            sweepAngle: sweepAngle,
          ));

          startAngle += sweepAngle;
        }

        final filterActive = provider.selectedAssetClassFilter != null;

        return Card(
          elevation: 2,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          color: const Color(0xFF1B2A4A).withValues(alpha: 0.2), // Navy Blue sutil
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Alocação da Carteira',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                    ),
                    if (filterActive)
                      TextButton.icon(
                        onPressed: () => provider.selectAssetClassFilter(null),
                        icon: const Icon(Icons.filter_list_off, size: 16, color: Color(0xFF10B981)),
                        label: const Text(
                          'Limpar Filtro',
                          style: TextStyle(color: Color(0xFF10B981), fontWeight: FontWeight.bold),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 24),
                LayoutBuilder(
                  builder: (context, constraints) {
                    final isWide = constraints.maxWidth > 600;
                    final content = [
                      // Área do gráfico
                      SizedBox(
                        width: 200,
                        height: 200,
                        child: AnimatedBuilder(
                          animation: _animation,
                          builder: (context, child) {
                            return Stack(
                              alignment: Alignment.center,
                              children: [
                                CustomPaint(
                                  size: const Size(200, 200),
                                  painter: DonutChartPainter(
                                    slices: slices,
                                    animationPercent: _animation.value,
                                    selectedClass: provider.selectedAssetClassFilter,
                                  ),
                                ),
                                // Centro do Donut
                                Container(
                                  width: 120,
                                  height: 120,
                                  decoration: const BoxDecoration(
                                    color: Color(0xFF0F172A), // Fundo escuro do dashboard
                                    shape: BoxShape.circle,
                                  ),
                                  child: Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Text(
                                        provider.selectedAssetClassFilter ?? 'Total',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.w500,
                                          color: Colors.white.withValues(alpha: 0.6),
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                      const SizedBox(height: 4),
                                      FittedBox(
                                        fit: BoxFit.scaleDown,
                                        child: Padding(
                                          padding: const EdgeInsets.symmetric(horizontal: 8.0),
                                          child: Text(
                                            currencyFormatter.format(
                                              provider.selectedAssetClassFilter != null
                                                  ? (allocationMap[provider.selectedAssetClassFilter] ?? 0.0)
                                                  : totalMarketValue,
                                            ),
                                            style: const TextStyle(
                                              fontSize: 16,
                                              fontWeight: FontWeight.bold,
                                              color: Colors.white,
                                            ),
                                          ),
                                        ),
                                      ),
                                      if (provider.selectedAssetClassFilter != null) ...[
                                        const SizedBox(height: 2),
                                        Text(
                                          percentFormatter.format(
                                            (allocationMap[provider.selectedAssetClassFilter] ?? 0.0) / totalMarketValue,
                                          ),
                                          style: const TextStyle(
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF10B981),
                                          ),
                                        ),
                                      ]
                                    ],
                                  ),
                                ),
                              ],
                            );
                          },
                        ),
                      ),
                      const SizedBox(width: 40, height: 24),
                      // Área da Legenda
                      Expanded(
                        flex: isWide ? 1 : 0,
                        child: Column(
                          children: slices.map((slice) {
                            final isSelected = provider.selectedAssetClassFilter == slice.name;
                            final activeFilter = provider.selectedAssetClassFilter != null;

                            return Padding(
                              padding: const EdgeInsets.only(bottom: 12.0),
                              child: InkWell(
                                onTap: () {
                                  if (isSelected) {
                                    provider.selectAssetClassFilter(null);
                                  } else {
                                    provider.selectAssetClassFilter(slice.name);
                                  }
                                },
                                borderRadius: BorderRadius.circular(8),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(vertical: 6.0, horizontal: 8.0),
                                  decoration: BoxDecoration(
                                    color: isSelected
                                        ? slice.color.withValues(alpha: 0.15)
                                        : Colors.transparent,
                                    borderRadius: BorderRadius.circular(8),
                                    border: Border.all(
                                      color: isSelected ? slice.color : Colors.transparent,
                                      width: 1,
                                    ),
                                  ),
                                  child: Row(
                                    children: [
                                      Container(
                                        width: 12,
                                        height: 12,
                                        decoration: BoxDecoration(
                                          color: slice.color,
                                          shape: BoxShape.circle,
                                        ),
                                      ),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Text(
                                          slice.name,
                                          style: TextStyle(
                                            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                                            color: activeFilter && !isSelected
                                                ? Colors.white.withValues(alpha: 0.4)
                                                : Colors.white,
                                          ),
                                        ),
                                      ),
                                      Column(
                                        crossAxisAlignment: CrossAxisAlignment.end,
                                        children: [
                                          Text(
                                            currencyFormatter.format(slice.value),
                                            style: TextStyle(
                                              fontWeight: FontWeight.w600,
                                              color: activeFilter && !isSelected
                                                  ? Colors.white.withValues(alpha: 0.4)
                                                  : Colors.white,
                                            ),
                                          ),
                                          Text(
                                            percentFormatter.format(slice.percentage),
                                            style: TextStyle(
                                              fontSize: 11,
                                              color: activeFilter && !isSelected
                                                  ? Colors.white.withValues(alpha: 0.3)
                                                  : Colors.white.withValues(alpha: 0.6),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            );
                          }).toList(),
                        ),
                      ),
                    ];

                    return isWide
                        ? Row(
                            crossAxisAlignment: CrossAxisAlignment.center,
                            children: content,
                          )
                        : Column(
                            children: content,
                          );
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class DonutSlice {
  final String name;
  final double value;
  final double percentage;
  final Color color;
  final double startAngle;
  final double sweepAngle;

  DonutSlice({
    required this.name,
    required this.value,
    required this.percentage,
    required this.color,
    required this.startAngle,
    required this.sweepAngle,
  });
}

class DonutChartPainter extends CustomPainter {
  final List<DonutSlice> slices;
  final double animationPercent;
  final String? selectedClass;

  DonutChartPainter({
    required this.slices,
    required this.animationPercent,
    this.selectedClass,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final outerRadius = size.width / 2;

    final rect = Rect.fromCircle(center: center, radius: outerRadius - 15);
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 30
      ..strokeCap = StrokeCap.butt
      ..isAntiAlias = true;

    // Fundo escuro sutil sob a rosca
    final bgPaint = Paint()
      ..color = const Color(0xFF1E293B).withValues(alpha: 0.3)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 30
      ..isAntiAlias = true;
    canvas.drawCircle(center, outerRadius - 15, bgPaint);

    for (var slice in slices) {
      final isSelected = selectedClass == slice.name;
      final activeFilter = selectedClass != null;

      // Aplicar sutil transparência nas fatias não selecionadas se houver um filtro
      Color color = slice.color;
      if (activeFilter && !isSelected) {
        color = slice.color.withValues(alpha: 0.2);
      }

      paint.color = color;

      // Desenhar o arco animado
      final sliceStartAngle = slice.startAngle;
      final sliceSweepAngle = slice.sweepAngle * animationPercent;

      if (isSelected) {
        // Destacar a fatia selecionada aumentando levemente a largura da borda
        final selectedPaint = Paint()
          ..color = color
          ..style = PaintingStyle.stroke
          ..strokeWidth = 34
          ..isAntiAlias = true;

        canvas.drawArc(rect, sliceStartAngle, sliceSweepAngle, false, selectedPaint);
      } else {
        canvas.drawArc(rect, sliceStartAngle, sliceSweepAngle, false, paint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant DonutChartPainter oldDelegate) {
    return oldDelegate.animationPercent != animationPercent ||
        oldDelegate.selectedClass != selectedClass ||
        oldDelegate.slices != slices;
  }
}
