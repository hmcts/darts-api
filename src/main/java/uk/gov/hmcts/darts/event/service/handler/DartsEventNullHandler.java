package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.log.api.LogApi;

@Service
@Slf4j
/*
 * This class is used for events that should have no action taken against them.
 */
public class DartsEventNullHandler extends EventHandlerBase {

    public DartsEventNullHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                                 EventRepository eventRepository,
                                 HearingRepository hearingRepository,
                                 CaseRepository caseRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 LogApi logApi,
                                 EventPersistenceService eventPersistenceService) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService);
    }

    @Override
    public EventEntity handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        log.debug(
            "Null handler selected for message: {} type: {} and subtype: {}. ",
            dartsEvent.getMessageId(),
            dartsEvent.getType(),
            dartsEvent.getSubType()
        );
        return null;
    }
}
