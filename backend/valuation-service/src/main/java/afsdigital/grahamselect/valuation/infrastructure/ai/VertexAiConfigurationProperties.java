package afsdigital.grahamselect.valuation.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Google Cloud Vertex AI Enterprise ZDR integration.
 */
@ConfigurationProperties(prefix = "app.ai.vertex")
public class VertexAiConfigurationProperties {

    private String projectId = "";
    private String location = "us-central1";
    private String modelName = "gemini-2.5-flash";
    private Duration timeout = Duration.ofSeconds(15);
    private double temperature = 0.1;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public boolean isConfigured() {
        return isNotBlank(projectId) && isNotBlank(location) && isNotBlank(modelName);
    }

    public String buildGenerateContentUrl() {
        String trimmedLocation = location.trim();
        String host = "global".equalsIgnoreCase(trimmedLocation)
                ? "aiplatform.googleapis.com"
                : trimmedLocation + "-aiplatform.googleapis.com";
        return String.format(
                "https://%s/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                host,
                projectId.trim(),
                trimmedLocation,
                modelName.trim()
        );
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
