package uk.gov.hmcts.darts.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.notification.dto.GovNotifyRequest;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;
import uk.gov.hmcts.darts.notification.exception.TemplateNotFoundException;
import uk.gov.hmcts.darts.notification.helper.TemplateIdHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.service.notify.NotificationClientException;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

@TestPropertySource(properties = {"darts.notification.disabled=true"})
class NotificationDisabledTest extends IntegrationBase {

    public static final String TEST_EMAIL_ADDRESS = "test@test.com";

    @Autowired
    private NotificationService notificationService;
    @MockBean
    private TemplateIdHelper templateIdHelper;
    @MockBean
    private GovNotifyService govNotifyService;

    @Test()
    void doesntSendNotificationWhenDisabled() throws TemplateNotFoundException, NotificationClientException {
        scheduleNotification();

        notificationService.sendNotificationToGovNotify();

        verify(govNotifyService, never()).sendNotification(any(GovNotifyRequest.class));
    }

    private void scheduleNotification() throws TemplateNotFoundException {
        var caseId = dartsDatabase.save(someMinimalCase()).getId();
        when(templateIdHelper.findTemplateId("some-template")).thenReturn("976bf288-705d-4cbb-b24f-c5529abf14cf");
        SaveNotificationToDbRequest request = SaveNotificationToDbRequest.builder()
            .eventId("some-template")
            .caseId(caseId)
            .emailAddresses(TEST_EMAIL_ADDRESS)
            .templateValues(Map.of("key1", "value1"))
            .build();
        notificationService.scheduleNotification(request);
    }
}
