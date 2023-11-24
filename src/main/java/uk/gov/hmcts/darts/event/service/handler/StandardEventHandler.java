package uk.gov.hmcts.darts.event.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.event.model.CreatedHearing;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.handler.base.EventHandlerBase;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class StandardEventHandler extends EventHandlerBase {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handle(final DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        CreatedHearing createdHearing = createHearingAndSaveEvent(dartsEvent, eventHandler);

        if (isTheHearingNewOrTheCourtroomIsDifferent(
            createdHearing.isHearingNew(),
            createdHearing.isCourtroomDifferentFromHearing()
        )) {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, CASE_UPDATE);
            eventPublisher.publishEvent(notifyEvent);
        }
    }
}
