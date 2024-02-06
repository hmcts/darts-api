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
import uk.gov.hmcts.darts.event.service.impl.DarNotifyServiceImpl;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.STOP_RECORDING;

@Service
public class StopAndCloseHandler extends EventHandlerBase {

    private final DarNotifyServiceImpl darNotifyService;

    public StopAndCloseHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                               EventRepository eventRepository,
                               HearingRepository hearingRepository,
                               CaseRepository caseRepository,
                               ApplicationEventPublisher eventPublisher,
                               DarNotifyServiceImpl darNotifyService) {
        super(eventRepository, hearingRepository, caseRepository, eventPublisher, retrieveCoreObjectService);
        this.darNotifyService = darNotifyService;
    }

    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        var hearing = createHearingAndSaveEvent(dartsEvent, eventHandler); // saveEvent
        var courtCase = hearing.getHearingEntity().getCourtCase();

        var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, STOP_RECORDING);
        darNotifyService.notifyDarPc(notifyEvent);

        //setting the case to closed after notifying DAR Pc to ensure notification is sent.
        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(dartsEvent.getDateTime());
    }

}
