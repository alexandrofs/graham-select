package afsdigital.grahamselect.common.notifications.application.usecase;

import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.notifications.application.repository.WhatsappPreferencePort;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappAlertType;
import afsdigital.grahamselect.common.notifications.domain.entities.WhatsappNotificationPreference;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class NotifyDividendAlertsUseCase {

    private final WhatsappPreferencePort preferencePort;
    private final GetCustodyPositionsUseCase getCustodyPositionsUseCase;
    private final SendWhatsappAlertUseCase sendWhatsappAlertUseCase;

    public void notifyForNewAnnouncements(List<DividendAnnouncement> newAnnouncements) {
        if (newAnnouncements == null || newAnnouncements.isEmpty()) {
            return;
        }

        List<WhatsappNotificationPreference> allOptedIn = preferencePort.findAllOptedIn();
        if (allOptedIn == null || allOptedIn.isEmpty()) {
            return;
        }

        List<WhatsappNotificationPreference> optedInUsers = allOptedIn.stream()
                .filter(WhatsappNotificationPreference::isNotifyDividends)
                .toList();

        if (optedInUsers.isEmpty()) {
            return;
        }

        Map<String, List<DividendAnnouncement>> byTicker = newAnnouncements.stream()
                .filter(a -> a != null && a.getTicker() != null)
                .collect(Collectors.groupingBy(a -> a.getTicker().trim().toUpperCase()));

        if (byTicker.isEmpty()) {
            return;
        }

        log.info("Avaliando alertas de proventos para {} usuários opt-in e {} novos anúncios",
                optedInUsers.size(), newAnnouncements.size());

        for (WhatsappNotificationPreference pref : optedInUsers) {
            try {
                List<CustodyPositionDTO> positions = getCustodyPositionsUseCase.execute(pref.getUserId());
                if (positions == null || positions.isEmpty()) {
                    continue;
                }
                for (CustodyPositionDTO position : positions) {
                    if (position.ticker() == null || position.quantity() == null
                            || position.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    String ticker = position.ticker().trim().toUpperCase();
                    List<DividendAnnouncement> matching = byTicker.get(ticker);
                    if (matching == null || matching.isEmpty()) {
                        continue;
                    }
                    for (DividendAnnouncement announcement : matching) {
                        BigDecimal netValue = announcement.getNetValue();
                        if (netValue == null || netValue.compareTo(BigDecimal.ZERO) <= 0
                                || announcement.getComDate() == null || announcement.getType() == null) {
                            continue;
                        }
                        BigDecimal estimatedTotal = position.quantity()
                                .multiply(netValue)
                                .setScale(2, RoundingMode.HALF_UP);
                        String dedupKey = String.format("dividend:%s:%s:%s:%s",
                                pref.getUserId(),
                                ticker,
                                announcement.getComDate(),
                                announcement.getType().name());
                        String paymentInfo = announcement.getPaymentDate() != null
                                ? " | Pagamento: " + announcement.getPaymentDate()
                                : "";
                        String message = String.format(
                                "Kerno Provento: %s anunciou %s. Data COM: %s%s. Valor líquido estimado para sua posição: R$ %s.",
                                ticker,
                                announcement.getType().name(),
                                announcement.getComDate(),
                                paymentInfo,
                                estimatedTotal.toPlainString()
                        );
                        sendWhatsappAlertUseCase.sendAlert(
                                pref,
                                WhatsappAlertType.DIVIDEND_ANNOUNCEMENT,
                                dedupKey,
                                message
                        );
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao avaliar alertas de proventos para userId={}: {}",
                        pref.getUserId(), e.getMessage(), e);
            }
        }
    }
}
