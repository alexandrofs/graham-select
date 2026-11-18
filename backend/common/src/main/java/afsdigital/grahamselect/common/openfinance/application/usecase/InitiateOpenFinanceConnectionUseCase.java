package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;
import afsdigital.grahamselect.common.openfinance.application.dto.ConsentUrlResponseDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.InitiateConnectionRequestDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class InitiateOpenFinanceConnectionUseCase {

    private final OpenFinanceConnectionPort connectionPort;
    private final OpenFinanceConnectorPort connectorPort;
    private final FamilyProfilePort familyProfilePort;

    private static final Map<String, String> INSTITUTION_NAMES = Map.of(
            "XP_INVESTIMENTOS", "XP Investimentos",
            "BTG_PACTUAL", "BTG Pactual",
            "NUBANK", "Nubank / NuInvest",
            "ITAU", "Itaú Corretora",
            "INTER", "Banco Inter"
    );

    private static final Map<String, String> INSTITUTION_LOGOS = Map.of(
            "XP_INVESTIMENTOS", "https://assets.grahamselect.com/institutions/xp.png",
            "BTG_PACTUAL", "https://assets.grahamselect.com/institutions/btg.png",
            "NUBANK", "https://assets.grahamselect.com/institutions/nubank.png",
            "ITAU", "https://assets.grahamselect.com/institutions/itau.png",
            "INTER", "https://assets.grahamselect.com/institutions/inter.png"
    );

    public ConsentUrlResponseDTO execute(String userId, InitiateConnectionRequestDTO request, String redirectUri) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId é obrigatório.");
        }
        if (request == null || request.institutionId() == null || request.institutionId().isBlank()) {
            throw new IllegalArgumentException("institutionId é obrigatório.");
        }

        log.info("Iniciando conexão Open Finance: userId={}, institutionId={}, profileId={}",
                userId, request.institutionId(), request.profileId());

        if (request.profileId() != null) {
            Optional<FamilyProfile> profile = familyProfilePort.findActiveByIdAndMasterUserId(request.profileId(), userId);
            if (profile.isEmpty() || !profile.get().isActive()) {
                log.error("Tentativa de conectar Open Finance a perfil familiar inexistente ou inativo: userId={}, profileId={}",
                        userId, request.profileId());
                throw new IllegalArgumentException("Perfil familiar não encontrado ou inativo.");
            }
        }

        String institutionId = request.institutionId().trim();
        String institutionName = INSTITUTION_NAMES.getOrDefault(institutionId, institutionId);
        String institutionLogo = INSTITUTION_LOGOS.getOrDefault(institutionId, "");

        Optional<OpenFinanceConnection> existingOpt = connectionPort
                .findByUserIdAndInstitutionIdAndProfileId(userId, institutionId, request.profileId());

        OpenFinanceConnection connection;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (existingOpt.isPresent()) {
            connection = existingOpt.get();
            log.info("Reaproveitando conexão existente Open Finance: id={}, userId={}, institutionId={}",
                    connection.getId(), userId, institutionId);
            connection.setStatus(OpenFinanceConnectionStatus.PENDING_CONSENT);
            connection.setErrorMessage(null);
            connection.setUpdatedAt(now);
        } else {
            connection = OpenFinanceConnection.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .profileId(request.profileId())
                    .institutionId(institutionId)
                    .institutionName(institutionName)
                    .institutionLogoUrl(institutionLogo)
                    .status(OpenFinanceConnectionStatus.PENDING_CONSENT)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }

        String state = connection.getId() + "_" + UUID.randomUUID().toString().replace("-", "");
        connection.setConsentId(state);

        OpenFinanceConnection saved = connectionPort.save(connection);

        String consentUrl = connectorPort.generateConsentUrl(institutionId, state, redirectUri);
        log.info("URL de consentimento Open Finance gerada com sucesso: connectionId={}, state={}", saved.getId(), state);

        return new ConsentUrlResponseDTO(saved.getId(), consentUrl, state);
    }
}
