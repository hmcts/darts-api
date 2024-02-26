package uk.gov.hmcts.darts.log.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.service.AtsLoggerService;
import uk.gov.hmcts.darts.log.service.EventLoggerService;
import uk.gov.hmcts.darts.log.service.LogJobService;
import uk.gov.hmcts.darts.log.util.LogJobReport;

@Service
@RequiredArgsConstructor
public class LogApiImpl implements LogApi {

    private final EventLoggerService eventLoggerService;
    private final AtsLoggerService atsLoggerService;

    private final LogJobService logJobService;

    @Override
    public void eventReceived(DartsEvent event) {
        eventLoggerService.eventReceived(event);
    }

    @Override
    public void missingCourthouse(DartsEvent event) {
        eventLoggerService.missingCourthouse(event);
    }

    @Override
    public void missingNodeRegistry(DartsEvent event) {
        eventLoggerService.missingNodeRegistry(event);
    }

    @Override
    public void processedDailyListJob(LogJobReport report) {
        logJobService.logJobReport(report);
    }

    public void atsProcessingUpdate(MediaRequestEntity mediaRequestEntity) {
        atsLoggerService.atsProcessingUpdate(mediaRequestEntity);
    }
}