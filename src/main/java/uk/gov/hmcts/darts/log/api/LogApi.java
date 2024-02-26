package uk.gov.hmcts.darts.log.api;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.util.LogJobReport;

public interface LogApi {
    void eventReceived(DartsEvent event);

    void missingCourthouse(DartsEvent event);

    void missingNodeRegistry(DartsEvent event);

    void processedDailyListJob(LogJobReport report);

    void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity);
}