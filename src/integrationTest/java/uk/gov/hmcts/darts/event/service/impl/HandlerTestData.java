package uk.gov.hmcts.darts.event.service.impl;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.TooFewActualInvocations;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.event.client.DartsGatewayClient;
import uk.gov.hmcts.darts.event.enums.DarNotifyType;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithGatewayStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class HandlerTestData extends IntegrationBaseWithGatewayStub {

    protected static final String SOME_COURTHOUSE = "some-courthouse";
    protected static final String UNKNOWN_COURTHOUSE = "unknown-courthouse";
    protected static final String SOME_ROOM = "some-room";
    protected static final String SOME_OTHER_ROOM = "some-other-room";

    protected static final String SOME_CASE_NUMBER = "CASE1";
    protected static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 23, 10, 0, 0);
    protected static final OffsetDateTime HEARING_DATE_ODT = HEARING_DATE.atOffset(ZoneOffset.UTC);

    protected final OffsetDateTime today = now();

    protected static final String DAR_NOTIFY_URL = "http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx";

    @MockBean
    protected DartsGatewayClient dartsGatewayClient;

    @Captor
    protected ArgumentCaptor<DarNotifyEvent> darNotifyEventArgumentCaptor;

    @SuppressWarnings("PMD.DoNotUseThreads")
    protected void verifyDarNotificationNotReceived() throws InterruptedException {
        Thread.sleep(1000);
        Mockito.verify(dartsGatewayClient, times(0)).darNotify(any(DarNotifyEvent.class));
    }

    protected void verifyDarNotificationCount(int count) {
        await().until(() -> {
            try {
                Mockito.verify(dartsGatewayClient, times(count)).darNotify(darNotifyEventArgumentCaptor.capture());
                return true;
            }  catch (TooFewActualInvocations ex) {
                return false;
            }
        });
    }

    protected void verifyDarNotification(DarNotifyEvent darNotifyEvent,
                                         DarNotifyType type,
                                         String courthouse,
                                         String courtroom) {
        assertEquals(type.getNotificationType(), darNotifyEvent.getNotificationType());
        assertEquals(DAR_NOTIFY_URL, darNotifyEvent.getNotificationUrl());
        assertEquals(courthouse, darNotifyEvent.getCourthouse());
        assertEquals(courtroom, darNotifyEvent.getCourtroom());
    }

    protected void verifyDarNotifications(List<DarNotifyEvent> actualNotifications, List<DarNotifyType> expectedTypes, String courtroom) {
        for (DarNotifyType darNotifyType : expectedTypes) {
            var foundNotification = actualNotifications.stream()
                .filter(notification -> notification.getNotificationType().equals(darNotifyType.getNotificationType()))
                .findFirst();
            assertTrue(foundNotification.isPresent());
            verifyDarNotification(foundNotification.get(), darNotifyType, SOME_COURTHOUSE, courtroom);
        }
    }
}
