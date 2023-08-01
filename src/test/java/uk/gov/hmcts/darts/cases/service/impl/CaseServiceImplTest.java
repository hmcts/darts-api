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
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.TestUtils;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.common.util.TestUtils.substituteHearingDateWithToday;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class CaseServiceImplTest {

    public static final String SWANSEA = "SWANSEA";
    private static final LocalDate HEARING_DATE = LocalDate.of(1990, Month.FEBRUARY, 19);

    CaseServiceImpl service;

    @Mock
    CaseRepository caseRepository;

    @Mock
    HearingRepository hearingRepository;

    @Mock
    CourthouseRepository courthouseRepository;

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
        mapper = new CasesMapper(courthouseRepository);
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
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
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

        List<ScheduledCase> resultList = service.getCases(request);
        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile("Tests/cases/CaseServiceTest/getCasesOk1/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
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

        List<ScheduledCase> resultList = service.getCases(request);
        String actualResponse = objectMapper.writeValueAsString(resultList);
        String expectedResponse = getContentsFromFile("Tests/cases/CaseServiceTest/getCasesOk2/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
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

        service.getCases(request);
        Mockito.verify(retrieveCoreObjectService).retrieveOrCreateCourtroom(eq(SWANSEA), eq("99"));
    }

    @Test
    void testAddCaseWithCourtroom() throws IOException {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");

        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString())).thenReturn(
            courtroomEntity);
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        Mockito.when(hearingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        String courtroom = "1";
        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest(courtroom);
        ScheduledCase result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityCaptor.capture());

        CourtCaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity savedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("2", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendantList());
        assertNotNull(savedCaseEntity.getProsecutorList());
        assertNotNull(savedCaseEntity.getDefenceList());


        assertEquals("1", savedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, savedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertNotNull(savedHearingEntity.getJudgeList());
        assertNotNull(savedHearingEntity.getCourtCase());

        String actualResponse = objectMapper.writeValueAsString(result);
        String expectedResponse = substituteHearingDateWithToday(getContentsFromFile(
            "Tests/cases/CaseServiceTest/addCase/expectedResponseWithCourtroom.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void testAddCaseWithoutCourtroom() throws IOException {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);

        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest(null);
        ScheduledCase result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verifyNoInteractions(hearingRepository);

        CourtCaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("2", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendantList());
        assertNotNull(savedCaseEntity.getProsecutorList());
        assertNotNull(savedCaseEntity.getDefenceList());

        String actualResponse = objectMapper.writeValueAsString(result);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/addCase/expectedResponseWithoutCourtroom.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testAddCaseNonExistingCourthouse() {

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest(null);

        DartsApiException thrownException = assertThrows(
            DartsApiException.class,
            () -> service.addCaseOrUpdate(request)
        );

        assertEquals("Provided courthouse does not exist", thrownException.getMessage());
    }

    @Test
    void testAddCaseNonExistingCourtroom() throws IOException {

        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");

        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        Mockito.when(hearingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString())).thenReturn(
            courtroomEntity);


        String courtroom = "1";
        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest(courtroom);
        ScheduledCase result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityCaptor.capture());

        CourtCaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity savedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("2", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendantList());
        assertNotNull(savedCaseEntity.getProsecutorList());
        assertNotNull(savedCaseEntity.getDefenceList());


        assertEquals("1", savedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, savedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertNotNull(savedHearingEntity.getJudgeList());
        assertNotNull(savedHearingEntity.getCourtCase());

        String actualResponse = objectMapper.writeValueAsString(result);
        String expectedResponse = substituteHearingDateWithToday(getContentsFromFile(
            "Tests/cases/CaseServiceTest/addCase/expectedResponseWithCourtroom.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testUpdateCaseWithExistingHearing() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString())).thenReturn(
            courtroomEntity);
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            LocalDate.now()
        ));

        Mockito.when(hearingRepository.findAll()).thenReturn(existingHearings);
        Mockito.when(hearingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest("1");
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity updatedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());


        assertEquals("1", updatedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, updatedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(1, updatedHearingEntity.getJudgeList().size());
        assertEquals(LocalDate.now(), updatedHearingEntity.getHearingDate());
        assertNotNull(updatedHearingEntity.getCourtCase());
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
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));

        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(
            anyString(),
            anyString()
        )).thenReturn(CommonTestDataUtil.createCourtroom(courthouseEntity, "2"));
        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            LocalDate.now()
        ));

        Mockito.when(hearingRepository.findAll()).thenReturn(existingHearings);
        Mockito.when(hearingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest("2");
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity newHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());


        assertEquals("2", newHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, newHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(1, newHearingEntity.getJudgeList().size());
        assertEquals(LocalDate.now(), newHearingEntity.getHearingDate());
        assertNotNull(newHearingEntity.getCourtCase());
    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithOldHearingDateWithCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));

        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString())).thenReturn(
            courtroomEntity);
        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            HEARING_DATE
        ), CommonTestDataUtil.createHearing(
            existingCaseEntity,
            CommonTestDataUtil.createCourtroom(courthouseEntity, "2"),
            HEARING_DATE
        ));

        Mockito.when(hearingRepository.findAll()).thenReturn(existingHearings);
        Mockito.when(hearingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest("1");
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity newHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());


        assertEquals("1", newHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, newHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(1, newHearingEntity.getJudgeList().size());
        assertEquals(LocalDate.now(), newHearingEntity.getHearingDate());
        assertNotNull(newHearingEntity.getCourtCase());
    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));

        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateCourtroom(anyString(), anyString())).thenReturn(
            courtroomEntity);
        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            LocalDate.now()
        ), CommonTestDataUtil.createHearing(
            existingCaseEntity,
            CommonTestDataUtil.createCourtroom(courthouseEntity, "2"),
            LocalDate.now()
        ));

        Mockito.when(hearingRepository.findAll()).thenReturn(existingHearings);
        Mockito.when(hearingRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest("1");
        service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity updatedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());


        assertEquals("1", updatedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, updatedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(1, updatedHearingEntity.getJudgeList().size());
        assertEquals(LocalDate.now(), updatedHearingEntity.getHearingDate());
        assertNotNull(updatedHearingEntity.getCourtCase());
    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithoutCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));

        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest(null);
        ScheduledCase result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verifyNoInteractions(hearingRepository);

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());

        assertNull(result.getJudges());

    }


    private HearingEntity createHearingEntity() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(
            SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "2");
        CourtCaseEntity caseEntity = CommonTestDataUtil.createCase(
            "Case0000009",
            courthouseEntity
        );
        return CommonTestDataUtil.createHearing(
            caseEntity,
            courtroomEntity,
            LocalDate.of(2023, Month.JULY, 20)
        );
    }
}
