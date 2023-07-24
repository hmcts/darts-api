package uk.gov.hmcts.darts.cases.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.TestUtils;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
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

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class CaseServiceImplTest {

    public static final String SWANSEA = "SWANSEA";
    @InjectMocks
    CaseServiceImpl service;

    @Mock
    CourtroomRepository courtroomRepository;

    @Mock
    CaseRepository caseRepository;

    @Mock
    HearingRepository hearingRepository;

    @Mock
    CourthouseRepository courthouseRepository;

    @Mock
    CommonApi commonApi;

    @Captor
    ArgumentCaptor<CaseEntity> caseEntityArgumentCaptor;

    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityCaptor;

    @BeforeEach
    void setUp() {
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
        Mockito.verify(commonApi).retrieveOrCreateCourtroom(eq(SWANSEA), eq("99"));
    }

    @Test
    void testAddCaseWithCourtroom() throws IOException {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");

        Mockito.when(courthouseRepository.findByCourthouseName(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(courtroomRepository.findByNames(any(), any())).thenReturn(courtroomEntity);
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

        CaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity savedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("2", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendants());
        assertNotNull(savedCaseEntity.getProsecutors());
        assertNotNull(savedCaseEntity.getDefenders());


        assertEquals("1", savedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, savedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertNotNull(savedHearingEntity.getJudges());
        assertNotNull(savedHearingEntity.getCourtCase());

        String actualResponse = objectMapper.writeValueAsString(result);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/addCase/expectedResponseWithCourtroom.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void testAddCaseWithoutCourtroom() throws IOException {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);

        Mockito.when(courthouseRepository.findByCourthouseName(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest(null);
        ScheduledCase result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verifyNoInteractions(hearingRepository);

        CaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("2", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendants());
        assertNotNull(savedCaseEntity.getProsecutors());
        assertNotNull(savedCaseEntity.getDefenders());

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
    void testAddCaseNonExistingCourtroom() {

        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);

        Mockito.when(courthouseRepository.findByCourthouseName(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });


        String courtroom = "1";
        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest(courtroom);

        DartsApiException thrownException = assertThrows(
            DartsApiException.class,
            () -> service.addCaseOrUpdate(request)
        );

        assertEquals("Provided courtroom does not exist", thrownException.getMessage());
    }

    @Test
    void testUpdateCaseWithExistingHearing() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseName(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(courtroomRepository.findByNames(any(), any())).thenReturn(courtroomEntity);

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

        CaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity updatedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendants().size());
        assertEquals(3, updatedCaseEntity.getProsecutors().size());
        assertEquals(3, updatedCaseEntity.getDefenders().size());


        assertEquals("1", updatedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, updatedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(1, updatedHearingEntity.getJudges().size());
        assertEquals(LocalDate.now(), updatedHearingEntity.getHearingDate());
        assertNotNull(updatedHearingEntity.getCourtCase());
    }


    @Test
    void testUpdateCaseWithMultipleHearingsWithCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseName(any())).thenReturn(Optional.of(courthouseEntity));
        Mockito.when(courtroomRepository.findByNames(any(), any())).thenReturn(courtroomEntity);

        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

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

        CaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();
        HearingEntity updatedHearingEntity = hearingEntityCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendants().size());
        assertEquals(3, updatedCaseEntity.getProsecutors().size());
        assertEquals(3, updatedCaseEntity.getDefenders().size());


        assertEquals("1", updatedHearingEntity.getCourtroom().getName());
        assertEquals(SWANSEA, updatedHearingEntity.getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(1, updatedHearingEntity.getJudges().size());
        assertEquals(LocalDate.now(), updatedHearingEntity.getHearingDate());
        assertNotNull(updatedHearingEntity.getCourtCase());
    }

    @Test
    void testUpdateCaseWithMultipleHearingsWithoutCourtroomInRequest() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);

        Mockito.when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName(anyString(), anyString()))
            .thenReturn(Optional.of(existingCaseEntity));
        Mockito.when(courthouseRepository.findByCourthouseName(any())).thenReturn(Optional.of(courthouseEntity));

        Mockito.when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest(null);
        ScheduledCase result = service.addCaseOrUpdate(request);

        Mockito.verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        Mockito.verifyNoInteractions(hearingRepository);

        CaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendants().size());
        assertEquals(3, updatedCaseEntity.getProsecutors().size());
        assertEquals(3, updatedCaseEntity.getDefenders().size());

        assertNull(result.getJudges());

    }


    private HearingEntity createHearingEntity() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(
            SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "2");
        CaseEntity caseEntity = CommonTestDataUtil.createCase(
            "Case0000009",
            courthouseEntity
        );
        return CommonTestDataUtil.createHearing(
            caseEntity,
            courtroomEntity,
            LocalDate.of(2023, Month.JULY, 20), Collections.singletonList("Judge2"), LocalTime.of(9, 0, 0)
        );
    }
}
