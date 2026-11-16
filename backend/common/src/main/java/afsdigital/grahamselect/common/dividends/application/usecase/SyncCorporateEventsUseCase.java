package afsdigital.grahamselect.common.dividends.application.usecase;

import afsdigital.grahamselect.common.dividends.application.dto.CorporateEventSyncSummaryDTO;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.application.repository.DividendCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactCrawlerPort;
import afsdigital.grahamselect.common.dividends.application.repository.RelevantFactPort;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import afsdigital.grahamselect.valuation.application.repository.CustodyTickerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SyncCorporateEventsUseCase {

    private final CustodyTickerPort custodyTickerPort;
    private final DividendCrawlerPort dividendCrawlerPort;
    private final RelevantFactCrawlerPort relevantFactCrawlerPort;
    private final DividendAnnouncementPort dividendAnnouncementPort;
    private final RelevantFactPort relevantFactPort;

    public CorporateEventSyncSummaryDTO execute() {
        return execute(null);
    }

    public CorporateEventSyncSummaryDTO execute(List<String> explicitTickers) {
        log.info("Iniciando caso de uso SyncCorporateEventsUseCase...");

        List<String> tickers = (explicitTickers != null && !explicitTickers.isEmpty())
                ? explicitTickers
                : custodyTickerPort.findDistinctActiveTickers();

        if (tickers == null || tickers.isEmpty()) {
            log.info("Nenhum ticker ativo encontrado para sincronização de eventos corporativos.");
            return new CorporateEventSyncSummaryDTO(0, 0, 0, 0, Instant.now(), "SUCCESS");
        }

        log.info("Sincronizando eventos corporativos para {} tickers: {}", tickers.size(), tickers);

        int newDividends = 0;
        int updatedDividends = 0;
        int newFacts = 0;

        try {
            // 1. Proventos
            List<DividendAnnouncement> announcements = dividendCrawlerPort.crawlDividends(tickers);
            if (announcements != null) {
                for (DividendAnnouncement announcement : announcements) {
                    if (announcement == null || announcement.getTicker() == null) continue;
                    try {
                        boolean exists = dividendAnnouncementPort.existsByTickerAndComDateAndTypeAndRate(
                                announcement.getTicker(),
                                announcement.getComDate(),
                                announcement.getType(),
                                announcement.getRate()
                        );
                        dividendAnnouncementPort.save(announcement);
                        if (exists) {
                            updatedDividends++;
                        } else {
                            newDividends++;
                        }
                    } catch (Exception e) {
                        log.error("Erro ao persistir anúncio de provento para ticker={}: {}", announcement.getTicker(), e.getMessage(), e);
                    }
                }
            }

            // 2. Fatos Relevantes
            List<RelevantFact> facts = relevantFactCrawlerPort.crawlRelevantFacts(tickers);
            if (facts != null) {
                for (RelevantFact fact : facts) {
                    if (fact == null || fact.getTicker() == null) continue;
                    try {
                        boolean exists = relevantFactPort.existsByTickerAndPublicationDate(
                                fact.getTicker(),
                                fact.getPublicationDate()
                        );
                        if (!exists) {
                            relevantFactPort.save(fact);
                            newFacts++;
                        }
                    } catch (Exception e) {
                        log.error("Erro ao persistir fato relevante para ticker={}: {}", fact.getTicker(), e.getMessage(), e);
                    }
                }
            }

            CorporateEventSyncSummaryDTO summary = new CorporateEventSyncSummaryDTO(
                    tickers.size(),
                    newDividends,
                    updatedDividends,
                    newFacts,
                    Instant.now(),
                    "SUCCESS"
            );
            log.info("Sincronização de eventos corporativos concluída com sucesso: {}", summary);
            return summary;
        } catch (Exception e) {
            log.error("Erro inesperado durante a execução de SyncCorporateEventsUseCase: {}", e.getMessage(), e);
            throw e;
        }
    }
}
