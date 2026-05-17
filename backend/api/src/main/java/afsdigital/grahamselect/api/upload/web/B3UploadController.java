package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.common.upload.application.repository.B3ImportStatusPort;
import afsdigital.grahamselect.common.upload.application.usecase.UploadB3FileUseCase;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/upload/b3")
@RequiredArgsConstructor
public class B3UploadController {

    private final UploadB3FileUseCase uploadB3FileUseCase;
    private final B3ImportStatusPort importStatusPort;

    @PostMapping
    public ResponseEntity<B3UploadAcceptedResponse> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new B3UploadValidationException("Apenas arquivos .xlsx (Excel) da B3 são suportados no momento.");
        }

        byte[] fileBytes = file.getBytes();
        validateHeaders(fileBytes);

        FileUploadedEvent event;
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes)) {
            event = uploadB3FileUseCase.execute(is, file.getOriginalFilename(), jwt.getSubject());
        }

        return ResponseEntity.accepted().body(new B3UploadAcceptedResponse(
                "Arquivo B3 enviado com sucesso para processamento assíncrono.",
                event.correlationId(),
                "RECEIVED"
        ));
    }

    @GetMapping("/status/{correlationId}")
    public ResponseEntity<B3ImportStatusEnvelope> getUploadStatus(
            @PathVariable String correlationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return importStatusPort.findByCorrelationIdAndUserId(correlationId, jwt.getSubject())
                .map(status -> ResponseEntity.ok(new B3ImportStatusEnvelope(new B3ImportStatusView(
                        status.correlationId(),
                        status.fileName(),
                        status.status().name(),
                        status.processedRows(),
                        status.successfulRows(),
                        status.failedRows(),
                        status.duplicatedRows(),
                        status.message()
                ))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void validateHeaders(byte[] fileBytes) throws IOException {
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             ReadableWorkbook wb = new ReadableWorkbook(is)) {
            Sheet sheet = wb.getFirstSheet();
            try (Stream<Row> rows = sheet.openStream()) {
                Optional<Row> firstRow = rows.findFirst();
                if (firstRow.isEmpty()) {
                    throw new B3UploadValidationException("Arquivo vazio");
                }
                List<String> headers = firstRow.get().stream()
                        .map(Cell::asString)
                        .filter(java.util.Objects::nonNull)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toList();

                List<String> requiredHeaders = List.of("ticker", "data", "quantidade", "preço");
                if (!headers.containsAll(requiredHeaders)) {
                    throw new B3UploadValidationException("Arquivo inválido: Colunas obrigatórias não encontradas.");
                }
            }
        }
    }
}
