package uk.gov.hmcts.darts.retention.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.service.impl.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CaseRetentionStub;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;

class ApplyRetentionCaseAssociatedObjectsProcessorIntTest extends IntegrationBase {

    public static final LocalDate D_2020_10_1 = LocalDate.of(2020, 10, 1);
    public static final LocalDate D_2020_10_2 = LocalDate.of(2020, 10, 2);
    private static final OffsetDateTime DT_2025 = OffsetDateTime.of(2025, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2026 = OffsetDateTime.of(2026, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2027 = OffsetDateTime.of(2027, 1, 1, 1, 0, 0, 0, UTC);
    private static final OffsetDateTime DT_2028 = OffsetDateTime.of(2028, 1, 1, 1, 0, 0, 0, UTC);

    @Autowired
    CaseRepository caseRepository;
    @Autowired
    HearingRepository hearingRepository;
    @Autowired
    HearingStub hearingStub;
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
    @SpyBean
    ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl singleCaseProcessor;
    @Autowired
    CourtCaseStub caseStub;

    @Autowired
    ApplyRetentionCaseAssociatedObjectsProcessor processor;

    List<MediaEntity> medias;
    CourtCaseEntity caseA;
    CourtCaseEntity caseB;

    @BeforeEach
    void setup() {
        /*
        Common test data setup:

        case A -> hearing A1 -> media 0, media 1
        case A -> hearing A2 -> media 2
        case B -> hearing B  -> media 0

        media 0 -> hearing A1 -> case A
        media 0 -> hearing B  -> case B
        media 1 -> hearing A1 -> case A
        media 2 -> hearing A2 -> case A
        */

        // given
        caseA = caseStub.createAndSaveCourtCaseWithHearings(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(1);
            courtCase.setClosed(true);
        });
        caseB = caseStub.createAndSaveCourtCaseWithHearings(courtCase -> {
            courtCase.setRetentionUpdated(true);
            courtCase.setRetentionRetries(2);
            courtCase.setClosed(true);
        });

        var hearA1 = caseA.getHearings().get(0);
        var hearA2 = caseA.getHearings().get(1);
        var hearA3 = caseA.getHearings().get(2);
        var hearB = caseB.getHearings().get(0);

        medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        hearA1.addMedia(medias.get(0));
        hearA1.addMedia(medias.get(1));
        hearA2.addMedia(medias.get(2));
        hearB.addMedia(medias.get(0));
        hearingRepository.save(hearA2);
        hearingRepository.save(hearA3);
        hearingRepository.save(hearA1);
        hearingRepository.save(hearB);

        caseRetentionStub.createCaseRetentionObject(caseA, DT_2025);
        caseRetentionStub.createCaseRetentionObject(caseA, DT_2026);
        caseRetentionStub.createCaseRetentionObject(caseB, DT_2027);
        caseRetentionStub.createCaseRetentionObject(caseB, DT_2028);

        eodStub.createAndSaveEod(medias.get(0), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(medias.get(1), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
        eodStub.createAndSaveEod(medias.get(2), ARM_DROP_ZONE, ARM, eod -> eod.setUpdateRetention(false));
    }

    @Test
    void testSuccessfullyApplyRetentionToCaseMedias() {

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.get(0).getId()).get();
        assertThat(media0.getRetainUntilTs()).isEqualTo(DT_2028);
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.get(0).isUpdateRetention()).isTrue();
        var media1 = mediaRepository.findById(medias.get(1).getId()).get();
        assertThat(media1.getRetainUntilTs()).isEqualTo(DT_2026);
        var eodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia1.get(0).isUpdateRetention()).isTrue();
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
    void testExceptionOnOneObjectCausesRollbackOfAllChangesToAllObjectsAndProcessingOfOtherCasesContinues() {

        // given
        doThrow(RuntimeException.class).when(eodRepository).findByMediaAndExternalLocationType(refEqMedia(medias.get(0)), refEq(EodHelper.armLocation()));

        // when
        processor.processApplyRetentionToCaseAssociatedObjects();

        // then
        var media0 = mediaRepository.findById(medias.get(0).getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.get(0).isUpdateRetention()).isFalse();
        var media1 = mediaRepository.findById(medias.get(1).getId()).get();
        assertThat(media1.getRetainUntilTs()).isNull();
        var eodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia1.get(0).isUpdateRetention()).isFalse();

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
        var media0 = mediaRepository.findById(medias.get(0).getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.get(0).isUpdateRetention()).isFalse();

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
        var media0 = mediaRepository.findById(medias.get(0).getId()).get();
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
        var media0 = mediaRepository.findById(medias.get(0).getId()).get();
        assertThat(media0.getRetainUntilTs()).isNull();
        var eodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), EodHelper.armDropZoneStatus(), EodHelper.armLocation());
        assertThat(eodsMedia0.get(0).isUpdateRetention()).isFalse();

        var actualCaseA = caseRepository.findById(caseA.getId());
        assertThat(actualCaseA.get().isRetentionUpdated()).isTrue();
        assertThat(actualCaseA.get().getRetentionRetries()).isEqualTo(2);

        verify(singleCaseProcessor).processApplyRetentionToCaseAssociatedObjects(caseB.getId());
    }


    private static MediaEntity refEqMedia(MediaEntity media) {
        return refEq(media, "courtroom", "hearingList", "retainUntilTs", "lastModifiedDateTime", "createdDateTime");
    }
}
