package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.api.UploadFinancialDataApiDelegate;
import afsdigital.grahamselect.model.UploadFinancialDataPost200Response;
import afsdigital.grahamselect.upload.application.usecase.UploadFinancialDataUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@Service
@Slf4j
public class UploadServiceDelegate implements UploadFinancialDataApiDelegate {

    private final UploadFinancialDataUseCase uploadFinancialDataUseCase;

    @Override
    public ResponseEntity<UploadFinancialDataPost200Response> uploadFinancialDataPost(MultipartFile file) {
        log.info("Receiving file");
        try {
            uploadFinancialDataUseCase.execute(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }
}
