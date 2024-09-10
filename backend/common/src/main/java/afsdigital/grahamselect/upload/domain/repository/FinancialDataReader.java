package afsdigital.grahamselect.upload.domain.repository;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;

import java.io.InputStream;
import java.util.List;

public interface FinancialDataReader {

    List<FinancialDataEvent> read(InputStream inputStream);

}
