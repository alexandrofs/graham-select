package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.common.upload.application.usecase.UploadB3FileUseCase;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.kafka.bootstrap-servers=localhost:9092"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class B3UploadControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadB3FileUseCase uploadB3FileUseCase;

    @Test
    public void shouldReturn400WhenExtensionIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/v1/upload/b3")
                .file(file)
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400WhenHeadersAreMissing() throws Exception {
        byte[] content = createExcel(new String[]{"Col1", "Col2"});
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        mockMvc.perform(multipart("/api/v1/upload/b3")
                .file(file)
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn202WhenFileIsValid() throws Exception {
        byte[] content = createExcel(new String[]{"Ticker", "Data", "Quantidade", "Preço"});
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        mockMvc.perform(multipart("/api/v1/upload/b3")
                .file(file)
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isAccepted());
    }

    private byte[] createExcel(String[] headers) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            for (int i = 0; i < headers.length; i++) {
                ws.value(0, i, headers[i]);
            }
        }
        return os.toByteArray();
    }
}
