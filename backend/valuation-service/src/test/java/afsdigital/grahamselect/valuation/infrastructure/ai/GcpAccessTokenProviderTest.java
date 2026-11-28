package afsdigital.grahamselect.valuation.infrastructure.ai;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GcpAccessTokenProviderTest {

    @Test
    void shouldScopeCacheCredentialsAndRefreshIfExpired() throws Exception {
        GoogleCredentials baseCredentials = mock(GoogleCredentials.class);
        GoogleCredentials scopedCredentials = mock(GoogleCredentials.class);
        when(baseCredentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")))
                .thenReturn(scopedCredentials);
        when(scopedCredentials.getAccessToken())
                .thenReturn(new AccessToken("cached-token", new Date(System.currentTimeMillis() + 3_600_000)));

        AtomicInteger supplierCalls = new AtomicInteger(0);
        GcpAccessTokenProvider provider = new GcpAccessTokenProvider(() -> {
            supplierCalls.incrementAndGet();
            return baseCredentials;
        });

        String first = provider.getAccessToken();
        String second = provider.getAccessToken();

        assertThat(first).isEqualTo("cached-token");
        assertThat(second).isEqualTo("cached-token");
        assertThat(supplierCalls.get()).isEqualTo(1);
        verify(baseCredentials, times(1)).createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        verify(scopedCredentials, times(2)).refreshIfExpired();
    }

    @Test
    void shouldThrowIOExceptionWhenAccessTokenIsNullOrBlank() throws Exception {
        GoogleCredentials baseCredentials = mock(GoogleCredentials.class);
        GoogleCredentials scopedCredentials = mock(GoogleCredentials.class);
        when(baseCredentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform")))
                .thenReturn(scopedCredentials);
        when(scopedCredentials.getAccessToken())
                .thenReturn(null)
                .thenReturn(new AccessToken("   ", new Date(System.currentTimeMillis() + 3_600_000)));

        GcpAccessTokenProvider provider = new GcpAccessTokenProvider(() -> baseCredentials);

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("empty GCP OAuth2 access token");

        assertThatThrownBy(provider::getAccessToken)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("empty GCP OAuth2 access token");
    }
}
