package afsdigital.grahamselect.common.dividends.application.repository;

import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;

import java.util.List;

public interface DividendAlertPublisherPort {
    void publishNewDividendAnnouncements(List<DividendAnnouncement> announcements);
}
