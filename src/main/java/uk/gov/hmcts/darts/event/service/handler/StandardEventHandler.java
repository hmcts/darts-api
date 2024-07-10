package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.log.api.LogApi;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class StandardEventHandler extends EventHandlerBase {

    public StandardEventHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                                EventRepository eventRepository,
                                HearingRepository hearingRepository,
                                CaseRepository caseRepository,
                                ApplicationEventPublisher eventPublisher,
                                LogApi logApi,
                                EventPersistenceService eventPersistenceService) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService);
    }

    @Override
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        createHearingAndSaveEvent(dartsEvent, eventHandler);
    }
}
