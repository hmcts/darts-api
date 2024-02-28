package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DarNotifyServiceImpl {

    private final DartsGatewayClient dartsGatewayClient;
    private final CaseRepository caseRepository;
    private final NodeRegisterRepository nodeRegisterRepository;
    private final LogApi logApi;

    @Value("${darts.dar-pc-notification.url-format}")
    private String notificationUrlFormat;

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
            Optional<NodeRegisterEntity> courtroomOpt = nodeRegisterRepository.findDarPcByCourtroomId(event.getCourtRoomId());
            if (courtroomOpt.isEmpty()) {
                logApi.missingNodeRegistry(dartsEvent);
                return;
            }
            String notificationUrl = MessageFormat.format(notificationUrlFormat, courtroomOpt.get().getIpAddress());
            DarNotifyEvent darNotifyEvent = DarNotifyEvent.builder()
                .notificationUrl(notificationUrl)
                .notificationType(darNotifyType.getNotificationType())
                .timestamp(dartsEvent.getDateTime())
                .courthouse(dartsEvent.getCourthouse())
                .courtroom(dartsEvent.getCourtroom())
                .caseNumbers(openCaseNumbers)
                .build();


            dartsGatewayClient.darNotify(darNotifyEvent);
            log.info("Response from DarNotify for event {} is successful", event.getDartsEvent().getEventId());
        } else {
            log.info("No open cases for courthouse, ignoring DAR event {}", event.getDartsEvent().getEventId());
        }
    }
}
