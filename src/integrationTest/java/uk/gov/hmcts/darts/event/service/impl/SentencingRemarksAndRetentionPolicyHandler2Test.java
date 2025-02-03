package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;
import uk.gov.hmcts.darts.transcriptions.api.TranscriptionsApi;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionRequestDetails;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

class SentencingRemarksAndRetentionPolicyHandler2Test extends IntegrationBaseWithWiremock {

    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String SWANSEA_COURTHOUSE = "SWANSEA";
    private static final String SOME_COURTROOM = "courtroom1";
    private static final String REQUESTER_EMAIL = "test.user@example.com";


    @MockitoBean
    TranscriptionsApi transcriptionsApi;

    @Autowired
    private EventDispatcher eventDispatcher;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(REQUESTER_EMAIL))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        dartsDatabase.createTestUserAccount();
    }

    //Handle a generic DartsException that is not a Duplicate one.
    @Test
    void handleNonDuplicateDartsException() {

        when(transcriptionsApi.saveTranscriptionRequest(any(TranscriptionRequestDetails.class), anyBoolean()))
            .thenThrow(new DartsApiException(TranscriptionApiError.AUDIO_NOT_FOUND, "Test"));
        dartsDatabase.createCourthouseUnlessExists(SWANSEA_COURTHOUSE);
        var sentencingRemarksDartsEvent = createSentencingRemarksDartsEventFor(SWANSEA_COURTHOUSE);

        assertThrows(DartsApiException.class, () ->
            eventDispatcher.receive(sentencingRemarksDartsEvent));

    }


    private DartsEvent createSentencingRemarksDartsEventFor(String courthouseName) {
        var eventTime = OffsetDateTime.parse("2023-07-01T01:00:09Z");
        var startTime = OffsetDateTime.parse("2023-06-13T08:13:09Z");
        var endTime = startTime.plusHours(2);
        return new DartsEvent()
            .messageId("some-message-id")
            .type("40735")
            .subType("10808")
            .courthouse(courthouseName)
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .courtroom(SOME_COURTROOM)
            .dateTime(eventTime)
            .startTime(startTime)
            .endTime(endTime);
    }

}