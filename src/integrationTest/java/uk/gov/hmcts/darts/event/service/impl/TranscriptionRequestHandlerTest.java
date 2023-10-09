package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TranscriptionRequestHandlerTest extends IntegrationBase {

    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String SWANSEA_COURTHOUSE = "SWANSEA";
    private static final String SOME_COURTROOM = "courtroom1";

    @Autowired
    private EventDispatcher eventDispatcher;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of("test.user@example.com"))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        dartsDatabase.createTestUserAccount();
    }

    @Test
    @SuppressWarnings({"PMD.LawOfDemeter"})
    void successSaveToDatabase() {
        dartsDatabase.createCourthouseUnlessExists("Swansea");
        var startTime = OffsetDateTime.of(2023, 6, 13, 8, 13, 9, 0, ZoneOffset.UTC);
        var endTime = startTime.plusHours(2);
        DartsEvent dartsEvent = new DartsEvent()
            .messageId("some-message-id")
            .type("3010")
            .courthouse(SWANSEA_COURTHOUSE)
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .courtroom(SOME_COURTROOM)
            .startTime(startTime)
            .endTime(endTime);


        eventDispatcher.receive(dartsEvent);

        var persistedTranscriptions = dartsDatabase.getTranscriptionRepository().findAll();
        TranscriptionEntity persistedTranscription = persistedTranscriptions.get(0);
        assertEquals(startTime, persistedTranscription.getStartTime());
        assertEquals(endTime, persistedTranscription.getEndTime());
        assertNotNull(persistedTranscription.getHearing());
        assertEquals(SOME_CASE_NUMBER, persistedTranscription.getCourtCase().getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), persistedTranscription.getTranscriptionStatus().getId());


        var persistedTranscriptionWorkflows = dartsDatabase.getTranscriptionWorkflowRepository().findAll();
        TranscriptionWorkflowEntity persistedTranscriptionWorkflow = persistedTranscriptionWorkflows.get(1);
        assertEquals(
            TranscriptionStatusEnum.APPROVED.getId(),
            persistedTranscriptionWorkflow.getTranscriptionStatus().getId()
        );
        assertEquals("Transcription Automatically approved", persistedTranscriptionWorkflow.getWorkflowComment());

    }

}
