package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.model.CourtroomCourthouseCourtcase;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class StandardEventHandler extends EventHandlerBase {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(final DartsEvent dartsEvent) {
        CourtroomCourthouseCourtcase courtroomCourthouseCourtcase = getOrCreateCourtroomCourtHouseAndCases(dartsEvent);

        if (isTheHearingNewOrTheCourtroomIsDifferent(
            courtroomCourthouseCourtcase.isHearingNew(),
            courtroomCourthouseCourtcase.isCourtroomDifferentFromHearing()
        )) {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, CASE_UPDATE);
            eventPublisher.publishEvent(notifyEvent);
        }
    }
}
