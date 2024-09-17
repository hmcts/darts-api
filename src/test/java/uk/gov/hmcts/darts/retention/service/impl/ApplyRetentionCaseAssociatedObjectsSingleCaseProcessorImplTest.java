package uk.gov.hmcts.darts.retention.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.retention.mapper.CaseRetentionConfidenceReasonMapper;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor;
import uk.gov.hmcts.darts.test.common.data.ExternalObjectDirectoryTestData;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createCaseRetention;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createRetentionPolicyType;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.COMPLETE;

@ExtendWith(MockitoExtension.class)
@Slf4j
@SuppressWarnings("checkstyle:LineLength")
class ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImplTest {

    private static final OffsetDateTime DATETIME_2025 = OffsetDateTime.of(2025, 1, 1, 10, 10, 0, 0, UTC);
    private static final OffsetDateTime DATETIME_2026 = OffsetDateTime.of(2026, 1, 1, 10, 10, 0, 0, UTC);
    private static final OffsetDateTime DATETIME_2027 = OffsetDateTime.of(2027, 1, 1, 10, 10, 0, 0, UTC);
    private static final OffsetDateTime DATETIME_2028 = OffsetDateTime.of(2028, 1, 1, 10, 10, 0, 0, UTC);

    private static final OffsetDateTime RETENTION_UPDATED_DATE = OffsetDateTime.of(2024, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String POLICY_A_NAME = "Policy A";
    private static final String SOME_PAST_DATE_TIME = "2000-01-01T00:00:00Z";
    private static final String SOME_FUTURE_DATE_TIME = "2100-01-01T00:00:00Z";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private CaseService caseService;
    @Mock
    private ExternalObjectDirectoryRepository eodRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private AnnotationDocumentRepository annotationDocumentRepository;
    @Mock
    private TranscriptionService transcriptionService;
    @Mock
    private CaseDocumentRepository caseDocumentRepository;
    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private CourtCaseEntity case1PerfectlyClosed;
    private CourtCaseEntity case2PerfectlyClosed;
    private CourtCaseEntity case3NotPerfectlyClosed;
    private CourtCaseEntity case4NotPerfectlyClosed;


    private CaseRetentionEntity caseRetentionA1;
    private CaseRetentionEntity caseRetentionB1;
    private CaseRetentionEntity caseRetentionC1;
    private CaseRetentionEntity caseRetentionD1;

    private ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor caseObjectsProcessor;

    @BeforeEach
    void beforeEach() {
        var caseRetentionConfidenceReasonMapper = new CaseRetentionConfidenceReasonMapper(armDataManagementConfiguration);

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        caseObjectsProcessor = new ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl(
            caseRetentionRepository, caseService, eodRepository, mediaRepository, annotationDocumentRepository, transcriptionService,
            caseDocumentRepository, transcriptionDocumentRepository, caseRetentionConfidenceReasonMapper, userIdentity,
            currentTimeHelper, objectMapper);

        case1PerfectlyClosed = CommonTestDataUtil.createCaseWithId("case1", 101);
        case1PerfectlyClosed.setRetentionUpdated(true);
        case1PerfectlyClosed.setRetentionRetries(1);
        case1PerfectlyClosed.setClosed(true);
        case1PerfectlyClosed.setRetConfScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED);
        case1PerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
        case1PerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case1PerfectlyClosed, 1, 3);

        case2PerfectlyClosed = CommonTestDataUtil.createCaseWithId("case1", 102);
        case2PerfectlyClosed.setRetentionUpdated(true);
        case2PerfectlyClosed.setRetentionRetries(2);
        case2PerfectlyClosed.setClosed(true);
        case2PerfectlyClosed.setRetConfScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED);
        case2PerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
        case2PerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case2PerfectlyClosed, 1, 1);

        case3NotPerfectlyClosed = CommonTestDataUtil.createCaseWithId("case3", 103);
        case3NotPerfectlyClosed.setRetentionUpdated(true);
        case3NotPerfectlyClosed.setRetentionRetries(1);
        case3NotPerfectlyClosed.setClosed(true);
        case3NotPerfectlyClosed.setRetConfScore(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED);
        case3NotPerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.AGED_CASE);
        case3NotPerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case3NotPerfectlyClosed, 1, 3);

        case4NotPerfectlyClosed = CommonTestDataUtil.createCaseWithId("case4", 104);
        case4NotPerfectlyClosed.setRetentionUpdated(true);
        case4NotPerfectlyClosed.setRetentionRetries(2);
        case4NotPerfectlyClosed.setClosed(true);
        case4NotPerfectlyClosed.setRetConfScore(RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED);
        case4NotPerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.AGED_CASE);
        case4NotPerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case4NotPerfectlyClosed, 1, 1);

        var hearA1 = case1PerfectlyClosed.getHearings().get(0);
        hearA1.setScheduledStartTime(LocalTime.NOON);
        var hearA2 = case1PerfectlyClosed.getHearings().get(1);
        hearA2.setScheduledStartTime(LocalTime.NOON);
        var hearA3 = case1PerfectlyClosed.getHearings().get(2);
        hearA3.setScheduledStartTime(LocalTime.NOON);

        var hearB1 = case2PerfectlyClosed.getHearings().getFirst();
        hearB1.setScheduledStartTime(LocalTime.NOON);

        var hearC1 = case3NotPerfectlyClosed.getHearings().getFirst();
        hearC1.setScheduledStartTime(LocalTime.NOON);
        var hearC2 = case3NotPerfectlyClosed.getHearings().get(1);
        hearC2.setScheduledStartTime(LocalTime.NOON);
        var hearC3 = case3NotPerfectlyClosed.getHearings().get(2);
        hearC3.setScheduledStartTime(LocalTime.NOON);

        var hearD1 = case4NotPerfectlyClosed.getHearings().getFirst();
        hearD1.setScheduledStartTime(LocalTime.NOON);

        var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);

        var testUser = CommonTestDataUtil.createUserAccount();

        caseRetentionA1 = createCaseRetention(case1PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
        var caseRetentionA2 = createCaseRetention(case1PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2026, COMPLETE, testUser);
        var caseRetentionA3 = createCaseRetention(case1PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2027, COMPLETE, testUser);
        caseRetentionB1 = createCaseRetention(case2PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2028, COMPLETE, testUser);
        caseRetentionC1 = createCaseRetention(case3NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
        var caseRetentionC2 = createCaseRetention(case3NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2026, COMPLETE, testUser);
        var caseRetentionC3 = createCaseRetention(case3NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2027, COMPLETE, testUser);
        caseRetentionD1 = createCaseRetention(case4NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);

        case1PerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionA1, caseRetentionA2, caseRetentionA3));
        case2PerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionB1));
        case3NotPerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionC1, caseRetentionC2, caseRetentionC3));
        case4NotPerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionD1));

        lenient().when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);

    }

    @Test
    void processApplyRetentionToCaseAssociatedObjectsForMediaWhereCaseIsPerfectlyClosed() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = createMedia(List.of(case1PerfectlyClosed.getHearings().getFirst(),
                                          case2PerfectlyClosed.getHearings().getFirst()),
                                  456);

        var eodA1 = ExternalObjectDirectoryTestData.eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA1);
        var eodA2 = ExternalObjectDirectoryTestData.eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA2);
        var eodB1 = ExternalObjectDirectoryTestData.eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        when(mediaRepository.findAllByCaseId(case1PerfectlyClosed.getId())).thenReturn(List.of(mediaA1, mediaA2, mediaB1));

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case2PerfectlyClosed)).thenReturn(Optional.of(caseRetentionB1));

        when(eodRepository.findByMediaAndExternalLocationType(mediaA1, EodHelper.armLocation())).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaAndExternalLocationType(mediaA2, EodHelper.armLocation())).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaAndExternalLocationType(mediaB1, EodHelper.armLocation())).thenReturn(List.of(eodB1));

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(1, mediaA1.getRetConfScore());
        assertNull(mediaA1.getRetConfReason());

        assertEquals(1, mediaA2.getRetConfScore());
        assertNull(mediaA2.getRetConfReason());

        assertEquals(1, mediaB1.getRetConfScore());
        assertNull(mediaB1.getRetConfReason());

    }

    @Test
    void processApplyRetentionToCaseAssociatedObjectsForMediaWhereCaseIsNotPerfectlyClosed() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case3NotPerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case3NotPerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = createMedia(List.of(case3NotPerfectlyClosed.getHearings().getFirst(),
                                          case4NotPerfectlyClosed.getHearings().getFirst()),
                                  456);

        var eodA1 = ExternalObjectDirectoryTestData.eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA1);
        var eodA2 = ExternalObjectDirectoryTestData.eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA2);
        var eodB1 = ExternalObjectDirectoryTestData.eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaB1);

        when(caseService.getCourtCaseById(case3NotPerfectlyClosed.getId())).thenReturn(case3NotPerfectlyClosed);

        when(mediaRepository.findAllByCaseId(case3NotPerfectlyClosed.getId())).thenReturn(List.of(mediaA1, mediaA2, mediaB1));

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case3NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionC1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        when(eodRepository.findByMediaAndExternalLocationType(mediaA1, EodHelper.armLocation())).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaAndExternalLocationType(mediaA2, EodHelper.armLocation())).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaAndExternalLocationType(mediaB1, EodHelper.armLocation())).thenReturn(List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case3NotPerfectlyClosed.getId());

        // then
        assertEquals(2, mediaA1.getRetConfScore());
        String expectedResult = "{\\\"ret_conf_applied_ts\\\":\\\"2024-06-20T10:00:00Z\\\",\\\"cases\\\":[{\\\"courthouse\\\":\\\"CASE_COURTHOUSE\\\",\\\"case_number\\\":\\\"case3\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-20T10:00:00Z\\\",\\\"ret_conf_reason\\\":\\\"AGED_CASE\\\"}]}";
        assertEquals(expectedResult, mediaA1.getRetConfReason());

        assertEquals(2, mediaA2.getRetConfScore());
        assertEquals(expectedResult, mediaA2.getRetConfReason());

        assertEquals(2, mediaB1.getRetConfScore());
        String expectedResult2 = "{\\\"ret_conf_applied_ts\\\":\\\"2024-06-20T10:00:00Z\\\",\\\"cases\\\":[{\\\"courthouse\\\":\\\"CASE_COURTHOUSE\\\",\\\"case_number\\\":\\\"case3\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-20T10:00:00Z\\\",\\\"ret_conf_reason\\\":\\\"AGED_CASE\\\"},{\\\"courthouse\\\":\\\"CASE_COURTHOUSE\\\",\\\"case_number\\\":\\\"case4\\\",\\\"ret_conf_updated_ts\\\":\\\"2024-06-20T10:00:00Z\\\",\\\"ret_conf_reason\\\":\\\"AGED_CASE\\\"}]}";
        assertEquals(expectedResult2, mediaB1.getRetConfReason());

    }


    public MediaEntity createMedia(List<HearingEntity> hearings, int mediaId) {
        var hearing = hearings.getFirst();
        MediaEntity mediaEntity = new MediaEntity();
        OffsetDateTime startTime = OffsetDateTime.of(hearing.getHearingDate(), hearing.getScheduledStartTime(), ZoneOffset.UTC);
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(startTime.plusHours(1));
        mediaEntity.setChannel(1);
        mediaEntity.setHearingList(hearings);
        mediaEntity.setCourtroom(hearing.getCourtroom());
        mediaEntity.setId(mediaId);
        return mediaEntity;
    }
}