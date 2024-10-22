package uk.gov.hmcts.darts.retention.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.service.impl.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl;
import uk.gov.hmcts.darts.test.common.MediaIdMatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;
import uk.gov.hmcts.darts.testutils.stubs.CaseDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.CaseRetentionStub;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class ApplyRetentionCaseAssociatedObjectsProcessorIntTest extends IntegrationBase {

    private static final OffsetDateTime DT_2025 = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2026 = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2027 = OffsetDateTime.of(2027, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2028 = OffsetDateTime.of(2028, 1, 1, 1, 0, 0, 0, UTC);

    private UserAccountEntity testUser;

    @Autowired
    CaseRepository caseRepository;
    @Autowired
    HearingRepository hearingRepository;
    @Autowired
    CaseRetentionStub caseRetentionStub;
    @Autowired
    CaseRetentionRepository caseRetentionRepository;
    @Autowired
    MediaRepository mediaRepository;
    @Autowired
    ExternalObjectDirectoryStub eodStub;
    @SpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @Autowired
    AnnotationStub annotationStub;
    @Autowired
    TranscriptionStub transcriptionStub;
    @Autowired
    AnnotationRepository annotationRepository;
    @Autowired
    AnnotationDocumentRepository annotationDocumentRepository;
    @Autowired
    TranscriptionRepository transcriptionRepository;
    @Autowired
    TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Autowired
    CourtCaseStub caseStub;
    @Autowired
    CaseDocumentStub caseDocumentStub;
    @Autowired
    CaseDocumentRepository caseDocumentRepository;
    @SpyBean
    ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl singleCaseProcessor;
    @MockBean
    UserIdentity userIdentity;

    @Autowired
    ApplyRetentionCaseAssociatedObjectsProcessor processor;

    List<MediaEntity> medias;
    CourtCaseEntity caseA;
    CourtCaseEntity caseB;
    CourtCaseEntity caseC;


    @BeforeEach
    void setup() {
        /*
        Common test data setup:

        case A -> hearing A1 -> media 0, media 1
        case A -> hearing A2 -> media 2
        case B -> hearing B  -> media 0
        case C -> hearing C  -> media 3

        media 0 -> hearing A1 -> case A
        media 0 -> hearing B  -> case B
        media 1 -> hearing A1 -> case A
        media 2 -> hearing A2 -> case A
        media 3 -> hearing C  -> case C
        */

        // given
        caseA = caseStub.createAndSaveCourtCaseWithHearings();
        caseA.setRetentionUpdated(true);
        caseA.setRetentionRetries(1);
        caseA.setClosed(true);

        caseB = caseStub.createAndSaveCourtCaseWithHearings();
        caseB.setRetentionUpdated(true);
        caseB.setRetentionRetries(2);
        caseB.setClosed(true);

        caseC = caseStub.createAndSaveCourtCaseWithHearings();
        caseC.setRetentionUpdated(true);
        caseC.setRetentionRetries(2);
        caseC.setClosed(true);

        medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();

        var hearA1 = caseA.getHearings().getFirst();
        var hearA2 = caseA.getHearings().get(1);
        var hearA3 = caseA.getHearings().get(2);
        var hearB = caseB.getHearings().getFirst();
        var hearC = caseC.getHearings().getFirst();
        hearA1.addMedia(medias.getFirst());
        hearA1.addMedia(medias.get(1));
        hearA2.addMedia(medias.get(2));
        hearB.addMedia(medias.getFirst());
        hearC.addMedia(medias.get(3));

        hearingRepository.save(hearA2);
        hearingRepository.save(hearA3);
        hearingRepository.save(hearA1);
        hearingRepository.save(hearB);
        hearingRepository.save(hearC);

        caseRetentionStub.createCaseRetentionObject(caseA, DT_2025);
        caseRetentionStub.createCaseRetentionObject(caseA, DT_2026);
        caseRetentionStub.createCaseRetentionObject(caseB, DT_2027);
        caseRetentionStub.createCaseRetentionObject(caseB, DT_2028);
        caseRetentionStub.createCaseRetentionObject(caseC, DT_2028);

        eodStub.createAndSaveEod(medias.get(0), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(medias.get(1), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(medias.get(2), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(medias.get(3), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));

        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void testSuccessfullyApplyRetentionToCaseMedias() {

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.getFirst().getId()).get();
        assertThat(media0.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.getFirst(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.getFirst().isUpdateRetention()).isTrue();
        var media1 = mediaRepository.findById(medias.get(1).getId()).get();
        assertThat(media1.getRetainUntilTs()).isEqualTo(DT_2026);
        var eodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia1.getFirst().isUpdateRetention()).isTrue();
        var media2 = mediaRepository.findById(medias.get(2).getId()).get();
        assertThat(media2.getRetainUntilTs()).isEqualTo(DT_2026);

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isFalse();
        assertThat(actualCaseA.get().getRetentionRetries()).isEqualTo(1);
        var actualCaseB = caseRepository.findById(caseB.getId());
        assertThat(actualCaseB.get().isRetentionUpdated()).isFalse();
        assertThat(actualCaseB.get().getRetentionRetries()).isEqualTo(2);
    }

    @Test
    void testSuccessfullyApplyRetentionToCaseMediasIncludingLinkedMedias() {
        // given
        MediaLinkedCaseEntity mediaLinkedCase1 = createMediaLinkedCase(medias.get(3), caseA);
        dartsDatabase.getMediaLinkedCaseRepository().save(mediaLinkedCase1);
        MediaLinkedCaseEntity mediaLinkedCase2 = createMediaLinkedCase(medias.get(3), caseC);
        dartsDatabase.getMediaLinkedCaseRepository().save(mediaLinkedCase2);
        MediaLinkedCaseEntity mediaLinkedCase3 = createMediaLinkedCase(medias.get(1), caseC);
        dartsDatabase.getMediaLinkedCaseRepository().save(mediaLinkedCase3);

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.getFirst().getId()).get();
        assertThat(media0.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.getFirst(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.getFirst().isUpdateRetention()).isTrue();
        var media1 = mediaRepository.findById(medias.get(1).getId()).get();
        assertThat(media1.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia1.getFirst().isUpdateRetention()).isTrue();
        var media2 = mediaRepository.findById(medias.get(2).getId()).get();
        assertThat(media2.getRetainUntilTs()).isEqualTo(DT_2026);
        var media3 = mediaRepository.findById(medias.get(3).getId()).get();
        assertThat(media3.getRetainUntilTs()).isEqualTo(DT_2028);

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isFalse();
        assertThat(actualCaseA.get().getRetentionRetries()).isEqualTo(1);
        var actualCaseB = caseRepository.findById(caseB.getId());
        assertThat(actualCaseB.get().isRetentionUpdated()).isFalse();
        assertThat(actualCaseB.get().getRetentionRetries()).isEqualTo(2);
        var actualCaseC = caseRepository.findById(caseC.getId());
        assertThat(actualCaseC.get().isRetentionUpdated()).isFalse();
        assertThat(actualCaseC.get().getRetentionRetries()).isEqualTo(2);
    }

    @Test
    void testSuccessfullyApplyRetentionToCaseAnnotations() {
        /*
        Test data setup:

        case A -> hearing 1A -> annotation1A -> annotationDoc1, annotationDoc2
        case A -> hearing 1A -> annotation2A -> annotationDoc3
        case A -> hearing 2A -> annotation2A -> annotationDoc3
        case A -> hearing 2A -> annotation3A -> annotationDoc4, annotationDoc5
        case B -> hearing 1B -> annotation1A -> annotationDoc1, annotationDoc2

        annotationDoc1 -> annotation1A -> hearing 1A -> case A
        annotationDoc2 -> annotation1A -> hearing 1A -> case A
        annotationDoc3 -> annotation2A -> hearing 1A -> case A
        annotationDoc3 -> annotation2A -> hearing 2A -> case A
        annotationDoc4 -> annotation3A -> hearing 2A -> case A
        annotationDoc5 -> annotation1B -> hearing 1B -> case B
        annotationDoc1 -> annotation1A -> hearing 1B -> case B
        annotationDoc2 -> annotation1A -> hearing 1B -> case B
        */

        // given
        var hear1A = caseA.getHearings().getFirst();
        var hear2A = caseA.getHearings().get(1);
        var hear1B = caseB.getHearings().getFirst();

        var annotation1A = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear1A);
        var annotation2A = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear1A);
        var annotation3A = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear2A);
        var annotation1B = annotationStub.createAndSaveAnnotationEntityWith(testUser, "TestAnnotation", hear1B);

        annotation1A.addHearing(hear1B);
        annotationRepository.save(annotation1A);
        annotation2A.addHearing(hear2A);
        annotationRepository.save(annotation2A);

        var annotationDoc1 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation1A);
        var annotationDoc2 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation1A);
        var annotationDoc3 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation2A);
        var annotationDoc4 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation3A);
        var annotationDoc5 = annotationStub.createAndSaveAnnotationDocumentEntity(annotation1B);

        eodStub.createAndSaveEod(annotationDoc1, ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(annotationDoc2, ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(annotationDoc3, ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(annotationDoc4, ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(annotationDoc5, ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var actualAnnotationDoc1 = annotationDocumentRepository.findById(annotationDoc1.getId()).get();
        assertThat(actualAnnotationDoc1.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsAnnotationDoc1 = eodRepository.findByAnnotationDocumentEntityAndExternalLocationType(
            annotationDoc1, EodHelper.armLocation());
        assertThat(eodsAnnotationDoc1.getFirst().isUpdateRetention()).isTrue();
        var actualAnnotationDoc3 = annotationDocumentRepository.findById(annotationDoc3.getId()).get();
        assertThat(actualAnnotationDoc3.getRetainUntilTs()).isEqualTo(DT_2026);
        var eodsAnnotationDoc3 = eodRepository.findByAnnotationDocumentEntityAndExternalLocationType(
            annotationDoc3, EodHelper.armLocation());
        assertThat(eodsAnnotationDoc3.getFirst().isUpdateRetention()).isTrue();
        var actualAnnotationDoc4 = annotationDocumentRepository.findById(annotationDoc4.getId()).get();
        assertThat(actualAnnotationDoc4.getRetainUntilTs()).isEqualTo(DT_2026);
        var actualAnnotationDoc5 = annotationDocumentRepository.findById(annotationDoc5.getId()).get();
        assertThat(actualAnnotationDoc5.getRetainUntilTs()).isEqualTo(DT_2028);

        var actualCaseA = caseRepository.findById(caseA.getId()).get();
        assertThat(actualCaseA.isRetentionUpdated()).isFalse();
        assertThat(actualCaseA.getRetentionRetries()).isEqualTo(1);
        var actualCaseB = caseRepository.findById(caseB.getId()).get();
        assertThat(actualCaseB.isRetentionUpdated()).isFalse();
        assertThat(actualCaseB.getRetentionRetries()).isEqualTo(2);
    }

    @Test
    void testSuccessfullyApplyRetentionToCaseTranscriptionDocuments() {
        /*
        Test data setup:

        case A -> hearing 1 -> transcription1 -> transcriptionDoc1, transcriptionDoc2
        case A -> hearing 1 -> transcription2 -> transcriptionDoc3
        case A -> hearing 2 -> transcription2 -> transcriptionDoc3
        case B -> hearing 3 -> transcription1 -> transcriptionDoc1, transcriptionDoc2

        */

        // given
        var hear1 = caseA.getHearings().getFirst();
        var hear2 = caseA.getHearings().get(1);
        var hear3 = caseB.getHearings().getFirst();

        var tr1 = transcriptionStub.createTranscription(hear1);
        tr1.addHearing(hear3);
        transcriptionRepository.save(tr1);
        var tr2 = transcriptionStub.createTranscription(hear1);
        tr2.addHearing(hear2);
        transcriptionRepository.save(tr2);

        transcriptionStub.updateTranscriptionWithDocument(tr1, STORED, INBOUND, UUID.randomUUID());
        transcriptionStub.updateTranscriptionWithDocument(tr1, STORED, INBOUND, UUID.randomUUID());
        transcriptionStub.updateTranscriptionWithDocument(tr2, STORED, INBOUND, UUID.randomUUID());

        var trDoc1 = tr1.getTranscriptionDocumentEntities().getFirst();
        eodStub.createAndSaveExternalObjectDirectory(trDoc1.getId(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        var trDoc2 = tr1.getTranscriptionDocumentEntities().get(1);
        eodStub.createAndSaveExternalObjectDirectory(trDoc2.getId(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        var trDoc3 = tr2.getTranscriptionDocumentEntities().getFirst();
        eodStub.createAndSaveExternalObjectDirectory(trDoc3.getId(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var actualTranscriptionDoc1 = transcriptionDocumentRepository.findById(trDoc1.getId()).get();
        assertThat(actualTranscriptionDoc1.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsTranscriptionDoc1 = eodRepository.findByTranscriptionDocumentEntityAndExternalLocationType(trDoc1, EodHelper.armLocation());
        assertThat(eodsTranscriptionDoc1.getFirst().isUpdateRetention()).isTrue();
        var actualTranscriptionDoc2 = transcriptionDocumentRepository.findById(trDoc2.getId()).get();
        assertThat(actualTranscriptionDoc2.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsTranscriptionDoc2 = eodRepository.findByTranscriptionDocumentEntityAndExternalLocationType(trDoc2, EodHelper.armLocation());
        assertThat(eodsTranscriptionDoc2.getFirst().isUpdateRetention()).isTrue();
        var actualTranscriptionDoc3 = transcriptionDocumentRepository.findById(trDoc3.getId()).get();
        assertThat(actualTranscriptionDoc3.getRetainUntilTs()).isEqualTo(DT_2026);

        var actualCaseA = caseRepository.findById(caseA.getId()).get();
        assertThat(actualCaseA.isRetentionUpdated()).isFalse();
        assertThat(actualCaseA.getRetentionRetries()).isEqualTo(1);
    }

    @Test
    void testSuccessfullyApplyRetentionToCaseDocuments() {
        /*
        Test data setup:

        case A -> -> caseDoc1, caseDoc2
        case B -> -> caseDoc3

        */

        // given
        var caseDoc1 = caseDocumentStub.createAndSaveCaseDocumentEntity(caseA, testUser);
        var caseDoc2 = caseDocumentStub.createAndSaveCaseDocumentEntity(caseA, testUser);
        var caseDoc3 = caseDocumentStub.createAndSaveCaseDocumentEntity(caseB, testUser);

        eodStub.createExternalObjectDirectory(caseDoc1, EodHelper.armDropZoneStatus(), EodHelper.armLocation(), UUID.randomUUID());
        eodStub.createExternalObjectDirectory(caseDoc2, EodHelper.armDropZoneStatus(), EodHelper.armLocation(), UUID.randomUUID());
        eodStub.createExternalObjectDirectory(caseDoc3, EodHelper.armDropZoneStatus(), EodHelper.armLocation(), UUID.randomUUID());

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var actualCaseDoc1 = caseDocumentRepository.findById(caseDoc1.getId()).get();
        assertThat(actualCaseDoc1.getRetainUntilTs()).isEqualTo(DT_2026);
        var eodsCaseDoc1 = eodRepository.findByCaseDocumentAndExternalLocationType(actualCaseDoc1, EodHelper.armLocation());
        assertThat(eodsCaseDoc1.getFirst().isUpdateRetention()).isTrue();
        var actualCaseDoc2 = caseDocumentRepository.findById(caseDoc2.getId()).get();
        assertThat(actualCaseDoc2.getRetainUntilTs()).isEqualTo(DT_2026);
        var eodsCaseDoc2 = eodRepository.findByCaseDocumentAndExternalLocationType(actualCaseDoc2, EodHelper.armLocation());
        assertThat(eodsCaseDoc2.getFirst().isUpdateRetention()).isTrue();
        var actualCaseDoc3 = caseDocumentRepository.findById(caseDoc3.getId()).get();
        assertThat(actualCaseDoc3.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsCaseDoc3 = eodRepository.findByCaseDocumentAndExternalLocationType(actualCaseDoc3, EodHelper.armLocation());
        assertThat(eodsCaseDoc3.getFirst().isUpdateRetention()).isTrue();
    }

    @Test
    void testExceptionOnOneObjectCausesRollbackOfAllChangesToAllObjectsAndProcessingOfOtherCasesContinues() {

        // given
        doThrow(RuntimeException.class).when(eodRepository).findByMediaAndExternalLocationType(argThat(
            new MediaIdMatcher(medias.getFirst().getId())), refEq(EodHelper.armLocation()));

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.getFirst().getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.getFirst(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.getFirst().isUpdateRetention()).isFalse();
        var media1 = mediaRepository.findById(medias.get(1).getId()).get();
        assertThat(media1.getRetainUntilTs()).isNull();
        var eodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia1.getFirst().isUpdateRetention()).isFalse();

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isTrue();
        assertThat(actualCaseA.get().getRetentionRetries()).isEqualTo(2);

        verify(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(caseB.getId());
    }

    @Test
    void testRetentionIsNotAppliedIfAssociatedCasesAreNotAllClosed() {

        // given
        caseB.setClosed(false);
        caseRepository.save(caseB);

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.getFirst().getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.getFirst(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.getFirst().isUpdateRetention()).isFalse();

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isFalse();

        verify(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(caseB.getId());
    }

    @Test
    void testRetentionIsNotAppliedIfMissingArmEod() {

        // given
        eodRepository.deleteAll();

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.getFirst().getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var media1 = mediaRepository.findById(medias.get(1).getId()).get();
        assertThat(media1.getRetainUntilTs()).isNull();
        var media2 = mediaRepository.findById(medias.get(2).getId()).get();
        assertThat(media2.getRetainUntilTs()).isNull();

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isTrue();
        assertThat(actualCaseA.get().getRetentionRetries()).isEqualTo(2);
        var actualCaseB = caseRepository.findById(caseB.getId());
        assertThat(actualCaseB.get().isRetentionUpdated()).isTrue();
        assertThat(actualCaseB.get().getRetentionRetries()).isEqualTo(3);
    }

    @Test
    void testRetentionIsNotAppliedIfNoRetentionsFoundOnMediaCases() {

        // given
        caseRetentionRepository.deleteAll();

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.getFirst().getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.getFirst(), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.getFirst().isUpdateRetention()).isFalse();

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isTrue();
        assertThat(actualCaseA.get().getRetentionRetries()).isEqualTo(2);

        verify(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(caseB.getId());
    }

    private MediaLinkedCaseEntity createMediaLinkedCase(MediaEntity media, CourtCaseEntity courtCase) {
        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setMedia(media);
        mediaLinkedCase.setCourtCase(courtCase);
        return mediaLinkedCase;
    }
}