package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.events.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.events.model.DartsEvent;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class InterpreterUsedHandler extends EventHandlerBase {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent) {
        var result = getOrCreateCourtroomCourtHouseAndCases(dartsEvent);
        var courtCase = result.getCourtCaseEntity();

        if (result.isHearingNew() || result.isCourtroomDifferentFromHearing()) {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, CASE_UPDATE);
            eventPublisher.publishEvent(notifyEvent);
        }
        courtCase.setInterpreterUsed(true);
    }
}
