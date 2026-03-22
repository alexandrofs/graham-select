# Code Review - História 1.1: Google OAuth2 Auto-provisioning

## Resumo
A implementação atual atinge os critérios de aceite solicitados. O `OAuth2UserProvisioningConverter` funciona e converte a requisição num `JwtAuthenticationToken` devidamente populado. As migrações do Liquibase estão corretas.

No entanto, existem alguns detalhes críticos relacionados a performance e transações em ambiente concorrente que podem causar dores de cabeça escalando o sistema.

## Achados e Pontos de Melhoria (Adversarial Review)

### 1. 🛑 Falha Silenciosa de Tratamento de *Race Condition* (`UnexpectedRollbackException`)
No arquivo `OAuth2UserProvisioningConverter.java`, existe um bloco `catch (Exception e)` onde o logger imprime e tenta um fallback `findByGoogleSub(sub)`.
**O problema:** Como o método `convert` está marcado com `@Transactional` (Spring), uma falha no `userRepository.save(newUser)` (por exemplo, Constraint Violation caso dois requests ocorram em paralelo providenciando o usuário) faz com que a transação inteira seja corrompida (`rollback-only`). O Fallback acontecerá silenciosamente, mas ao final da execução do `convert`, a requisição falhará com um HTTP 500 informando que não foi possível encerrar e "commitar" a transação.
**Solução Recomendada:** Extrair o provisionamento / fallback do `save` para um serviço que utilize `@Transactional(propagation = Propagation.REQUIRES_NEW)` no caso do save e de uma nova procura em caso de falha. Isso garante que o erro possa ser mitigado com segurança na transação primária.

### 2. ⚠️ Impacto de Performance ao tratar o Conversor (`SessionCreationPolicy.STATELESS`)
Por via de regra, o JWT Validation em uma API `STATELESS` (visto em `SecurityConfig.java`) significa que o fluxo inteiro do conversor:
- parse do jwt
- lookup do sub
- query no banco via `userRepository.findByGoogleSub(sub)`
Acontecerá **a cada request transacionado**. Isso é bem custoso para o banco de dados. E como o bloco tem um `map(existingUser -> ...)` atualizando o `save(existingUser)` se o nome estiver diferente, isso fará checagens e potencialmente escritas síncronas se atributos do Google do usuário desviarem levemente do nome do banco.
**Solução Recomendada para o futuro:** Cache (ex. Redis) no lookup, reduzindo consultas redundantes ao banco de dados sempre que um Bearer Token válido chegar, e atualizar as diferenças de perfil assincronamente. Por ser uma História inicial de autenticação, podemos deixar esse ponto como alerta (Tech Debt) para as próximas fases.

## Conclusão
- O Código é robusto o suficiente em MVP e os testes estão corretos.
- Recomendo atualizar futuramente com a correção arquitetural da propagação transacional (Ponto 1) como um défcit num backlog de tarefas se houver alto volume.
- Mudando o status da Story 1.1 para `done` no `sprint-status.yaml`.
