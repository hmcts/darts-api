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
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;

@Slf4j
@Service
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class InterpreterUsedHandler extends EventHandlerBase {

    public InterpreterUsedHandler(RetrieveCoreObjectService retrieveCoreObjectService,
          EventRepository eventRepository,
          HearingRepository hearingRepository,
          CaseRepository caseRepository,
          ApplicationEventPublisher eventPublisher) {
        super(eventRepository, hearingRepository, caseRepository, eventPublisher, retrieveCoreObjectService);
    }

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        var hearing = createHearingAndSaveEvent(dartsEvent, eventHandler);
        var courtCase = hearing.getHearingEntity().getCourtCase();

        if (hearing.isHearingNew() || hearing.isCourtroomDifferentFromHearing()) {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, CASE_UPDATE);
            eventPublisher.publishEvent(notifyEvent);
        }
        courtCase.setInterpreterUsed(true);
    }
}
