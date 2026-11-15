package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessGuard;
import afsdigital.grahamselect.api.upload.infrastructure.parser.SinacorPdfTradeParser;
import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.upload.application.usecase.UploadB3FileUseCase;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload/brokerage-note")
@RequiredArgsConstructor
public class BrokerageNoteUploadController {

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB

    private final UploadB3FileUseCase uploadB3FileUseCase;
    private final SinacorPdfTradeParser sinacorPdfTradeParser;
    private final PremiumFeatureAccessGuard premiumFeatureAccessGuard;
    private final FamilyProfilePort familyProfilePort;

    @PostMapping
    public ResponseEntity<B3UploadAcceptedResponse> uploadBrokerageNote(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "profileId", required = false) String profileId,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        String userId = jwt.getSubject();
        String originalFilename = file != null ? file.getOriginalFilename() : null;
        log.info("Recebido upload de Nota de Corretagem SINACOR: userId={}, profileId={}, fileName={}",
                userId, profileId, originalFilename);

        if (file == null || file.isEmpty() || originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            log.error("Erro na validação da nota de corretagem: Formato não suportado ou vazio. Arquivo: {}", originalFilename);
            throw new B3UploadValidationException("Apenas arquivos .pdf de notas de corretagem (padrão SINACOR) são suportados neste endpoint.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("Erro na validação da nota de corretagem: Tamanho excede 15MB: {} bytes. Arquivo: {}", file.getSize(), originalFilename);
            throw new B3UploadValidationException("O arquivo excede o limite máximo permitido de 15MB.");
        }

        String safeFileName = Paths.get(originalFilename).getFileName().toString();
        String normalizedProfileId = resolveAndValidateProfileId(profileId, userId);

        byte[] fileBytes = file.getBytes();
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            if (!sinacorPdfTradeParser.isSinacorNote(is)) {
                log.error("Erro na validação da nota de corretagem: Marcadores SINACOR não encontrados no PDF: {}", safeFileName);
                throw new B3UploadValidationException("Arquivo PDF inválido: Não foi possível identificar o padrão de Nota de Corretagem SINACOR.");
            }
        }

        FileUploadedEvent event;
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            event = uploadB3FileUseCase.execute(is, safeFileName, userId, normalizedProfileId);
        }

        log.info("Nota de corretagem aceita para processamento: userId={}, correlationId={}, fileName={}",
                userId, event.correlationId(), safeFileName);

        return ResponseEntity.accepted().body(new B3UploadAcceptedResponse(
                "Nota de corretagem enviada com sucesso para processamento assíncrono.",
                event.correlationId(),
                "RECEIVED"
        ));
    }

    private String resolveAndValidateProfileId(String profileId, String userId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        premiumFeatureAccessGuard.canAccessPremiumFeatures();
        UUID parsedId;
        try {
            parsedId = UUID.fromString(profileId.trim());
        } catch (IllegalArgumentException e) {
            log.error("Formato de UUID inválido para profileId no upload de nota: userId={}, profileId={}", userId, profileId, e);
            throw new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
        }
        familyProfilePort.findActiveByIdAndMasterUserId(parsedId, userId)
                .orElseThrow(() -> {
                    log.error("Perfil familiar não encontrado ou não pertence ao usuário: userId={}, profileId={}", userId, parsedId);
                    return new FamilyProfileNotFoundException("Perfil familiar não encontrado.");
                });
        return parsedId.toString();
    }
}
