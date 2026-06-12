import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../domain/entities/allocation_goal.dart';
import '../providers/allocation_provider.dart';

class AllocationStrategyPage extends StatefulWidget {
  const AllocationStrategyPage({super.key});

  @override
  State<AllocationStrategyPage> createState() => _AllocationStrategyPageState();
}

class _AllocationStrategyPageState extends State<AllocationStrategyPage>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();

  // Class controllers
  final _acoesController = TextEditingController();
  final _fiisController = TextEditingController();
  final _rendaFixaController = TextEditingController();
  final _outrosController = TextEditingController();

  // Ticker fields list
  final List<_TickerField> _tickerFields = [];

  double _classSum = 0;
  bool _initialized = false;

  // Shimmer animation
  late final AnimationController _shimmerController;
  late final Animation<double> _shimmerAnimation;

  @override
  void initState() {
    super.initState();

    _shimmerController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
    _shimmerAnimation = Tween<double>(begin: -1.5, end: 2.5).animate(
      CurvedAnimation(parent: _shimmerController, curve: Curves.easeInOut),
    );

    _acoesController.addListener(_updateSums);
    _fiisController.addListener(_updateSums);
    _rendaFixaController.addListener(_updateSums);
    _outrosController.addListener(_updateSums);

    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AllocationProvider>().loadGoals();
    });
  }

  @override
  void dispose() {
    _acoesController.removeListener(_updateSums);
    _fiisController.removeListener(_updateSums);
    _rendaFixaController.removeListener(_updateSums);
    _outrosController.removeListener(_updateSums);
    _acoesController.dispose();
    _fiisController.dispose();
    _rendaFixaController.dispose();
    _outrosController.dispose();
    for (final field in _tickerFields) {
      field.tickerController.removeListener(_updateSums);
      field.percentageController.removeListener(_updateSums);
      field.tickerController.dispose();
      field.percentageController.dispose();
    }
    _shimmerController.dispose();
    super.dispose();
  }

  void _updateSums() {
    double sum = 0;
    sum += double.tryParse(_acoesController.text.replaceAll(',', '.')) ?? 0;
    sum += double.tryParse(_fiisController.text.replaceAll(',', '.')) ?? 0;
    sum += double.tryParse(_rendaFixaController.text.replaceAll(',', '.')) ?? 0;
    sum += double.tryParse(_outrosController.text.replaceAll(',', '.')) ?? 0;

    setState(() {
      _classSum = sum;
    });
  }

  void _initializeControllers(List<AllocationGoal> goals) {
    double acoes = 0;
    double fiis = 0;
    double rendaFixa = 0;
    double outros = 0;

    // Dispose old ticker controllers to avoid memory leaks
    for (final field in _tickerFields) {
      field.tickerController.removeListener(_updateSums);
      field.percentageController.removeListener(_updateSums);
      field.tickerController.dispose();
      field.percentageController.dispose();
    }
    _tickerFields.clear();

    for (final goal in goals) {
      if (goal.goalType == 'ASSET_CLASS') {
        if (goal.targetKey == 'ACOES') acoes = goal.targetPercentage;
        if (goal.targetKey == 'FIIS') fiis = goal.targetPercentage;
        if (goal.targetKey == 'RENDA_FIXA') rendaFixa = goal.targetPercentage;
        if (goal.targetKey == 'OUTROS') outros = goal.targetPercentage;
      } else if (goal.goalType == 'TICKER') {
        final tc = TextEditingController(text: goal.targetKey);
        final pc = TextEditingController(text: goal.targetPercentage.toString().replaceAll(RegExp(r'\.0$'), ''));
        tc.addListener(_updateSums);
        pc.addListener(_updateSums);
        _tickerFields.add(_TickerField(tickerController: tc, percentageController: pc));
      }
    }

    _acoesController.text = acoes > 0 ? acoes.toString().replaceAll(RegExp(r'\.0$'), '') : '';
    _fiisController.text = fiis > 0 ? fiis.toString().replaceAll(RegExp(r'\.0$'), '') : '';
    _rendaFixaController.text = rendaFixa > 0 ? rendaFixa.toString().replaceAll(RegExp(r'\.0$'), '') : '';
    _outrosController.text = outros > 0 ? outros.toString().replaceAll(RegExp(r'\.0$'), '') : '';

    _updateSums();
  }

  void _addTickerField() {
    setState(() {
      final tc = TextEditingController();
      final pc = TextEditingController();
      tc.addListener(_updateSums);
      pc.addListener(_updateSums);
      _tickerFields.add(_TickerField(tickerController: tc, percentageController: pc));
    });
  }

  void _removeTickerField(int index) {
    setState(() {
      _tickerFields[index].tickerController.removeListener(_updateSums);
      _tickerFields[index].percentageController.removeListener(_updateSums);
      _tickerFields[index].tickerController.dispose();
      _tickerFields[index].percentageController.dispose();
      _tickerFields.removeAt(index);
      _updateSums();
    });
  }

  void _submit() {
    if (_formKey.currentState!.validate()) {
      if (_classSum != 100) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('A soma das metas de classe deve fechar em exatamente 100%.'),
            backgroundColor: Colors.red,
          ),
        );
        return;
      }

      final Set<String> uniqueTickers = {};
      final List<AllocationGoal> goalsToSave = [];

      // Add classes
      if (_acoesController.text.isNotEmpty) {
        goalsToSave.add(AllocationGoal(
          goalType: 'ASSET_CLASS',
          targetKey: 'ACOES',
          targetPercentage: double.parse(_acoesController.text.replaceAll(',', '.')),
        ));
      }
      if (_fiisController.text.isNotEmpty) {
        goalsToSave.add(AllocationGoal(
          goalType: 'ASSET_CLASS',
          targetKey: 'FIIS',
          targetPercentage: double.parse(_fiisController.text.replaceAll(',', '.')),
        ));
      }
      if (_rendaFixaController.text.isNotEmpty) {
        goalsToSave.add(AllocationGoal(
          goalType: 'ASSET_CLASS',
          targetKey: 'RENDA_FIXA',
          targetPercentage: double.parse(_rendaFixaController.text.replaceAll(',', '.')),
        ));
      }
      if (_outrosController.text.isNotEmpty) {
        goalsToSave.add(AllocationGoal(
          goalType: 'ASSET_CLASS',
          targetKey: 'OUTROS',
          targetPercentage: double.parse(_outrosController.text.replaceAll(',', '.')),
        ));
      }

      // Add tickers
      for (final field in _tickerFields) {
        final ticker = field.tickerController.text.trim().toUpperCase();
        final pctStr = field.percentageController.text.trim();
        if (ticker.isNotEmpty && pctStr.isNotEmpty) {
          if (!uniqueTickers.add(ticker)) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Ativo duplicado não é permitido: $ticker'),
                backgroundColor: Colors.red,
              ),
            );
            return;
          }
          goalsToSave.add(AllocationGoal(
            goalType: 'TICKER',
            targetKey: ticker,
            targetPercentage: double.parse(pctStr.replaceAll(',', '.')),
          ));
        }
      }

      context.read<AllocationProvider>().saveGoals(goalsToSave).then((_) {
        if (!mounted) return;
        final provider = context.read<AllocationProvider>();
        if (provider.status == AllocationStatus.success) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Estratégia de alocação salva com sucesso!'),
              backgroundColor: Colors.green,
            ),
          );
        } else if (provider.status == AllocationStatus.error) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(provider.errorMessage ?? 'Erro ao salvar metas.'),
              backgroundColor: Colors.red,
            ),
          );
        }
      });
    }
  }

  Widget _shimmerBlock({double width = 60, double height = 16}) {
    return AnimatedBuilder(
      animation: _shimmerAnimation,
      builder: (context, _) {
        final t = ((_shimmerAnimation.value + 1.5) / 4.0).clamp(0.0, 1.0);
        final opacity = 0.05 + 0.15 * (1 - (2 * t - 1).abs());
        return Container(
          width: width,
          height: height,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            color: Colors.white.withOpacity(opacity),
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
          _shimmerBlock(width: 250, height: 32),
          const SizedBox(height: 12),
          _shimmerBlock(width: double.infinity, height: 18),
          const SizedBox(height: 8),
          _shimmerBlock(width: 300, height: 18),
          const SizedBox(height: 32),
          _shimmerBlock(width: 180, height: 24),
          const SizedBox(height: 16),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 16,
            mainAxisSpacing: 16,
            childAspectRatio: 2.5,
            children: List.generate(4, (_) => Card(child: Container())),
          ),
          const SizedBox(height: 32),
          _shimmerBlock(width: 180, height: 24),
          const SizedBox(height: 16),
          _shimmerBlock(width: double.infinity, height: 60),
          const SizedBox(height: 32),
          _shimmerBlock(width: 200, height: 48),
        ],
      ),
    );
  }

  Widget _buildPremiumPaywall() {
    return Center(
      child: Container(
        constraints: const BoxConstraints(maxWidth: 600),
        margin: const EdgeInsets.all(24.0),
        child: Card(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(32.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primary.withOpacity(0.1),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.workspace_premium_outlined,
                    size: 64,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'Configuração de Estratégia (Premium)',
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
                Text(
                  'A definição de metas de alocação e o Motor de Graham de recomendações estão disponíveis exclusivamente para usuários assinantes do plano Premium.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                      ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                const Divider(),
                const SizedBox(height: 16),
                _buildBenefitRow(Icons.check_circle_outline, 'Defina metas personalizadas por classe de ativos'),
                _buildBenefitRow(Icons.check_circle_outline, 'Metas granulares complementares para tickers específicos'),
                _buildBenefitRow(Icons.check_circle_outline, 'Motor de recomendação de aportes com o filtro de Graham'),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: () {
                    // Navigate to profile to update tier
                    Navigator.of(context).pushNamed('/profile');
                  },
                  child: const Text('Fazer Upgrade na Página de Perfil'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBenefitRow(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        children: [
          Icon(icon, size: 20, color: Theme.of(context).colorScheme.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(fontSize: 14),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState(String message) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text(
              'Falha ao carregar metas de alocação:',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: () => context.read<AllocationProvider>().loadGoals(),
              icon: const Icon(Icons.refresh),
              label: const Text('Recarregar'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(200, 56),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Consumer<AllocationProvider>(
        builder: (context, provider, child) {
          if (provider.status == AllocationStatus.loading && !_initialized) {
            return _buildSkeletonLoader();
          }

          if (provider.status == AllocationStatus.error) {
            final errMsg = provider.errorMessage ?? '';
            if (errMsg.contains('Premium') || errMsg.contains('exclusiva')) {
              return _buildPremiumPaywall();
            }
            return _buildErrorState(errMsg);
          }

          if (provider.status == AllocationStatus.success && !_initialized) {
            _initialized = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              _initializeControllers(provider.goals);
            });
          }

          final remainingPct = 100.0 - _classSum;
          final isPerfect = _classSum == 100.0;

          return Form(
            key: _formKey,
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Configuração de Estratégia',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Defina suas metas de alocação para subsidiar o cálculo de recomendações inteligentes do Motor de Graham.',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.7),
                        ),
                  ),
                  const SizedBox(height: 24),

                  // Real-time Sum Indicator Banner
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                    decoration: BoxDecoration(
                      color: isPerfect
                          ? Colors.green.withOpacity(0.1)
                          : Colors.amber.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: isPerfect
                            ? Colors.green.withOpacity(0.3)
                            : Colors.amber.withOpacity(0.3),
                      ),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          isPerfect ? Icons.check_circle : Icons.warning_amber_rounded,
                          color: isPerfect ? Colors.green : Colors.amber,
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Text(
                            isPerfect
                                ? 'Estratégia de classes equilibrada! (100% alocados)'
                                : '${_classSum.toStringAsFixed(0)}% alocados em classes. ${remainingPct.abs().toStringAsFixed(0)}% ${remainingPct > 0 ? "restantes" : "excedentes"}.',
                            style: TextStyle(
                              color: isPerfect ? Colors.green[200] : Colors.amber[200],
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 32),

                  // Section 1: Asset Classes
                  Text(
                    'Metas por Classe de Ativos',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Distribua sua alocação total entre as classes de ativos (A soma deve ser exatamente 100%).',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
                        ),
                  ),
                  const SizedBox(height: 16),
                  LayoutBuilder(
                    builder: (context, constraints) {
                      final useGrid = constraints.maxWidth > 600;
                      if (useGrid) {
                        return GridView.count(
                          crossAxisCount: 2,
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          crossAxisSpacing: 16,
                          mainAxisSpacing: 16,
                          childAspectRatio: 3.5,
                          children: _buildClassInputList(),
                        );
                      } else {
                        return Column(
                          children: _buildClassInputList()
                              .map((w) => Padding(
                                    padding: const EdgeInsets.only(bottom: 16.0),
                                    child: w,
                                  ))
                              .toList(),
                        );
                      }
                    },
                  ),
                  const SizedBox(height: 32),

                  // Section 2: Specific Tickers
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          'Metas por Ativo Específico',
                          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                        ),
                      ),
                      TextButton.icon(
                        onPressed: _addTickerField,
                        icon: const Icon(Icons.add),
                        label: const Text('Adicionar Ativo'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Especifique metas para ações ou FIIs individuais (Opcional — metas granulares complementares).',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
                        ),
                  ),
                  const SizedBox(height: 16),

                  if (_tickerFields.isEmpty)
                    Card(
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(24),
                        alignment: Alignment.center,
                        child: Text(
                          'Nenhum ativo específico configurado. Clique em "Adicionar Ativo" para configurar.',
                          style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withOpacity(0.5),
                          ),
                        ),
                      ),
                    )
                  else
                    ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: _tickerFields.length,
                      itemBuilder: (context, index) {
                        final field = _tickerFields[index];
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12.0),
                          child: Row(
                            children: [
                              Expanded(
                                flex: 3,
                                child: TextFormField(
                                  controller: field.tickerController,
                                  textCapitalization: TextCapitalization.characters,
                                  decoration: const InputDecoration(
                                    labelText: 'Ticker (ex: PETR4)',
                                    border: OutlineInputBorder(),
                                  ),
                                  validator: (value) {
                                    if (value == null || value.trim().isEmpty) {
                                      return 'Informe o ticker';
                                    }
                                    return null;
                                  },
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                flex: 2,
                                child: TextFormField(
                                  controller: field.percentageController,
                                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                                  inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'^\d*[\.,]?\d*'))],
                                  decoration: const InputDecoration(
                                    labelText: 'Meta (%)',
                                    border: OutlineInputBorder(),
                                    suffixText: '%',
                                  ),
                                  validator: (value) {
                                    if (value == null || value.trim().isEmpty) {
                                      return 'Informe a meta';
                                    }
                                    final val = double.tryParse(value.replaceAll(',', '.'));
                                    if (val == null || val <= 0 || val > 100) {
                                      return 'Inválido';
                                    }
                                    return null;
                                  },
                                ),
                              ),
                              const SizedBox(width: 8),
                              IconButton(
                                icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                                onPressed: () => _removeTickerField(index),
                              ),
                            ],
                          ),
                        );
                      },
                    ),

                  const SizedBox(height: 48),

                  // Submit Button
                  ElevatedButton(
                    onPressed: (isPerfect && provider.status != AllocationStatus.loading) ? _submit : null,
                    child: provider.status == AllocationStatus.loading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text('Salvar Estratégia'),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  List<Widget> _buildClassInputList() {
    return [
      _buildClassInputField(_acoesController, 'Ações (ex: ACOES)'),
      _buildClassInputField(_fiisController, 'Fundos Imobiliários (ex: FIIS)'),
      _buildClassInputField(_rendaFixaController, 'Renda Fixa (ex: RENDA_FIXA)'),
      _buildClassInputField(_outrosController, 'Outros (ex: OUTROS)'),
    ];
  }

  Widget _buildClassInputField(TextEditingController controller, String label) {
    return TextFormField(
      controller: controller,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'^\d*[\.,]?\d*'))],
      decoration: InputDecoration(
        labelText: label,
        border: const OutlineInputBorder(),
        suffixText: '%',
      ),
      validator: (value) {
        if (value == null || value.trim().isEmpty) {
          return null; // classes can be left empty (equals 0%)
        }
        final val = double.tryParse(value.replaceAll(',', '.'));
        if (val == null || val < 0 || val > 100) {
          return 'Valor inválido (0-100)';
        }
        return null;
      },
    );
  }
}

class _TickerField {
  final TextEditingController tickerController;
  final TextEditingController percentageController;

  _TickerField({
    required this.tickerController,
    required this.percentageController,
  });
}
