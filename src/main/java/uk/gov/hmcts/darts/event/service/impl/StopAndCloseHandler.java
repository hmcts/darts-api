package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.STOP_RECORDING;

@Service
@RequiredArgsConstructor
public class StopAndCloseHandler extends EventHandlerBase {

    private final ApplicationEventPublisher eventPublisher;
    private final DarNotifyServiceImpl darNotifyService;

    @Override
    @Transactional
    public void handle(DartsEvent dartsEvent) {
        var hearing = createHearing(dartsEvent); // saveEvent
        var courtCase = hearing.getHearingEntity().getCourtCase();

        var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, STOP_RECORDING);
        darNotifyService.notifyDarPc(notifyEvent);

        courtCase.setClosed(TRUE);
        courtCase.setCaseClosedTimestamp(dartsEvent.getDateTime());
    }

}
