package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.repository.IntrinsicValueRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RankCompaniesService {

    private final IntrinsicValueRepository intrinsicValueRepository;

    public List<IntrinsicValue> rankCompanies() {
        log.info("Ranking logic postponed to API issue");
        return List.of();
    }

}
