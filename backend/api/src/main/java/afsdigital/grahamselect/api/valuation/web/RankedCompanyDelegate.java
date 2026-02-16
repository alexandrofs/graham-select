package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.RankedCompaniesApiDelegate;
import afsdigital.grahamselect.model.RankedCompany;
import afsdigital.grahamselect.valuation.application.service.RankCompaniesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RankedCompanyDelegate implements RankedCompaniesApiDelegate {

    private final RankCompaniesService rankCompaniesService;

    @Override
    public ResponseEntity<List<RankedCompany>> rankedCompaniesGet() {
        List<RankedCompany> response = rankCompaniesService.rankCompanies().stream()
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
