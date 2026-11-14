package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.AllocationGoalValidationException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class SaveAllocationGoalsUseCase {

    private final AllocationGoalPort allocationGoalPort;
    private final PortfolioTagPort portfolioTagPort;

    public SaveAllocationGoalsUseCase(AllocationGoalPort allocationGoalPort) {
        this(allocationGoalPort, null);
    }

    public SaveAllocationGoalsUseCase(AllocationGoalPort allocationGoalPort, PortfolioTagPort portfolioTagPort) {
        this.allocationGoalPort = allocationGoalPort;
        this.portfolioTagPort = portfolioTagPort;
    }

    public void execute(String userId, List<AllocationGoalDto> goals) {
        execute(userId, null, goals);
    }

    public void execute(String userId, UUID tagId, List<AllocationGoalDto> goals) {
        if (tagId != null) {
            validateTagOwnership(userId, tagId);
        }

        if (goals == null) {
            AllocationGoalValidationException ex = new AllocationGoalValidationException("A lista de metas não pode ser nula");
            log.error("Allocation goal validation failed for user {}, tagId={}: goals list is null", sanitize(userId), tagId, ex);
            throw ex;
        }

        log.info("Saving {} allocation goals for user {}, tagId={}", goals.size(), sanitize(userId), tagId);

        Set<String> uniqueKeys = new HashSet<>();
        boolean hasAssetClassGoals = false;
        BigDecimal classSum = BigDecimal.ZERO;

        for (AllocationGoalDto dto : goals) {
            if (dto == null) {
                log.error("Allocation goal validation failed for user {}: found null goal item", userId);
                throw new AllocationGoalValidationException("A meta de alocação não pode ser nula");
            }
            if (dto.goalType() == null || dto.goalType().trim().isEmpty()) {
                log.error("Allocation goal validation failed for user {}: goalType is null or empty", userId);
                throw new AllocationGoalValidationException("O tipo de meta (goalType) é obrigatório");
            }
            if (dto.targetKey() == null || dto.targetKey().trim().isEmpty()) {
                log.error("Allocation goal validation failed for user {}: targetKey is null or empty", userId);
                throw new AllocationGoalValidationException("A chave do ativo/classe (targetKey) é obrigatória");
            }
            if (dto.targetPercentage() == null) {
                log.error("Allocation goal validation failed for user {}: targetPercentage is null for key {}", userId, dto.targetKey());
                throw new AllocationGoalValidationException("O percentual de alocação (targetPercentage) é obrigatório para " + dto.targetKey());
            }
            if (dto.targetPercentage().compareTo(BigDecimal.ZERO) < 0) {
                log.error("Allocation goal validation failed for user {}: targetPercentage {} is negative for key {}", userId, dto.targetPercentage(), dto.targetKey());
                throw new AllocationGoalValidationException("O percentual de alocação não pode ser negativo para " + dto.targetKey());
            }

            String uniqueKey = dto.goalType() + "#" + dto.targetKey().trim().toUpperCase();
            if (!uniqueKeys.add(uniqueKey)) {
                log.error("Allocation goal validation failed for user {}: duplicate goal key found: {}", userId, uniqueKey);
                throw new AllocationGoalValidationException("Metas duplicadas não são permitidas para " + dto.targetKey());
            }

            if ("ASSET_CLASS".equals(dto.goalType())) {
                hasAssetClassGoals = true;
                classSum = classSum.add(dto.targetPercentage());
            }
        }

        if (!hasAssetClassGoals) {
            log.error("Allocation goal validation failed for user {}: no asset class goals provided", userId);
            throw new AllocationGoalValidationException("A lista de metas deve conter classes de ativos");
        }

        BigDecimal expected = new BigDecimal("100.00");
        if (classSum.compareTo(expected) != 0 && classSum.compareTo(new BigDecimal("100")) != 0) {
            AllocationGoalValidationException ex = new AllocationGoalValidationException(
                    "A soma das metas de classe deve ser exatamente 100%. Soma atual: " + classSum + "%");
            log.error("Allocation goal validation failed for user {}, tagId={}: class sum is {}% instead of 100%",
                    sanitize(userId), tagId, classSum, ex);
            throw ex;
        }

        if (tagId == null) {
            allocationGoalPort.saveAllocationGoals(userId, goals);
        } else {
            allocationGoalPort.saveAllocationGoals(userId, tagId, goals);
        }
    }

    private void validateTagOwnership(String userId, UUID tagId) {
        if (portfolioTagPort == null) {
            IllegalStateException ex = new IllegalStateException("PortfolioTagPort não configurado para escopo de tag.");
            log.error("Falha ao validar propriedade da tag {} para usuário {}", tagId, sanitize(userId), ex);
            throw ex;
        }
        if (portfolioTagPort.findByIdAndUserId(tagId, userId).isEmpty()) {
            TagNotFoundException ex = new TagNotFoundException("Tag não encontrada.");
            log.error("Tag não encontrada ou não pertence ao usuário {}. tagId={}", sanitize(userId), tagId, ex);
            throw ex;
        }
    }

    private static String sanitize(String value) {
        return value == null ? "null" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
