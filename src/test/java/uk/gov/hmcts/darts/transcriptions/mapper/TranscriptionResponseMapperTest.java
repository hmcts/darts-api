package uk.gov.hmcts.darts.transcriptions.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.AdminActionResponse;
import uk.gov.hmcts.darts.transcriptions.model.AdminApproveDeletionResponse;
import uk.gov.hmcts.darts.transcriptions.model.AdminMarkedForDeletionResponseItem;
import uk.gov.hmcts.darts.transcriptions.model.CaseResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.CourthouseResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.CourtroomResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionWorkflowsResponse;
import uk.gov.hmcts.darts.transcriptions.model.HearingResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentHideResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionResponseDetails;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionWorkflowsComment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionDocumentTestData.minimalTranscriptionDocument;
import static uk.gov.hmcts.darts.test.common.data.TranscriptionDocumentTestData.transcriptionDocumentWithAdminAction;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;
import static uk.gov.hmcts.darts.util.EntityIdPopulator.withIdsPopulated;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CouplingBetweenObjects")//Required to accuratly test the code
class TranscriptionResponseMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    private TranscriptionResponseMapper transcriptionResponseMapper;

    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @BeforeEach
    void setUp() {
        transcriptionResponseMapper = spy(new TranscriptionResponseMapper(hearingReportingRestrictionsRepository));
    }

    @Test
    void mapToTranscriptionTypeResponses() throws Exception {
        List<TranscriptionTypeEntity> transcriptionTypeEntities = CommonTestDataUtil.createTranscriptionTypeEntities();

        List<TranscriptionTypeResponse> transcriptionTypeResponses =
            transcriptionResponseMapper.mapToTranscriptionTypeResponses(transcriptionTypeEntities);
        String actualResponse = objectMapper.writeValueAsString(transcriptionTypeResponses);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionTypeResponseMapper/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionTypeResponse() throws Exception {
        TranscriptionTypeEntity transcriptionTypeEntity =
            CommonTestDataUtil.createTranscriptionTypeEntityFromEnum(TranscriptionTypeEnum.SENTENCING_REMARKS);

        TranscriptionTypeResponse transcriptionTypeResponse =
            transcriptionResponseMapper.mapToTranscriptionTypeResponse(transcriptionTypeEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionTypeResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionTypeResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }


    @Test
    void mapToTranscriptionUrgencyResponses() throws Exception {
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities = CommonTestDataUtil.createTranscriptionUrgencyEntities();

        List<TranscriptionUrgencyResponse> transcriptionUrgencyResponses =
            transcriptionResponseMapper.mapToTranscriptionUrgencyResponses(transcriptionUrgencyEntities);
        String actualResponse = objectMapper.writeValueAsString(transcriptionUrgencyResponses);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionUrgencyResponseMapper/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionUrgencyResponse() throws Exception {
        TranscriptionUrgencyEntity transcriptionUrgencyEntity =
            CommonTestDataUtil.createTranscriptionUrgencyEntityFromEnum(TranscriptionUrgencyEnum.STANDARD);

        TranscriptionUrgencyResponse transcriptionUrgencyResponse =
            transcriptionResponseMapper.mapToTranscriptionUrgencyResponse(transcriptionUrgencyEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionUrgencyResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionUrgencyResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoStatus() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, false, true, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseNoStatusSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoHearingIdAndValidCourtCase() throws Exception {
        String courtName = "1";
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setName(courtName);

        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(null, true, false, true, courtroomEntity);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);
        transcriptionEntity.setHearings(new HashSet<>());
        transcriptionEntity.setHearingDate(LocalDate.of(2023, 6, 20));
        transcriptionEntity.setCourtCases(Set.of(CommonTestDataUtil.createCase("case1")));
        transcriptionEntity.setCourtroom(CommonTestDataUtil.createCourtroom("1"));
        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityNoHearing.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithCourtroom() throws Exception {
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(null, true, false, true, null);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);
        transcriptionEntity.setHearings(new HashSet<>());
        transcriptionEntity.setHearingDate(LocalDate.of(2023, 6, 20));
        transcriptionEntity.setCourtCases(Set.of(CommonTestDataUtil.createCase("case1")));
        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityNoCourtroom.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoHearingsAndNoValidCourtCaseShouldFail() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);
        transcriptionEntity.setHearings(new HashSet<>());
        transcriptionEntity.setHearingDate(LocalDate.of(2023, 6, 20));

        var exception = assertThrows(
            DartsApiException.class,
            () -> transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity)
        );

        assertEquals(TranscriptionApiError.TRANSCRIPTION_NOT_FOUND, exception.getError());
    }


    @Test
    void mapToTranscriptionResponseWithWorkflow() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityWithWorkflow.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponse() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithOutLegacyComments() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);

        transcriptionEntity.getTranscriptionCommentEntities()
            .forEach(transcriptionCommentEntity -> {
                transcriptionCommentEntity.setTranscriptionWorkflow(new TranscriptionWorkflowEntity());
            });

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityWithoutLegacyComments.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }


    @Test
    void mapToTranscriptionResponseIsAutomated() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);
        transcriptionEntity.setIsManualTranscription(false);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityAutomated.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseInternalServerError() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        hearing1.setCourtCase(null);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);


        var exception = assertThrows(
            DartsApiException.class,
            () -> transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity)
        );

        assertEquals(TranscriptionApiError.TRANSCRIPTION_NOT_FOUND, exception.getError());
    }

    @Test
    void mapToTranscriptionResponseWithRequestor() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityWithRequestor.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithHideFromRequestor() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);
        transcriptionEntity.setHideRequestFromRequestor(true);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        assertEquals(true, transcriptionResponse.getHideRequestFromRequestor());
    }

    @Test
    void mapTransactionEntityToTransactionDetails() {

        LocalDate hearingDate = LocalDate.now();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingDate(hearingDate);

        Integer courthouseId = 300;
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(courthouseId);

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setCourthouse(courthouseEntity);

        hearingEntity.setCourtroom(courtroomEntity);

        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(TranscriptionStatusEnum.COMPLETE.getId());

        String caseNumber = "casenumber";
        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(caseNumber);

        Set<HearingEntity> hearingEntitys = new HashSet<>();

        // create the transaction to be mapped
        Integer transactionId = 200;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transactionId);
        transcriptionEntity.setHearings(hearingEntitys);
        transcriptionEntity.setTranscriptionStatus(transcriptionStatus);
        transcriptionEntity.setIsManualTranscription(false);
        hearingEntity.setCourtCase(caseEntity);
        transcriptionEntity.getHearings().add(hearingEntity);

        // run test and make the assertions
        GetTranscriptionDetailAdminResponse fndResponse = transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity);
        assertEquals(transactionId, fndResponse.getTranscriptionId());
        assertEquals(TranscriptionStatusEnum.COMPLETE.getId(), fndResponse.getTranscriptionStatusId());
        assertEquals(false, fndResponse.getIsManualTranscription());
        assertEquals(caseNumber, fndResponse.getCaseNumber());
        assertEquals(hearingDate, fndResponse.getHearingDate());
        assertEquals(courthouseId, fndResponse.getCourthouseId());
    }

    @Test
    void mapTransactionEntityNoHearingToTransactionDetails() {

        Integer courthouseId = 300;
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(courthouseId);

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setCourthouse(courthouseEntity);

        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(TranscriptionStatusEnum.COMPLETE.getId());

        String caseNumber = "casenumber";
        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setCourthouse(courthouseEntity);

        Set<HearingEntity> hearingEntitys = new HashSet<>();

        // create the transaction to be mapped
        Integer transactionId = 200;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transactionId);
        transcriptionEntity.setHearings(hearingEntitys);
        transcriptionEntity.setTranscriptionStatus(transcriptionStatus);
        transcriptionEntity.setIsManualTranscription(false);
        transcriptionEntity.getCourtCases().add(caseEntity);

        // run test and make the assertions
        GetTranscriptionDetailAdminResponse fndResponse = transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity);
        assertEquals(transactionId, fndResponse.getTranscriptionId());
        assertEquals(TranscriptionStatusEnum.COMPLETE.getId(), fndResponse.getTranscriptionStatusId());
        assertEquals(false, fndResponse.getIsManualTranscription());
        assertEquals(caseNumber, fndResponse.getCaseNumber());
        assertNull(fndResponse.getHearingDate());
        assertEquals(courthouseId, fndResponse.getCourthouseId());
    }

    @Test
    void mapTransactionEntityNoCaseToTransactionDetails() {
        LocalDate hearingDate = LocalDate.now();
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setHearingDate(hearingDate);

        Integer courthouseId = 300;
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(courthouseId);

        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setCourthouse(courthouseEntity);

        hearingEntity.setCourtroom(courtroomEntity);

        TranscriptionStatusEntity transcriptionStatus = new TranscriptionStatusEntity();
        transcriptionStatus.setId(TranscriptionStatusEnum.COMPLETE.getId());

        Set<HearingEntity> hearingEntitys = new HashSet<>();

        // create the transaction to be mapped
        Integer transactionId = 200;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transactionId);
        transcriptionEntity.setHearings(hearingEntitys);
        transcriptionEntity.setTranscriptionStatus(transcriptionStatus);
        transcriptionEntity.setIsManualTranscription(false);
        transcriptionEntity.getHearings().add(hearingEntity);

        // run test and make the assertions
        GetTranscriptionDetailAdminResponse fndResponse = transcriptionResponseMapper.mapTransactionEntityToTransactionDetails(transcriptionEntity);
        assertEquals(transactionId, fndResponse.getTranscriptionId());
        assertEquals(TranscriptionStatusEnum.COMPLETE.getId(), fndResponse.getTranscriptionStatusId());
        assertEquals(false, fndResponse.getIsManualTranscription());
        assertNull(fndResponse.getCaseNumber());
        assertEquals(hearingDate, fndResponse.getHearingDate());
        assertEquals(courthouseId, fndResponse.getCourthouseId());
    }

    @Test
    void mapSearchTranscriptionDocumentSearchResult() {
        Integer transactionId = 200;
        Integer transcriptionDocumentId = 300;

        Integer caseId = 900;
        String caseNumber = "case" + caseId;

        Integer hearingCaseId = caseId + 2;
        String hearingCaseNumber = "hearing case" + hearingCaseId;

        String courthouseDisplayNumber = "courthouse" + caseId;

        String hearingcourthouseDisplayName = "hearingcourthouse" + hearingCaseId;

        LocalDate hearingDate = LocalDate.now().plusMonths(10);

        boolean isManualTranscription = true;
        boolean isHidden = false;

        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             caseId,
                                                                             caseNumber,
                                                                             hearingCaseId,
                                                                             hearingCaseNumber,
                                                                             courthouseDisplayNumber,
                                                                             hearingcourthouseDisplayName,
                                                                             hearingDate,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertEquals(hearingCaseNumber, response.getCase().getCaseNumber());
        assertEquals(hearingDate, response.getHearing().getHearingDate());

        // ensure we prioritise the courthouse directly mapped to the transcription not the hearing courthouse
        assertEquals(hearingcourthouseDisplayName, response.getCourthouse().getDisplayName());
        assertEquals(isManualTranscription, response.getIsManualTranscription());
        assertEquals(isHidden, response.getIsHidden());
    }

    @Test
    void mapSearchTranscriptionDocumentSearchResultWithNoHearingCourthouse() {
        Integer transactionId = 200;
        Integer transcriptionDocumentId = 300;

        Integer caseId = 900;
        String caseNumber = "case" + caseId;

        Integer hearingCaseId = caseId + 2;
        String hearingCaseNumber = "hearing case" + hearingCaseId;

        LocalDate hearingDate = LocalDate.now().plusMonths(10);

        String courthouseDisplayName = "courthouse" + caseId;

        boolean isManualTranscription = true;
        boolean isHidden = false;
        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             caseId,
                                                                             caseNumber,
                                                                             hearingCaseId,
                                                                             hearingCaseNumber,
                                                                             courthouseDisplayName,
                                                                             null,
                                                                             hearingDate,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertEquals(hearingCaseNumber, response.getCase().getCaseNumber());
        assertEquals(hearingDate, response.getHearing().getHearingDate());
        assertEquals(courthouseDisplayName, response.getCourthouse().getDisplayName());
        assertEquals(isManualTranscription, response.getIsManualTranscription());
        assertEquals(isHidden, response.getIsHidden());
    }

    @Test
    void mapSearchTranscriptionDocumentSearchResultWithNoHearingCase() {
        Integer transactionId = 200;
        Integer transcriptionDocumentId = 300;

        Integer caseId = 900;
        String caseNumber = "case" + caseId;

        LocalDate hearingDate = LocalDate.now().plusMonths(10);

        String courthouseDisplayName = "courthouse" + caseId;

        boolean isManualTranscription = true;
        boolean isHidden = false;
        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             caseId,
                                                                             caseNumber,
                                                                             null,
                                                                             null,
                                                                             courthouseDisplayName,
                                                                             null,
                                                                             hearingDate,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertEquals(caseNumber, response.getCase().getCaseNumber());
        assertEquals(hearingDate, response.getHearing().getHearingDate());
        assertEquals(courthouseDisplayName, response.getCourthouse().getDisplayName());
        assertEquals(isManualTranscription, response.getIsManualTranscription());
        assertEquals(isHidden, response.getIsHidden());
    }

    @Test
    void mapSearchTranscriptionDocumentSearchNoCaseNoHearingAndNoOwner() {
        Integer transactionId = 200;
        Integer transcriptionDocumentId = 300;

        boolean isManualTranscription = true;
        boolean isHidden = false;

        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertNull(response.getCase().getCaseNumber());
        assertNull(response.getHearing());
        assertNull(response.getCourthouse().getDisplayName());
        assertEquals(isManualTranscription, response.getIsManualTranscription());
        assertEquals(isHidden, response.getIsHidden());
    }

    @Test
    void mapTransactionEntityToDocumentIdSearchResult() {
        Integer transId = 200;
        Integer userId = 500;
        Integer transDocumentId = 400;
        OffsetDateTime uploadedAt = OffsetDateTime.now();

        UserAccountEntity accountEntity = new UserAccountEntity();
        accountEntity.setId(userId);

        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transId);

        String fileName = "file";
        String fileType = "fileType";
        Integer fileBytes = 299;
        boolean hidden = true;
        OffsetDateTime lastModifiedAt = OffsetDateTime.parse("2007-12-03T10:15:30+00:00");
        String clipId = "clipId";
        String contentObjectId = "contentObjectId";
        String checksum = "checksum";
        UserAccountEntity lastModifiedBy = withIdsPopulated(minimalUserAccount());

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setId(transDocumentId);
        transcriptionDocumentEntity.setTranscription(transcriptionEntity);
        transcriptionDocumentEntity.setUploadedBy(accountEntity);
        transcriptionDocumentEntity.setUploadedDateTime(uploadedAt);
        transcriptionDocumentEntity.setFileName(fileName);
        transcriptionDocumentEntity.setFileType(fileType);
        transcriptionDocumentEntity.setFileSize(fileBytes);
        transcriptionDocumentEntity.setHidden(hidden);
        transcriptionDocumentEntity.setContentObjectId(contentObjectId);
        transcriptionDocumentEntity.setClipId(clipId);
        transcriptionDocumentEntity.setChecksum(checksum);
        transcriptionDocumentEntity.setLastModifiedTimestamp(lastModifiedAt);
        transcriptionDocumentEntity.setLastModifiedBy(lastModifiedBy);

        GetTranscriptionDocumentByIdResponse response = transcriptionResponseMapper.getSearchByTranscriptionDocumentId(transcriptionDocumentEntity);

        assertEquals(transId, response.getTranscriptionId());
        assertEquals(transDocumentId, response.getTranscriptionDocumentId());
        assertEquals(fileName, response.getFileName());
        assertEquals(fileType, response.getFileType());
        assertEquals(fileBytes, response.getFileSizeBytes());
        assertEquals(userId, response.getUploadedBy());
        assertEquals(uploadedAt, response.getUploadedAt());
        assertEquals(hidden, response.getIsHidden());
        assertEquals(checksum, response.getChecksum());
        assertEquals(lastModifiedBy.getId(), response.getLastModifiedBy());
        assertEquals(lastModifiedAt, response.getLastModifiedAt());
        assertEquals(clipId, response.getClipId());
        assertEquals(contentObjectId, response.getContentObjectId());
    }

    @Test
    void includesAdminActionWhenDocumentIsHidden() {
        var hiddenTranscriptionDocument = withIdsPopulated(transcriptionDocumentWithAdminAction());
        var adminActionEntity = hiddenTranscriptionDocument.getAdminActions().getFirst();

        var response = transcriptionResponseMapper.getSearchByTranscriptionDocumentId(hiddenTranscriptionDocument);

        var adminActionResponse = response.getAdminAction();
        assertEquals(adminActionEntity.getHiddenBy().getId(), adminActionResponse.getHiddenById());
        assertEquals(adminActionEntity.getComments(), adminActionResponse.getComments());
        assertEquals(adminActionEntity.getHiddenDateTime(), adminActionResponse.getHiddenAt());
        assertEquals(adminActionEntity.getMarkedForManualDelBy().getId(), adminActionResponse.getMarkedForManualDeletionById());
        assertEquals(adminActionEntity.getObjectHiddenReason().getId(), adminActionResponse.getReasonId());
        assertEquals(adminActionEntity.getTicketReference(), adminActionResponse.getTicketReference());
        assertEquals(adminActionEntity.getMarkedForManualDelDateTime(), adminActionResponse.getMarkedForManualDeletionAt());
    }

    @Test
    void doesntIncludeAdminActionWhenDocumentIsVisible() {
        var visibleTranscriptionDocument = minimalTranscriptionDocument();

        var response = transcriptionResponseMapper.getSearchByTranscriptionDocumentId(visibleTranscriptionDocument);

        assertNull(response.getAdminAction());
    }

    @Test
    void mapHideResponse() {
        Integer documentId = 100;
        boolean hide = true;

        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        documentEntity.setId(documentId);
        documentEntity.setHidden(hide);

        Integer objectAdminActionId = 101;
        String comments = "comments";
        String reference = "reference";

        UserAccountEntity userAccountEntity = new UserAccountEntity();

        ObjectHiddenReasonEntity reasonEntity = mock(ObjectHiddenReasonEntity.class);
        when(reasonEntity.getId()).thenReturn(2332);

        OffsetDateTime creationDate = OffsetDateTime.now();
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionId);
        objectAdminActionEntity.setComments(comments);
        objectAdminActionEntity.setTicketReference(reference);
        objectAdminActionEntity.setId(objectAdminActionId);
        objectAdminActionEntity.setHiddenBy(userAccountEntity);
        objectAdminActionEntity.setHiddenDateTime(creationDate);
        objectAdminActionEntity.setMarkedForManualDelBy(userAccountEntity);
        objectAdminActionEntity.setMarkedForManualDelDateTime(creationDate);
        objectAdminActionEntity.setObjectHiddenReason(reasonEntity);

        TranscriptionDocumentHideResponse response = transcriptionResponseMapper.mapHideOrShowResponse(documentEntity, objectAdminActionEntity);

        assertEquals(response.getId(), documentEntity.getId());
        assertEquals(response.getIsHidden(), documentEntity.isHidden());
        assertEquals(response.getAdminAction().getReasonId(), objectAdminActionEntity.getObjectHiddenReason().getId());
        assertEquals(response.getAdminAction().getComments(), objectAdminActionEntity.getComments());
        assertEquals(response.getAdminAction().getTicketReference(), objectAdminActionEntity.getTicketReference());
        assertEquals(response.getAdminAction().getId(), objectAdminActionEntity.getId());
        assertEquals(response.getAdminAction().getHiddenAt(), objectAdminActionEntity.getHiddenDateTime());
        assertEquals(response.getAdminAction().getHiddenById(), objectAdminActionEntity.getHiddenBy().getId());
        assertEquals(response.getAdminAction().getHiddenById(), userAccountEntity.getId());
        assertEquals(response.getAdminAction().getMarkedForManualDeletionById(), objectAdminActionEntity.getMarkedForManualDelBy().getId());
        assertEquals(response.getAdminAction().getMarkedForManualDeletionAt(), objectAdminActionEntity.getMarkedForManualDelDateTime());
        assertEquals(response.getAdminAction().getIsMarkedForManualDeletion(), objectAdminActionEntity.isMarkedForManualDeletion());
    }

    @Test
    void mapShowResponseWithNoObjectAdminAction() {
        Integer documentId = 100;
        boolean hide = true;

        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        documentEntity.setId(documentId);
        documentEntity.setHidden(hide);

        TranscriptionDocumentHideResponse response = transcriptionResponseMapper.mapHideOrShowResponse(documentEntity, null);

        assertEquals(response.getId(), documentEntity.getId());
        assertEquals(response.getIsHidden(), documentEntity.isHidden());
    }

    @Test
    void mapTranscriptionDocumentMarkedForDeletion() {

        TranscriptionDocumentEntity documentEntity = PersistableFactory.getTranscriptionDocument()
            .complexTranscriptionDocument().build();

        AdminMarkedForDeletionResponseItem response = transcriptionResponseMapper.mapTranscriptionDocumentMarkedForDeletion(documentEntity);

        ObjectAdminActionEntity adminActionEntity = documentEntity.getAdminActions().getFirst();
        assertEquals(adminActionEntity.getId(), response.getAdminAction().getId());
        assertEquals(adminActionEntity.getComments(), response.getAdminAction().getComments());
        assertEquals(adminActionEntity.getTicketReference(), response.getAdminAction().getTicketReference());
        assertEquals(adminActionEntity.getHiddenDateTime(), response.getAdminAction().getHiddenAt());
        assertEquals(adminActionEntity.getMarkedForManualDelDateTime(), response.getAdminAction().getMarkedForManualDeletionAt());
        assertEquals(adminActionEntity.isMarkedForManualDeletion(), response.getAdminAction().getIsMarkedForManualDeletion());
        assertEquals(adminActionEntity.getHiddenBy().getId(), response.getAdminAction().getHiddenById());
        assertEquals(adminActionEntity.getMarkedForManualDelBy().getId(), response.getAdminAction().getMarkedForManualDeletionById());
        assertEquals(adminActionEntity.getObjectHiddenReason().getId(), response.getAdminAction().getReasonId());

        HearingEntity hearingEntity = documentEntity.getTranscription().getHearing();
        assertEquals(hearingEntity.getId(), response.getHearing().getId());
        assertEquals(hearingEntity.getHearingDate(), response.getHearing().getHearingDate());

        CourtroomEntity courtroomEntity = documentEntity.getTranscription().getHearing().getCourtroom();
        assertEquals(courtroomEntity.getId(), response.getCourtroom().getId());
        assertEquals(courtroomEntity.getName(), response.getCourtroom().getName());

        CourthouseEntity courthouseEntity = documentEntity.getTranscription().getHearing().getCourtroom().getCourthouse();
        assertEquals(courthouseEntity.getId(), response.getCourthouse().getId());
        assertEquals(courthouseEntity.getDisplayName(), response.getCourthouse().getDisplayName());

        TranscriptionEntity transcriptionEntity = documentEntity.getTranscription();
        assertEquals(transcriptionEntity.getId(), response.getTranscription().getId());

        CourtCaseEntity caseEntity = documentEntity.getTranscription().getHearing().getCourtCase();
        assertEquals(caseEntity.getId(), response.getCase().getId());
        assertEquals(caseEntity.getCaseNumber(), response.getCase().getCaseNumber());
    }


    @Test
    void mapAdminApproveDeletionResponseShouldReturnFullResponse() {
        // Arrange
        Integer documentId = 100;
        boolean isHidden = true;

        TranscriptionDocumentEntity documentEntity = new TranscriptionDocumentEntity();
        documentEntity.setId(documentId);
        documentEntity.setHidden(isHidden);

        Integer objectAdminActionId = 101;
        String comments = "Test comment";
        String reference = "Reference-123";

        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(102);

        ObjectHiddenReasonEntity reasonEntity = mock(ObjectHiddenReasonEntity.class);
        when(reasonEntity.getId()).thenReturn(103);

        OffsetDateTime creationDate = OffsetDateTime.now();
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionId);
        objectAdminActionEntity.setComments(comments);
        objectAdminActionEntity.setTicketReference(reference);
        objectAdminActionEntity.setHiddenBy(userAccountEntity);
        objectAdminActionEntity.setHiddenDateTime(creationDate);
        objectAdminActionEntity.setMarkedForManualDeletion(true);
        objectAdminActionEntity.setMarkedForManualDelBy(userAccountEntity);
        objectAdminActionEntity.setMarkedForManualDelDateTime(creationDate);
        objectAdminActionEntity.setObjectHiddenReason(reasonEntity);

        // Act
        AdminApproveDeletionResponse response = transcriptionResponseMapper.mapAdminApproveDeletionResponse(documentEntity, objectAdminActionEntity);

        // Assert
        assertEquals(documentId, response.getId());
        assertTrue(response.getIsHidden());

        AdminActionResponse adminAction = response.getAdminAction();
        assertNotNull(adminAction);
        assertEquals(objectAdminActionId, adminAction.getId());
        assertEquals(reasonEntity.getId(), adminAction.getReasonId());
        assertEquals(comments, adminAction.getComments());
        assertEquals(reference, adminAction.getTicketReference());
        assertEquals(creationDate, adminAction.getHiddenAt());
        assertEquals(userAccountEntity.getId(), adminAction.getHiddenById());
        assertTrue(adminAction.getIsMarkedForManualDeletion());
        assertEquals(userAccountEntity.getId(), adminAction.getMarkedForManualDeletionById());
        assertEquals(creationDate, adminAction.getMarkedForManualDeletionAt());
    }

    @Test
    void mapToTranscriptionWorkflowsResponseWithLegacyComment() {
        HearingEntity hearing = CommonTestDataUtil.createHearing("1", LocalDate.of(2020, 10, 10));
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing);

        TranscriptionCommentEntity comment1a = new TranscriptionCommentEntity();
        comment1a.setComment("1a");
        comment1a.setCommentTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 1, 0, 0, ZoneOffset.UTC));

        TranscriptionCommentEntity comment1b = new TranscriptionCommentEntity();
        comment1b.setComment("1b");
        comment1b.setCommentTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));


        UserAccountEntity userAccount = CommonTestDataUtil.createUserAccount();
        TranscriptionWorkflowEntity transcriptionWorkflow1 = new TranscriptionWorkflowEntity();
        transcriptionWorkflow1.setTranscription(TestUtils.getFirst(transcriptionList));
        transcriptionWorkflow1.setTranscriptionStatus(CommonTestDataUtil.createTranscriptionStatusEntityFromEnum(TranscriptionStatusEnum.APPROVED));
        transcriptionWorkflow1.setId(1);
        transcriptionWorkflow1.setWorkflowTimestamp(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));
        transcriptionWorkflow1.setTranscriptionComments(List.of(comment1a, comment1b));
        transcriptionWorkflow1.setWorkflowActor(userAccount);

        TranscriptionCommentEntity comment2a = new TranscriptionCommentEntity();
        comment2a.setComment("2a");
        comment2a.setCommentTimestamp(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));

        TranscriptionCommentEntity comment2b = new TranscriptionCommentEntity();
        comment2b.setComment("2b");
        comment2b.setCommentTimestamp(OffsetDateTime.of(2020, 10, 10, 11, 1, 0, 0, ZoneOffset.UTC));

        TranscriptionWorkflowEntity transcriptionWorkflow2 = new TranscriptionWorkflowEntity();
        transcriptionWorkflow2.setTranscription(TestUtils.getFirst(transcriptionList));
        transcriptionWorkflow2.setTranscriptionStatus(CommonTestDataUtil.createTranscriptionStatusEntityFromEnum(TranscriptionStatusEnum.APPROVED));
        transcriptionWorkflow2.setId(2);
        transcriptionWorkflow2.setWorkflowTimestamp(OffsetDateTime.of(2020, 10, 11, 10, 0, 0, 0, ZoneOffset.UTC));
        transcriptionWorkflow2.setTranscriptionComments(List.of(comment2a, comment2b));
        transcriptionWorkflow2.setWorkflowActor(userAccount);

        List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities = List.of(transcriptionWorkflow1, transcriptionWorkflow2);

        TranscriptionCommentEntity comment3a = new TranscriptionCommentEntity();
        comment3a.setComment("3a");
        TranscriptionCommentEntity comment3b = new TranscriptionCommentEntity();
        comment3b.setComment("3b");
        List<TranscriptionCommentEntity> migratedTranscriptionComment = List.of(comment3a, comment3b);

        List<GetTranscriptionWorkflowsResponse> getTranscriptionWorkflowsResponses = transcriptionResponseMapper.mapToTranscriptionWorkflowsResponse(
            transcriptionWorkflowEntities, migratedTranscriptionComment);

        List<String> responseCommentList = getTranscriptionWorkflowsResponses.stream().flatMap(
            response -> response.getComments().stream().map(TranscriptionWorkflowsComment::getComment)).toList();

        assertEquals(6, responseCommentList.size());
        assertEquals("2a", responseCommentList.getFirst());
        assertEquals("2b", responseCommentList.get(1));
        assertEquals("1a", responseCommentList.get(2));
        assertEquals("1b", responseCommentList.get(3));
        assertEquals("3a", responseCommentList.get(4));
        assertEquals("3b", responseCommentList.get(5));
    }

    @Test
    void mapToTranscriptionResponseWithApprovedTimeStamp() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        Set<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptions(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = TestUtils.getFirst(transcriptionList);

        TranscriptionWorkflowEntity transcriptionWorkflow = new TranscriptionWorkflowEntity();
        transcriptionWorkflow.setTranscription(TestUtils.getFirst(transcriptionList));
        transcriptionWorkflow.setTranscriptionStatus(CommonTestDataUtil.createTranscriptionStatusEntityFromEnum(TranscriptionStatusEnum.APPROVED));
        transcriptionWorkflow.setId(1);
        transcriptionWorkflow.setWorkflowTimestamp(OffsetDateTime.of(2020, 10, 10, 11, 0, 0, 0, ZoneOffset.UTC));

        transcriptionEntity.setTranscriptionWorkflowEntities(List.of(transcriptionWorkflow));

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseApprovedSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapTranscriptionDocumentMarkedForDeletion_nullHearing_shouldReutrnTranscriptionCaseDetails() {
        CourtCaseEntity courtCaseEntity = mock(CourtCaseEntity.class);
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        CourthouseEntity courtHouseEntity = mock(CourthouseEntity.class);
        doReturn(courtHouseEntity).when(courtroomEntity).getCourthouse();

        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setHearings(null);
        transcription.setCourtCases(Set.of(courtCaseEntity));
        transcription.setCourtroom(courtroomEntity);
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcription);

        CaseResponseDetails caseResponseDetails = mock(CaseResponseDetails.class);
        CourtroomResponseDetails courtroomResponseDetails = mock(CourtroomResponseDetails.class);
        CourthouseResponseDetails courthouseResponseDetails = mock(CourthouseResponseDetails.class);
        TranscriptionResponseDetails transcriptionResponseDetails = mock(TranscriptionResponseDetails.class);

        doReturn(caseResponseDetails).when(transcriptionResponseMapper).mapCase(courtCaseEntity);
        doReturn(courtroomResponseDetails).when(transcriptionResponseMapper).mapCourtroom(courtroomEntity);
        doReturn(courthouseResponseDetails).when(transcriptionResponseMapper).mapCourthouse(courtHouseEntity);
        doReturn(transcriptionResponseDetails).when(transcriptionResponseMapper).mapTranscriptionEntity(transcription);

        AdminMarkedForDeletionResponseItem responseDetails = transcriptionResponseMapper
            .mapTranscriptionDocumentMarkedForDeletion(transcriptionDocumentEntity);


        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getCase()).isEqualTo(caseResponseDetails);
        assertThat(responseDetails.getCourtroom()).isEqualTo(courtroomResponseDetails);
        assertThat(responseDetails.getCourthouse()).isEqualTo(courthouseResponseDetails);
        assertThat(responseDetails.getTranscription()).isEqualTo(transcriptionResponseDetails);
        assertThat(responseDetails.getHearing()).isNull();
    }

    @Test
    void mapTranscriptionDocumentMarkedForDeletion_nullHearingAndNullTranscriptionCourtroom_shouldReutrnCourtHouseFromCase() {
        CourtCaseEntity courtCaseEntity = mock(CourtCaseEntity.class);
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        CourthouseEntity courtHouseEntity = mock(CourthouseEntity.class);
        doReturn(courtHouseEntity).when(courtCaseEntity).getCourthouse();

        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setHearings(null);
        transcription.setCourtCases(Set.of(courtCaseEntity));
        transcription.setCourtroom(courtroomEntity);
        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setTranscription(transcription);

        CaseResponseDetails caseResponseDetails = mock(CaseResponseDetails.class);
        CourtroomResponseDetails courtroomResponseDetails = mock(CourtroomResponseDetails.class);
        CourthouseResponseDetails courthouseResponseDetails = mock(CourthouseResponseDetails.class);
        TranscriptionResponseDetails transcriptionResponseDetails = mock(TranscriptionResponseDetails.class);

        doReturn(caseResponseDetails).when(transcriptionResponseMapper).mapCase(courtCaseEntity);
        doReturn(courtroomResponseDetails).when(transcriptionResponseMapper).mapCourtroom(courtroomEntity);
        doReturn(courthouseResponseDetails).when(transcriptionResponseMapper).mapCourthouse(courtHouseEntity);
        doReturn(transcriptionResponseDetails).when(transcriptionResponseMapper).mapTranscriptionEntity(transcription);

        AdminMarkedForDeletionResponseItem responseDetails = transcriptionResponseMapper
            .mapTranscriptionDocumentMarkedForDeletion(transcriptionDocumentEntity);

        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getCase()).isEqualTo(caseResponseDetails);
        assertThat(responseDetails.getCourtroom()).isEqualTo(courtroomResponseDetails);
        assertThat(responseDetails.getCourthouse()).isEqualTo(courthouseResponseDetails);
        assertThat(responseDetails.getTranscription()).isEqualTo(transcriptionResponseDetails);
        assertThat(responseDetails.getHearing()).isNull();
    }

    @Test
    void mapTranscriptionEntity_whenNull_shouldReturnNull() {
        assertNull(transcriptionResponseMapper.mapTranscriptionEntity(null));
    }

    @Test
    void mapTranscriptionEntity_whenNotNull_shouldReturnCorrectlyMappedData() {
        TranscriptionEntity transcription = new TranscriptionEntity();
        transcription.setId(123);

        TranscriptionResponseDetails responseDetails = transcriptionResponseMapper.mapTranscriptionEntity(transcription);
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getId()).isEqualTo(123);
    }

    @Test
    void mapHearing_whenNull_shouldReturnNull() {
        assertNull(transcriptionResponseMapper.mapHearing(null));
    }

    @Test
    void mapHearing_whenNotNull_shouldReturnCorrectlyMappedData() {
        HearingEntity hearing = new HearingEntity();
        hearing.setId(123);
        hearing.setHearingDate(LocalDate.of(2023, 6, 20));

        HearingResponseDetails responseDetails = transcriptionResponseMapper.mapHearing(hearing);
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getId()).isEqualTo(123);
        assertThat(responseDetails.getHearingDate()).isEqualTo(LocalDate.of(2023, 6, 20));
    }

    @Test
    void mapCase_whenNull_shouldReturnNull() {
        assertNull(transcriptionResponseMapper.mapCase(null));
    }

    @Test
    void mapCase_whenNotNull_shouldReturnCorrectlyMappedData() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setId(123);
        courtCase.setCaseNumber("some-case-number");

        CaseResponseDetails responseDetails = transcriptionResponseMapper.mapCase(courtCase);
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getId()).isEqualTo(123);
        assertThat(responseDetails.getCaseNumber()).isEqualTo("some-case-number");
    }

    @Test
    void mapCourtroom_whenNull_shouldReturnNull() {
        assertNull(transcriptionResponseMapper.mapCourtroom(null));
    }

    @Test
    void mapCourtroom_whenNotNull_shouldReturnCorrectlyMappedData() {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setId(123);
        courtroom.setName("some-name");

        CourtroomResponseDetails responseDetails = transcriptionResponseMapper.mapCourtroom(courtroom);
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getId()).isEqualTo(123);
        assertThat(responseDetails.getName()).isEqualTo("SOME-NAME");
    }

    @Test
    void mapCourthouse_whenNull_shouldReturnNull() {
        assertNull(transcriptionResponseMapper.mapTranscriptionEntity(null));
    }

    @Test
    void mapCourthouse_whenNotNull_shouldReturnCorrectlyMappedData() {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setId(123);
        courthouse.setDisplayName("some-display-name");

        CourthouseResponseDetails responseDetails = transcriptionResponseMapper.mapCourthouse(courthouse);
        assertThat(responseDetails).isNotNull();
        assertThat(responseDetails.getId()).isEqualTo(123);
        assertThat(responseDetails.getDisplayName()).isEqualTo("some-display-name");
    }

    @Test
    void getFirstNotNull_whenAllNull_shouldReturnNull() {
        assertNull(transcriptionResponseMapper.getFirstNotNull(null, null, null));
    }

    @Test
    void getFirstNotNull_whenFirstNull_shouldReturnSecond() {
        assertEquals("second", transcriptionResponseMapper.getFirstNotNull(null, "second", null));
    }

    @Test
    void getFirstNotNull_whenFirstNotNull_shouldReturnFirst() {
        assertEquals("first", transcriptionResponseMapper.getFirstNotNull("first", "second", null));
    }
}