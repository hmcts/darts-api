package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.buildUserWithRoleFor;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum.WORKING_DAYS_12;

class SentencingRemarksAndRetentionPolicyHandlerTest extends HandlerTestData {

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private NodeRegisterStub nodeRegisterStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void setUp() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtroomEntity courtroom = dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);
        nodeRegisterStub.setupNodeRegistry(courtroom);
        dartsGateway.darNotificationReturnsSuccess();
    }

    @Test
    void givenSentencingRemarksAndRetentionPolicyEventReceivedAndCourtCaseAndHearingDoesNotExist_thenNotifyDarUpdate() {
        eventDispatcher.receive(createSentencingRemarksDartsEventFor(SOME_COURTHOUSE));
        transactionalUtil.executeInTransaction(() -> {
            var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

            var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

            var persistedEvent = dartsDatabase.getAllEvents().getFirst();

            assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
            assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
            assertThat(hearingsForCase.size()).isEqualTo(1);
            assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);

            dartsGateway.verifyReceivedNotificationType(3);
            dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
        });
    }

    @Test
    void givenSentencingRemarksAndRetentionPolicyEventReceivedAndHearingDoesNotExist_thenNotifyDarUpdate() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(SOME_CASE_NUMBER, SOME_COURTHOUSE, SOME_ROOM);

        eventDispatcher.receive(createSentencingRemarksDartsEventFor(SOME_COURTHOUSE));
        transactionalUtil.executeInTransaction(() -> {
            var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

            var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

            var persistedEvent = dartsDatabase.getAllEvents().getFirst();

            assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
            assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
            assertThat(hearingsForCase.size()).isEqualTo(1);
            assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);

            dartsGateway.verifyReceivedNotificationType(3);
            dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
        });
    }


    @Test
    void givenSentencingRemarksAndRetentionPolicyEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdate() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(SOME_CASE_NUMBER, SOME_COURTHOUSE, SOME_ROOM);

        CourtroomEntity otherCourtroom = dartsDatabase.givenTheCourtHouseHasRoom(caseEntity.getCourthouse(), SOME_OTHER_ROOM);
        nodeRegisterStub.setupNodeRegistry(otherCourtroom);

        eventDispatcher.receive(createSentencingRemarksDartsEventFor(SOME_COURTHOUSE).courtroom(SOME_OTHER_ROOM));
        transactionalUtil.executeInTransaction(() -> {
            var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

            var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_OTHER_ROOM, HEARING_DATE_ODT.toLocalDate());

            var persistedEvent = dartsDatabase.getAllEvents().getFirst();

            assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM.toUpperCase(Locale.ROOT));
            assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
            assertThat(hearingsForCase.size()).isEqualTo(1);
            assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);

            assertTrue(dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());

            dartsGateway.verifyReceivedNotificationType(3);
            dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
        });
    }

    @Test
    void givenSentencingRemarksAndRetentionPolicyEventReceivedAndCaseAndHearingExistAndRoomHasNotChanged_thenDoNotNotifyDar() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            HEARING_DATE
        );

        eventDispatcher.receive(createSentencingRemarksDartsEventFor(SOME_COURTHOUSE));
        transactionalUtil.executeInTransaction(() -> {
            var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

            var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

            var persistedEvent = dartsDatabase.getAllEvents().getFirst();

            assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
            assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
            assertThat(hearingsForCase.size()).isEqualTo(1);
            assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);

            dartsGateway.verifyDoesntReceiveDarEvent();
        });
    }

    @Test
    void createsTranscriptionWithCorrectValues() {
        dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);
        var sentencingRemarksDartsEvent = createSentencingRemarksDartsEventFor(SOME_COURTHOUSE);

        eventDispatcher.receive(sentencingRemarksDartsEvent);

        var persistedTranscriptions = dartsDatabase.getTranscriptionRepository().findAll();
        assertThat(persistedTranscriptions).hasSize(1);
        var persistedTranscription = persistedTranscriptions.getFirst();
        assertThat(persistedTranscription.getStartTime()).isEqualTo(sentencingRemarksDartsEvent.getStartTime());
        assertThat(persistedTranscription.getEndTime()).isEqualTo(sentencingRemarksDartsEvent.getEndTime());
        assertThat(persistedTranscription.getHearing()).isNotNull();
        assertThat(persistedTranscription.getCourtCase().getCaseNumber()).isEqualTo(SOME_CASE_NUMBER);
        assertThat(persistedTranscription.getTranscriptionStatus().getId()).isEqualTo(APPROVED.getId());
        assertThat(persistedTranscription.getTranscriptionUrgency().getId()).isEqualTo(WORKING_DAYS_12.getId());

        var transcriptionWorkflows = dartsDatabase.getTranscriptionWorkflowRepository().findAll().stream()
            .filter(t -> SOME_CASE_NUMBER.equals(t.getTranscription().getCourtCase().getCaseNumber()))
            .toList();

        assertThat(transcriptionWorkflows).extracting("transcriptionStatus.id")
            .hasSameElementsAs(List.of(REQUESTED.getId(), APPROVED.getId()));

        var transcriptionComments = dartsDatabase.getTranscriptionCommentRepository().findAll();
        assertThat(transcriptionComments).hasSize(1);
        assertThat(transcriptionComments.getFirst().getComment()).isEqualTo("Transcription Automatically approved");
    }

    @Test
    void schedulesNotificationToTranscriptionCompany() {
        var courthouse = dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);
        var transcriber = givenATranscriberIsAuthorisedFor(courthouse);

        eventDispatcher.receive(createSentencingRemarksDartsEventFor(SOME_COURTHOUSE));

        var notifications = dartsDatabase.getNotificationFor(SOME_CASE_NUMBER);
        assertThat(notifications).extracting("emailAddress")
            .hasSameElementsAs(List.of(transcriber.getEmailAddress()));

    }

    @Test
    void storesRetentionInformation() {
        dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);
        var retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.setCaseRetentionFixedPolicy(RetentionPolicyEnum.DEFAULT.getPolicyKey());
        retentionPolicy.setCaseTotalSentence("20Y2M0D");
        eventDispatcher.receive(createSentencingRemarksWithRetentionDartsEventFor(SOME_COURTHOUSE, retentionPolicy));

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();

        assertEquals(1, caseManagementRetentionEntities.size());
        assertEquals(retentionPolicy.getCaseTotalSentence(), caseManagementRetentionEntities.getFirst().getTotalSentence());
        RetentionPolicyTypeEntity caseManagementRetentionPolicyType = dartsDatabase.getRetentionPolicyTypeRepository()
            .findById(caseManagementRetentionEntities.getFirst().getRetentionPolicyTypeEntity().getId())
            .get();
        assertEquals(retentionPolicy.getCaseRetentionFixedPolicy(), caseManagementRetentionPolicyType.getFixedPolicyKey());
    }


    @Test
    void doesNotStoreRetentionInformationIfNonePresent() {
        dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);

        eventDispatcher.receive(createSentencingRemarksWithRetentionDartsEventFor(SOME_COURTHOUSE, null));

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(0, caseManagementRetentionEntities.size());
    }

    @Test
    void ignoresDuplicateEvent() {
        dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);
        var sentencingRemarksDartsEvent = createSentencingRemarksDartsEventFor(SOME_COURTHOUSE);

        eventDispatcher.receive(sentencingRemarksDartsEvent);

        //receive same event again, but should save to db and ignore duplicate exception
        eventDispatcher.receive(sentencingRemarksDartsEvent);

        var persistedTranscriptions = dartsDatabase.getTranscriptionRepository().findAll();
        assertEquals(1, persistedTranscriptions.size());
        List<EventEntity> allEvents = dartsDatabase.getEventRepository().findAll();
        assertEquals(2, allEvents.size());
    }

    private UserAccountEntity givenATranscriberIsAuthorisedFor(CourthouseEntity courthouse) {
        var transcriber = buildUserWithRoleFor(TRANSCRIBER, courthouse);
        dartsDatabase.saveUserWithGroup(transcriber);
        return transcriber;
    }

    private DartsEvent createSentencingRemarksDartsEventFor(String courthouseName) {
        var startTime = OffsetDateTime.parse("2023-06-13T08:13:09Z");
        var endTime = startTime.plusHours(2);
        return new DartsEvent()
            .messageId("some-message-id")
            .type("40750")
            .subType("11527")
            .courthouse(courthouseName)
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .courtroom(SOME_ROOM)
            .dateTime(HEARING_DATE_ODT)
            .startTime(startTime)
            .endTime(endTime);
    }

    private DartsEvent createSentencingRemarksWithRetentionDartsEventFor(String courthouseName, DartsEventRetentionPolicy retentionPolicy) {
        DartsEvent event = this.createSentencingRemarksDartsEventFor(courthouseName);
        event.setRetentionPolicy(retentionPolicy);
        return event;
    }

}
