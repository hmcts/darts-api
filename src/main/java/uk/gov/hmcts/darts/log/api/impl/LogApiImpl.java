package uk.gov.hmcts.darts.log.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.service.EventLoggerService;

@Service
@RequiredArgsConstructor
public class LogApiImpl implements LogApi {

    private final EventLoggerService eventLoggerService;

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
}
