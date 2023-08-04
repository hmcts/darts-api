package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createDefenceList;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createDefendantList;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createProsecutorList;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CasesMapperTest {
    public static final String SWANSEA = "SWANSEA";
    public static final String CASE_NUMBER = "casenumber1";
    ObjectMapper objectMapper;
    @Mock
    CourthouseRepository courthouseRepository;
    private CasesMapper caseMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @BeforeEach
    void setUp() {
        courthouseRepository = Mockito.mock(CourthouseRepository.class);
        caseMapper = new CasesMapper(courthouseRepository);
    }

    @Test
    void testOk() throws IOException {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);

        List<ScheduledCase> scheduledCases = caseMapper.mapToCourtCases(hearings);

        String actualResponse = objectMapper.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile("Tests/cases/CasesMapperTest/testOk/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testOkWithCase() throws IOException {
        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCourthouse(CommonTestDataUtil.createCourthouse("Test house"));

        HearingEntity hearing = CommonTestDataUtil.createHearing(caseEntity, null,
                                                                 LocalDate.of(2023, Month.JULY, 7)
        );

        ScheduledCase scheduledCases = caseMapper.mapToCourtCase(hearing, caseEntity);

        String actualResponse = objectMapper.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testOk/expectedResponseWithCase.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testMapAddCaseRequestToCaseEntityWithNonExistingCourthouse() {
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.empty());

        CourtCaseEntity caseEntity = new CourtCaseEntity();

        AddCaseRequest request = new AddCaseRequest(SWANSEA, CASE_NUMBER);


        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> caseMapper.mapAddCaseRequestToCaseEntity(request, caseEntity)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getError().getHttpStatus());
        assertEquals("Provided courthouse does not exist", exception.getMessage());
    }

    @Test
    void testMapAddCaseRequestToCaseEntityWithExistingCourthouse() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));

        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(CASE_NUMBER);
        caseEntity.setCourthouse(courthouseEntity);
        caseEntity.setProsecutorList(createProsecutorList(caseEntity));
        caseEntity.setDefenceList(createDefenceList(caseEntity));
        caseEntity.setDefendantList(createDefendantList(caseEntity));


        AddCaseRequest request = new AddCaseRequest(SWANSEA, CASE_NUMBER);
        request.setProsecutors(new ArrayList<>(List.of("New Prosecutor")));
        request.setDefenders(new ArrayList<>(List.of("New Defenders")));
        request.setDefendants(new ArrayList<>(List.of("New Defendants")));

        CourtCaseEntity scheduledCases = caseMapper.mapAddCaseRequestToCaseEntity(request, caseEntity);
        assertEquals(CASE_NUMBER, scheduledCases.getCaseNumber());
        assertEquals(SWANSEA, scheduledCases.getCourthouse().getCourthouseName());
        assertEquals(3, scheduledCases.getProsecutorList().size());
        assertEquals(3, scheduledCases.getDefenceList().size());
        assertEquals(3, scheduledCases.getDefendantList().size());

    }

    @Test
    void testMapAddCaseRequestToCaseEntityWithExistingDetails() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(any())).thenReturn(Optional.of(courthouseEntity));

        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(CASE_NUMBER);
        caseEntity.setCourthouse(courthouseEntity);

        caseEntity.setProsecutorList(createProsecutorList(caseEntity));
        caseEntity.setDefenceList(createDefenceList(caseEntity));
        caseEntity.setDefendantList(createDefendantList(caseEntity));

        AddCaseRequest request = new AddCaseRequest(SWANSEA, CASE_NUMBER);
        request.setProsecutors(new ArrayList<>(List.of("prosecutor_casenumber1_1")));
        request.setDefenders(new ArrayList<>(List.of("defence_casenumber1_1")));
        request.setDefendants(new ArrayList<>(List.of("defendant_casenumber1_1")));

        CourtCaseEntity scheduledCases = caseMapper.mapAddCaseRequestToCaseEntity(request, caseEntity);
        assertEquals(CASE_NUMBER, scheduledCases.getCaseNumber());
        assertEquals(SWANSEA, scheduledCases.getCourthouse().getCourthouseName());
        assertEquals(2, scheduledCases.getProsecutorList().size());
        assertEquals(2, scheduledCases.getDefenceList().size());
        assertEquals(2, scheduledCases.getDefendantList().size());

    }

    @Test
    void testOrderedByTime() throws IOException {
        List<HearingEntity> hearingList = new ArrayList<>();
        int counter = 1;
        String caseNumString = "caseNum_";
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(9, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(18, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(4, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(15, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(12, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter++, LocalTime.of(10, 0, 0)));
        hearingList.add(CommonTestDataUtil.createHearing(caseNumString + counter, LocalTime.of(16, 0, 0)));

        List<ScheduledCase> scheduledCases = caseMapper.mapToCourtCases(hearingList);

        String actualResponse = objectMapper.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testOrderedByTime/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testMapToSingleCase() throws Exception {

        CourtCaseEntity caseEntity = CommonTestDataUtil.createCaseWithId("Case00001", 1);

        SingleCase singleCase = caseMapper.mapToSingleCase(caseEntity);

        String actualResponse = objectMapper.writeValueAsString(singleCase);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testMapToSingleCase/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);


    }

}
