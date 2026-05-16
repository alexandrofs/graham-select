import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../providers/profile_provider.dart';

class InvestorProfilePage extends StatefulWidget {
  const InvestorProfilePage({super.key});

  @override
  State<InvestorProfilePage> createState() => _InvestorProfilePageState();
}

class _InvestorProfilePageState extends State<InvestorProfilePage> {
  String? _selectedProfile;

  final List<Map<String, String>> _profiles = [
    {
      'id': 'CONSERVATIVE',
      'title': 'Conservador',
      'description': 'Prioriza a preservação do capital e tem baixa tolerância a riscos.',
    },
    {
      'id': 'MODERATE',
      'title': 'Moderado',
      'description': 'Busca um equilíbrio entre segurança e rentabilidade, aceitando riscos moderados.',
    },
    {
      'id': 'AGGRESSIVE',
      'title': 'Arrojado',
      'description': 'Busca maximizar a rentabilidade a longo prazo e tem alta tolerância a volatilidade.',
    },
  ];

  Future<void> _saveProfile() async {
    if (_selectedProfile == null) return;

    try {
      await context.read<ProfileProvider>().updateInvestorProfile(_selectedProfile!);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Perfil atualizado com sucesso!')),
        );
        context.go('/');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro ao atualizar perfil: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Perfil do Investidor'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Qual seu perfil de investidor?',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            const Text(
              'Isso nos ajuda a personalizar as recomendações de Benjamin Graham para você.',
              style: TextStyle(fontSize: 16, color: Colors.grey),
            ),
            const SizedBox(height: 32),
            Expanded(
              child: ListView.separated(
                itemCount: _profiles.length,
                separatorBuilder: (context, index) => const SizedBox(height: 16),
                itemBuilder: (context, index) {
                  final profile = _profiles[index];
                  final isSelected = _selectedProfile == profile['id'];

                  return InkWell(
                    onTap: () {
                      setState(() {
                        _selectedProfile = profile['id'];
                      });
                    },
                    child: Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(
                        color: isSelected ? Colors.blue.withValues(alpha: 0.1) : Colors.transparent,
                        border: Border.all(
                          color: isSelected ? Colors.blue : Colors.grey.shade800,
                          width: 2,
                        ),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                profile['title']!,
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: isSelected ? Colors.blue : Colors.white,
                                ),
                              ),
                              if (isSelected)
                                const Icon(Icons.check_circle, color: Colors.blue),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(
                            profile['description']!,
                            style: TextStyle(color: Colors.grey.shade400),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 56,
              child: ElevatedButton(
                onPressed: _selectedProfile != null ? _saveProfile : null,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: const Text('Confirmar Perfil', style: TextStyle(fontSize: 18)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
