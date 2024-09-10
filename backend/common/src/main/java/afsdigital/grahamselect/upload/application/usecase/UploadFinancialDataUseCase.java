package afsdigital.grahamselect.upload.application.usecase;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.upload.application.service.FinancialDataPublisherService;
import afsdigital.grahamselect.upload.domain.repository.FinancialDataReader;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
public class UploadFinancialDataUseCase {

    private final FinancialDataReader financialDataReader;
    private final FinancialDataPublisherService dataPublisherService;

    public void execute(InputStream inputStream) {
        List<FinancialDataEvent> eventList = financialDataReader.read(inputStream);
        eventList.forEach(dataPublisherService::publish);
    }
}
