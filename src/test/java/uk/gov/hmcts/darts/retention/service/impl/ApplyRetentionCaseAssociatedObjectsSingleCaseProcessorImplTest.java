package uk.gov.hmcts.darts.retention.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.mapper.CaseRetentionConfidenceReasonMapper;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createCaseRetention;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createRetentionPolicyType;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.COMPLETE;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getExternalObjectDirectoryTestData;

@ExtendWith(MockitoExtension.class)
@Slf4j
@SuppressWarnings("PMD.NcssCount")
class ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImplTest {

    private static final OffsetDateTime DATETIME_2025 = OffsetDateTime.of(2025, 1, 1, 10, 10, 0, 0, UTC);
    private static final OffsetDateTime DATETIME_2026 = OffsetDateTime.of(2026, 1, 1, 10, 10, 0, 0, UTC);
    private static final OffsetDateTime DATETIME_2027 = OffsetDateTime.of(2027, 1, 1, 10, 10, 0, 0, UTC);
    private static final OffsetDateTime DATETIME_2028 = OffsetDateTime.of(2028, 1, 1, 10, 10, 0, 0, UTC);

    private static final OffsetDateTime RETENTION_UPDATED_DATE = OffsetDateTime.of(2024, 6, 20, 10, 0, 0, 0, UTC);
    private static final String POLICY_A_NAME = "Policy A";
    private static final String SOME_PAST_DATE_TIME = "2000-01-01T00:00:00Z";
    private static final String SOME_FUTURE_DATE_TIME = "2100-01-01T00:00:00Z";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

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
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private MediaLinkedCaseRepository mediaLinkedCaseRepository;

    private EodHelperMocks eodHelperMocks;

    private CourtCaseEntity case1PerfectlyClosed;
    private CourtCaseEntity case2PerfectlyClosed;
    private CourtCaseEntity case3NotPerfectlyClosed;
    private CourtCaseEntity case4NotPerfectlyClosed;
    private CourtCaseEntity case5PerfectlyClosed;

    private CaseRetentionEntity caseRetentionA1;
    private CaseRetentionEntity caseRetentionB1;
    private CaseRetentionEntity caseRetentionC1;
    private CaseRetentionEntity caseRetentionD1;

    private UserAccountEntity testUser;

    private ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor caseObjectsProcessor;

    @BeforeEach
    void beforeEach() {
        eodHelperMocks = new EodHelperMocks();

        var caseRetentionConfidenceReasonMapper = new CaseRetentionConfidenceReasonMapper(armDataManagementConfiguration);

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        caseObjectsProcessor = new ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl(
            caseRetentionRepository, caseService, eodRepository, mediaRepository, annotationDocumentRepository, transcriptionService,
            caseDocumentRepository, transcriptionDocumentRepository, caseRetentionConfidenceReasonMapper,
            currentTimeHelper, objectMapper, mediaLinkedCaseRepository);

        case1PerfectlyClosed = spy(CommonTestDataUtil.createCaseWithId("case1", 101));
        case1PerfectlyClosed.setRetentionUpdated(true);
        case1PerfectlyClosed.setRetentionRetries(1);
        case1PerfectlyClosed.setClosed(true);
        case1PerfectlyClosed.setRetConfScore(CASE_PERFECTLY_CLOSED);
        case1PerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
        case1PerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case1PerfectlyClosed, 1, 3);

        case2PerfectlyClosed = spy(CommonTestDataUtil.createCaseWithId("case1", 102));
        case2PerfectlyClosed.setRetentionUpdated(true);
        case2PerfectlyClosed.setRetentionRetries(2);
        case2PerfectlyClosed.setClosed(true);
        case2PerfectlyClosed.setRetConfScore(CASE_PERFECTLY_CLOSED);
        case2PerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
        case2PerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case2PerfectlyClosed, 1, 1);

        case3NotPerfectlyClosed = spy(CommonTestDataUtil.createCaseWithId("case3", 103));
        case3NotPerfectlyClosed.setRetentionUpdated(true);
        case3NotPerfectlyClosed.setRetentionRetries(1);
        case3NotPerfectlyClosed.setClosed(true);
        case3NotPerfectlyClosed.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
        case3NotPerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.AGED_CASE);
        case3NotPerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case3NotPerfectlyClosed, 1, 3);

        case4NotPerfectlyClosed = spy(CommonTestDataUtil.createCaseWithId("case4", 104));
        case4NotPerfectlyClosed.setRetentionUpdated(true);
        case4NotPerfectlyClosed.setRetentionRetries(2);
        case4NotPerfectlyClosed.setClosed(true);
        case4NotPerfectlyClosed.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
        case4NotPerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.AGED_CASE);
        case4NotPerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case4NotPerfectlyClosed, 1, 1);

        case5PerfectlyClosed = spy(CommonTestDataUtil.createCaseWithId("case5", 105));
        case5PerfectlyClosed.setRetentionUpdated(true);
        case5PerfectlyClosed.setRetentionRetries(1);
        case5PerfectlyClosed.setClosed(true);
        case5PerfectlyClosed.setRetConfScore(CASE_PERFECTLY_CLOSED);
        case5PerfectlyClosed.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
        case5PerfectlyClosed.setRetConfUpdatedTs(RETENTION_UPDATED_DATE);
        CommonTestDataUtil.createHearingsForCase(case5PerfectlyClosed, 1, 1);

        var hearA1 = case1PerfectlyClosed.getHearings().getFirst();
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

        var hearE1 = case5PerfectlyClosed.getHearings().getFirst();
        hearE1.setScheduledStartTime(LocalTime.NOON);

        var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);

        testUser = CommonTestDataUtil.createUserAccount();

        caseRetentionA1 = createCaseRetention(case1PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
        var caseRetentionA2 = createCaseRetention(case1PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2026, COMPLETE, testUser);
        var caseRetentionA3 = createCaseRetention(case1PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2027, COMPLETE, testUser);
        caseRetentionB1 = createCaseRetention(case2PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2028, COMPLETE, testUser);
        caseRetentionC1 = createCaseRetention(case3NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
        var caseRetentionC2 = createCaseRetention(case3NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2026, COMPLETE, testUser);
        var caseRetentionC3 = createCaseRetention(case3NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2027, COMPLETE, testUser);
        caseRetentionD1 = createCaseRetention(case4NotPerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
        var caseRetentionE1 = createCaseRetention(case5PerfectlyClosed, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);

        case1PerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionA1, caseRetentionA2, caseRetentionA3));
        case2PerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionB1));
        case3NotPerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionC1, caseRetentionC2, caseRetentionC3));
        case4NotPerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionD1));
        case5PerfectlyClosed.setCaseRetentionEntities(List.of(caseRetentionE1));

        lenient().when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);

    }

    @AfterEach
    void close() {
        eodHelperMocks.close();
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForMediaWhereMultipleCasesIsPerfectlyClosed() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = CommonTestDataUtil.createMedia(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                                            case2PerfectlyClosed.getHearings().getFirst()),
                                                     456);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA1);
        var eodA2 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA2);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        doReturn(List.of(mediaA1, mediaA2, mediaB1)).when(case1PerfectlyClosed).getAllAssociatedMedias();

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case2PerfectlyClosed)).thenReturn(Optional.of(caseRetentionB1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA2.getId(), externalLocationTypes)).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, mediaA1.getRetConfScore());
        assertNull(mediaA1.getRetConfReason());
        assertTrue(eodA1.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaA2.getRetConfScore());
        assertNull(mediaA2.getRetConfReason());
        assertTrue(eodA2.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaB1.getRetConfScore());
        assertNull(mediaB1.getRetConfReason());
        assertTrue(eodB1.isUpdateRetention());
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForDetsMediaWhereMultipleCasesIsPerfectlyClosed() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = CommonTestDataUtil.createMedia(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                                            case2PerfectlyClosed.getHearings().getFirst()),
                                                     456);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.DETS, mediaA1);
        var eodA2 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.DETS, mediaA2);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.DETS, mediaB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        doReturn(List.of(mediaA1, mediaA2, mediaB1)).when(case1PerfectlyClosed).getAllAssociatedMedias();

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case2PerfectlyClosed)).thenReturn(Optional.of(caseRetentionB1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA2.getId(), externalLocationTypes)).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, mediaA1.getRetConfScore());
        assertNull(mediaA1.getRetConfReason());
        assertFalse(eodA1.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaA2.getRetConfScore());
        assertNull(mediaA2.getRetConfReason());
        assertFalse(eodA2.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaB1.getRetConfScore());
        assertNull(mediaB1.getRetConfReason());
        assertFalse(eodB1.isUpdateRetention());
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForMediaWhereMultipleCasesIsPerfectlyClosedIncludingLinkedMedia() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = CommonTestDataUtil.createMedia(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                                            case2PerfectlyClosed.getHearings().getFirst()),
                                                     456);

        var mediaC1 = CommonTestDataUtil.createMedia(case5PerfectlyClosed.getHearings().getFirst());

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA1);
        var eodA2 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA2);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaB1);
        var eodC1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaC1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        doReturn(List.of(mediaA1, mediaA2, mediaB1, mediaC1)).when(case1PerfectlyClosed).getAllAssociatedMedias();

        var mediaC1LinkedToCase1 = createMediaLinkedCase(mediaC1, case5PerfectlyClosed);
        //Adding mediaLinkedWithNull case (refering legacy data that has courtroom/casenumber but could not be linked to a case correctly)
        //Previously this would cause a nullpointer this should not happen anymore
        var mediaLinkedToNullCase = createMediaLinkedCase(mediaC1, null);
        when(mediaLinkedCaseRepository.findByMedia(mediaA1)).thenReturn(List.of(mediaC1LinkedToCase1, mediaLinkedToNullCase));

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case2PerfectlyClosed)).thenReturn(Optional.of(caseRetentionB1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case5PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA2.getId(), externalLocationTypes)).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaC1.getId(), externalLocationTypes)).thenReturn(List.of(eodC1));

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, mediaA1.getRetConfScore());
        assertNull(mediaA1.getRetConfReason());
        assertTrue(eodA1.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaA2.getRetConfScore());
        assertNull(mediaA2.getRetConfReason());
        assertTrue(eodA2.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaB1.getRetConfScore());
        assertNull(mediaB1.getRetConfReason());
        assertTrue(eodB1.isUpdateRetention());

        assertTrue(eodC1.isUpdateRetention());
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForMediaWhereMultipleCasesIsNotPerfectlyClosed() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case3NotPerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case3NotPerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = CommonTestDataUtil.createMedia(Set.of(case3NotPerfectlyClosed.getHearings().getFirst(),
                                                            case4NotPerfectlyClosed.getHearings().getFirst()),
                                                     456);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA1);
        var eodA2 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA2);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaB1);

        when(caseService.getCourtCaseById(case3NotPerfectlyClosed.getId())).thenReturn(case3NotPerfectlyClosed);

        doReturn(List.of(mediaA1, mediaA2, mediaB1)).when(case3NotPerfectlyClosed).getAllAssociatedMedias();

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case3NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionC1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA2.getId(), externalLocationTypes)).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case3NotPerfectlyClosed.getId());

        // then
        assertEquals(CASE_NOT_PERFECTLY_CLOSED, mediaA1.getRetConfScore());
        assertTrue(eodA1.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case3\"," +
            "\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\",\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(mediaA1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, mediaA2.getRetConfScore());
        assertTrue(eodA2.isUpdateRetention());
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(mediaA2.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, mediaB1.getRetConfScore());
        assertTrue(eodB1.isUpdateRetention());
        String expectedResult2 = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case3\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}," +
            "{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult2, StringEscapeUtils.unescapeJson(mediaB1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForMediaWhereOneCaseIsPerfectlyClosedAndOneCaseIsNotPerfectlyClosed() {
        // given
        var mediaA1 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().getFirst());
        var mediaA2 = CommonTestDataUtil.createMedia(case1PerfectlyClosed.getHearings().get(1));
        mediaA2.setId(mediaA2.getId() + 1);
        var mediaB1 = CommonTestDataUtil.createMedia(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                                            case4NotPerfectlyClosed.getHearings().getFirst()),
                                                     456);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA1);
        var eodA2 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaA2);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForMedia(ExternalLocationTypeEnum.ARM, mediaB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);
        doReturn(List.of(mediaA1, mediaA2, mediaB1)).when(case1PerfectlyClosed).getAllAssociatedMedias();

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaA2.getId(), externalLocationTypes)).thenReturn(List.of(eodA2));
        when(eodRepository.findByMediaIdAndExternalLocationTypes(mediaB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, mediaA1.getRetConfScore());
        assertTrue(eodA1.isUpdateRetention());

        assertEquals(CASE_PERFECTLY_CLOSED, mediaA2.getRetConfScore());
        assertTrue(eodA2.isUpdateRetention());

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, mediaB1.getRetConfScore());
        assertTrue(eodB1.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(mediaB1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForAnnotationDocumentWhereOneCaseIsPerfectlyClosedAndOneCaseIsNotPerfectlyClosed() {
        // given
        var annotationA1 = CommonTestDataUtil.createAnnotationEntity(111);
        var annotationB1 = CommonTestDataUtil.createAnnotationEntity(222);
        annotationA1.setHearings(Set.of(case1PerfectlyClosed.getHearings().getFirst()));
        annotationB1.setHearings(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                        case4NotPerfectlyClosed.getHearings().getFirst()));

        var annotationDocumentA1 = annotationA1.getAnnotationDocuments().getFirst();
        var annotationDocumentB1 = annotationB1.getAnnotationDocuments().getFirst();
        annotationDocumentA1.setAnnotation(annotationA1);
        annotationDocumentB1.setAnnotation(annotationB1);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForAnnotationDocument(ExternalLocationTypeEnum.ARM,
                                                                                                              annotationDocumentA1);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForAnnotationDocument(ExternalLocationTypeEnum.ARM,
                                                                                                              annotationDocumentB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        doReturn(List.of(annotationDocumentA1, annotationDocumentB1)).when(case1PerfectlyClosed).getAllAssociatedAnnotationDocuments();

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByAnnotationDocumentIdAndExternalLocationTypes(annotationDocumentA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByAnnotationDocumentIdAndExternalLocationTypes(annotationDocumentB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, annotationDocumentA1.getRetConfScore());
        assertTrue(eodA1.isUpdateRetention());

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, annotationDocumentB1.getRetConfScore());
        assertTrue(eodB1.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(annotationDocumentB1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForDetsAnnotationDocumentWhereOneCaseIsPerfectlyClosedAndOneCaseIsNotPerfectlyClosed() {
        // given
        var annotationA1 = CommonTestDataUtil.createAnnotationEntity(111);
        var annotationB1 = CommonTestDataUtil.createAnnotationEntity(222);
        annotationA1.setHearings(Set.of(case1PerfectlyClosed.getHearings().getFirst()));
        annotationB1.setHearings(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                        case4NotPerfectlyClosed.getHearings().getFirst()));

        var annotationDocumentA1 = annotationA1.getAnnotationDocuments().getFirst();
        var annotationDocumentB1 = annotationB1.getAnnotationDocuments().getFirst();
        annotationDocumentA1.setAnnotation(annotationA1);
        annotationDocumentB1.setAnnotation(annotationB1);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForAnnotationDocument(ExternalLocationTypeEnum.DETS,
                                                                                                              annotationDocumentA1);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForAnnotationDocument(ExternalLocationTypeEnum.DETS,
                                                                                                              annotationDocumentB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        doReturn(List.of(annotationDocumentA1, annotationDocumentB1)).when(case1PerfectlyClosed).getAllAssociatedAnnotationDocuments();

        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByAnnotationDocumentIdAndExternalLocationTypes(annotationDocumentA1.getId(), externalLocationTypes)).thenReturn(List.of(eodA1));
        when(eodRepository.findByAnnotationDocumentIdAndExternalLocationTypes(annotationDocumentB1.getId(), externalLocationTypes)).thenReturn(List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, annotationDocumentA1.getRetConfScore());
        assertFalse(eodA1.isUpdateRetention());

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, annotationDocumentB1.getRetConfScore());
        assertFalse(eodB1.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(annotationDocumentB1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForTranscriptionDocumentWhereOneCaseIsPerfectlyClosedAndOneCaseIsNotPerfectlyClosed() {
        // given
        Set<TranscriptionEntity> transcriptionsA1 = CommonTestDataUtil.createTranscriptions(case1PerfectlyClosed.getHearings().getFirst());
        Set<TranscriptionEntity> transcriptionsB1 = CommonTestDataUtil.createTranscriptions(case4NotPerfectlyClosed.getHearings().getFirst());
        var transcriptionA1 = TestUtils.getFirstLong(transcriptionsA1);
        var transcriptionB1 = TestUtils.getFirstLong(transcriptionsB1);
        transcriptionB1.setHearings(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                           case4NotPerfectlyClosed.getHearings().getFirst()));

        var transcriptionDocumentA1 = transcriptionA1.getTranscriptionDocumentEntities().getFirst();
        var transcriptionDocumentB1 = transcriptionB1.getTranscriptionDocumentEntities().getFirst();
        transcriptionDocumentA1.setTranscription(transcriptionA1);
        transcriptionDocumentB1.setTranscription(transcriptionB1);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForTranscriptionDocument(ExternalLocationTypeEnum.ARM,
                                                                                                                 transcriptionDocumentA1);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForTranscriptionDocument(ExternalLocationTypeEnum.ARM,
                                                                                                                 transcriptionDocumentB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        when(transcriptionService.getAllCaseTranscriptionDocuments(case1PerfectlyClosed.getId())).thenReturn(
            List.of(transcriptionDocumentA1, transcriptionDocumentB1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(transcriptionDocumentA1.getId(), externalLocationTypes)).thenReturn(
            List.of(eodA1));
        when(eodRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(transcriptionDocumentB1.getId(), externalLocationTypes)).thenReturn(
            List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, transcriptionDocumentA1.getRetConfScore());
        assertTrue(eodA1.isUpdateRetention());

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, transcriptionDocumentB1.getRetConfScore());
        assertTrue(eodB1.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(transcriptionDocumentB1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForDetsTranscriptionDocumentWhereOneCaseIsPerfectlyClosedAndOneCaseIsNotPerfectlyClosed() {
        // given
        Set<TranscriptionEntity> transcriptionsA1 = CommonTestDataUtil.createTranscriptions(case1PerfectlyClosed.getHearings().getFirst());
        Set<TranscriptionEntity> transcriptionsB1 = CommonTestDataUtil.createTranscriptions(case4NotPerfectlyClosed.getHearings().getFirst());
        var transcriptionA1 = TestUtils.getFirstLong(transcriptionsA1);
        var transcriptionB1 = TestUtils.getFirstLong(transcriptionsB1);
        transcriptionB1.setHearings(Set.of(case1PerfectlyClosed.getHearings().getFirst(),
                                           case4NotPerfectlyClosed.getHearings().getFirst()));

        var transcriptionDocumentA1 = transcriptionA1.getTranscriptionDocumentEntities().getFirst();
        var transcriptionDocumentB1 = transcriptionB1.getTranscriptionDocumentEntities().getFirst();
        transcriptionDocumentA1.setTranscription(transcriptionA1);
        transcriptionDocumentB1.setTranscription(transcriptionB1);

        var eodA1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForTranscriptionDocument(ExternalLocationTypeEnum.DETS,
                                                                                                                 transcriptionDocumentA1);
        var eodB1 = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForTranscriptionDocument(ExternalLocationTypeEnum.DETS,
                                                                                                                 transcriptionDocumentB1);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);

        when(transcriptionService.getAllCaseTranscriptionDocuments(case1PerfectlyClosed.getId())).thenReturn(
            List.of(transcriptionDocumentA1, transcriptionDocumentB1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionD1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(transcriptionDocumentA1.getId(), externalLocationTypes)).thenReturn(
            List.of(eodA1));
        when(eodRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(transcriptionDocumentB1.getId(), externalLocationTypes)).thenReturn(
            List.of(eodB1));

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, transcriptionDocumentA1.getRetConfScore());
        assertFalse(eodA1.isUpdateRetention());

        assertEquals(CASE_NOT_PERFECTLY_CLOSED, transcriptionDocumentB1.getRetConfScore());
        assertFalse(eodB1.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(transcriptionDocumentB1.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForCaseDocumentWhereCaseIsPerfectlyClosed() {
        // given
        var caseDocument = CommonTestDataUtil.createCaseDocumentEntity(case1PerfectlyClosed, testUser);

        var eod = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForCaseDocument(ExternalLocationTypeEnum.ARM, caseDocument);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);
        doReturn(List.of(caseDocument)).when(case1PerfectlyClosed).getCaseDocumentEntities();
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByCaseDocumentIdAndExternalLocationTypes(caseDocument.getId(), externalLocationTypes)).thenReturn(List.of(eod));

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());
        assertTrue(eod.isUpdateRetention());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, caseDocument.getRetConfScore());
        assertNull(caseDocument.getRetConfReason());

    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForDetsCaseDocumentWhereCaseIsPerfectlyClosed() {
        // given
        var caseDocument = CommonTestDataUtil.createCaseDocumentEntity(case1PerfectlyClosed, testUser);

        var eod = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForCaseDocument(ExternalLocationTypeEnum.DETS, caseDocument);

        when(caseService.getCourtCaseById(case1PerfectlyClosed.getId())).thenReturn(case1PerfectlyClosed);
        doReturn(List.of(caseDocument)).when(case1PerfectlyClosed).getCaseDocumentEntities();
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case1PerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByCaseDocumentIdAndExternalLocationTypes(caseDocument.getId(), externalLocationTypes)).thenReturn(List.of(eod));

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case1PerfectlyClosed.getId());

        // then
        assertEquals(CASE_PERFECTLY_CLOSED, caseDocument.getRetConfScore());
        assertFalse(eod.isUpdateRetention());
        assertNull(caseDocument.getRetConfReason());

    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForCaseDocumentWhereCaseIsNotPerfectlyClosed() {
        // given
        var caseDocument = CommonTestDataUtil.createCaseDocumentEntity(case4NotPerfectlyClosed, testUser);

        var eod = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForCaseDocument(ExternalLocationTypeEnum.ARM, caseDocument);

        when(caseService.getCourtCaseById(case4NotPerfectlyClosed.getId())).thenReturn(case4NotPerfectlyClosed);
        doReturn(List.of(caseDocument)).when(case4NotPerfectlyClosed).getCaseDocumentEntities();
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByCaseDocumentIdAndExternalLocationTypes(caseDocument.getId(), externalLocationTypes)).thenReturn(List.of(eod));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case4NotPerfectlyClosed.getId());

        // then
        assertEquals(CASE_NOT_PERFECTLY_CLOSED, caseDocument.getRetConfScore());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(caseDocument.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void processApplyRetentionToCaseAssociatedObjects_ForDetsCaseDocumentWhereCaseIsNotPerfectlyClosed() {
        // given
        var caseDocument = CommonTestDataUtil.createCaseDocumentEntity(case4NotPerfectlyClosed, testUser);

        var eod = getExternalObjectDirectoryTestData().eodStoredInExternalLocationTypeForCaseDocument(ExternalLocationTypeEnum.DETS, caseDocument);

        when(caseService.getCourtCaseById(case4NotPerfectlyClosed.getId())).thenReturn(case4NotPerfectlyClosed);
        doReturn(List.of(caseDocument)).when(case4NotPerfectlyClosed).getCaseDocumentEntities();
        when(caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(case4NotPerfectlyClosed)).thenReturn(Optional.of(caseRetentionA1));

        List<ExternalLocationTypeEntity> externalLocationTypes = List.of(eodHelperMocks.getArmLocation(), eodHelperMocks.getDetsLocation());
        when(eodRepository.findByCaseDocumentIdAndExternalLocationTypes(caseDocument.getId(), externalLocationTypes)).thenReturn(List.of(eod));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(RETENTION_UPDATED_DATE);

        // when
        caseObjectsProcessor.processApplyRetentionToCaseAssociatedObjects(case4NotPerfectlyClosed.getId());

        // then
        assertEquals(CASE_NOT_PERFECTLY_CLOSED, caseDocument.getRetConfScore());
        assertFalse(eod.isUpdateRetention());
        String expectedResult = "{\"ret_conf_applied_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"cases\":[{\"courthouse\":\"CASE_COURTHOUSE\",\"case_number\":\"case4\",\"ret_conf_updated_ts\":\"2024-06-20T10:00:00.000Z\"," +
            "\"ret_conf_reason\":\"AGED_CASE\"}]}";
        JSONAssert.assertEquals(expectedResult, StringEscapeUtils.unescapeJson(caseDocument.getRetConfReason()), JSONCompareMode.NON_EXTENSIBLE);
    }

    private MediaLinkedCaseEntity createMediaLinkedCase(MediaEntity media, CourtCaseEntity courtCase) {
        MediaLinkedCaseEntity mediaLinkedCase = new MediaLinkedCaseEntity();
        mediaLinkedCase.setMedia(media);
        mediaLinkedCase.setCourtCase(courtCase);
        return mediaLinkedCase;
    }
}