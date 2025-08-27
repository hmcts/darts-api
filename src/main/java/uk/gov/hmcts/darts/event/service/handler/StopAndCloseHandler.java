package uk.gov.hmcts.darts.event.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.exception.DarNotifyError;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;
import uk.gov.hmcts.darts.event.service.impl.DarNotifyServiceImpl;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.impl.CloseCaseWithRetentionServiceImpl;
import uk.gov.hmcts.darts.util.DataUtil;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.STOP_RECORDING;

@Service
@Slf4j
public class StopAndCloseHandler extends EventHandlerBase {

    private final DarNotifyServiceImpl darNotifyService;
    private final CloseCaseWithRetentionServiceImpl closeCaseWithRetentionServiceImpl;
    
    public StopAndCloseHandler(RetrieveCoreObjectService retrieveCoreObjectService,
                               EventRepository eventRepository,
                               HearingRepository hearingRepository,
                               CaseRepository caseRepository,
                               ApplicationEventPublisher eventPublisher,
                               DarNotifyServiceImpl darNotifyService,
                               CloseCaseWithRetentionServiceImpl closeCaseWithRetentionServiceImpl,
                               LogApi logApi,
                               EventPersistenceService eventPersistenceService) {
        super(retrieveCoreObjectService, eventRepository, hearingRepository, caseRepository, eventPublisher, logApi, eventPersistenceService);
        this.darNotifyService = darNotifyService;
        this.closeCaseWithRetentionServiceImpl = closeCaseWithRetentionServiceImpl;
    }

    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        DataUtil.preProcess(dartsEvent);
        var hearingAndEvent = createHearingAndSaveEvent(dartsEvent, eventHandler); // saveEvent
        var courtCase = hearingAndEvent.getHearingEntity().getCourtCase();

        try {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, STOP_RECORDING, hearingAndEvent.getCourtroomId());
            darNotifyService.notifyDarPc(notifyEvent);
        } catch (DarNotifyError e) {
            log.warn("Dar notify failed for {}", dartsEvent, e);
            // if DAR notify fails, continue processing the event
        }

        closeCaseWithRetentionServiceImpl.closeCaseAndSetRetention(dartsEvent, hearingAndEvent, courtCase);
    }
}