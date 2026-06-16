# Blind Hunter Code Review Prompt

Você é um revisor de código cínico e cético. Analise o diff a seguir buscando problemas de concorrência, qualidade de código, erros lógicos e falhas nas migrações. Identifique potenciais brechas ou bugs.

## Diff a ser Revisado

```diff
diff --git a/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java
index 0e30dd3..8a12b7e 100644
--- a/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java
+++ b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java
@@ -2,6 +2,7 @@ package afsdigital.grahamselect.api.valuation.infrastructure.persistence;
 
 import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
 import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repositories.AllocationGoalJpaRepository;
+import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
 import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
 import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
 import lombok.RequiredArgsConstructor;
@@ -18,10 +19,12 @@ import java.util.UUID;
 public class AllocationGoalJpaAdapter implements AllocationGoalPort {
 
     private final AllocationGoalJpaRepository repository;
+    private final UserRepository userRepository;
 
     @Override
     @Transactional
     public void saveAllocationGoals(String userId, List<AllocationGoalDto> goals) {
+        userRepository.findByGoogleSubForWrite(userId);
         repository.deleteByUserId(userId);
 
         if (goals == null || goals.isEmpty()) {
diff --git a/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java
index 2b4656d..91249a6 100644
--- a/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java
+++ b/backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/jpa/repositories/AllocationGoalJpaRepository.java
@@ -2,6 +2,9 @@ package afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.rep
 
 import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.AllocationGoalEntity;
 import org.springframework.data.jpa.repository.JpaRepository;
+import org.springframework.data.jpa.repository.Modifying;
+import org.springframework.data.jpa.repository.Query;
+import org.springframework.data.repository.query.Param;
 import org.springframework.stereotype.Repository;
 
 import java.util.List;
@@ -10,5 +13,8 @@ import java.util.UUID;
 @Repository
 public interface AllocationGoalJpaRepository extends JpaRepository<AllocationGoalEntity, UUID> {
     List<AllocationGoalEntity> findByUserId(String userId);
-    void deleteByUserId(String userId);
+
+    @Modifying
+    @Query("DELETE FROM AllocationGoalEntity a WHERE a.userId = :userId")
+    void deleteByUserId(@Param("userId") String userId);
 }
diff --git a/backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java b/backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java
index 386adb5..0385822 100644
--- a/backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java
+++ b/backend/common/src/main/java/afsdigital/grahamselect/common/user/infrastructure/persistence/UserRepository.java
@@ -1,7 +1,11 @@
 package afsdigital.grahamselect.common.user.infrastructure.persistence;
 
 import afsdigital.grahamselect.common.user.domain.entities.User;
+import jakarta.persistence.LockModeType;
 import org.springframework.data.jpa.repository.JpaRepository;
+import org.springframework.data.jpa.repository.Lock;
+import org.springframework.data.jpa.repository.Query;
+import org.springframework.data.repository.query.Param;
 import org.springframework.stereotype.Repository;
 
 import java.util.Optional;
@@ -9,4 +13,8 @@ import java.util.Optional;
 @Repository
 public interface UserRepository extends JpaRepository<User, Long> {
     Optional<User> findByGoogleSub(String googleSub);
+
+    @Lock(LockModeType.PESSIMISTIC_WRITE)
+    @Query("SELECT u FROM User u WHERE u.googleSub = :googleSub")
+    Optional<User> findByGoogleSubForWrite(@Param("googleSub") String googleSub);
 }
diff --git a/backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml b/backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml
index 6bdfe97..ce05c68 100644
--- a/backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml
+++ b/backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml
@@ -16,3 +16,10 @@ databaseChangeLog:
                   type: DECIMAL(18,4)
                   constraints:
                     nullable: true
+      rollback:
+        - dropColumn:
+            tableName: graham_recommendations
+            columnName: eps_used
+        - dropColumn:
+            tableName: graham_recommendations
+            columnName: bvps_used
diff --git a/docs/bmad/implementation-artifacts/deferred-work.md b/docs/bmad/implementation-artifacts/deferred-work.md
index afe8980..1dbe8eb 100644
--- a/docs/bmad/implementation-artifacts/deferred-work.md
+++ b/docs/bmad/implementation-artifacts/deferred-work.md
@@ -8,7 +8,7 @@ Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as
 
 ## Deferred from: code review of 4-2-premium-allocation-strategy-config.md (2026-06-08)
 
-- Risco de Race Condition no Fluxo de Delete+Insert do JPA Adapter [backend/api/src/main/java/afsdigital/grahamselect/api/valuation/infrastructure/persistence/AllocationGoalJpaAdapter.java:62] — A abordagem de deletar todas as metas de um usuário e depois inserir as novas pode sofrer com condições de corrida se duas transações concorrentes executarem para o mesmo userId simultaneamente.
+(Nenhum item pendente)
 
 ## Deferred from: code review of 4-3-graham-valuation-engine.md (2026-06-12)
 
@@ -19,7 +19,6 @@ Este arquivo registra itens de dívida técnica ou melhorias adiadas durante as
 ## Deferred from: code review of 4-4-explainable-ai-reasoning-box.md (2026-06-13)
 
 - Risco de Quebra em Tempo de Execução na Desserialização do JSON [frontend/lib/src/features/ranking/data/models/graham_recommendation_model.dart:584] — O cast direto do JSON num? as double? pode falhar se o Java serializar como String.
-- Ausência de Rollback nas Migrações Liquibase [backend/common/src/main/resources/db/changelog/21-add-eps-bvps-to-graham-recommendations.yaml:1] — A migração adicionando colunas não possui instruções de rollback declaradas.
 - Rolagem Conflitante no Bottom Sheet [frontend/lib/src/features/ranking/presentation/pages/graham_recommendations_page.dart:1] — Conflito potencial de gestos entre o SingleChildScrollView interno e a folha deslizável nativa.
 
 ## Deferred from: code review of 5-1-wealth-income-goal-setup.md (2026-06-13)
```

Retorne as suas descobertas/críticas em uma lista em formato Markdown simples.
