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
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;


@ExtendWith(MockitoExtension.class)
class TranscriptionResponseMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    private TranscriptionResponseMapper transcriptionResponseMapper;

    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @BeforeEach
    void setUp() {
        transcriptionResponseMapper = new TranscriptionResponseMapper(hearingReportingRestrictionsRepository);
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
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseNoStatusSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoHearingIdAndValidCourtCase() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);
        transcriptionEntity.setHearings(new ArrayList<>());
        transcriptionEntity.setHearingDate(LocalDate.of(2023, 6, 20));
        transcriptionEntity.setCourtCases(List.of(CommonTestDataUtil.createCase("case1")));
        transcriptionEntity.setCourtroom(CommonTestDataUtil.createCourtroom("1"));
        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityNoHearing.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoHearingsAndNoValidCourtCaseShouldFail() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);
        transcriptionEntity.setHearings(new ArrayList<>());
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
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

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
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseIsAutomated() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);
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
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);


        var exception = assertThrows(
            DartsApiException.class,
            () -> transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity)
        );

        assertEquals(TranscriptionApiError.TRANSCRIPTION_NOT_FOUND, exception.getError());
    }

    @Test
    void mapToTranscriptionResponseWithRequestor() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false, true);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        GetTranscriptionByIdResponse transcriptionResponse =
            transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityWithRequestor.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
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

        List<HearingEntity> hearingEntityList = new ArrayList<>();

        // create the transaction to be mapped
        Integer transactionId = 200;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transactionId);
        transcriptionEntity.setHearings(hearingEntityList);
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

        List<HearingEntity> hearingEntityList = new ArrayList<>();

        // create the transaction to be mapped
        Integer transactionId = 200;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transactionId);
        transcriptionEntity.setHearings(hearingEntityList);
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

        List<HearingEntity> hearingEntityList = new ArrayList<>();

        // create the transaction to be mapped
        Integer transactionId = 200;
        TranscriptionEntity transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(transactionId);
        transcriptionEntity.setHearings(hearingEntityList);
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

        Integer hearingCaseId = 1000;
        String hearingCaseNumber = "hearing case" + caseId;

        Integer courthouseId = 906;
        String courthouseDisplayNumber = "courthouse" + caseId;

        Integer hearingcourthouseId = 901;
        String hearingcourthouseDisplayName = "hearingcourthouse" + caseId;

        Integer hearingid = 902;
        LocalDate hearingDate = LocalDate.now().plusMonths(10);

        boolean isManualTranscription = true;
        boolean isHidden = false;

        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             caseId,
                                                                             caseNumber,
                                                                             hearingCaseId,
                                                                             hearingCaseNumber,
                                                                             courthouseId,
                                                                             courthouseDisplayNumber,
                                                                             hearingcourthouseId,
                                                                             hearingcourthouseDisplayName,
                                                                             hearingid,
                                                                             hearingDate,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertEquals(hearingCaseId, response.getCase().getId());
        assertEquals(hearingCaseNumber, response.getCase().getCaseNumber());
        assertEquals(hearingid, response.getHearing().getId());
        assertEquals(hearingDate, response.getHearing().getHearingDate());

        // ensure we prioritise the courthouse directly mapped to the transcription not the hearing courthouse
        assertEquals(hearingcourthouseId, response.getCourthouse().getId());
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

        Integer hearingCaseId = 1000;
        String hearingCaseNumber = "hearing case" + caseId;

        Integer hearingid = 902;
        LocalDate hearingDate = LocalDate.now().plusMonths(10);

        Integer courthouseId = 906;
        String courthouseDisplayName = "courthouse" + caseId;

        boolean isManualTranscription = true;
        boolean isHidden = false;

        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             caseId,
                                                                             caseNumber,
                                                                             hearingCaseId,
                                                                             hearingCaseNumber,
                                                                             courthouseId,
                                                                             courthouseDisplayName,
                                                                             null,
                                                                             null,
                                                                             hearingid,
                                                                             hearingDate,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertEquals(hearingCaseId, response.getCase().getId());
        assertEquals(hearingCaseNumber, response.getCase().getCaseNumber());
        assertEquals(hearingid, response.getHearing().getId());
        assertEquals(hearingDate, response.getHearing().getHearingDate());
        assertEquals(courthouseId, response.getCourthouse().getId());
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

        Integer hearingid = 902;
        LocalDate hearingDate = LocalDate.now().plusMonths(10);

        Integer courthouseId = 906;
        String courthouseDisplayName = "courthouse" + caseId;

        boolean isManualTranscription = true;
        boolean isHidden = false;

        TranscriptionDocumentResult result = new TranscriptionDocumentResult(transcriptionDocumentId,
                                                                             transactionId,
                                                                             caseId,
                                                                             caseNumber,
                                                                             null,
                                                                             null,
                                                                             courthouseId,
                                                                             courthouseDisplayName,
                                                                             null,
                                                                             null,
                                                                             hearingid,
                                                                             hearingDate,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertEquals(caseId, response.getCase().getId());
        assertEquals(caseNumber, response.getCase().getCaseNumber());
        assertEquals(hearingid, response.getHearing().getId());
        assertEquals(hearingDate, response.getHearing().getHearingDate());
        assertEquals(courthouseId, response.getCourthouse().getId());
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
                                                                             null,
                                                                             null,
                                                                             null,
                                                                             isManualTranscription,
                                                                             isHidden);


        SearchTranscriptionDocumentResponse response = transcriptionResponseMapper.mapSearchTranscriptionDocumentResult(result);
        assertEquals(transcriptionDocumentId, response.getTranscriptionDocumentId());
        assertEquals(transactionId, response.getTranscriptionId());
        assertNull(response.getCase().getId());
        assertNull(response.getHearing());
        assertNull(response.getCourthouse().getId());
        assertEquals(isManualTranscription, response.getIsManualTranscription());
        assertEquals(isHidden, response.getIsHidden());
    }
}