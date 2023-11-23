package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DarNotifyServiceImpl {

    private final DartsGatewayClient dartsGatewayClient;
    private final CaseRepository caseRepository;

    @Async
    @EventListener
    public void onApplicationEvent(DarNotifyApplicationEvent event) {
        notifyDarPc(event);
    }

    public void notifyDarPc(DarNotifyApplicationEvent event) {
        var darNotifyType = event.getDarNotifyType();
        var dartsEvent = event.getDartsEvent();

        List<String> openCaseNumbers = caseRepository.findOpenCaseNumbers(dartsEvent.getCourthouse(), dartsEvent.getCaseNumbers());
        if (!openCaseNumbers.isEmpty()) {
            DarNotifyEvent darNotifyEvent = DarNotifyEvent.builder()
                .notificationType(darNotifyType.getNotificationType())
                .timestamp(dartsEvent.getDateTime())
                .courthouse(dartsEvent.getCourthouse())
                .courtroom(dartsEvent.getCourtroom())
                .caseNumbers(openCaseNumbers)
                .build();

            dartsGatewayClient.darNotify(darNotifyEvent);
        }
    }
}
