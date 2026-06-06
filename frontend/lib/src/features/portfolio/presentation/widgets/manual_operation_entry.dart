import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../providers/portfolio_provider.dart';

class ManualOperationEntry extends StatefulWidget {
  const ManualOperationEntry({super.key});

  @override
  State<ManualOperationEntry> createState() => _ManualOperationEntryState();
}

class _ManualOperationEntryState extends State<ManualOperationEntry> {
  final _formKey = GlobalKey<FormState>();
  final _tickerController = TextEditingController();
  final _quantityController = TextEditingController();
  final _priceController = TextEditingController();
  final _brokerController = TextEditingController();
  DateTime _selectedDate = DateTime.now();
  String _type = 'COMPRA';

  @override
  void dispose() {
    _tickerController.dispose();
    _quantityController.dispose();
    _priceController.dispose();
    _brokerController.dispose();
    super.dispose();
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2000),
      lastDate: DateTime.now(),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
      });
    }
  }

  void _submit() {
    if (_formKey.currentState!.validate()) {
      context.read<PortfolioProvider>().createManualTrade(
        ticker: _tickerController.text.toUpperCase(),
        side: _type,
        tradeDate: _selectedDate,
        quantity: double.parse(_quantityController.text.replaceAll(',', '.')),
        price: double.parse(_priceController.text.replaceAll(',', '.')),
        broker: _brokerController.text,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<PortfolioProvider>(
      builder: (context, provider, child) {
        if (provider.tradeStatus == TradeStatus.success) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Operação salva com sucesso!'), backgroundColor: Colors.green),
            );
            provider.resetTradeStatus();
            Navigator.of(context).pop();
          });
        }

        if (provider.tradeStatus == TradeStatus.error) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(provider.tradeError ?? 'Erro ao salvar'), backgroundColor: Colors.red),
            );
            provider.resetTradeStatus();
          });
        }

        return Container(
          padding: const EdgeInsets.only(left: 24, right: 24, top: 24, bottom: 40),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surface,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
          ),
          child: Form(
            key: _formKey,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Nova Operação',
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      IconButton(
                        onPressed: () => Navigator.of(context).pop(),
                        icon: const Icon(Icons.close),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  
                  // Tipo de Operação Toggle
                  Center(
                    child: SegmentedButton<String>(
                      segments: const [
                        ButtonSegment(value: 'COMPRA', label: Text('Compra'), icon: Icon(Icons.add_circle_outline)),
                        ButtonSegment(value: 'VENDA', label: Text('Venda'), icon: Icon(Icons.remove_circle_outline)),
                      ],
                      selected: {_type},
                      onSelectionChanged: (value) => setState(() => _type = value.first),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Ticker Autocomplete
                  Autocomplete<String>(
                    textEditingController: _tickerController,
                    optionsBuilder: (TextEditingValue textEditingValue) {
                      if (textEditingValue.text == '') {
                        return const Iterable<String>.empty();
                      }
                      // Mock tickers
                      final options = ['PETR4', 'VALE3', 'ITUB4', 'BBAS3', 'BBDC4', 'WEGE3', 'ABEV3', 'MGLU3'];
                      return options.where((String option) {
                        return option.contains(textEditingValue.text.toUpperCase());
                      });
                    },
                    onSelected: (String selection) {
                      _tickerController.text = selection;
                    },
                    fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                      return TextFormField(
                        controller: controller,
                        focusNode: focusNode,
                        decoration: const InputDecoration(
                          labelText: 'Ticker (ex: PETR4)',
                          border: OutlineInputBorder(),
                          prefixIcon: Icon(Icons.search),
                        ),
                        validator: (value) => value == null || value.isEmpty ? 'Informe o ticker' : null,
                      );
                    },
                  ),
                  const SizedBox(height: 16),

                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _quantityController,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(
                            labelText: 'Quantidade',
                            border: OutlineInputBorder(),
                          ),
                          inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
                          validator: (value) => value == null || value.isEmpty ? 'Obrigatório' : null,
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: TextFormField(
                          controller: _priceController,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          decoration: const InputDecoration(
                            labelText: 'Preço Médio (R\$)',
                            border: OutlineInputBorder(),
                          ),
                          inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))],
                          validator: (value) => value == null || value.isEmpty ? 'Obrigatório' : null,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  TextFormField(
                    controller: _brokerController,
                    decoration: const InputDecoration(
                      labelText: 'Corretora',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.account_balance),
                    ),
                    validator: (value) => value == null || value.isEmpty ? 'Informe a corretora' : null,
                  ),
                  const SizedBox(height: 16),

                  ListTile(
                    title: const Text('Data da Operação'),
                    subtitle: Text('${_selectedDate.day.toString().padLeft(2, '0')}/${_selectedDate.month.toString().padLeft(2, '0')}/${_selectedDate.year}'),
                    leading: const Icon(Icons.calendar_today),
                    onTap: () => _selectDate(context),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                      side: BorderSide(color: Theme.of(context).dividerColor),
                    ),
                  ),
                  const SizedBox(height: 32),

                  ElevatedButton(
                    onPressed: provider.tradeStatus == TradeStatus.loading ? null : _submit,
                    child: provider.tradeStatus == TradeStatus.loading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : const Text('Salvar Operação'),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
