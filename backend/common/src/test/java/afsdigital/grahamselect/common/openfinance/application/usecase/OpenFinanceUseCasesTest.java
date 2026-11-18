package afsdigital.grahamselect.common.openfinance.application.usecase;

import afsdigital.grahamselect.common.family.application.repository.FamilyProfilePort;
import afsdigital.grahamselect.common.family.domain.entities.FamilyProfile;
import afsdigital.grahamselect.common.openfinance.application.dto.ConsentUrlResponseDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.ExchangeConsentCallbackDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.InitiateConnectionRequestDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceConnectionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceInstitutionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinancePositionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceSyncResultDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTokenPairDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTransactionDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceSyncLogPort;
import afsdigital.grahamselect.common.openfinance.application.repository.TokenEncryptionPort;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncLog;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import afsdigital.grahamselect.common.portfolio.application.usecase.SaveTradeUseCase;
import afsdigital.grahamselect.common.portfolio.application.usecase.SendPortfolioUpdateNotificationUseCase;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenFinanceUseCasesTest {

    @Mock
    private OpenFinanceConnectionPort connectionPort;

    @Mock
    private OpenFinanceSyncLogPort syncLogPort;

    @Mock
    private OpenFinanceConnectorPort connectorPort;

    @Mock
    private TokenEncryptionPort tokenEncryptionPort;

    @Mock
    private FamilyProfilePort familyProfilePort;

    @Mock
    private SaveTradeUseCase saveTradeUseCase;

    @Mock
    private SendPortfolioUpdateNotificationUseCase sendPortfolioUpdateNotificationUseCase;

    private ListOpenFinanceInstitutionsUseCase listInstitutionsUseCase;
    private InitiateOpenFinanceConnectionUseCase initiateConnectionUseCase;
    private CompleteOpenFinanceConsentUseCase completeConsentUseCase;
    private SyncOpenFinanceConnectionUseCase syncConnectionUseCase;
    private RevokeOpenFinanceConnectionUseCase revokeConnectionUseCase;
    private ListUserOpenFinanceConnectionsUseCase listUserConnectionsUseCase;

    @BeforeEach
    void setUp() {
        listInstitutionsUseCase = new ListOpenFinanceInstitutionsUseCase();
        initiateConnectionUseCase = new InitiateOpenFinanceConnectionUseCase(connectionPort, connectorPort, familyProfilePort);
        completeConsentUseCase = new CompleteOpenFinanceConsentUseCase(connectionPort, connectorPort, tokenEncryptionPort);
        syncConnectionUseCase = new SyncOpenFinanceConnectionUseCase(
                connectionPort, syncLogPort, connectorPort, tokenEncryptionPort,
                saveTradeUseCase, sendPortfolioUpdateNotificationUseCase
        );
        revokeConnectionUseCase = new RevokeOpenFinanceConnectionUseCase(connectionPort, connectorPort, tokenEncryptionPort);
        listUserConnectionsUseCase = new ListUserOpenFinanceConnectionsUseCase(connectionPort);
    }

    @Test
    @DisplayName("Should list available partner institutions")
    void testListInstitutions() {
        List<OpenFinanceInstitutionDTO> institutions = listInstitutionsUseCase.execute();
        assertThat(institutions).isNotEmpty();
        assertThat(institutions).extracting(OpenFinanceInstitutionDTO::id)
                .contains("XP_INVESTIMENTOS", "BTG_PACTUAL", "NUBANK", "ITAU", "INTER");
    }

    @Test
    @DisplayName("Should initiate connection and generate consent URL for master account")
    void testInitiateConnectionMasterAccount() {
        String userId = "user-123";
        InitiateConnectionRequestDTO request = new InitiateConnectionRequestDTO("XP_INVESTIMENTOS", null);

        when(connectionPort.findByUserIdAndInstitutionIdAndProfileId(userId, "XP_INVESTIMENTOS", null))
                .thenReturn(Optional.empty());
        when(connectorPort.generateConsentUrl(eq("XP_INVESTIMENTOS"), any(), eq("https://app/callback")))
                .thenReturn("https://auth.xp.com.br/consent?state=xyz");
        when(connectionPort.save(any(OpenFinanceConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConsentUrlResponseDTO response = initiateConnectionUseCase.execute(userId, request, "https://app/callback");

        assertThat(response).isNotNull();
        assertThat(response.consentUrl()).isEqualTo("https://auth.xp.com.br/consent?state=xyz");
        assertThat(response.connectionId()).isNotNull();

        ArgumentCaptor<OpenFinanceConnection> captor = ArgumentCaptor.forClass(OpenFinanceConnection.class);
        verify(connectionPort).save(captor.capture());
        OpenFinanceConnection saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getProfileId()).isNull();
        assertThat(saved.getStatus()).isEqualTo(OpenFinanceConnectionStatus.PENDING_CONSENT);
    }

    @Test
    @DisplayName("Should validate family profile when initiating connection with profileId")
    void testInitiateConnectionFamilyProfile() {
        String userId = "user-123";
        UUID profileId = UUID.randomUUID();
        InitiateConnectionRequestDTO request = new InitiateConnectionRequestDTO("BTG_PACTUAL", profileId);

        FamilyProfile profile = FamilyProfile.builder()
                .id(profileId)
                .masterUserId(userId)
                .active(true)
                .build();

        when(familyProfilePort.findActiveByIdAndMasterUserId(profileId, userId)).thenReturn(Optional.of(profile));
        when(connectionPort.findByUserIdAndInstitutionIdAndProfileId(userId, "BTG_PACTUAL", profileId))
                .thenReturn(Optional.empty());
        when(connectorPort.generateConsentUrl(eq("BTG_PACTUAL"), any(), eq("https://app/callback")))
                .thenReturn("https://auth.btg.com.br/consent?state=xyz");
        when(connectionPort.save(any(OpenFinanceConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConsentUrlResponseDTO response = initiateConnectionUseCase.execute(userId, request, "https://app/callback");

        assertThat(response).isNotNull();
        verify(familyProfilePort).findActiveByIdAndMasterUserId(profileId, userId);
    }

    @Test
    @DisplayName("Should throw exception if family profile not found or inactive")
    void testInitiateConnectionInvalidFamilyProfile() {
        String userId = "user-123";
        UUID profileId = UUID.randomUUID();
        InitiateConnectionRequestDTO request = new InitiateConnectionRequestDTO("BTG_PACTUAL", profileId);

        when(familyProfilePort.findActiveByIdAndMasterUserId(profileId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initiateConnectionUseCase.execute(userId, request, "https://app/callback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Perfil familiar");
    }

    @Test
    @DisplayName("Should complete consent and activate connection with encrypted tokens")
    void testCompleteConsent() {
        String userId = "user-123";
        UUID connectionId = UUID.randomUUID();
        ExchangeConsentCallbackDTO callback = new ExchangeConsentCallbackDTO(connectionId, "auth_code_123", "state_123");

        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(connectionId)
                .userId(userId)
                .institutionId("XP_INVESTIMENTOS")
                .status(OpenFinanceConnectionStatus.PENDING_CONSENT)
                .consentId("state_123")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        OpenFinanceTokenPairDTO tokenPair = new OpenFinanceTokenPairDTO(
                "plain_access_token",
                "plain_refresh_token",
                LocalDateTime.now(ZoneOffset.UTC).plusDays(90),
                "consent_999"
        );

        when(connectionPort.findByIdAndUserId(connectionId, userId)).thenReturn(Optional.of(connection));
        when(connectorPort.exchangeConsentCode("XP_INVESTIMENTOS", "auth_code_123", "state_123", "https://app/callback"))
                .thenReturn(tokenPair);
        when(tokenEncryptionPort.encrypt("plain_access_token")).thenReturn("enc_access_token");
        when(tokenEncryptionPort.encrypt("plain_refresh_token")).thenReturn("enc_refresh_token");
        when(connectionPort.save(any(OpenFinanceConnection.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenFinanceConnectionDTO result = completeConsentUseCase.execute(userId, callback, "https://app/callback");

        assertThat(result.status()).isEqualTo(OpenFinanceConnectionStatus.ACTIVE);
        assertThat(connection.getEncryptedAccessToken()).isEqualTo("enc_access_token");
        assertThat(connection.getEncryptedRefreshToken()).isEqualTo("enc_refresh_token");
        assertThat(connection.getConsentId()).isEqualTo("consent_999");
    }

    @Test
    @DisplayName("Should reject complete consent when state does not match")
    void testCompleteConsentStateMismatch() {
        String userId = "user-123";
        UUID connectionId = UUID.randomUUID();
        ExchangeConsentCallbackDTO callback = new ExchangeConsentCallbackDTO(connectionId, "auth_code_123", "divergent_state");

        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(connectionId)
                .userId(userId)
                .institutionId("XP_INVESTIMENTOS")
                .status(OpenFinanceConnectionStatus.PENDING_CONSENT)
                .consentId("expected_state")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        when(connectionPort.findByIdAndUserId(connectionId, userId)).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> completeConsentUseCase.execute(userId, callback, "https://app/callback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("State de consentimento");
    }

    @Test
    @DisplayName("Should sync connection, deduplicate trades and send SSE notification")
    void testSyncConnectionSuccess() {
        UUID connectionId = UUID.randomUUID();
        String userId = "user-123";

        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(connectionId)
                .userId(userId)
                .institutionId("XP_INVESTIMENTOS")
                .institutionName("XP Investimentos")
                .status(OpenFinanceConnectionStatus.ACTIVE)
                .encryptedAccessToken("enc_access")
                .encryptedRefreshToken("enc_refresh")
                .consentExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(30))
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        when(connectionPort.findByIdAndUserId(connectionId, userId)).thenReturn(Optional.of(connection));
        when(tokenEncryptionPort.decrypt("enc_access")).thenReturn("plain_access");
        when(tokenEncryptionPort.decrypt("enc_refresh")).thenReturn("plain_refresh");

        List<OpenFinancePositionDTO> positions = List.of(
                new OpenFinancePositionDTO("PETR4", BigDecimal.valueOf(100), BigDecimal.valueOf(35.50), "XP")
        );
        List<OpenFinanceTransactionDTO> transactions = List.of(
                new OpenFinanceTransactionDTO("tx-1", "PETR4", LocalDate.of(2026, 3, 10), BigDecimal.valueOf(100), BigDecimal.valueOf(35.50), "COMPRA", "XP", "Compra ações"),
                new OpenFinanceTransactionDTO("tx-2", "VALE3", LocalDate.of(2026, 3, 11), BigDecimal.valueOf(50), BigDecimal.valueOf(60.00), "VENDA", "XP", "Venda ações"),
                new OpenFinanceTransactionDTO("tx-3", "DIVIDENDO", LocalDate.of(2026, 3, 12), BigDecimal.valueOf(1), BigDecimal.valueOf(10.00), "PROVENTO", "XP", "Dividendo em conta") // Non-trade
        );

        when(connectorPort.fetchPositions(eq("XP_INVESTIMENTOS"), eq("plain_access"))).thenReturn(positions);
        when(connectorPort.fetchTransactions(eq("XP_INVESTIMENTOS"), eq("plain_access"), any(), any())).thenReturn(transactions);

        // Trade 1 is saved, Trade 2 is duplicated (returns null)
        when(saveTradeUseCase.execute(any(Trade.class)))
                .thenReturn(Trade.builder().id(UUID.randomUUID()).build())
                .thenReturn(null);

        when(connectionPort.save(any(OpenFinanceConnection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncLogPort.save(any(OpenFinanceSyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenFinanceSyncResultDTO result = syncConnectionUseCase.execute(connectionId, userId, OpenFinanceSyncType.MANUAL);

        assertThat(result.status()).isEqualTo(OpenFinanceSyncStatus.SUCCESS);
        assertThat(result.positionsFetched()).isEqualTo(1);
        assertThat(result.transactionsFetched()).isEqualTo(3);
        assertThat(result.tradesCreated()).isEqualTo(1);
        assertThat(result.tradesDuplicated()).isEqualTo(1);

        verify(sendPortfolioUpdateNotificationUseCase).execute(userId);
    }

    @Test
    @DisplayName("Should automatically refresh token during sync when token is expired")
    void testSyncAutoRefreshToken() {
        UUID connectionId = UUID.randomUUID();
        String userId = "user-123";

        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(connectionId)
                .userId(userId)
                .institutionId("XP_INVESTIMENTOS")
                .institutionName("XP Investimentos")
                .status(OpenFinanceConnectionStatus.ACTIVE)
                .encryptedAccessToken("enc_old_access")
                .encryptedRefreshToken("enc_refresh")
                .consentExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10)) // Expired!
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        OpenFinanceTokenPairDTO refreshedTokens = new OpenFinanceTokenPairDTO(
                "plain_new_access",
                "plain_new_refresh",
                LocalDateTime.now(ZoneOffset.UTC).plusHours(2),
                "consent_123"
        );

        when(connectionPort.findByIdAndUserId(connectionId, userId)).thenReturn(Optional.of(connection));
        when(tokenEncryptionPort.decrypt("enc_old_access")).thenReturn("plain_old_access");
        when(tokenEncryptionPort.decrypt("enc_refresh")).thenReturn("plain_refresh");

        when(connectorPort.refreshToken("XP_INVESTIMENTOS", "plain_refresh")).thenReturn(refreshedTokens);
        when(tokenEncryptionPort.encrypt("plain_new_access")).thenReturn("enc_new_access");
        when(tokenEncryptionPort.encrypt("plain_new_refresh")).thenReturn("enc_new_refresh");

        when(connectorPort.fetchPositions(eq("XP_INVESTIMENTOS"), eq("plain_new_access"))).thenReturn(List.of());
        when(connectorPort.fetchTransactions(eq("XP_INVESTIMENTOS"), eq("plain_new_access"), any(), any())).thenReturn(List.of());

        when(connectionPort.save(any(OpenFinanceConnection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(syncLogPort.save(any(OpenFinanceSyncLog.class))).thenAnswer(inv -> inv.getArgument(0));

        OpenFinanceSyncResultDTO result = syncConnectionUseCase.execute(connectionId, userId, OpenFinanceSyncType.SCHEDULED);

        assertThat(result.status()).isEqualTo(OpenFinanceSyncStatus.SUCCESS);
        assertThat(connection.getEncryptedAccessToken()).isEqualTo("enc_new_access");
        verify(connectorPort).refreshToken("XP_INVESTIMENTOS", "plain_refresh");
    }

    @Test
    @DisplayName("Should revoke connection locally and remotely")
    void testRevokeConnection() {
        UUID connectionId = UUID.randomUUID();
        String userId = "user-123";

        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(connectionId)
                .userId(userId)
                .institutionId("XP_INVESTIMENTOS")
                .consentId("consent_123")
                .encryptedAccessToken("enc_access")
                .status(OpenFinanceConnectionStatus.ACTIVE)
                .build();

        when(connectionPort.findByIdAndUserId(connectionId, userId)).thenReturn(Optional.of(connection));
        when(tokenEncryptionPort.decrypt("enc_access")).thenReturn("plain_access");
        when(connectionPort.save(any(OpenFinanceConnection.class))).thenAnswer(inv -> inv.getArgument(0));

        revokeConnectionUseCase.execute(connectionId, userId);

        assertThat(connection.getStatus()).isEqualTo(OpenFinanceConnectionStatus.REVOKED);
        assertThat(connection.getEncryptedAccessToken()).isNull();
        assertThat(connection.getEncryptedRefreshToken()).isNull();
        verify(connectorPort).revokeConsent("XP_INVESTIMENTOS", "consent_123", "plain_access");
    }

    @Test
    @DisplayName("Should list sanitized connections for user")
    void testListUserConnections() {
        String userId = "user-123";
        OpenFinanceConnection connection = OpenFinanceConnection.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .institutionId("XP_INVESTIMENTOS")
                .institutionName("XP Investimentos")
                .status(OpenFinanceConnectionStatus.ACTIVE)
                .encryptedAccessToken("enc_token")
                .build();

        when(connectionPort.findByUserId(userId)).thenReturn(List.of(connection));

        List<OpenFinanceConnectionDTO> list = listUserConnectionsUseCase.execute(userId);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).institutionId()).isEqualTo("XP_INVESTIMENTOS");
    }
}
