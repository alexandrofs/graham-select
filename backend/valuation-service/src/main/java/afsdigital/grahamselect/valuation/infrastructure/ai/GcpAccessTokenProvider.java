package afsdigital.grahamselect.valuation.infrastructure.ai;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.util.List;

/**
 * Provides OAuth2 Bearer access tokens for Google Cloud Vertex AI using
 * Application Default Credentials (ADC / Workload Identity / Service Account JSON).
 */
public class GcpAccessTokenProvider {

    private static final List<String> CLOUD_PLATFORM_SCOPES =
            List.of("https://www.googleapis.com/auth/cloud-platform");

    private final CredentialsSupplier credentialsSupplier;
    private volatile GoogleCredentials scopedCredentials;

    public GcpAccessTokenProvider() {
        this(GoogleCredentials::getApplicationDefault);
    }

    GcpAccessTokenProvider(CredentialsSupplier credentialsSupplier) {
        this.credentialsSupplier = credentialsSupplier;
    }

    public String getAccessToken() throws IOException {
        GoogleCredentials credentials = getOrCreateScopedCredentials();
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        if (token == null || token.getTokenValue() == null || token.getTokenValue().isBlank()) {
            throw new IOException("Obtained empty GCP OAuth2 access token");
        }
        return token.getTokenValue();
    }

    private GoogleCredentials getOrCreateScopedCredentials() throws IOException {
        GoogleCredentials cached = this.scopedCredentials;
        if (cached == null) {
            synchronized (this) {
                cached = this.scopedCredentials;
                if (cached == null) {
                    cached = credentialsSupplier.get().createScoped(CLOUD_PLATFORM_SCOPES);
                    this.scopedCredentials = cached;
                }
            }
        }
        return cached;
    }

    @FunctionalInterface
    interface CredentialsSupplier {
        GoogleCredentials get() throws IOException;
    }
}
