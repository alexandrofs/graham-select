package afsdigital.grahamselect.valuation.infrastructure.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VertexAiConfigurationPropertiesTest {

    @Test
    void shouldBuildRegionalEndpointUrl() {
        VertexAiConfigurationProperties properties = new VertexAiConfigurationProperties();
        properties.setProjectId("kerno-prod");
        properties.setLocation("us-central1");
        properties.setModelName("gemini-2.5-flash");

        assertThat(properties.buildGenerateContentUrl()).isEqualTo(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/kerno-prod/locations/us-central1/publishers/google/models/gemini-2.5-flash:generateContent"
        );
    }

    @Test
    void shouldBuildGlobalEndpointUrlWithoutGlobalPrefix() {
        VertexAiConfigurationProperties properties = new VertexAiConfigurationProperties();
        properties.setProjectId("kerno-prod");
        properties.setLocation("global");
        properties.setModelName("gemini-2.5-flash");

        assertThat(properties.buildGenerateContentUrl()).isEqualTo(
                "https://aiplatform.googleapis.com/v1/projects/kerno-prod/locations/global/publishers/google/models/gemini-2.5-flash:generateContent"
        );
    }
}
