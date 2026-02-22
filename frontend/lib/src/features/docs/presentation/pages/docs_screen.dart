import 'package:flutter/material.dart';
import '../../../../core/theme/app_theme.dart';
import 'package:go_router/go_router.dart';

class DocsScreen extends StatefulWidget {
  const DocsScreen({super.key});

  @override
  State<DocsScreen> createState() => _DocsScreenState();
}

class _DocsScreenState extends State<DocsScreen> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Documentação'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
      ),
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Sidebar para navegação (útil em Desktop/Web)
          _buildSidebar(context),
          // Área principal de conteúdo
          Expanded(
            child: _buildContentArea(context),
          ),
        ],
      ),
    );
  }

  Widget _buildSidebar(BuildContext context) {
    return Container(
      width: 250,
      color: AppTheme.surfaceColor,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: 16),
        children: [
          _buildSidebarItem(context, 'Introdução', Icons.info_outline, 0),
          _buildSidebarItem(context, 'Como fazer Upload', Icons.upload_file, 1),
          _buildSidebarItem(context, 'Ranking de Empresas', Icons.leaderboard, 2),
        ],
      ),
    );
  }

  Widget _buildSidebarItem(
    BuildContext context, 
    String title, 
    IconData icon, 
    int index
  ) {
    final isSelected = _selectedIndex == index;
    return ListTile(
      leading: Icon(
        icon,
        color: isSelected ? AppTheme.primaryColor : AppTheme.textColor.withValues(alpha: 0.7),
      ),
      title: Text(
        title,
        style: TextStyle(
          color: isSelected ? AppTheme.primaryColor : AppTheme.textColor,
          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        ),
      ),
      selected: isSelected,
      selectedTileColor: AppTheme.primaryColor.withValues(alpha: 0.1),
      onTap: () {
        setState(() {
          _selectedIndex = index;
        });
      },
    );
  }

  Widget _buildContentArea(BuildContext context) {
    switch (_selectedIndex) {
      case 0:
        return _buildIntroducao(context);
      case 1:
        return _buildUpload(context);
      case 2:
        return _buildRanking(context);
      default:
        return _buildIntroducao(context);
    }
  }

  Widget _buildIntroducao(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(32.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Introdução ao Sistema',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'Bem-vindo ao Graham Select. Este sistema foi desenhado para investidores e analistas que buscam utilizar a metodologia de Valor de Benjamin Graham. Aqui você poderá importar, processar e analisar dados financeiros reais.',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.8),
              height: 1.5,
            ),
          ),
          const SizedBox(height: 32),
          _buildSectionCard(
            context,
            'O que você encontrará aqui?',
            'Neste manual, explicamos os processos fundamentais da plataforma: como inserir dados na ferramenta (Upload) e como interpretar os resultados com base no modelo de Graham (Ranking). Selecione um tópico ao lado para explorar.',
            Icons.lightbulb_outline,
          ),
        ],
      ),
    );
  }
  
  Widget _buildUpload(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(32.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Como fazer Upload',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'O sistema depende de dados financeiros (como cotação, P/L, ROE) para aplicar os critérios de Graham. O Upload é a porta de entrada para esses dados.',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.8),
              height: 1.5,
            ),
          ),
          const SizedBox(height: 32),
          _buildStepLine(context, '1', 'Acesse a aba "Fazer Upload de Dados" a partir da Tela Inicial.'),
          _buildStepLine(context, '2', 'Selecione um arquivo CSV com os indicadores financeiros. O sistema valida os campos automaticamente.'),
          _buildStepLine(context, '3', 'Aguarde o processamento que analisa e armazena as informações.'),
          const SizedBox(height: 32),
          _buildSectionCard(
            context,
            'Atenção aos Formatos',
            'No momento, aceitamos apenas arquivos CSV formatados com ponto e vírgula (;). O arquivo extraído do StatusInvest (Busca Avançada) é o formato padrão suportado.',
            Icons.warning_amber_rounded,
          ),
        ],
      ),
    );
  }

  Widget _buildRanking(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(32.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Ranking de Empresas',
            style: Theme.of(context).textTheme.headlineMedium?.copyWith(
              color: Colors.white,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'Após importar os dados com sucesso, a aplicação calcula o "Valor Intrínseco" e a "Margem de Segurança" (baseado na Fórmula de Graham) para ordenar as melhores oportunidades.',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.8),
              height: 1.5,
            ),
          ),
          const SizedBox(height: 32),
          _buildStepLine(context, '1', 'Acesse "Ver Ranking de Empresas". O sistema já exibirá as empresas ordenadas pelas mais descontadas.'),
          _buildStepLine(context, '2', 'Verifique a métrica "Margem de Segurança": quanto maior, mais atrativa de acordo com a teoria de Graham.'),
          _buildStepLine(context, '3', 'Avalie também os dados extras, como Preço Justo comparado à Cotação atual.'),
          const SizedBox(height: 32),
          _buildSectionCard(
            context,
            'Nota sobre Investimentos',
            'Lembre-se: os resultados do sistema dão suporte à sua tomada de decisão quantitativa, mas não dispensam uma análise qualitativa profunda de cada negócio.',
            Icons.info_outline,
          ),
        ],
      ),
    );
  }

  Widget _buildStepLine(BuildContext context, String step, String description) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: AppTheme.primaryColor,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                step,
                style: const TextStyle(
                  color: Colors.black,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              description,
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color: Colors.white,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionCard(BuildContext context, String title, String content, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: AppTheme.primaryColor.withValues(alpha: 0.2),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: AppTheme.primaryColor),
              const SizedBox(width: 12),
              Text(
                title,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Text(
            content,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.7),
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}
