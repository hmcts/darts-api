package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import static uk.gov.hmcts.darts.event.enums.DarNotifyType.STOP_RECORDING;

@Service
@RequiredArgsConstructor
public class DarStopHandler extends EventHandlerBase {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        createHearingAndSaveEvent(dartsEvent, eventHandler); // saveEvent
        var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, STOP_RECORDING);
        eventPublisher.publishEvent(notifyEvent);
    }

}
