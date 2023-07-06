package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.enums.DarNotifyType;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.DarNotifyService;

@Service
@Slf4j
@RequiredArgsConstructor
public class DarNotifyServiceImpl implements DarNotifyService {

    private final DartsGatewayClient dartsGatewayClient;

    @Override
    public void darNotify(DartsEvent dartsEvent, DarNotifyType darNotifyType) {
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
