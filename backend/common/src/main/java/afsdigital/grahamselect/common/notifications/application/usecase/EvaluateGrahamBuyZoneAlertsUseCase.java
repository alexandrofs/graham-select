package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.domain.entities.ValuationCompletedEvent;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushPreferencePort;
import afsdigital.grahamselect.common.notifications.application.repository.WebPushSubscriptionPort;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushPreference;
import afsdigital.grahamselect.common.notifications.domain.entities.WebPushSubscription;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class EvaluateGrahamBuyZoneAlertsUseCase {

    private static final BigDecimal DEFAULT_MIN_MARGIN = new BigDecimal("20.00");
    private static final ZoneId B3_ZONE = ZoneId.of("America/Sao_Paulo");

    private final WebPushPreferencePort preferencePort;
    private final WebPushSubscriptionPort subscriptionPort;
    private final RankingRepository rankingRepository;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    private final SendWebPushAlertUseCase sendWebPushAlertUseCase;

    public void execute() {
        log.info("Iniciando avaliação de alertas de Zona de Compra Graham via Web Push");

        List<WebPushPreference> optedInPreferences = preferencePort.findAllOptedIn();
        if (optedInPreferences.isEmpty()) {
            log.info("Nenhum investidor com consentimento Web Push ativo encontrado");
            return;
        }

        List<RankedCompany> topRanked = rankingRepository.findTop20BestRanked();
        if (topRanked == null || topRanked.isEmpty()) {
            log.warn("Nenhuma empresa encontrada no ranking de valuation");
            return;
        }

        LocalDate nowUtc = LocalDate.now(ZoneOffset.UTC);
        List<RankedCompany> validCompanies = new ArrayList<>();

        for (RankedCompany c : topRanked) {
            if (c == null || c.getSymbol() == null || c.getSymbol().isBlank()) {
                continue;
            }
            LocalDate updatedAt = c.getIntrinsicValueUpdatedAt();
            if (updatedAt == null) {
                log.warn("[WEBPUSH-VALUATION] Empresa {} sem data de valuation. Descartando.", c.getSymbol());
                continue;
            }
            long daysOld = ChronoUnit.DAYS.between(updatedAt, nowUtc);
            if (daysOld > ValuationCompletedEvent.STALE_DATA_THRESHOLD_DAYS) {
                log.warn("[WEBPUSH-VALUATION] Dados de valuation obsoletos para {}: {} dias (limite: {})",
                        c.getSymbol(), daysOld, ValuationCompletedEvent.STALE_DATA_THRESHOLD_DAYS);
                continue;
            }
            if (c.getMarginOfSafety() == null || c.getMarginOfSafety().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (c.getMarginOfSafety().compareTo(ValuationCompletedEvent.OUTLIER_MARGIN_THRESHOLD) > 0) {
                log.error("[WEBPUSH-VALUATION-CB] Outlier de margem de segurança detectado para {}: {}",
                        c.getSymbol(), c.getMarginOfSafety());
                continue;
            }
            if (c.getCurrentPrice() == null || c.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0
                    || c.getIntrinsicValue() == null || c.getIntrinsicValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            validCompanies.add(c);
        }

        if (validCompanies.isEmpty()) {
            log.info("Nenhuma empresa válida atende aos critérios de integridade de valuation para alerta");
            return;
        }

        LocalDate todaySaoPaulo = LocalDate.now(B3_ZONE);

        for (WebPushPreference preference : optedInPreferences) {
            String userId = preference.getUserId();
            List<WebPushSubscription> activeSubscriptions = subscriptionPort.findActiveByUserId(userId);
            if (activeSubscriptions.isEmpty()) {
                continue;
            }

            Set<String> custodyTickers;
            if (preference.isOnlyCustodyTickers()) {
                try {
                    List<CustodyPositionDTO> positions = getCustodyPositionsUseCase.execute(userId);
                    custodyTickers = positions != null
                            ? positions.stream()
                            .filter(p -> p != null && p.ticker() != null && p.quantity() != null
                                    && p.quantity().compareTo(BigDecimal.ZERO) > 0)
                            .map(p -> p.ticker().trim().toUpperCase())
                            .collect(Collectors.toSet())
                            : Collections.emptySet();
                } catch (Exception e) {
                    log.error("Erro ao obter posições em custódia para Web Push userId={}: {}", userId, e.getMessage(), e);
                    continue;
                }
            } else {
                custodyTickers = Collections.emptySet();
            }

            BigDecimal minMarginPct = preference.getMinMarginOfSafetyPct() != null
                    ? preference.getMinMarginOfSafetyPct()
                    : DEFAULT_MIN_MARGIN;

            for (RankedCompany company : validCompanies) {
                String ticker = company.getSymbol().trim().toUpperCase();
                if (preference.isOnlyCustodyTickers() && !custodyTickers.contains(ticker)) {
                    continue;
                }

                BigDecimal marginPct = company.getMarginOfSafety()
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

                if (marginPct.compareTo(minMarginPct) < 0) {
                    continue;
                }

                String title = "Zona de Compra Graham: " + ticker;
                String body = String.format(Locale.ROOT,
                        "%s atingiu margem de segurança de %.2f%% (Preço: R$ %.2f | Preço Justo Graham: R$ %.2f)",
                        ticker, marginPct, company.getCurrentPrice(), company.getIntrinsicValue());
                String url = "/graham-recommendations";

                for (WebPushSubscription sub : activeSubscriptions) {
                    String dedupKey = "graham-buy-zone:" + userId + ":" + ticker + ":" + todaySaoPaulo + ":" + sub.getId();
                    sendWebPushAlertUseCase.sendAlert(
                            userId, sub, ticker, WebPushAlertType.GRAHAM_BUY_ZONE,
                            dedupKey, title, body, url
                    );
                }
            }
        }
    }
}
