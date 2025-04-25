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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.helper.AdminCasesSearchRequestHelper;
import uk.gov.hmcts.darts.cases.helper.AdvancedSearchRequestHelper;
import uk.gov.hmcts.darts.cases.mapper.CaseTranscriptionMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesAnnotationMapper;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.AdminSingleCaseResponseItem;
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
import uk.gov.hmcts.darts.common.entity.EventEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
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
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.util.pagination.PaginatedList;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createEventWith;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.ExcessiveImports", "PMD.AvoidDuplicateLiterals",
    "PMD.CouplingBetweenObjects"//Required to accutatly test the class
})
class CaseServiceImplTest {

    private static final String SWANSEA = "SWANSEA";
    private static final String TEST_COURT_CASE = "CASE_COURTHOUSE";
    private static final OffsetDateTime FIXED_DATETIME = OffsetDateTime.of(2024, 3, 25, 10, 0, 0, 0, ZoneOffset.UTC);

    private CaseServiceImpl caseService;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;

    @Mock
    private AdvancedSearchRequestHelper advancedSearchRequestHelper;
    @Mock
    private AdminCasesSearchRequestHelper adminCasesSearchRequestHelper;

    @Mock
    private TranscriptionRepository transcriptionRepository;
    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Captor
    private ArgumentCaptor<CourtCaseEntity> caseEntityArgumentCaptor;

    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private AnnotationRepository annotationRepository;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private LogApi logApi;
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        Pattern unallocatedCaseRegex = Pattern.compile(".*\\d{8}-\\d{6}.*");
        CasesMapper casesMapper = new CasesMapper(retrieveCoreObjectService, hearingReportingRestrictionsRepository, caseRetentionRepository, authorisationApi,
                                                  logApi,
                                                  unallocatedCaseRegex);

        CasesAnnotationMapper annotationMapper = new CasesAnnotationMapper();
        caseService = new CaseServiceImpl(
            casesMapper,
            annotationMapper,
            hearingRepository,
            eventRepository,
            caseRepository,
            annotationRepository,
            retrieveCoreObjectService,
            advancedSearchRequestHelper,
            adminCasesSearchRequestHelper,
            transcriptionRepository,
            transcriptionDocumentRepository,
            authorisationApi,
            logApi,
            new CaseTranscriptionMapper()
        );
        this.objectMapper = TestUtils.getObjectMapper();
    }

    @Test
    void testGetCasesById() throws Exception {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        CourtCaseEntity courtCaseEntity = hearings.getFirst().getCourtCase();
        courtCaseEntity.setHearings(hearings);
        when(caseRepository.findById(any())).thenReturn(Optional.of(courtCaseEntity));

        SingleCase result = caseService.getCasesById(101);

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetCasesById/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void testGetCasesByIdHearingsNotActual() {
        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");

        when(caseRepository.findById(any())).thenReturn(Optional.of(courtCaseEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> caseService.getCasesById(101));

        assertEquals("CASE_107", exception.getError().getErrorTypeNumeric());
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

        List<ScheduledCase> resultList = caseService.getHearings(request);
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

        List<ScheduledCase> resultList = caseService.getHearings(request);
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

        caseService.getHearings(request);
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

        PostCaseResponse result = caseService.addCaseOrUpdate(request);

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
        assertEquals(courtCase.getCreatedById(), savedCaseEntity.getCreatedById());
        assertEquals(courtCase.getLastModifiedById(), savedCaseEntity.getLastModifiedById());
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
            () -> caseService.addCaseOrUpdate(request)
        );

        assertEquals("Provided courthouse does not exist", thrownException.getMessage());
    }

    @Test
    void testGetCaseHearingsWhenHearingIsActualTrue() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            LocalDate.now(),
            true
        ));

        when(hearingRepository.findByCaseIds(List.of(existingCaseEntity.getId()))).thenReturn(existingHearings);

        List<Hearing> caseHearings = caseService.getCaseHearings(existingCaseEntity.getId());

        assertEquals(existingHearings.getFirst().getId(), caseHearings.getFirst().getId());
        assertEquals(existingHearings.getFirst().getCourtroom().getName(), caseHearings.getFirst().getCourtroom());
        assertEquals(existingHearings.getFirst().getHearingDate(), caseHearings.getFirst().getDate());

    }

    @Test
    void testGetCaseHearingsWhenHearingIsActualFalse() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);

        List<HearingEntity> existingHearings = Lists.newArrayList(CommonTestDataUtil.createHearing(
            existingCaseEntity,
            courtroomEntity,
            LocalDate.now(),
            false
        ));

        when(hearingRepository.findByCaseIds(List.of(existingCaseEntity.getId()))).thenReturn(existingHearings);

        List<Hearing> caseHearings = caseService.getCaseHearings(existingCaseEntity.getId());

        assertEquals(0, caseHearings.size());
    }

    @Test
    void testGetCaseHearingsWhenCaseDoesNotExistThrowsException() {
        when(hearingRepository.findByCaseIds(any())).thenReturn(Collections.emptyList());

        var exception = assertThrows(DartsApiException.class, () ->
            caseService.getCaseHearings(1));

        assertEquals("The requested case cannot be found", exception.getMessage());
    }

    @Test
    void testGetCaseHearingsWhenCaseIsExpiredThrowsException() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtCaseEntity existingCaseEntity = CommonTestDataUtil.createCase("case1", courthouseEntity);
        existingCaseEntity.setId(1);
        existingCaseEntity.setDataAnonymised(true);

        when(caseRepository.findById(existingCaseEntity.getId())).thenReturn(Optional.of(existingCaseEntity));
        var exception = assertThrows(DartsApiException.class, () ->
            caseService.getCaseHearings(1));

        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
    }

    @Test
    void testGetEventsByCaseId() throws Exception {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "1");
        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");

        when(caseRepository.findById(courtCaseEntity.getId())).thenReturn(Optional.of(courtCaseEntity));
        OffsetDateTime hearingDate = OffsetDateTime.parse("2024-07-01T12:00Z");

        HearingEntity hearing = CommonTestDataUtil.createHearing(
            courtCaseEntity,
            courtroomEntity,
            hearingDate.toLocalDate(),
            true
        );

        List<EventEntity> events = Lists.newArrayList(createEventWith("eventName", "event", hearing, hearingDate));
        when(eventRepository.findAllByCaseId(courtCaseEntity.getId())).thenReturn(events);
        when(caseRepository.findById(courtCaseEntity.getId())).thenReturn(Optional.of(courtCaseEntity));
        List<Event> result = caseService.getEventsByCaseId(courtCaseEntity.getId());

        String actualResponse = objectMapper.writeValueAsString(result);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CaseServiceTest/testGetEventsByCase/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testGetEventsByCaseIdIsAnonymous() {
        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");
        courtCaseEntity.setDataAnonymised(true);
        when(caseRepository.findById(courtCaseEntity.getId())).thenReturn(Optional.of(courtCaseEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> caseService.getEventsByCaseId(courtCaseEntity.getId()));
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
    }

    @Test
    void updateCase_WithNonExistingCourtroomAndMatchingHearingDate() {
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
        caseService.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());


    }

    @Test
    void updateCase_WithMultipleHearingsWithOldHearingDateWithCourtroomInRequest() {
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
        caseService.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());
    }

    @Test
    void updateCase_WithMultipleHearingsWithCourtroomInRequest() {
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
        caseService.addCaseOrUpdate(request);

        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());
    }

    @Test
    void updateCase_WithMultipleHearingsWithoutCourtroomInRequest() {
        // given
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

        // when
        caseService.addCaseOrUpdate(request);

        // then
        verify(caseRepository).saveAndFlush(caseEntityArgumentCaptor.capture());
        verifyNoInteractions(hearingRepository);

        CourtCaseEntity updatedCaseEntity = caseEntityArgumentCaptor.getValue();

        assertEquals(SWANSEA, updatedCaseEntity.getCourthouse().getCourthouseName());
        assertEquals("case1", updatedCaseEntity.getCaseNumber());
        assertEquals(3, updatedCaseEntity.getDefendantList().size());
        assertEquals(3, updatedCaseEntity.getProsecutorList().size());
        assertEquals(3, updatedCaseEntity.getDefenceList().size());

    }

    @Test
    void adminGetCaseById_ShouldReturnCase_WhenCaseExists() {
        // given
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        CourtCaseEntity courtCaseEntity = hearings.getFirst().getCourtCase();
        courtCaseEntity.setHearings(hearings);

        when(caseRepository.findById(1)).thenReturn(Optional.of(courtCaseEntity));

        // when
        AdminSingleCaseResponseItem result = caseService.adminGetCaseById(1);

        // then
        assertNotNull(result);
        assertEquals(courtCaseEntity.getId(), result.getId());
        assertEquals(courtCaseEntity.getCourthouse().getId(), result.getCourthouse().getId());
        assertEquals(courtCaseEntity.getCourthouse().getDisplayName(), result.getCourthouse().getDisplayName());
        assertEquals(courtCaseEntity.getCaseNumber(), result.getCaseNumber());
        assertEquals(2, result.getDefendants().size());
        assertEquals(2, result.getJudges().size());
        assertEquals(2, result.getProsecutors().size());
        assertEquals(2, result.getDefenders().size());
        assertEquals(0, result.getReportingRestrictions().size());
        assertEquals(Boolean.FALSE, result.getIsDataAnonymised());

        verify(caseRepository, times(1)).findById(1);
    }

    @Test
    void adminGetCaseById_ShouldThrowException_WhenCaseDoesNotExist() {
        // given
        when(caseRepository.findById(1)).thenReturn(Optional.empty());

        // when
        DartsApiException exception = assertThrows(DartsApiException.class, () -> caseService.adminGetCaseById(1));

        // then
        assertEquals("CASE_104", exception.getError().getErrorTypeNumeric());
        assertEquals("The requested case cannot be found", exception.getMessage());
        verify(caseRepository, times(1)).findById(1);
    }

    private HearingEntity createHearingEntity() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouseEntity, "2");
        CourtCaseEntity caseEntity = CommonTestDataUtil.createCase("Case0000009", courthouseEntity);
        return CommonTestDataUtil.createHearing(caseEntity, courtroomEntity, LocalDate.of(2023, Month.JULY, 20), true);
    }

    @Test
    @SuppressWarnings("unchecked")//Caused by mockio this can never be incorrect
    void getEventsByCaseIdPaginated_shouldPaginatedCorrectly_whenGivenTypicalData() {
        final int caseId = 123;
        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");
        when(caseRepository.findById(caseId)).thenReturn(Optional.ofNullable(courtCaseEntity));

        PaginatedList<Event> expectedResult = mock(PaginatedList.class);
        PaginationDto<Event> paginationDto = mock(PaginationDto.class);

        when(paginationDto.toPaginatedList(any(), any(), any(), any(), any()))
            .thenReturn(expectedResult);

        PaginatedList<Event> result = caseService.getEventsByCaseId(caseId, paginationDto);

        ArgumentCaptor<Function<Pageable, Page<EventEntity>>> pageArgumentCapturor = ArgumentCaptor.forClass(Function.class);
        ArgumentCaptor<Function<EventEntity, Event>> dataMapperArgumentCapturor = ArgumentCaptor.forClass(Function.class);

        verify(paginationDto)
            .toPaginatedList(
                pageArgumentCapturor.capture(),
                dataMapperArgumentCapturor.capture(),
                eq(List.of(HearingEntity_.HEARING_DATE, EventEntity_.TIMESTAMP)),
                eq(List.of(Sort.Direction.DESC, Sort.Direction.DESC)),
                eq(Map.of("eventId", "ee.id",
                          "hearingDate", "he.hearingDate",
                          "timestamp", "ee.timestamp",
                          "eventName", "et.eventName",
                          "courtroom", "ee.courtroom",
                          "text", "ee.eventText"))
            );
        assertThat(pageArgumentCapturor.getValue()).isNotNull();

        Pageable pageable = mock(Pageable.class);
        Page<Event> page = mock(Page.class);
        when(eventRepository.findAllByCaseIdPaginated(caseId, pageable)).thenReturn(page);
        assertThat(pageArgumentCapturor.getValue().apply(pageable))
            .isEqualTo(page);
        verify(eventRepository).findAllByCaseIdPaginated(caseId, pageable);
        assertThat(dataMapperArgumentCapturor.getValue()).isNotNull();
        assertThat(result).isEqualTo(expectedResult);
        verify(caseRepository).findById(caseId);
    }

    @Test
    void getEventsByCaseIdPaginated_whenCaseIsExpited_shouldThrowError() {
        final int caseId = 123;
        CourtCaseEntity courtCaseEntity = CommonTestDataUtil.createCase("1");
        courtCaseEntity.setDataAnonymised(true);
        when(caseRepository.findById(caseId)).thenReturn(Optional.ofNullable(courtCaseEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> caseService.getEventsByCaseId(caseId, null));
        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
    }
}