package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.tags.application.repository.PortfolioTagPort;
import afsdigital.grahamselect.common.tags.application.service.exceptions.TagNotFoundException;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class GetAllocationGoalsUseCase {

    private final AllocationGoalPort allocationGoalPort;
    private final PortfolioTagPort portfolioTagPort;

    public GetAllocationGoalsUseCase(AllocationGoalPort allocationGoalPort) {
        this(allocationGoalPort, null);
    }

    public GetAllocationGoalsUseCase(AllocationGoalPort allocationGoalPort, PortfolioTagPort portfolioTagPort) {
        this.allocationGoalPort = allocationGoalPort;
        this.portfolioTagPort = portfolioTagPort;
    }

    public List<AllocationGoalDto> execute(String userId) {
        return execute(userId, null);
    }

    public List<AllocationGoalDto> execute(String userId, UUID tagId) {
        log.info("Fetching allocation goals for user {}, tagId={}", sanitize(userId), tagId);
        if (tagId == null) {
            return allocationGoalPort.findByUserId(userId);
        }
        validateTagOwnership(userId, tagId);
        return allocationGoalPort.findByUserId(userId, tagId);
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
