package uk.gov.hmcts.darts.event.service.handler;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.START_RECORDING;

@Service
public class DarStartHandler extends EventHandlerBase {

    public DarStartHandler(RetrieveCoreObjectService retrieveCoreObjectService,
          EventRepository eventRepository,
          HearingRepository hearingRepository,
          CaseRepository caseRepository,
          ApplicationEventPublisher eventPublisher) {
        super(eventRepository, hearingRepository, caseRepository, eventPublisher, retrieveCoreObjectService);
    }

    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        createHearingAndSaveEvent(dartsEvent, eventHandler); // saveEvent
        var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, START_RECORDING);
        eventPublisher.publishEvent(notifyEvent);
    }

}
