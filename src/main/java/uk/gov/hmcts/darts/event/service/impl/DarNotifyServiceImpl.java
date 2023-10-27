package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class DarNotifyServiceImpl {

    private final DartsGatewayClient dartsGatewayClient;

    @Async
    @EventListener
    public void onApplicationEvent(DarNotifyApplicationEvent event) {
        var darNotifyType = event.getDarNotifyType();
        var dartsEvent = event.getDartsEvent();

        DarNotifyEvent darNotifyEvent = DarNotifyEvent.builder()
            .notificationType(darNotifyType.getNotificationType())
            .timestamp(dartsEvent.getDateTime())
            .courthouse(dartsEvent.getCourthouse())
            .courtroom(dartsEvent.getCourtroom())
            .caseNumbers(dartsEvent.getCaseNumbers())
            .build();

        dartsGatewayClient.darNotify(darNotifyEvent);
    }
}
