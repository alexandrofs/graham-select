import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../portfolio/presentation/providers/portfolio_provider.dart';
import '../providers/goals_provider.dart';
import '../widgets/goal_summary_card.dart';

class MoneyInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
      TextEditingValue oldValue, TextEditingValue newValue) {
    if (newValue.selection.baseOffset == 0) {
      return newValue;
    }

    final String cleanText = newValue.text.replaceAll(RegExp(r'[^0-9]'), '');
    if (cleanText.isEmpty) {
      return newValue.copyWith(
        text: '',
        selection: const TextSelection.collapsed(offset: 0),
      );
    }

    final double value = double.parse(cleanText);
    final formatter = NumberFormat.simpleCurrency(locale: 'pt_BR');
    final String newText = formatter.format(value / 100);

    return newValue.copyWith(
      text: newText,
      selection: TextSelection.collapsed(offset: newText.length),
    );
  }
}

class GoalsPage extends StatefulWidget {
  static bool isTesting = false;
  const GoalsPage({super.key});

  @override
  State<GoalsPage> createState() => _GoalsPageState();
}

class _GoalsPageState extends State<GoalsPage> with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _targetValueController = TextEditingController();
  final _contributionController = TextEditingController();
  final _yearsController = TextEditingController();

  String _goalType = 'PATRIMONY_TARGET';
  bool _initialized = false;
  late GoalsProvider _goalsProvider;
  late AnimationController _shimmerController;
  late Animation<double> _shimmerAnimation;

  @override
  void initState() {
    super.initState();
    _goalsProvider = context.read<GoalsProvider>();
    _goalsProvider.addListener(_onGoalsProviderChanged);
    
    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    );
    if (!GoalsPage.isTesting) {
      _shimmerController.repeat(reverse: true);
    }
    _shimmerAnimation = Tween<double>(begin: 0.05, end: 0.15).animate(
      CurvedAnimation(parent: _shimmerController, curve: Curves.easeInOut),
    );

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _goalsProvider.loadGoal();
        context.read<PortfolioProvider>().loadSummary();
      }
    });
  }

  @override
  void dispose() {
    _shimmerController.dispose();
    _goalsProvider.removeListener(_onGoalsProviderChanged);
    _targetValueController.dispose();
    _contributionController.dispose();
    _yearsController.dispose();
    super.dispose();
  }

  void _onGoalsProviderChanged() {
    if (!mounted) return;
    final provider = context.read<GoalsProvider>();
    if (provider.status == GoalsStatus.success && provider.currentGoal != null && !_initialized) {
      final goal = provider.currentGoal!;
      final formatter = NumberFormat.simpleCurrency(locale: 'pt_BR');
      
      setState(() {
        _goalType = goal.goalType;
        _targetValueController.text = formatter.format(goal.targetValue);
        _contributionController.text = formatter.format(goal.monthlyContribution);
        _yearsController.text = goal.estimatedYears.toString();
        _initialized = true;
      });
    }
  }

  double _parseMoneyValue(String text) {
    final cleanText = text.replaceAll(RegExp(r'[^0-9]'), '');
    if (cleanText.isEmpty) return 0.0;
    return double.parse(cleanText) / 100.0;
  }

  Future<void> _submitForm() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    final double targetValue = _parseMoneyValue(_targetValueController.text);
    final double monthlyContribution = _parseMoneyValue(_contributionController.text);
    final int estimatedYears = int.parse(_yearsController.text);

    final goalsProvider = context.read<GoalsProvider>();
    await goalsProvider.saveGoal(
      goalType: _goalType,
      targetValue: targetValue,
      monthlyContribution: monthlyContribution,
      estimatedYears: estimatedYears,
    );

    if (!mounted) return;

    if (goalsProvider.status == GoalsStatus.success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Meta salva com sucesso ✅'),
          backgroundColor: Color(0xFF10B981),
          duration: Duration(seconds: 1),
        ),
      );
      // Recarrega o sumário do portfólio para atualizar o cálculo de diferença
      context.read<PortfolioProvider>().loadSummary();
    } else if (goalsProvider.status == GoalsStatus.error) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(goalsProvider.errorMessage ?? 'Erro ao salvar meta'),
          backgroundColor: const Color(0xFFEF4444),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final goalsProvider = context.watch<GoalsProvider>();
    final portfolioProvider = context.watch<PortfolioProvider>();

    const Color kNavyBlue = Color(0xFF1B2A4A);
    const Color kEmerald = Color(0xFF10B981);

    if (goalsProvider.status == GoalsStatus.loading && !_initialized) {
      return Scaffold(
        body: _buildSkeletonLoader(),
      );
    }

    final double currentPatrimony = portfolioProvider.summary?.totalEquity ?? 0.0;

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Minhas Metas',
              style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    color: kNavyBlue,
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 24),
            Form(
              key: _formKey,
              child: Card(
                elevation: 2,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Defina seu Objetivo Financeiro',
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              color: kNavyBlue,
                              fontWeight: FontWeight.bold,
                            ),
                      ),
                      const SizedBox(height: 16),
                      // Tipo de Meta (SegmentedButton)
                      Text(
                        'Tipo de Meta',
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                              color: kNavyBlue.withAlpha((0.7 * 255).round()),
                              fontWeight: FontWeight.w600,
                            ),
                      ),
                      const SizedBox(height: 8),
                      SizedBox(
                        width: double.infinity,
                        child: SegmentedButton<String>(
                          segments: const [
                            ButtonSegment<String>(
                              value: 'PATRIMONY_TARGET',
                              label: Text('Patrimônio Alvo'),
                            ),
                            ButtonSegment<String>(
                              value: 'MONTHLY_INCOME_TARGET',
                              label: Text('Renda Mensal Alvo'),
                            ),
                          ],
                          selected: {_goalType},
                          onSelectionChanged: (newSelection) {
                            setState(() {
                              _goalType = newSelection.first;
                            });
                          },
                        ),
                      ),
                      const SizedBox(height: 20),
                      // Valor Alvo
                      TextFormField(
                        controller: _targetValueController,
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                          MoneyInputFormatter(),
                        ],
                        decoration: const InputDecoration(
                          labelText: 'Valor Alvo',
                          prefixText: 'R\$ ',
                          border: OutlineInputBorder(),
                        ),
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Este campo é obrigatório';
                          }
                          final parsedValue = _parseMoneyValue(value);
                          if (parsedValue <= 0) {
                            return 'O valor deve ser maior que R\$ 0,00';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 16),
                      // Aporte Mensal Pretendido
                      TextFormField(
                        controller: _contributionController,
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                          MoneyInputFormatter(),
                        ],
                        decoration: const InputDecoration(
                          labelText: 'Aporte Mensal Pretendido',
                          prefixText: 'R\$ ',
                          border: OutlineInputBorder(),
                        ),
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Este campo é obrigatório';
                          }
                          final parsedValue = _parseMoneyValue(value);
                          if (parsedValue <= 0) {
                            return 'O valor deve ser maior que R\$ 0,00';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 16),
                      // Prazo Estimado
                      TextFormField(
                        controller: _yearsController,
                        keyboardType: TextInputType.number,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: const InputDecoration(
                          labelText: 'Prazo Estimado (em anos)',
                          border: OutlineInputBorder(),
                        ),
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Este campo é obrigatório';
                          }
                          final parsedValue = int.tryParse(value);
                          if (parsedValue == null || parsedValue < 1 || parsedValue > 50) {
                            return 'O prazo deve estar entre 1 e 50 anos';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 24),
                      // Botão Salvar
                      SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: FilledButton(
                          onPressed: goalsProvider.status == GoalsStatus.loading
                              ? null
                              : _submitForm,
                          style: FilledButton.styleFrom(
                            backgroundColor: kEmerald,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8),
                            ),
                          ),
                          child: goalsProvider.status == GoalsStatus.loading
                              ? const SizedBox(
                                  height: 20,
                                  width: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white,
                                  ),
                                )
                              : const Text('Salvar Meta'),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
            // Exibir resumo da meta se houver meta salva e não estiver carregando
            if (goalsProvider.currentGoal != null)
              GoalSummaryCard(
                goal: goalsProvider.currentGoal!,
                currentPatrimony: currentPatrimony,
              ),
          ],
        ),
      ),
    );
  }

  Widget _shimmerBlock({double width = 60, double height = 16}) {
    const Color kNavyBlue = Color(0xFF1B2A4A);
    return AnimatedBuilder(
      animation: _shimmerAnimation,
      builder: (context, _) {
        return Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            color: kNavyBlue.withAlpha((_shimmerAnimation.value * 255).round()),
          ),
        );
      },
    );
  }

  Widget _buildSkeletonLoader() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _shimmerBlock(width: 200, height: 32),
          const SizedBox(height: 24),
          Card(
            elevation: 2,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _shimmerBlock(width: 150, height: 24),
                  const SizedBox(height: 24),
                  _shimmerBlock(width: double.infinity, height: 50),
                  const SizedBox(height: 16),
                  _shimmerBlock(width: double.infinity, height: 50),
                  const SizedBox(height: 16),
                  _shimmerBlock(width: double.infinity, height: 50),
                  const SizedBox(height: 24),
                  _shimmerBlock(width: double.infinity, height: 48),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
