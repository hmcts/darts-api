package uk.gov.hmcts.darts.event.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DarNotifyAsyncHelper {
    private final LogApi logApi;
    private final NodeRegisterRepository nodeRegisterRepository;
    private final DartsGatewayClient dartsGatewayClient;

    @Value("${darts.dar-pc-notification.url-format}")
    private String notificationUrlFormat;


    /*
    This method should not be called directly, DarNotifyServiceImpl should be called instead.
     */
    @Async
    public void notifyDarPcAsync(DarNotifyApplicationEvent event, List<String> openCaseNumbers) {
        var dartsEvent = event.getDartsEvent();
        Optional<NodeRegisterEntity> courtroomOpt = nodeRegisterRepository.findDarPcByCourtroomId(event.getCourtRoomId());
        if (courtroomOpt.isEmpty()) {
            logApi.missingNodeRegistry(dartsEvent);
            return;
        }
        String notificationUrl = MessageFormat.format(notificationUrlFormat, courtroomOpt.get().getIpAddress());
        var darNotifyType = event.getDarNotifyType();
        DarNotifyEvent darNotifyEvent = DarNotifyEvent.builder()
            .notificationUrl(notificationUrl)
            .notificationType(darNotifyType.getNotificationType())
            .timestamp(dartsEvent.getDateTime())
            .courthouse(dartsEvent.getCourthouse().toUpperCase(Locale.ROOT))
            .courtroom(dartsEvent.getCourtroom().toUpperCase(Locale.ROOT))
            .caseNumbers(openCaseNumbers)
            .build();

        try {
            dartsGatewayClient.darNotify(darNotifyEvent);
            log.info("DarNotify successful: event_id={}, notification_url={}", event.getDartsEvent().getEventId(), notificationUrl);
        } catch (Exception ex) {
            log.error("DarNotify failed: event_id={}, notification_url={}", event.getDartsEvent().getEventId(), notificationUrl, ex);
        }
    }
}
