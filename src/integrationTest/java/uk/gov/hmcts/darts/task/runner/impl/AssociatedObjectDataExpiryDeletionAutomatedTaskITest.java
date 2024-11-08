package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.TriConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity_;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.task.runner.HasIntegerId;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("UnstructuredDataExpiryDeletionAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class AssociatedObjectDataExpiryDeletionAutomatedTaskITest extends PostgresIntegrationBase {
    private final AssociatedObjectDataExpiryDeletionAutomatedTask associatedObjectDataExpiryDeletionAutomatedTask;

    //TranscriptionDocumentEntity
    @Test
    void positiveTranscriptionDocumentEntityExpired() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        TranscriptionDocuments transcriptionDocuments = setupTranscriptionDocuments(hearingEntity);

        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity1, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity2, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity3, true);

        runTask();

        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity1, true);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity2, false);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity3, true);
    }

    @Test
    void positiveTranscriptionDocumentEntityExpiredButDoesNotHaveArm() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        TranscriptionDocuments transcriptionDocuments = setupTranscriptionDocuments(hearingEntity);

        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity1, false);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity2, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity3, true);

        runTask();

        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity1, false);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity2, false);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity3, true);


    }

    @Test
    void positiveTranscriptionDocumentEntityExpiredButArmNotStored() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        TranscriptionDocuments transcriptionDocuments = setupTranscriptionDocuments(hearingEntity);

        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity1, true, false);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity2, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity3, true);

        runTask();

        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity1, false);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity2, false);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity3, true);
    }

    //MediaEntity

    @Test
    void positiveMediaEntityExpired() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        MediaEntries mediaEntries = setupMediaEntries(hearingEntity);

        assignExternalObjectDirectory(mediaEntries.mediaEntity1, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity2, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity3, true);

        runTask();

        assertMediaEntity(mediaEntries.mediaEntity1, true);
        assertMediaEntity(mediaEntries.mediaEntity2, false);
        assertMediaEntity(mediaEntries.mediaEntity3, true);

    }

    @Test
    void positiveMediaEntityExpiredButDoesNotHaveArm() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        MediaEntries mediaEntries = setupMediaEntries(hearingEntity);

        assignExternalObjectDirectory(mediaEntries.mediaEntity1, false);
        assignExternalObjectDirectory(mediaEntries.mediaEntity2, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity3, true);

        runTask();

        assertMediaEntity(mediaEntries.mediaEntity1, false);
        assertMediaEntity(mediaEntries.mediaEntity2, false);
        assertMediaEntity(mediaEntries.mediaEntity3, true);

    }

    @Test
    void positiveMediaEntityExpiredButArmNotStored() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        MediaEntries mediaEntries = setupMediaEntries(hearingEntity);

        assignExternalObjectDirectory(mediaEntries.mediaEntity1, true, false);
        assignExternalObjectDirectory(mediaEntries.mediaEntity2, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity3, true);

        runTask();

        assertMediaEntity(mediaEntries.mediaEntity1, false);
        assertMediaEntity(mediaEntries.mediaEntity2, false);
        assertMediaEntity(mediaEntries.mediaEntity3, true);

    }

    //Annotation Document

    @Test
    @SneakyThrows
    void positiveAnnotationDocumentEntityExpired() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        AnnotationDocuments annotationDocuments = setupAnnotationDocuments(hearingEntity);

        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity1, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity2, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity3, true);

        runTask();

        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity1, true);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity2, false);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity3, true);
    }


    @Test
    void positiveAnnotationDocumentEntityExpiredButDoesNotHaveArm() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        AnnotationDocuments annotationDocuments = setupAnnotationDocuments(hearingEntity);

        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity1, false);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity2, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity3, true);

        runTask();

        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity1, false);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity2, false);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity3, true);

    }

    @Test
    void positiveAnnotationDocumentEntityExpiredButArmNotStored() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));
        AnnotationDocuments annotationDocuments = setupAnnotationDocuments(hearingEntity);

        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity1, true, false);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity2, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity3, true);

        runTask();

        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity1, false);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity2, false);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity3, true);

    }

    //Case Document


    @Test
    void positiveCaseDocumentEntityExpired() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CaseDocuments caseDocuments = setupCaseDocuments(courtCaseEntity);

        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity1, true);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity2, true);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity3, true);

        runTask();

        assertCaseDocument(caseDocuments.caseDocumentEntity1, true);
        assertCaseDocument(caseDocuments.caseDocumentEntity2, false);
        assertCaseDocument(caseDocuments.caseDocumentEntity3, true);
    }

    @Test
    void positiveCaseDocumentEntityExpiredButDoesNotHaveArm() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CaseDocuments caseDocuments = setupCaseDocuments(courtCaseEntity);

        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity1, false);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity2, true);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity3, true);

        runTask();

        assertCaseDocument(caseDocuments.caseDocumentEntity1, false);
        assertCaseDocument(caseDocuments.caseDocumentEntity2, false);
        assertCaseDocument(caseDocuments.caseDocumentEntity3, true);
    }

    @Test
    void positiveCaseDocumentEntityExpiredButDoesArmNotStored() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CaseDocuments caseDocuments = setupCaseDocuments(courtCaseEntity);

        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity1, true, false);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity2, true);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity3, true);

        runTask();

        assertCaseDocument(caseDocuments.caseDocumentEntity1, false);
        assertCaseDocument(caseDocuments.caseDocumentEntity2, false);
        assertCaseDocument(caseDocuments.caseDocumentEntity3, true);
    }

    //Misc
    @Test
    void positiveAllExpired() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        CourtroomEntity courtroomEntity = dartsDatabase.createCourtroomUnlessExists("Bristol", "Courtroom1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub().createHearing(courtCaseEntity, courtroomEntity, LocalDateTime.now().minusDays(1));

        final CaseDocuments caseDocuments = setupCaseDocuments(courtCaseEntity);
        final AnnotationDocuments annotationDocuments = setupAnnotationDocuments(hearingEntity);
        final MediaEntries mediaEntries = setupMediaEntries(hearingEntity);
        final TranscriptionDocuments transcriptionDocuments = setupTranscriptionDocuments(hearingEntity);

        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity1, true);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity2, true);
        assignExternalObjectDirectory(caseDocuments.caseDocumentEntity3, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity1, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity2, true);
        assignExternalObjectDirectory(annotationDocuments.annotationDocumentEntity3, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity1, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity2, true);
        assignExternalObjectDirectory(mediaEntries.mediaEntity3, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity1, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity2, true);
        assignExternalObjectDirectory(transcriptionDocuments.transcriptionDocumentEntity3, true);

        runTask();
        assertCaseDocument(caseDocuments.caseDocumentEntity1, true);
        assertCaseDocument(caseDocuments.caseDocumentEntity2, false);
        assertCaseDocument(caseDocuments.caseDocumentEntity3, true);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity1, true);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity2, false);
        assertAnnotationDocument(annotationDocuments.annotationDocumentEntity3, true);
        assertMediaEntity(mediaEntries.mediaEntity1, true);
        assertMediaEntity(mediaEntries.mediaEntity2, false);
        assertMediaEntity(mediaEntries.mediaEntity3, true);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity1, true);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity2, false);
        assertTranscriptionDocument(transcriptionDocuments.transcriptionDocumentEntity3, true);
    }

    //Support
    record CaseDocuments(CaseDocumentEntity caseDocumentEntity1, CaseDocumentEntity caseDocumentEntity2, CaseDocumentEntity caseDocumentEntity3) {
    }

    record AnnotationDocuments(AnnotationDocumentEntity annotationDocumentEntity1, AnnotationDocumentEntity annotationDocumentEntity2,
                               AnnotationDocumentEntity annotationDocumentEntity3) {
    }

    record MediaEntries(MediaEntity mediaEntity1, MediaEntity mediaEntity2, MediaEntity mediaEntity3) {
    }

    record TranscriptionDocuments(TranscriptionDocumentEntity transcriptionDocumentEntity1, TranscriptionDocumentEntity transcriptionDocumentEntity2,
                                  TranscriptionDocumentEntity transcriptionDocumentEntity3) {
    }


    private CaseDocuments setupCaseDocuments(CourtCaseEntity courtCaseEntity) {
        CaseDocumentEntity caseDocumentEntity1 = dartsDatabase
            .getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, (caseDocument) -> {
                caseDocument.setDeleted(false);
                caseDocument.setRetainUntilTs(OffsetDateTime.now().minusHours(1));
            });
        CaseDocumentEntity caseDocumentEntity2 = dartsDatabase
            .getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, (caseDocument) -> {
                caseDocument.setDeleted(false);
                caseDocument.setRetainUntilTs(OffsetDateTime.now().plusDays(1));
            });
        CaseDocumentEntity caseDocumentEntity3 = dartsDatabase
            .getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, (caseDocument) -> {
                caseDocument.setDeleted(false);
                caseDocument.setRetainUntilTs(OffsetDateTime.now().minusHours(1));
            });
        return new CaseDocuments(caseDocumentEntity1, caseDocumentEntity2, caseDocumentEntity3);
    }


    private AnnotationDocuments setupAnnotationDocuments(HearingEntity hearingEntity) {
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationEntityWith(dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity(),
                                               "Some Annotation Text", hearingEntity);
        AnnotationDocumentEntity annotationDocumentEntity1 = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntity(annotation);
        annotationDocumentEntity1.setDeleted(false);
        annotationDocumentEntity1.setRetainUntilTs(OffsetDateTime.now().minusHours(1));

        AnnotationDocumentEntity annotationDocumentEntity2 = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntity(annotation);
        annotationDocumentEntity2.setDeleted(false);
        annotationDocumentEntity2.setRetainUntilTs(OffsetDateTime.now().plusDays(1));

        AnnotationDocumentEntity annotationDocumentEntity3 = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntity(annotation);
        annotationDocumentEntity3.setDeleted(false);
        annotationDocumentEntity3.setRetainUntilTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.getAnnotationDocumentRepository().saveAll(
            List.of(annotationDocumentEntity1, annotationDocumentEntity2, annotationDocumentEntity3));
        return new AnnotationDocuments(annotationDocumentEntity1, annotationDocumentEntity2, annotationDocumentEntity3);
    }


    private MediaEntries setupMediaEntries(HearingEntity hearingEntity) {
        MediaEntity mediaEntity1 = PersistableFactory.getMediaTestData()
            .createMediaWith(hearingEntity.getCourtroom(), OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), 1);
        mediaEntity1.setDeleted(false);
        mediaEntity1.setRetainUntilTs(OffsetDateTime.now().minusHours(1));

        MediaEntity mediaEntity2 = PersistableFactory.getMediaTestData()
            .createMediaWith(hearingEntity.getCourtroom(), OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), 1);
        mediaEntity2.setDeleted(false);
        mediaEntity2.setRetainUntilTs(OffsetDateTime.now().plusDays(1));

        MediaEntity mediaEntity3 = PersistableFactory.getMediaTestData()
            .createMediaWith(hearingEntity.getCourtroom(), OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), 1);
        mediaEntity3.setDeleted(false);
        mediaEntity3.setRetainUntilTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.saveAll(mediaEntity1, mediaEntity2, mediaEntity3);
        return new MediaEntries(mediaEntity1, mediaEntity2, mediaEntity3);
    }


    private TranscriptionDocuments setupTranscriptionDocuments(HearingEntity hearingEntity) {
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(
            hearingEntity, dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());

        TranscriptionDocumentEntity transcriptionDocumentEntity1
            = dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(
            transcription, dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        transcriptionDocumentEntity1.setDeleted(false);
        transcriptionDocumentEntity1.setRetainUntilTs(OffsetDateTime.now().minusHours(1));

        TranscriptionDocumentEntity transcriptionDocumentEntity2
            = dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(
            transcription, dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        transcriptionDocumentEntity2.setDeleted(false);
        transcriptionDocumentEntity2.setRetainUntilTs(OffsetDateTime.now().plusDays(1));

        TranscriptionDocumentEntity transcriptionDocumentEntity3
            = dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(
            transcription, dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity());
        transcriptionDocumentEntity3.setDeleted(false);
        transcriptionDocumentEntity3.setRetainUntilTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.getTranscriptionDocumentRepository().saveAll(
            List.of(transcriptionDocumentEntity1, transcriptionDocumentEntity2, transcriptionDocumentEntity3));
        return new TranscriptionDocuments(transcriptionDocumentEntity1, transcriptionDocumentEntity2, transcriptionDocumentEntity3);
    }


    private void runTask() {
        dartsDatabase.getTransactionalUtil().executeInTransaction(() -> {
            associatedObjectDataExpiryDeletionAutomatedTask.preRunTask();
            associatedObjectDataExpiryDeletionAutomatedTask.runTask();
        });
    }

    private void assertCaseDocument(CaseDocumentEntity providedCaseDocumentEntity, boolean isExpired) {
        CaseDocumentEntity foundCaseDocumentEntity = (CaseDocumentEntity) dartsDatabase.getEntityManager()
            .createNativeQuery("SELECT * FROM case_document WHERE cad_id = " + providedCaseDocumentEntity.getId(), CaseDocumentEntity.class)
            .getSingleResult();

        if (isExpired) {
            assertThat(foundCaseDocumentEntity.isDeleted())
                .as("CaseDocumentEntity isDeleted").isTrue();
            assertThat(foundCaseDocumentEntity.getDeletedTs())
                .as("CaseDocumentEntity deletedTs").isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
            assertThat(foundCaseDocumentEntity.getDeletedBy().getId())
                .as("CaseDocumentEntity deletedBy").isEqualTo(TestUtils.AUTOMATION_USER_ID);
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedCaseDocumentEntity)
            ).isEmpty();
        } else {
            assertThat(foundCaseDocumentEntity.isDeleted())
                .as("CaseDocumentEntity isDeleted").isFalse();
            assertThat(foundCaseDocumentEntity.getDeletedTs())
                .as("CaseDocumentEntity deletedTs").isNull();
            assertThat(foundCaseDocumentEntity.getDeletedBy())
                .as("CaseDocumentEntity deletedBy").isNull();
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedCaseDocumentEntity)
            ).isNotEmpty();
        }
        assertAuditEntries(AuditActivity.CASE_DOCUMENT_EXPIRED, foundCaseDocumentEntity, isExpired);
    }

    private void assertAnnotationDocument(AnnotationDocumentEntity providedDocumentEntity, boolean isExpired) {
        AnnotationDocumentEntity foundDocumentEntity = (AnnotationDocumentEntity) dartsDatabase.getEntityManager()
            .createNativeQuery("SELECT * FROM annotation_document WHERE ado_id = " + providedDocumentEntity.getId(), AnnotationDocumentEntity.class)
            .getSingleResult();

        if (isExpired) {
            assertThat(foundDocumentEntity.isDeleted())
                .as("AnnotationDocumentEntity isDeleted").isTrue();
            assertThat(foundDocumentEntity.getDeletedTs())
                .as("AnnotationDocumentEntity deletedTs").isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
            assertThat(foundDocumentEntity.getDeletedBy().getId())
                .as("AnnotationDocumentEntity deletedBy").isEqualTo(TestUtils.AUTOMATION_USER_ID);
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedDocumentEntity)
            ).isEmpty();
        } else {
            assertThat(foundDocumentEntity.isDeleted())
                .as("AnnotationDocumentEntity isDeleted").isFalse();
            assertThat(foundDocumentEntity.getDeletedTs())
                .as("AnnotationDocumentEntity deletedTs").isNull();
            assertThat(foundDocumentEntity.getDeletedBy())
                .as("AnnotationDocumentEntity deletedBy").isNull();
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedDocumentEntity)
            ).isNotEmpty();
        }
        assertAuditEntries(AuditActivity.ANNOTATION_EXPIRED, foundDocumentEntity, isExpired);
    }

    private void assertMediaEntity(MediaEntity providedDocumentEntity, boolean isExpired) {
        MediaEntity foundDocumentEntity = (MediaEntity) dartsDatabase.getEntityManager()
            .createNativeQuery("SELECT * FROM media WHERE med_id = " + providedDocumentEntity.getId(), MediaEntity.class)
            .getSingleResult();

        if (isExpired) {
            assertThat(foundDocumentEntity.isDeleted())
                .as("MediaEntity isDeleted").isTrue();
            assertThat(foundDocumentEntity.getDeletedTs())
                .as("MediaEntity deletedTs").isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
            assertThat(foundDocumentEntity.getDeletedBy().getId())
                .as("MediaEntity deletedBy").isEqualTo(TestUtils.AUTOMATION_USER_ID);
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedDocumentEntity)
            ).isEmpty();
        } else {
            assertThat(foundDocumentEntity.isDeleted())
                .as("MediaEntity isDeleted").isFalse();
            assertThat(foundDocumentEntity.getDeletedTs())
                .as("MediaEntity deletedTs").isNull();
            assertThat(foundDocumentEntity.getDeletedBy())
                .as("MediaEntity deletedBy").isNull();
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedDocumentEntity)
            ).isNotEmpty();
        }
        assertAuditEntries(AuditActivity.AUDIO_EXPIRED, foundDocumentEntity, isExpired);
    }

    private void assertTranscriptionDocument(TranscriptionDocumentEntity providedDocumentEntity, boolean isExpired) {
        TranscriptionDocumentEntity foundDocumentEntity = (TranscriptionDocumentEntity) dartsDatabase.getEntityManager()
            .createNativeQuery("SELECT * FROM transcription_document WHERE trd_id = " + providedDocumentEntity.getId(), TranscriptionDocumentEntity.class)
            .getSingleResult();

        if (isExpired) {
            assertThat(foundDocumentEntity.isDeleted())
                .as("TranscriptionDocumentEntity isDeleted").isTrue();
            assertThat(foundDocumentEntity.getDeletedTs())
                .as("TranscriptionDocumentEntity deletedTs").isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
            assertThat(foundDocumentEntity.getDeletedBy().getId())
                .as("TranscriptionDocumentEntity deletedBy").isEqualTo(TestUtils.AUTOMATION_USER_ID);
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedDocumentEntity)
            ).isEmpty();
        } else {
            assertThat(foundDocumentEntity.isDeleted())
                .as("TranscriptionDocumentEntity isDeleted").isFalse();
            assertThat(foundDocumentEntity.getDeletedTs())
                .as("TranscriptionDocumentEntity deletedTs").isNull();
            assertThat(foundDocumentEntity.getDeletedBy())
                .as("TranscriptionDocumentEntity deletedBy").isNull();
            assertThat(
                dartsDatabase.getExternalObjectDirectoryStub().findAllFor(providedDocumentEntity)
            ).isNotEmpty();
        }
        assertAuditEntries(AuditActivity.TRANSCRIPT_EXPIRED, foundDocumentEntity, isExpired);
    }


    private <T> void assignExternalObjectDirectory(
        T entity, TriConsumer<T, ObjectRecordStatusEnum, ExternalLocationTypeEnum> externalLocationTypeEnumConsumer,
        boolean assignArm, boolean storeArm) {

        externalLocationTypeEnumConsumer.accept(entity, ObjectRecordStatusEnum.STORED, ExternalLocationTypeEnum.INBOUND);
        externalLocationTypeEnumConsumer.accept(entity, ObjectRecordStatusEnum.STORED, ExternalLocationTypeEnum.UNSTRUCTURED);

        if (assignArm) {
            externalLocationTypeEnumConsumer.accept(
                entity,
                storeArm ? ObjectRecordStatusEnum.STORED : ObjectRecordStatusEnum.FAILURE,
                ExternalLocationTypeEnum.ARM);
        }
    }

    private void assignExternalObjectDirectory(CaseDocumentEntity caseDocumentEntity,
                                               boolean assignArm) {
        assignExternalObjectDirectory(caseDocumentEntity, assignArm, true);
    }

    private void assignExternalObjectDirectory(CaseDocumentEntity caseDocumentEntity,
                                               boolean assignArm, boolean storeArm) {
        assignExternalObjectDirectory(
            caseDocumentEntity,
            (caseDocument, objectRecordStatusEnum, externalLocationType) -> dartsDatabase.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(caseDocument, objectRecordStatusEnum, externalLocationType, UUID.randomUUID()),
            assignArm, storeArm);
    }

    private void assignExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                               boolean assignArm) {
        assignExternalObjectDirectory(annotationDocumentEntity, assignArm, true);
    }

    private void assignExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                               boolean assignArm, boolean storeArm) {
        assignExternalObjectDirectory(
            annotationDocumentEntity,
            (annotationDocument, objectRecordStatusEnum, externalLocationType) -> dartsDatabase.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(annotationDocument, objectRecordStatusEnum, externalLocationType, UUID.randomUUID()),
            assignArm, storeArm);
    }

    private void assignExternalObjectDirectory(MediaEntity mediaEntity,
                                               boolean assignArm) {
        assignExternalObjectDirectory(mediaEntity, assignArm, true);
    }

    private void assignExternalObjectDirectory(MediaEntity mediaEntity,
                                               boolean assignArm, boolean storeArm) {
        assignExternalObjectDirectory(
            mediaEntity,
            (media, objectRecordStatusEnum, externalLocationType) -> dartsDatabase.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationType, UUID.randomUUID()),
            assignArm, storeArm);
    }

    private void assignExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                               boolean assignArm) {
        assignExternalObjectDirectory(transcriptionDocumentEntity, assignArm, true);
    }

    private void assignExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                               boolean assignArm, boolean storeArm) {
        assignExternalObjectDirectory(
            transcriptionDocumentEntity,
            (transcriptionDocument, objectRecordStatusEnum, externalLocationType) -> dartsDatabase.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(transcriptionDocument, objectRecordStatusEnum, externalLocationType, UUID.randomUUID()),
            assignArm, storeArm);
    }

    private void assertAuditEntries(AuditActivity auditActivity, HasIntegerId hasIntegerId, boolean isAnonymised) {
        List<AuditEntity> caseExpiredAuditEntries = dartsDatabase.getAuditRepository()
            .findAll((Specification<AuditEntity>) (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(AuditEntity_.additionalData), String.valueOf(hasIntegerId.getId())),
                criteriaBuilder.equal(root.get(AuditEntity_.auditActivity).get("id"), auditActivity.getId())
            ));
        if (isAnonymised) {
            assertThat(caseExpiredAuditEntries).hasSize(1);
        } else {
            assertThat(caseExpiredAuditEntries).isEmpty();
        }
    }

}
