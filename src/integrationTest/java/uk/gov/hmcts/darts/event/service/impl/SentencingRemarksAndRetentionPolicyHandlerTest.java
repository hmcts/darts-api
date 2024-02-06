package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.testutils.data.UserAccountTestData.buildUserWithRoleFor;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.STANDARD;

class SentencingRemarksAndRetentionPolicyHandlerTest extends IntegrationBase {

    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String SWANSEA_COURTHOUSE = "SWANSEA";
    private static final String SOME_COURTROOM = "courtroom1";
    private static final String REQUESTER_EMAIL = "test.user@example.com";


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

    @Test
    void createsTranscriptionWithCorrectValues() {
        dartsDatabase.createCourthouseUnlessExists(SWANSEA_COURTHOUSE);
        var sentencingRemarksDartsEvent = createSentencingRemarksDartsEventFor(SWANSEA_COURTHOUSE);

        eventDispatcher.receive(sentencingRemarksDartsEvent);

        var persistedTranscriptions = dartsDatabase.getTranscriptionRepository().findAll();
        assertThat(persistedTranscriptions).hasSize(1);
        var persistedTranscription = persistedTranscriptions.get(0);
        assertThat(persistedTranscription.getStartTime()).isEqualTo(sentencingRemarksDartsEvent.getStartTime());
        assertThat(persistedTranscription.getEndTime()).isEqualTo(sentencingRemarksDartsEvent.getEndTime());
        assertThat(persistedTranscription.getHearing()).isNotNull();
        assertThat(persistedTranscription.getCourtCase().getCaseNumber()).isEqualTo(SOME_CASE_NUMBER);
        assertThat(persistedTranscription.getTranscriptionStatus().getId()).isEqualTo(APPROVED.getId());
        assertThat(persistedTranscription.getTranscriptionUrgency().getId()).isEqualTo(STANDARD.getId());
    }

    @Test
    void createsTranscriptionWorkflowWithCorrectValues() {
        dartsDatabase.createCourthouseUnlessExists(SWANSEA_COURTHOUSE);
        var sentencingRemarksDartsEvent = createSentencingRemarksDartsEventFor(SWANSEA_COURTHOUSE);

        eventDispatcher.receive(sentencingRemarksDartsEvent);

        var transcriptionWorkflows = dartsDatabase.getTranscriptionWorkflowRepository().findAll().stream()
              .filter(t -> SOME_CASE_NUMBER.equals(t.getTranscription().getCourtCase().getCaseNumber()))
              .toList();

        assertThat(transcriptionWorkflows).extracting("transcriptionStatus.id")
              .hasSameElementsAs(List.of(REQUESTED.getId(), APPROVED.getId()));
    }

    @Test
    void createsTranscriptionCommentsWithCorrectValues() {
        dartsDatabase.createCourthouseUnlessExists(SWANSEA_COURTHOUSE);
        var sentencingRemarksDartsEvent = createSentencingRemarksDartsEventFor(SWANSEA_COURTHOUSE);

        eventDispatcher.receive(sentencingRemarksDartsEvent);

        var transcriptionComments = dartsDatabase.getTranscriptionCommentRepository().findAll();
        assertThat(transcriptionComments).hasSize(1);
        assertThat(transcriptionComments.get(0).getComment()).isEqualTo("Transcription Automatically approved");
    }

    @Test
    void schedulesNotificationToTranscriptionCompany() {
        var courthouse = dartsDatabase.createCourthouseUnlessExists(SWANSEA_COURTHOUSE);
        var transcriber = givenATranscriberIsAuthorisedFor(courthouse);

        eventDispatcher.receive(createSentencingRemarksDartsEventFor(SWANSEA_COURTHOUSE));

        var notifications = dartsDatabase.getNotificationFor(SOME_CASE_NUMBER);
        assertThat(notifications).extracting("emailAddress")
              .hasSameElementsAs(List.of(transcriber.getEmailAddress(), REQUESTER_EMAIL));

    }

    private UserAccountEntity givenATranscriberIsAuthorisedFor(CourthouseEntity courthouse) {
        var transcriber = buildUserWithRoleFor(TRANSCRIBER, courthouse);
        dartsDatabase.saveUserWithGroup(transcriber);
        dartsDatabase.addToUserAccountTrash(transcriber.getEmailAddress());
        dartsDatabase.addToTrash(transcriber.getSecurityGroupEntities());
        return transcriber;
    }

    private DartsEvent createSentencingRemarksDartsEventFor(String courthouseName) {
        var eventTime = OffsetDateTime.parse("2023-07-01T01:00:09Z");
        var startTime = OffsetDateTime.parse("2023-06-13T08:13:09Z");
        var endTime = startTime.plusHours(2);
        return new DartsEvent()
              .messageId("some-message-id")
              .type("40730")
              .subType("10808")
              .courthouse(courthouseName)
              .caseNumbers(List.of(SOME_CASE_NUMBER))
              .courtroom(SOME_COURTROOM)
              .dateTime(eventTime)
              .startTime(startTime)
              .endTime(endTime);
    }

}
