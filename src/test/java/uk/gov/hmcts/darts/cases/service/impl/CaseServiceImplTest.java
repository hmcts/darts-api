package uk.gov.hmcts.darts.cases.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.helper.AdminCasesSearchRequestHelper;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.CaseTranscriptionMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesAnnotationMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createEventWith;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals"})
class CaseServiceImplTest {

    public static final String SWANSEA = "SWANSEA";
    public static final String TEST_COURT_CASE = "case_courthouse";
    public static final OffsetDateTime FIXED_DATETIME = OffsetDateTime.of(2024, 3, 25, 10, 0, 0, 0, ZoneOffset.UTC);

    CaseServiceImpl service;

    @Mock
    CaseRepository caseRepository;

    @Mock
    HearingRepository hearingRepository;

    @Mock
    EventRepository eventRepository;


    @Mock
    HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    CasesMapper mapper;

    CasesAnnotationMapper annotationMapper;

    @Mock
    RetrieveCoreObjectService retrieveCoreObjectService;

    @Mock
    AdvancedSearchRequestHelper advancedSearchRequestHelper;
    @Mock
    AdminCasesSearchRequestHelper adminCasesSearchRequestHelper;

    @Mock
    TranscriptionRepository transcriptionRepository;
    @Captor
    ArgumentCaptor<CourtCaseEntity> caseEntityArgumentCaptor;

    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private AnnotationRepository annotationRepository;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private LogApi logApi;
    private ObjectMapper objectMapper;

    @Mock
    private CaseTranscriptionMapper caseTranscriptionMapper;

    @BeforeEach
    void setUp() {
        Pattern unallocatedCaseRegex = Pattern.compile(".*\\d{8}-\\d{6}.*");
        mapper = new CasesMapper(retrieveCoreObjectService, hearingReportingRestrictionsRepository, caseRetentionRepository, authorisationApi, logApi,
                                 unallocatedCaseRegex);
        service = new CaseServiceImpl(
            mapper,
            annotationMapper,
            hearingRepository,
            eventRepository,
            caseRepository,
            annotationRepository,
            retrieveCoreObjectService,
            advancedSearchRequestHelper,
            adminCasesSearchRequestHelper,
            transcriptionRepository,
            authorisationApi,
            logApi,
            new CaseTranscriptionMapper()
        );
        this.objectMapper = TestUtils.getObjectMapper();
    }

    @Test
    void testGetCasesById() throws Exception {

        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");
        when(caseRepository.findById(any())).thenReturn(Optional.of(courtCaseEntity));

        SingleCase result = service.getCasesById(101);

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetCasesById/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void testGetCasesWithMultipleHearing() throws IOException {

        List<HearingEntity> hearingEntities = CommonTestDataUtil.createHearings(8);
        when(hearingRepository.findByCourthouseCourtroomAndDate(
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

        verify(logApi, times(1)).casesRequestedByDarPc(request);
    }

    @Test
    void testGetCasesWithSingleHearingAndDifferentCourtroom() throws IOException {
        HearingEntity hearingEntity = createHearingEntity();
        when(hearingRepository.findByCourthouseCourtroomAndDate(
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
        verify(logApi, times(1)).casesRequestedByDarPc(request);
    }


    @Test
    void testGetCasesCreateCourtroom() {
        String courtroomName = "99";

        when(hearingRepository.findByCourthouseCourtroomAndDate(
            any(),
            any(),
            any()
        )).thenReturn(Collections.emptyList());

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(SWANSEA);
        request.setCourtroom(courtroomName);
        request.setDate(LocalDate.of(2023, 6, 20));

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(10);
        when(authorisationApi.getCurrentUser()).thenReturn(userAccount);

        service.getHearings(request);
        verify(retrieveCoreObjectService).retrieveOrCreateCourtroom(eq(SWANSEA), eq("99"), any(UserAccountEntity.class));
        verify(logApi, times(1)).casesRequestedByDarPc(request);
    }

    @Test
    void testAddCase() throws IOException {
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });
        CourtCaseEntity courtCase = CommonTestDataUtil.createCase("testAddCase");
        when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(courtCase);
        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest();

        PostCaseResponse result = service.addCaseOrUpdate(request);

        String actualResponse = TestUtils.removeTags(List.of("case_id"), objectMapper.writeValueAsString(result));
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testAddCase/expectedResponseWithoutCourtroom.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

        verify(caseRepository, times(1)).saveAndFlush(caseEntityArgumentCaptor.capture());
        verifyNoInteractions(hearingRepository);

        CourtCaseEntity savedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(TEST_COURT_CASE, savedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals(FIXED_DATETIME, savedCaseEntity.getCreatedDateTime());
        assertEquals(FIXED_DATETIME, savedCaseEntity.getLastModifiedDateTime());
        assertEquals("testUsername", savedCaseEntity.getCreatedBy().getUserName());
        assertEquals("testUsername", savedCaseEntity.getLastModifiedBy().getUserName());
        assertEquals("testAddCase", savedCaseEntity.getCaseNumber());
        assertNotNull(savedCaseEntity.getDefendantList());
        assertNotNull(savedCaseEntity.getProsecutorList());
        assertNotNull(savedCaseEntity.getDefenceList());

    }

    @Test
    void testAddCaseNonExistingCourthouse() {

        AddCaseRequest request = CommonTestDataUtil.createAddCaseRequest();
        when(retrieveCoreObjectService.retrieveOrCreateCase(
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

        when(hearingRepository.findByCaseIds(List.of(existingCaseEntity.getId()))).thenReturn(existingHearings);

        List<Hearing> caseHearings = service.getCaseHearings(existingCaseEntity.getId());

        assertEquals(existingHearings.get(0).getId(), caseHearings.get(0).getId());
        assertEquals(existingHearings.get(0).getCourtroom().getName(), caseHearings.get(0).getCourtroom());
        assertEquals(existingHearings.get(0).getHearingDate(), caseHearings.get(0).getDate());

    }

    @Test
    void testGetEventsByCaseId() throws Exception {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");
        OffsetDateTime hearingDate = OffsetDateTime.parse("2024-07-01T12:00Z");

        HearingEntity hearing = CommonTestDataUtil.createHearing(
            courtCaseEntity,
            courtroomEntity,
            hearingDate.toLocalDate()
        );

        List<EventEntity> events = Lists.newArrayList(createEventWith("eventName", "event", hearing, hearingDate));

        when(eventRepository.findAllByCaseId(courtCaseEntity.getId())).thenReturn(events);

        List<Event> result = service.getEventsByCaseId(courtCaseEntity.getId());

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetEventsByCase/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testUpdateCaseWithNonExistingCourtroomAndMatchingHearingDate() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);
        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

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

        when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);

        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

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

        when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);

        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

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

        when(retrieveCoreObjectService.retrieveOrCreateCase(anyString(), anyString())).thenReturn(
            existingCaseEntity);
        JudgeEntity judge = CommonTestDataUtil.createJudge("Judge_1");
        when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString())).thenReturn(judge);
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[0];
        });

        AddCaseRequest request = CommonTestDataUtil.createUpdateCaseRequest();
        service.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        verifyNoInteractions(hearingRepository);

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