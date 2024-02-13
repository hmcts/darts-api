package uk.gov.hmcts.darts.event.service.handler;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.log.api.LogApi;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.START_RECORDING;

@Service
public class DarStartHandler extends EventHandlerBase {

    public DarStartHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                           EventRepository eventRepository,
                           HearingRepository hearingRepository,
                           CaseRepository caseRepository,
                           ApplicationEventPublisher eventPublisher,
                           AuthorisationApi authorisationApi,
                           LogApi logApi) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, authorisationApi, logApi);
    }

    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        CreatedHearingAndEvent hearingAndSaveEvent = createHearingAndSaveEvent(dartsEvent, eventHandler);// saveEvent
        var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, START_RECORDING, hearingAndSaveEvent.getCourtroomId());
        eventPublisher.publishEvent(notifyEvent);
    }

}
