import 'package:flutter/material.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/open_finance_institution.dart';

class OpenFinanceInstitutionDialog extends StatefulWidget {
  final List<OpenFinanceInstitution> institutions;
  final ValueChanged<OpenFinanceInstitution> onSelected;

  const OpenFinanceInstitutionDialog({
    super.key,
    required this.institutions,
    required this.onSelected,
  });

  @override
  State<OpenFinanceInstitutionDialog> createState() => _OpenFinanceInstitutionDialogState();
}

class _OpenFinanceInstitutionDialogState extends State<OpenFinanceInstitutionDialog> {
  String _searchQuery = '';

  @override
  Widget build(BuildContext context) {
    final filtered = widget.institutions.where((inst) {
      final nameMatches = inst.name.toLowerCase().contains(_searchQuery.toLowerCase());
      final descMatches = inst.description.toLowerCase().contains(_searchQuery.toLowerCase());
      return nameMatches || descMatches;
    }).toList();

    return Dialog(
      key: const Key('open_finance_institution_dialog'),
      backgroundColor: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 480, maxHeight: 600),
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      'Selecionar Instituição',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white70),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'Escolha sua corretora ou banco para autorizar a sincronização contínua de investimentos.',
                style: TextStyle(
                  color: AppTheme.textColor.withValues(alpha: 0.7),
                  fontSize: 13,
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                decoration: InputDecoration(
                  hintText: 'Buscar banco ou corretora...',
                  prefixIcon: const Icon(Icons.search, color: Colors.white60),
                  filled: true,
                  fillColor: AppTheme.backgroundColor,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: BorderSide.none,
                  ),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                ),
                style: const TextStyle(color: Colors.white),
                onChanged: (val) => setState(() => _searchQuery = val),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: filtered.isEmpty
                    ? Center(
                        child: Text(
                          'Nenhuma instituição encontrada.',
                          style: TextStyle(
                            color: AppTheme.textColor.withValues(alpha: 0.5),
                          ),
                        ),
                      )
                    : ListView.separated(
                        itemCount: filtered.length,
                        separatorBuilder: (_, _) => const Divider(color: Colors.white12, height: 1),
                        itemBuilder: (context, index) {
                          final inst = filtered[index];
                          return ListTile(
                            contentPadding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                            leading: CircleAvatar(
                              backgroundColor: AppTheme.backgroundColor,
                              radius: 20,
                              child: inst.logoUrl.isNotEmpty
                                  ? ClipOval(
                                      child: Image.network(
                                        inst.logoUrl,
                                        width: 40,
                                        height: 40,
                                        fit: BoxFit.cover,
                                        errorBuilder: (context, error, stackTrace) => const Icon(
                                          Icons.account_balance,
                                          color: AppTheme.accentColor,
                                          size: 20,
                                        ),
                                      ),
                                    )
                                  : const Icon(
                                      Icons.account_balance,
                                      color: AppTheme.accentColor,
                                      size: 20,
                                    ),
                            ),
                            title: Text(
                              inst.name,
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                            subtitle: Text(
                              inst.description,
                              style: TextStyle(
                                fontSize: 12,
                                color: AppTheme.textColor.withValues(alpha: 0.6),
                              ),
                            ),
                            trailing: const Icon(Icons.chevron_right, color: Colors.white54),
                            onTap: () {
                              Navigator.of(context).pop();
                              widget.onSelected(inst);
                            },
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
