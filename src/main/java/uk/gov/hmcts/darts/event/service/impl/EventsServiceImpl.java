package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.enums.DarNotifyType;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventsService;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventsServiceImpl implements EventsService {

    private final DartsGatewayClient dartsGatewayClient;

    private static final String NOTIFICATION_TYPE = DarNotifyType.CASE_UPDATE.getNotificationType();

    @Override
    public void notifyEvent(DartsEvent dartsEvent) {
        DarNotifyEvent darNotifyEvent = DarNotifyEvent.builder()
            .notificationType(NOTIFICATION_TYPE)
            .timestamp(dartsEvent.getDateTime())
            .courthouse(dartsEvent.getCourthouse())
            .courtroom(dartsEvent.getCourtroom())
            .caseNumbers(dartsEvent.getCaseNumbers())
            .build();

        dartsGatewayClient.notifyEvent(darNotifyEvent);
    }

}
