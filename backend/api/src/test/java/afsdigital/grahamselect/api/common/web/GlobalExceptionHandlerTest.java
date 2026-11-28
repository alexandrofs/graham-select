package afsdigital.grahamselect.api.common.web;

import afsdigital.grahamselect.common.cerrado.domain.exceptions.CerradoValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyCerradoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should map CerradoValidationException to HTTP 422 ProblemDetail")
    void shouldReturnProblemDetail422ForCerradoValidationException() throws Exception {
        mockMvc.perform(get("/test-cerrado-validation"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Cerrado Validation Error"))
                .andExpect(jsonPath("$.detail").value("A soma das metas das 5 Macro Classes deve ser exatamente 100.00%."));
    }

    @RestController
    static class DummyCerradoController {
        @GetMapping("/test-cerrado-validation")
        public void triggerCerradoValidation() {
            throw new CerradoValidationException("A soma das metas das 5 Macro Classes deve ser exatamente 100.00%.");
        }
    }
}
