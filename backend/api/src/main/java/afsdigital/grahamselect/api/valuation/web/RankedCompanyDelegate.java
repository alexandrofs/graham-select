package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.RankedCompaniesApiDelegate;
import afsdigital.grahamselect.model.RankedCompany;
import afsdigital.grahamselect.valuation.application.usecase.GetRankedCompaniesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankedCompanyDelegate implements RankedCompaniesApiDelegate {

    private final GetRankedCompaniesUseCase getRankedCompaniesUseCase;

    @Override
    public ResponseEntity<List<RankedCompany>> rankedCompaniesGet() {
        log.info("Handling request to get ranked companies");
        List<RankedCompany> response = getRankedCompaniesUseCase.execute().stream()
                .map(domain -> {
                    RankedCompany apiModel = new RankedCompany();
                    apiModel.setSymbol(domain.getSymbol());
                    apiModel.setName(domain.getName());
                    apiModel.setIntrinsicValue(domain.getIntrinsicValue().doubleValue());
                    apiModel.setCurrentPrice(domain.getCurrentPrice().doubleValue());
                    apiModel.setMarginOfSafety(domain.getMarginOfSafety().doubleValue());
                    return apiModel;
                })
                .toList();
        return ResponseEntity.ok(response);
    }
}
