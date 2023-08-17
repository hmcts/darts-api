package uk.gov.hmcts.darts.cases.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.TestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
class CaseServiceImplTest {

    public static final String SWANSEA = "SWANSEA";
    public static final String TEST_COURT_CASE = "case_courthouse";
    private static final LocalDate HEARING_DATE = LocalDate.of(1990, Month.FEBRUARY, 19);

    CaseServiceImpl service;

    @Mock
    CaseRepository caseRepository;

    @Mock
    HearingRepository hearingRepository;

    CasesMapper mapper;

    @Mock
    RetrieveCoreObjectService retrieveCoreObjectService;

    @Mock
    AdvancedSearchRequestHelper advancedSearchRequestHelper;

    @Captor
    ArgumentCaptor<CourtCaseEntity> caseEntityArgumentCaptor;

    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityCaptor;

    @BeforeEach
    void setUp() {
        mapper = new CasesMapper(retrieveCoreObjectService);
        service = new CaseServiceImpl(
            mapper,
            hearingRepository,
            caseRepository,
            retrieveCoreObjectService,
            advancedSearchRequestHelper
        );
        this.objectMapper = TestUtils.getObjectMapper();
    }

    @Test
    void testGetCasesById() throws Exception {

        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");
        Mockito.when(caseRepository.findById(any())).thenReturn(Optional.of(courtCaseEntity));

        SingleCase result = service.getCasesById(101);

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetCasesById/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void testGetCasesWithMultipleHearing() throws IOException {

        List<HearingEntity> hearingEntities = CommonTestDataUtil.createHearings(8);
        Mockito.when(hearingRepository.findByCourthouseCourtroomAndDate(
            any(),
            any(),
            any()
        )).thenReturn(hearingEntities);

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(SWANSEA);
        request.setCourtroom("1");
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getHearings(request);
        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetCasesWithMultipleHearing/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testGetCasesWithSingleHearingAndDifferentCourtroom() throws IOException {
        HearingEntity hearingEntity = createHearingEntity();
        Mockito.when(hearingRepository.findByCourthouseCourtroomAndDate(
            any(),
            any(),
            any()
        )).thenReturn(Collections.singletonList(hearingEntity));

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(SWANSEA);
        request.setCourtroom("2");
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getHearings(request);
        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetCasesWithSingleHearingAndDifferentCourtroom/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    void testGetCasesCreateCourtroom() {
        String courtroomName = "99";

        Mockito.when(hearingRepository.findByCourthouseCourtroomAndDate(
            any(),
            any(),
            any()
        )).thenReturn(Collections.emptyList());

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(SWANSEA);
        request.setCourtroom(courtroomName);
        request.setDate(LocalDate.of(2023, 6, 20));

        service.getHearings(request);
        Mockito.verify(retrieveCoreObjectService).retrieveOrCreateCourtroom(eq(SWANSEA), eq("99"));
    }

    @Test
    void testAddCase() throws IOException {
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });
        CourtCaseEntity courtCase = CommonTestDataUtil.createCase("testAddCase");
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(courtCase);
        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest();
        PostCaseResponse result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verifyNoInteractions(hearingRepository);

        CourtCaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(TEST_COURT_CASE, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("testAddCase", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendantList());
        assertNotNull(savedCaseEntity.getProsecutorList());
        assertNotNull(savedCaseEntity.getDefenceList());

        String actualResponse = objectMapper.writeValueAsString(result);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testAddCase/expectedResponseWithoutCourtroom.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testAddCaseNonExistingCourthouse() {

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest();
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCase(
            anyString(),
            anyString()
        )).thenThrow(new DartsApiException(
            CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST));

        DartsApiException thrownException = assertThrows(
            DartsApiException.class,
            () -> service.addCaseOrUpdate(request)
        );

        assertEquals("Provided courthouse does not exist", thrownException.getMessage());
    }

    @Test
    void testGetCaseHearings() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            LocalDate.now()
        ));

        Mockito.when(hearingRepository.findByCaseIds(List.of(existingCaseEntity.getId()))).thenReturn(existingHearings);

        List<Hearing> caseHearings = service.getCaseHearings(existingCaseEntity.getId());

        assertEquals(existingHearings.get(0).getId(), caseHearings.get(0).getId());
        assertEquals(existingHearings.get(0).getCourtroom().getName(), caseHearings.get(0).getCourtroom());
        assertEquals(existingHearings.get(0).getHearingDate(), caseHearings.get(0).getDate());

    }

    @Test
    void testUpdateCaseWithNonExistingCourtroomAndMatchingHearingDate() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);
        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());


    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithOldHearingDateWithCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);

        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());
    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);

        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());
    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithoutCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);
        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verifyNoInteractions(hearingRepository);

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());

    }


    private HearingEntity createHearingEntity() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "2");
        CourtCaseEntity caseEntity = CommonTestDataUtil.createCase("Case0000009", courthouseEntity);
        return CommonTestDataUtil.createHearing(caseEntity, courtroomEntity, LocalDate.of(2023, Month.JULY, 20));
    }
}
