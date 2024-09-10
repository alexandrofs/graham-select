package afsdigital.grahamselect.upload.application.usecase;


import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.upload.application.service.FinancialDataPublisherService;
import afsdigital.grahamselect.upload.domain.repository.FinancialDataReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UploadFinancialDataUseCaseTest {

    @Mock
    private FinancialDataReader mockFinancialDataReader;

    @Mock
    private FinancialDataPublisherService mockPublisherService;

    @InjectMocks
    private UploadFinancialDataUseCase useCase;

    @Test
    public void testExecuteProcessesAndPublishesFinancialData() {
        // Arrange
        InputStream fileInputStream = mock(InputStream.class);
        FinancialDataEvent event1 = FinancialDataEvent.builder().ticker("AAPL").price(150.0).build();
        FinancialDataEvent event2 = FinancialDataEvent.builder().ticker("GOOGL").price(150.0).build();
        List<FinancialDataEvent> events = Arrays.asList(event1, event2);

        when(mockFinancialDataReader.read(any(InputStream.class)))
                .thenReturn(events);

        // Act
        useCase.execute(fileInputStream);

        // Assert
        verify(mockFinancialDataReader, times(1)).read(fileInputStream);
        verify(mockPublisherService, times(1)).publish(event1);
        verify(mockPublisherService, times(1)).publish(event2);
    }

    @Test
    public void testExecuteNoFinancialData() {
        // Arrange
        InputStream fileInputStream = mock(InputStream.class);

        when(mockFinancialDataReader.read(any(InputStream.class)))
                .thenReturn(Collections.emptyList());

        // Act
        useCase.execute(fileInputStream);

        // Assert
        verify(mockFinancialDataReader, times(1)).read(fileInputStream);
        verify(mockPublisherService, never()).publish(any(FinancialDataEvent.class));
    }

    @Test
    public void testExecuteHandlesExceptionInFinancialDataReader() {
        // Arrange
        InputStream fileInputStream = mock(InputStream.class);
        when(mockFinancialDataReader.read(any(InputStream.class)))
                .thenThrow(new RuntimeException("Read error"));

        // Act & Assert
        try {
            useCase.execute(fileInputStream);
        } catch (RuntimeException e) {
            // Expected
            assertEquals("Read error", e.getMessage());
        }

        verify(mockFinancialDataReader, times(1)).read(fileInputStream);
        verify(mockPublisherService, never()).publish(Mockito.any(FinancialDataEvent.class));
    }

}
