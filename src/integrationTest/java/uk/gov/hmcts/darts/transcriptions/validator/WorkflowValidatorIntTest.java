package uk.gov.hmcts.darts.transcriptions.validator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;

import java.time.OffsetDateTime;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.APPROVED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.AWAITING_AUTHORISATION;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.CLOSED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.COMPLETE;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REJECTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.REQUESTED;
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@AutoConfigureMockMvc
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LawOfDemeter"})
class WorkflowValidatorIntTest extends IntegrationBase {

    @Autowired
    private WorkflowValidator workflowValidator;

    private CourtCaseEntity courtCase;
    private CourtroomEntity courtroom;
    private HearingEntity hearing;
    private UserAccountEntity testUser;


    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";

    @BeforeEach
    void setUp() {
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(SOME_COURTHOUSE);

        courtroom = createCourtRoomWithNameAtCourthouse(courthouse, SOME_COURTROOM);

        courtCase = createCaseAt(courthouse);
        courtCase.setCaseNumber("Case1");

        JudgeEntity judge = createJudgeWithName("aJudge");
        OffsetDateTime yesterday = OffsetDateTime.now(UTC).minusDays(1).withHour(9).withMinute(0).withSecond(0);

        hearing = createHearingWithDefaults(courtCase, courtroom, yesterday.toLocalDate(), judge);

        dartsDatabase.saveAll(hearing);

        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
    }

    @ParameterizedTest
    @Order(1)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, REQUESTED)
        );
    }

    @ParameterizedTest
    @Order(2)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusRequestedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, REQUESTED)
        );
    }

    @ParameterizedTest
    @Order(3)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(transcriptionTypeEnum),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, AWAITING_AUTHORISATION));
    }

    @ParameterizedTest
    @Order(4)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {
        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.SENTENCING_REMARKS, currentTranscriptionStatus, AWAITING_AUTHORISATION)
        );
    }

    @ParameterizedTest
    @Order(5)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusAwaitingAuthorisationReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, AWAITING_AUTHORISATION)
        );
    }

    @ParameterizedTest
    @Order(6)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToApprovedWorkflowStatusSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(transcriptionTypeEnum),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, APPROVED));
    }

    @Test
    @Order(7)
    void validateAutomaticChangeToApprovedWorkflowStatusSuccess() {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(TranscriptionTypeEnum.OTHER),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, APPROVED));
    }

    @ParameterizedTest
    @Order(8)
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.INCLUDING_VERDICT, currentTranscriptionStatus, APPROVED)
        );
    }

    @ParameterizedTest
    @Order(9)
    @EnumSource(names = {"AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToApprovedWorkflowStatusReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, APPROVED)
        );
    }

    @ParameterizedTest
    @Order(10)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusRejectedSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(transcriptionTypeEnum),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(AWAITING_AUTHORISATION),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, REJECTED));
    }

    @ParameterizedTest
    @Order(11)
    @EnumSource(names = {"REQUESTED", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.SENTENCING_REMARKS, currentTranscriptionStatus, REJECTED)
        );
    }

    @ParameterizedTest
    @Order(12)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusRejectedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, REJECTED)
        );
    }

    @ParameterizedTest
    @Order(13)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusWithTranscriberSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(transcriptionTypeEnum),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, WITH_TRANSCRIBER));
    }

    @Test
    @Order(14)
    void validateAutomaticChangeToWorkflowStatusWithTranscriberSuccess() {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(TranscriptionTypeEnum.OTHER),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(APPROVED),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, WITH_TRANSCRIBER));
    }

    @ParameterizedTest
    @Order(15)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, WITH_TRANSCRIBER)
        );
    }

    @ParameterizedTest
    @Order(16)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "REJECTED", "WITH_TRANSCRIBER", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusWithTranscriberReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, WITH_TRANSCRIBER)
        );
    }

    @ParameterizedTest
    @Order(17)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusCompleteSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(transcriptionTypeEnum),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, COMPLETE));
    }

    @Test
    @Order(18)
    void validateAutomaticChangeToWorkflowStatusCompleteSuccess() {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(TranscriptionTypeEnum.OTHER),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(WITH_TRANSCRIBER),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, COMPLETE));
    }

    @ParameterizedTest
    @Order(19)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED"})
    void validateManualChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, COMPLETE)
        );
    }

    @ParameterizedTest
    @Order(20)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "REJECTED", "COMPLETE", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusCompleteReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, COMPLETE)
        );
    }

    @ParameterizedTest
    @Order(21)
    @EnumSource(names = {"REQUESTED", "AWAITING_AUTHORISATION", "APPROVED", "WITH_TRANSCRIBER"})
    void validateManualChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(TranscriptionTypeEnum.ANTECEDENTS),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(currentTranscriptionStatus),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.fromId(transcription.getTranscriptionType().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, CLOSED));
    }

    @ParameterizedTest
    @Order(21)
    @EnumSource(names = {"REQUESTED", "APPROVED", "WITH_TRANSCRIBER"})
    void validateAutomaticChangeToWorkflowStatusClosedSuccess(TranscriptionStatusEnum currentTranscriptionStatus) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(TranscriptionTypeEnum.OTHER),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(currentTranscriptionStatus),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.fromId(transcription.getTranscriptionType().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, CLOSED));
    }

    @ParameterizedTest
    @Order(22)
    @EnumSource(names = {"REJECTED", "CLOSED"})
    void validateManualChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.COURT_LOG, currentTranscriptionStatus, CLOSED)
        );
    }

    @ParameterizedTest
    @Order(23)
    @EnumSource(names = {"REJECTED", "CLOSED"})
    void validateAutomaticChangeToWorkflowStatusClosedReturnsFalse(TranscriptionStatusEnum currentTranscriptionStatus) {

        assertFalse(
            workflowValidator.validateChangeToWorkflowStatus(TranscriptionTypeEnum.OTHER, currentTranscriptionStatus, CLOSED)
        );
    }

    @ParameterizedTest
    @Order(24)
    @EnumSource(names = {"SENTENCING_REMARKS", "INCLUDING_VERDICT", "ANTECEDENTS", "ARGUMENT_AND_SUBMISSION_OF_RULING", "COURT_LOG",
        "MITIGATION", "PROCEEDINGS_AFTER_VERDICT", "PROSECUTION_OPENING_OF_FACTS", "SPECIFIED_TIMES"})
    void validateManualChangeToWorkflowStatusRequestedSuccess(TranscriptionTypeEnum transcriptionTypeEnum) {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(transcriptionTypeEnum),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionStatusEnum currentTranscriptionStatus = TranscriptionStatusEnum.fromId(transcription.getTranscriptionStatus().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, currentTranscriptionStatus, REQUESTED));
    }

    @Test
    @Order(25)
    void validateAutomaticChangeToWorkflowStatusRequestedSuccess() {

        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createAndSaveTranscriptionEntity(
            courtCase,
            courtroom,
            hearing,
            dartsDatabase.getTranscriptionStub().getTranscriptionTypeByEnum(TranscriptionTypeEnum.OTHER),
            dartsDatabase.getTranscriptionStub().getTranscriptionStatusByEnum(REQUESTED),
            dartsDatabase.getTranscriptionStub().getTranscriptionUrgencyByEnum(TranscriptionUrgencyEnum.STANDARD),
            testUser
        );

        TranscriptionTypeEnum transcriptionTypeEnum = TranscriptionTypeEnum.fromId(transcription.getTranscriptionType().getId());

        assertTrue(workflowValidator.validateChangeToWorkflowStatus(transcriptionTypeEnum, REQUESTED, REQUESTED));
    }
}
