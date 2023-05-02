package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev")
class GovNotifyServiceTest {

    @Autowired
    GovNotifyService govNotifyService;

    @Value("${darts.notification.gov-notify.template.transcriber.requested_audio_is_available}")
    private String requestedAudioIsAvailable;

    @Test
    @Disabled("Will fail until test GovNotify API key is in jenkins build")
    void testRequestedAudioIsAvailableOutput() throws NotificationClientException {
        GovNotifyRequest govNotifyRequest = new GovNotifyRequest();
        govNotifyRequest.setTemplateId(requestedAudioIsAvailable);
        govNotifyRequest.setEmailAddress("test@test.com");
        Map<String, String> parameterMap = new ConcurrentHashMap<>();
        parameterMap.put("case_id", "TheCaseId");
        govNotifyRequest.setParameterMap(parameterMap);

        SendEmailResponse emailResponse = govNotifyService.sendNotification(govNotifyRequest);
        assertEquals("DARTS: Requested Audio is Available", emailResponse.getSubject());
        assertEquals("Hello,\r\n" +
                         "The audio you requested for case TheCaseId is now available.\r\n" +
                         "Please visit the My Audio section within DARTS to access your requested Audio.\r\n" +
                         "Regards\r\n" +
                         "DARTS", emailResponse.getBody());
    }

}
