import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_theme.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final ScrollController _scrollController = ScrollController();

  final heroKey = GlobalKey();
  final rankingKey = GlobalKey();
  final methodologyKey = GlobalKey();
  final resourcesKey = GlobalKey();
  final contactKey = GlobalKey();

  void _scrollTo(GlobalKey key) {
    final context = key.currentContext;
    if (context == null) return;
    Scrollable.ensureVisible(
      context,
      duration: const Duration(milliseconds: 600),
      curve: Curves.easeInOut,
      alignment: 0,
    );
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final isMobile = size.width < 900;
    final double horizontalPadding = isMobile ? 16 : 72;

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: Column(
        children: [
          _LandingMenu(
            isMobile: isMobile,
            onNavigate: (target) {
              switch (target) {
                case LandingSection.hero:
                  _scrollTo(heroKey);
                case LandingSection.ranking:
                  _scrollTo(rankingKey);
                case LandingSection.methodology:
                  _scrollTo(methodologyKey);
                case LandingSection.resources:
                  _scrollTo(resourcesKey);
                case LandingSection.contact:
                  _scrollTo(contactKey);
                case LandingSection.profile:
                  context.push('/profile');
              }
            },
            onCtaPressed: () => context.push('/ranking'),
          ),
          Expanded(
            child: SingleChildScrollView(
              controller: _scrollController,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _HeroSection(key: heroKey, padding: horizontalPadding),
                  _HowItWorksSection(padding: horizontalPadding),
                  _MethodologySection(
                    key: methodologyKey,
                    padding: horizontalPadding,
                  ),
                  _DifferentialsSection(
                    key: resourcesKey,
                    padding: horizontalPadding,
                  ),
                  _RankingPreviewSection(
                    key: rankingKey,
                    padding: horizontalPadding,
                  ),
                  _TestimonialsSection(padding: horizontalPadding),
                  _FaqSection(padding: horizontalPadding),
                  _FinalCtaSection(key: contactKey, padding: horizontalPadding),
                  const _Footer(),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

enum LandingSection { hero, ranking, methodology, resources, contact, profile }

class _LandingMenu extends StatelessWidget {
  const _LandingMenu({
    required this.isMobile,
    required this.onNavigate,
    required this.onCtaPressed,
  });

  final bool isMobile;
  final ValueChanged<LandingSection> onNavigate;
  final VoidCallback onCtaPressed;

  @override
  Widget build(BuildContext context) {
    final menuEntries = [
      ("Início", LandingSection.hero),
      ("Ranking", LandingSection.ranking),
      ("Metodologia", LandingSection.methodology),
      ("Recursos", LandingSection.resources),
      ("Perfil", LandingSection.profile),
    ];

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
      decoration: BoxDecoration(
        color: AppTheme.backgroundColor.withValues(alpha: 0.95),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.3),
            blurRadius: 16,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: SafeArea(
        bottom: false,
        child: Row(
          children: [
            Row(
              children: [
                Container(
                  height: 36,
                  width: 36,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                    color: AppTheme.primaryColor.withValues(alpha: 0.2),
                  ),
                  child: const Icon(
                    Icons.bar_chart_rounded,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(width: 12),
                Text(
                  'Graham Select',
                  style: Theme.of(
                    context,
                  ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
                ),
              ],
            ),
            const Spacer(),
            if (!isMobile) ...[
              for (final entry in menuEntries)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: TextButton(
                    onPressed: () => onNavigate(entry.$2),
                    child: Text(entry.$1),
                  ),
                ),
              const SizedBox(width: 16),
              FilledButton(
                onPressed: onCtaPressed,
                style: FilledButton.styleFrom(
                  backgroundColor: AppTheme.accentColor,
                  foregroundColor: AppTheme.textColor,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 24,
                    vertical: 16,
                  ),
                ),
                child: const Text('Ver Ranking'),
              ),
            ] else
              _MobileMenu(
                menuEntries: menuEntries,
                onNavigate: onNavigate,
                onCtaPressed: onCtaPressed,
              ),
          ],
        ),
      ),
    );
  }
}

class _MobileMenu extends StatelessWidget {
  const _MobileMenu({
    required this.menuEntries,
    required this.onNavigate,
    required this.onCtaPressed,
  });

  final List<(String, LandingSection)> menuEntries;
  final ValueChanged<LandingSection> onNavigate;
  final VoidCallback onCtaPressed;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      icon: const Icon(Icons.menu_rounded, size: 28),
      color: AppTheme.surfaceColor,
      onSelected: (value) {
        if (value == 'cta') {
          onCtaPressed();
        } else {
          final section = menuEntries[int.parse(value)].$2;
          onNavigate(section);
        }
      },
      itemBuilder: (context) => [
        for (var i = 0; i < menuEntries.length; i++)
          PopupMenuItem<String>(value: '$i', child: Text(menuEntries[i].$1)),
        const PopupMenuDivider(),
        const PopupMenuItem<String>(
          value: 'cta',
          child: Text(
            'Ver Ranking',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
      ],
    );
  }
}

class _HeroSection extends StatelessWidget {
  const _HeroSection({super.key, required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 64),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppTheme.backgroundColor, AppTheme.surfaceColor],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            runSpacing: 24,
            spacing: 32,
            alignment: WrapAlignment.spaceBetween,
            children: [
              SizedBox(
                width: 520,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: const Text(
                        'Ranking diário das 20 empresas mais baratas',
                      ),
                    ),
                    const SizedBox(height: 24),
                    Text(
                      'Invista com a metodologia de Benjamin Graham',
                      style: Theme.of(context).textTheme.displayMedium
                          ?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'O Graham Select coleta, filtra e classifica dados financeiros em segundos para entregar um ranking confiável de barganhas na bolsa.',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: AppTheme.textColor.withValues(alpha: 0.8),
                        height: 1.5,
                      ),
                    ),
                    const SizedBox(height: 32),
                    Wrap(
                      spacing: 16,
                      runSpacing: 12,
                      children: [
                        ElevatedButton(
                          onPressed: () => context.push('/ranking'),
                          child: const Text('Ver ranking agora'),
                        ),
                        OutlinedButton(
                          onPressed: () => context.push('/docs'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: AppTheme.textColor,
                            side: const BorderSide(color: AppTheme.textColor),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 32,
                              vertical: 18,
                            ),
                          ),
                          child: const Text('Entender metodologia'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    Row(
                      children: [
                        const Icon(Icons.refresh, color: AppTheme.accentColor),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Atualizado diariamente • Processamento < 5s',
                            style: Theme.of(context).textTheme.bodyMedium,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Container(
                width: 360,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceColor,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(
                    color: Colors.white.withValues(alpha: 0.08),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Highlights do ranking',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 16),
                    const _HighlightRow(
                      title: 'Empresas monitoradas',
                      value: '180+',
                    ),
                    const _HighlightRow(
                      title: 'Atualizações por dia',
                      value: '3',
                    ),
                    const _HighlightRow(
                      title: 'Filtros Graham aplicados',
                      value: '7',
                    ),
                    const Divider(height: 32),
                    Text(
                      '“Precisei só de 2 minutos para chegar à lista final de aportes.”',
                      style: Theme.of(context).textTheme.bodyLarge,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      '— Maria, consultora financeira',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppTheme.textColor.withValues(alpha: 0.7),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _HowItWorksSection extends StatelessWidget {
  const _HowItWorksSection({required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    final steps = [
      (
        'Coletamos dados',
        'Integramos fontes confiáveis e validamos cada indicador antes do processamento.',
      ),
      (
        'Aplicamos filtros',
        'Executamos as 7 regras de valor propostas por Benjamin Graham.',
      ),
      (
        'Entregamos o ranking',
        'Você recebe a lista das 20 ações mais descontadas com métricas explicadas.',
      ),
    ];

    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 72),
      color: AppTheme.surfaceColor.withValues(alpha: 0.4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Como funciona',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 16),
          Text(
            'Automatizamos a parte pesada da análise fundamentalista para você focar na decisão.',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.7),
            ),
          ),
          const SizedBox(height: 32),
          Wrap(
            spacing: 24,
            runSpacing: 24,
            children: [
              for (final step in steps)
                SizedBox(
                  width: 320,
                  child: _InfoCard(title: step.$1, description: step.$2),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MethodologySection extends StatelessWidget {
  const _MethodologySection({super.key, required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    final metrics = [
      (
        'P/L e P/VP',
        'Aplicamos limites clássicos (< 15 e < 1.5) para barrar valuations distorcidos.',
      ),
      (
        'Margem de segurança',
        'Requerimos múltiplos combinados abaixo de 22.5 para configurar desconto real.',
      ),
      (
        'Endividamento',
        'Excluímos empresas com dívida líquida absurda para preservar o perfil conservador.',
      ),
      (
        'Histórico de lucros',
        'Penalizamos quem não apresenta consistência nos últimos anos.',
      ),
    ];

    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 72),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Metodologia Benjamin Graham',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 16),
          Text(
            'Explicamos cada filtro aplicado para que você saiba exatamente por que uma ação entrou no ranking.',
            style: Theme.of(context).textTheme.bodyLarge?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.7),
            ),
          ),
          const SizedBox(height: 32),
          Wrap(
            spacing: 24,
            runSpacing: 24,
            children: [
              for (final metric in metrics)
                SizedBox(
                  width: 320,
                  child: _InfoCard(title: metric.$1, description: metric.$2),
                ),
            ],
          ),
          const SizedBox(height: 32),
          OutlinedButton.icon(
            onPressed: () => context.push('/docs'),
            icon: const Icon(Icons.menu_book_rounded),
            label: const Text('Baixar whitepaper completo'),
          ),
        ],
      ),
    );
  }
}

class _DifferentialsSection extends StatelessWidget {
  const _DifferentialsSection({super.key, required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    final diffs = [
      (
        'Atualização diária',
        'Ranking recalculado 3 vezes ao dia com dados auditáveis.',
      ),
      (
        'Processamento em segundos',
        '< 5 segundos entre o upload e a lista final.',
      ),
      (
        'Integrações prontas',
        'API REST e docs disponíveis para integrações futuras.',
      ),
    ];

    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 64),
      color: AppTheme.surfaceColor.withValues(alpha: 0.3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Por que confiar?',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 24),
          Wrap(
            spacing: 24,
            runSpacing: 24,
            children: [
              for (final diff in diffs)
                SizedBox(
                  width: 320,
                  child: _InfoCard(title: diff.$1, description: diff.$2),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _RankingPreviewSection extends StatelessWidget {
  const _RankingPreviewSection({super.key, required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    final sample = [
      ('Petrorio', 'P/L 8.1', 'P/VP 1.1', 'Margem 42%'),
      ('Banco Inter', 'P/L 12.3', 'P/VP 1.0', 'Margem 28%'),
      ('Ferbasa', 'P/L 6.7', 'P/VP 0.9', 'Margem 31%'),
    ];

    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 72),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Um gostinho do ranking',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: 24),
          Container(
            decoration: BoxDecoration(
              color: AppTheme.surfaceColor,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
            ),
            child: Column(
              children: [
                for (final row in sample)
                  ListTile(
                    title: Text(
                      row.$1,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    subtitle: Text('${row.$2} • ${row.$3}'),
                    trailing: Text(row.$4),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () => context.push('/ranking'),
            icon: const Icon(Icons.table_view_outlined),
            label: const Text('Acessar ranking completo'),
          ),
        ],
      ),
    );
  }
}

class _TestimonialsSection extends StatelessWidget {
  const _TestimonialsSection({required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    final quotes = [
      (
        '“Economizei horas de planilhas e negociei com muito mais convicção.”',
        'João, investidor pessoa física',
      ),
      (
        '“Uso o Graham Select para filtrar ideias antes de escrever meus relatórios.”',
        'Carla, analista CNPI',
      ),
    ];

    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 64),
      color: AppTheme.surfaceColor.withValues(alpha: 0.25),
      child: Wrap(
        spacing: 32,
        runSpacing: 24,
        children: [
          for (final quote in quotes)
            SizedBox(
              width: 360,
              child: _InfoCard(title: quote.$1, description: quote.$2),
            ),
        ],
      ),
    );
  }
}

class _FaqSection extends StatelessWidget {
  const _FaqSection({required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    final faqs = [
      (
        'De onde vêm os dados?',
        'Consumimos bases públicas e privadas com validações internas.',
      ),
      (
        'Com que frequência atualizam o ranking?',
        'Três vezes ao dia ou sempre que novos dados são carregados.',
      ),
      ('Posso exportar?', 'Sim, via dashboard ou API REST autenticada.'),
    ];

    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 64),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('FAQ', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 16),
          for (final faq in faqs)
            ExpansionTile(
              title: Text(
                faq.$1,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              children: [
                Padding(
                  padding: const EdgeInsets.only(
                    left: 16,
                    right: 16,
                    bottom: 16,
                  ),
                  child: Text(faq.$2),
                ),
              ],
            ),
        ],
      ),
    );
  }
}

class _FinalCtaSection extends StatelessWidget {
  const _FinalCtaSection({super.key, required this.padding});

  final double padding;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: padding, vertical: 72),
      decoration: BoxDecoration(
        color: AppTheme.primaryColor.withValues(alpha: 0.15),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Pronto para testar o Graham Select?',
            style: Theme.of(
              context,
            ).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          Text(
            'Acesse o ranking agora ou fale com o time para entender como integrar ao seu fluxo.',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 24),
          Wrap(
            spacing: 16,
            runSpacing: 12,
            children: [
              ElevatedButton(
                onPressed: () => context.push('/ranking'),
                child: const Text('Ver ranking'),
              ),
              OutlinedButton(
                onPressed: () => context.push('/upload'),
                child: const Text('Falar com o time'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Footer extends StatelessWidget {
  const _Footer();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
      color: AppTheme.surfaceColor,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Text(
            '© ${DateTime.now().year} Graham Select. Todos os direitos reservados.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 8),
          Text(
            'contato@grahamselect.com • +55 (11) 99999-9999',
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.7),
            ),
          ),
        ],
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.title, required this.description});

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withValues(alpha: 0.07)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          Text(
            description,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: AppTheme.textColor.withValues(alpha: 0.75),
            ),
          ),
        ],
      ),
    );
  }
}

class _HighlightRow extends StatelessWidget {
  const _HighlightRow({required this.title, required this.value});

  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(title, style: Theme.of(context).textTheme.bodyMedium),
          ),
          const SizedBox(width: 12),
          Text(
            value,
            textAlign: TextAlign.right,
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }
}
