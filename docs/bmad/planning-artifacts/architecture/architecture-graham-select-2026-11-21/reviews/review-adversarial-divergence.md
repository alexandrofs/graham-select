# Review: Adversarial Divergence Analysis

**Alvo:** `ARCHITECTURE-SPINE.md` (Kerno 2.0)  
**Data:** 2026-11-21  
**Avaliador:** Winston (Adversarial System Architect)

## Veredito
**APROVADO (Invariantes blindados contra divergência independente)**

## Cenários de Teste Adversarial

### Cenário 1: Duas equipes implementando o cálculo de Aporte (Mobile vs Backend)
- **Ataque:** Uma equipe implementa o cálculo do GPS no cliente (Dart) e outra no backend (Java), resultando em quantidades inteiras de ações discrepantes por arredondamento ou precedência de desempate.
- **Proteção do Spine:** `AD-1` fixa que o motor do GPS é 100% *stateless* e executado exclusivamente no backend (com o mobile atuando como consumidor do payload). O tie-breaker (Nota -> Margem Graham -> Déficit absoluto -> Ordem alfabética) e a aritmética em `BigDecimal` garantem determinismo absoluto. Zero divergência.

### Cenário 2: Expurgo LGPD incompleto ao criar novas tabelas
- **Ataque:** Um desenvolvedor adiciona a tabela de notas do Cerrado ou o cache de diagnósticos de IA e esquece de incluir no expurgo, deixando dados órfãos após exclusão de conta.
- **Proteção do Spine:** `AD-5`, `AD-BROWNFIELD-3` e `AD-BROWNFIELD-6` impõem a obrigatoriedade de atualização do `UserDataPurgeAdapter` e verificação no teste de integração `AccountDeletionIT`, com quebra automática de CI/CD em caso de omissão.

### Cenário 3: Exposição de valores patrimoniais em ambientes corporativos/públicos
- **Ataque:** O usuário abre o app em transporte público ou o SO captura miniatura no App Switcher revelando o saldo.
- **Proteção do Spine:** `AD-4` define a exigência mandatória de `FLAG_SECURE`/overlay de snapshot no ciclo de vida do Flutter e persistência local do Discrete Mode (`R$ ••••••`).

### Conclusão
O spine fixa com precisão todas as fronteiras críticas. Não foram detectadas brechas onde duas unidades abaixo poderiam divergir de forma incompatível.
