package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.enums.NotificationStatus;
import uk.gov.service.notify.NotificationClientException;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationLoggerServiceTest {

    private static final Long NOTIFICATION_ID = 2222L;
    private static final Integer CASE_ID = 1111;

    private static final String EVENT_ID = "event_id";
    private static final String TEMPLATE_ID = "a_template_id";
    private static final Integer ATTEMPTS = 1;
    private static final Integer TOO_MANY_ATTEMPTS = 10;
    private static final String ERROR_MESSAGE = "an_error_message";

    NotificationLoggerService notificationLoggerService;

    private static LogCaptor logCaptor;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(NotificationLoggerServiceImpl.class);
        logCaptor.setLogLevelToInfo();
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @BeforeEach
    void setUp() {
        notificationLoggerService = new NotificationLoggerServiceImpl();
    }

    @Test
    void testScheduleNotification() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setId(NOTIFICATION_ID);
        notificationEntity.setEventId(EVENT_ID);
        notificationEntity.setStatus(NotificationStatus.OPEN);
        notificationLoggerService.scheduleNotification(notificationEntity, CASE_ID);

        String expectedText = String.format("Notification scheduled: notificationId=%s, type=%s, caseId=%s, status=%s",
                                            NOTIFICATION_ID, EVENT_ID, CASE_ID, NotificationStatus.OPEN);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedText);
    }

    @Test
    void testSendingNotification() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setId(NOTIFICATION_ID);
        notificationEntity.setEventId(EVENT_ID);
        notificationEntity.setStatus(NotificationStatus.PROCESSING);
        notificationEntity.setAttempts(ATTEMPTS);
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(CASE_ID);
        notificationEntity.setCourtCase(courtCaseEntity);
        notificationLoggerService.sendingNotification(notificationEntity, TEMPLATE_ID, notificationEntity.getAttempts());

        String expectedText = String.format("Notification sending: notificationId=%s, type=%s, caseId=%s, templateId=%s, status=%s, attemptNo=%s",
                                            NOTIFICATION_ID, EVENT_ID, CASE_ID, TEMPLATE_ID, NotificationStatus.PROCESSING, ATTEMPTS);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedText);
    }

    @Test
    void testSentNotification() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setId(NOTIFICATION_ID);
        notificationEntity.setEventId(EVENT_ID);
        notificationEntity.setStatus(NotificationStatus.SENT);
        notificationEntity.setAttempts(ATTEMPTS);
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(CASE_ID);
        notificationEntity.setCourtCase(courtCaseEntity);
        notificationLoggerService.sentNotification(notificationEntity, TEMPLATE_ID, ATTEMPTS);

        String expectedText = String.format("Notification sent: notificationId=%s, type=%s, caseId=%s, templateId=%s, status=%s, attemptNo=%s",
                                            NOTIFICATION_ID, EVENT_ID, CASE_ID, TEMPLATE_ID, NotificationStatus.SENT, ATTEMPTS);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedText);
    }

    @Test
    void testRetryNotification() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setId(NOTIFICATION_ID);
        notificationEntity.setEventId(EVENT_ID);
        notificationEntity.setStatus(NotificationStatus.PROCESSING);
        notificationEntity.setAttempts(ATTEMPTS);
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(CASE_ID);
        notificationEntity.setCourtCase(courtCaseEntity);
        notificationLoggerService.errorRetryingNotification(notificationEntity, TEMPLATE_ID, new NotificationClientException(ERROR_MESSAGE));

        String expectedText = String.format("Notification GovNotify error, retrying: " +
                        "notificationId=%s, type=%s, caseId=%s, templateId=%s, status=%s, attemptNo=%s, error=%s",
                        NOTIFICATION_ID, EVENT_ID, CASE_ID, TEMPLATE_ID, NotificationStatus.PROCESSING, ATTEMPTS, ERROR_MESSAGE);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedText);
    }

    @Test
    void testFailedNotification() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setId(NOTIFICATION_ID);
        notificationEntity.setEventId(EVENT_ID);
        notificationEntity.setStatus(NotificationStatus.FAILED);
        notificationEntity.setAttempts(TOO_MANY_ATTEMPTS);
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(CASE_ID);
        notificationEntity.setCourtCase(courtCaseEntity);
        notificationLoggerService.failedNotification(notificationEntity, TEMPLATE_ID, new NotificationClientException(ERROR_MESSAGE));

        String expectedText = String.format("Notification failed to send: " +
                        "notificationId=%s, type=%s, caseId=%s, templateId=%s, status=%s, attemptNo=%s, error=%s",
                        NOTIFICATION_ID, EVENT_ID, CASE_ID, TEMPLATE_ID, NotificationStatus.FAILED, TOO_MANY_ATTEMPTS, ERROR_MESSAGE);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedText);
    }

}
