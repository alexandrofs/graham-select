package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceConnectionDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListUserOpenFinanceConnectionsUseCase {

    private final OpenFinanceConnectionPort connectionPort;

    public List<OpenFinanceConnectionDTO> execute(String userId) {
        log.info("Listando conexões Open Finance para o usuário: userId={}", userId);

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId é obrigatório.");
        }

        return connectionPort.findByUserId(userId).stream()
                .map(OpenFinanceConnectionDTO::fromDomain)
                .toList();
    }
}
