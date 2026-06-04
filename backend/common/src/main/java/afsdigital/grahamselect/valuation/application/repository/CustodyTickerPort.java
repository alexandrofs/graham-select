package afsdigital.grahamselect.valuation.application.repository;

import java.util.List;

public interface CustodyTickerPort {
    List<String> findDistinctActiveTickers();
}
