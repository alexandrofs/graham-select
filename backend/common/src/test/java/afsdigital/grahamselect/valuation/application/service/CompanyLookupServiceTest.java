package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.common.domain.entities.Company;
import afsdigital.grahamselect.valuation.application.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyLookupServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyLookupService companyLookupService;

    @Test
    public void shouldReturnExistingCompanyWhenFoundByTicker() {

        Company existingCompany = new Company("AAPL");

        when(companyRepository.findByTicker("AAPL")).thenReturn(existingCompany);

        Company result = companyLookupService.findOrCreateCompany("AAPL");

        assertNotNull(result);
        assertEquals(existingCompany.getId(), result.getId());
        assertEquals(existingCompany.getTicker(), result.getTicker());

        // Verify that the repository was only queried and not saved
        verify(companyRepository, times(1)).findByTicker("AAPL");
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    public void shouldCreateAndSaveNewCompanyWhenNotFoundByTicker() {

        when(companyRepository.findByTicker("TSLA")).thenReturn(null);

        Company newCompany = new Company("TSLA");

        when(companyRepository.save(any(Company.class))).thenReturn(newCompany);

        Company result = companyLookupService.findOrCreateCompany("TSLA");

        assertNotNull(result);
        assertEquals(newCompany.getId(), result.getId());
        assertEquals(newCompany.getTicker(), result.getTicker());

        // Verify that the repository was queried and then saved with a new company
        verify(companyRepository, times(1)).findByTicker("TSLA");
        verify(companyRepository, times(1)).save(any(Company.class));
    }

}
