package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.cases.model.SingleCase;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createDefenceList;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createDefendantList;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createProsecutorList;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class CasesMapperTest {
    public static final String SWANSEA = "SWANSEA";
    public static final String CASE_NUMBER = "casenumber1";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();

    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;
    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private LogApi logApi;

    private CasesMapper caseMapper;

    @BeforeEach
    void setUp() {
        Pattern unallocatedCaseRegex = Pattern.compile(".*\\d{8}-\\d{6}.*");
        caseMapper = new CasesMapper(retrieveCoreObjectService, hearingReportingRestrictionsRepository,
                                     caseRetentionRepository, authorisationApi, logApi, unallocatedCaseRegex);
    }

    @Test
    void testOk() throws IOException {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);

        List<ScheduledCase> scheduledCases = caseMapper.mapToScheduledCases(hearings);

        String actualResponse = OBJECT_MAPPER.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile("Tests/cases/CasesMapperTest/testOk/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void testOkWithCase() throws IOException {
        CourtCaseEntity caseEntity = new CourtCaseEntity();
        CourthouseEntity courthouse = CommonTestDataUtil.createCourthouse("Test house");
        caseEntity.setCourthouse(courthouse);

        CourtroomEntity courtroomEntity = CommonTestDataUtil.createCourtroom(courthouse, "1");

        HearingEntity hearing = CommonTestDataUtil.createHearing(caseEntity, courtroomEntity,
                                                                 LocalDate.of(2023, Month.JULY, 7), true
        );

        ScheduledCase scheduledCases = caseMapper.mapToScheduledCase(hearing);

        String actualResponse = OBJECT_MAPPER.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testOk/expectedResponseWithCase.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void testMapAddCaseRequestToCaseEntityWithExistingCourthouse() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);

        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(CASE_NUMBER);
        caseEntity.setCourthouse(courthouseEntity);
        caseEntity.setProsecutorList(createProsecutorList(caseEntity));
        caseEntity.setDefenceList(createDefenceList(caseEntity));
        caseEntity.setDefendantList(createDefendantList(caseEntity));


        AddCaseRequest request = new AddCaseRequest(SWANSEA, "1");
        request.setProsecutors(new ArrayList<>(List.of("New Prosecutor")));
        request.setDefenders(new ArrayList<>(List.of("New Defenders")));
        request.setDefendants(new ArrayList<>(List.of("New Defendants")));

        CourtCaseEntity scheduledCases = caseMapper.addDefendantProsecutorDefenderJudgeType(caseEntity, request);
        assertEquals(CASE_NUMBER, scheduledCases.getCaseNumber());
        assertEquals(SWANSEA, scheduledCases.getCourthouse().getCourthouseName());
        assertEquals(3, scheduledCases.getProsecutorList().size());
        assertEquals(3, scheduledCases.getDefenceList().size());
        assertEquals(3, scheduledCases.getDefendantList().size());

    }

    @Test
    void testMapAddCaseRequestToCaseEntityWithExistingDetails() {
        CourthouseEntity courthouseEntity = CommonTestDataUtil.createCourthouse(SWANSEA);

        CourtCaseEntity caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber(CASE_NUMBER);
        caseEntity.setCourthouse(courthouseEntity);

        caseEntity.setProsecutorList(createProsecutorList(caseEntity));
        caseEntity.setDefenceList(createDefenceList(caseEntity));
        caseEntity.setDefendantList(createDefendantList(caseEntity));

        AddCaseRequest request = new AddCaseRequest(SWANSEA, "1");
        request.setProsecutors(new ArrayList<>(List.of("prosecutor_casenumber1_1")));
        request.setDefenders(new ArrayList<>(List.of("defence_casenumber1_1")));
        request.setDefendants(new ArrayList<>(List.of("defendant_casenumber1_1")));

        CourtCaseEntity scheduledCases = caseMapper.addDefendantProsecutorDefenderJudgeType(caseEntity, request);
        assertEquals(CASE_NUMBER, scheduledCases.getCaseNumber());
        assertEquals(SWANSEA, scheduledCases.getCourthouse().getCourthouseName());
        assertEquals(2, scheduledCases.getProsecutorList().size());
        assertEquals(2, scheduledCases.getDefenceList().size());
        assertEquals(2, scheduledCases.getDefendantList().size());

    }

    @Test
    void testDefendantNameMatchingUnallocatedCaseNumberFormat() {
        CourtCaseEntity existingCourtCaseEntity = new CourtCaseEntity();
        existingCourtCaseEntity.setCaseNumber(CASE_NUMBER);
        AddCaseRequest request = new AddCaseRequest(SWANSEA, CASE_NUMBER);
        var unallocatedCaseNumber = "U20240603-103622, U20240603-03622";
        request.setDefendants(new ArrayList<>(List.of(unallocatedCaseNumber, "Mr Defendant")));

        CourtCaseEntity courtCaseEntity = caseMapper.addDefendantProsecutorDefenderJudgeType(existingCourtCaseEntity, request);

        assertEquals(1, courtCaseEntity.getDefendantList().size());
        Mockito.verify(logApi, Mockito.times(1)).defendantNotAdded(unallocatedCaseNumber, CASE_NUMBER);
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

        List<ScheduledCase> scheduledCases = caseMapper.mapToScheduledCases(hearingList);

        String actualResponse = OBJECT_MAPPER.writeValueAsString(scheduledCases);
        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testOrderedByTime/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void testMapToSingleCase() throws Exception {

        CourtCaseEntity caseEntity = CommonTestDataUtil.createCaseWithId("Case00001", 1);

        SingleCase singleCase = caseMapper.mapToSingleCase(caseEntity);

        String actualResponse = OBJECT_MAPPER.writeValueAsString(singleCase);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testMapToSingleCase/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void testMapToSingleCaseWithReportingRestriction() throws Exception {
        CourtCaseEntity caseEntity = CommonTestDataUtil.createCaseWithId("Case00001", 1);
        EventHandlerEntity reportingRestriction = new EventHandlerEntity();
        reportingRestriction.setEventName("test reporting restriction name");
        caseEntity.setReportingRestrictions(reportingRestriction);
        SingleCase singleCase = caseMapper.mapToSingleCase(caseEntity);

        String actualResponse = OBJECT_MAPPER.writeValueAsString(singleCase);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/CasesMapperTest/testMapToSingleCaseWithReportingRestriction/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


}
