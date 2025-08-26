package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchRequest;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchResponseItem;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getCourtCaseTestData;

@Slf4j
@TestPropertySource(properties = {
    "darts.cases.admin-search.max-results=20"
})
@SuppressWarnings({"PMD.NcssCount"})
class CaseServiceAdminSearchTest extends IntegrationBase {

    @Autowired
    private CaseService service;
    @Autowired
    private CourtCaseStub courtCaseStub;
    private CourthouseEntity swanseaCourthouse;
    private CourthouseEntity londonCourthouse;
    private UserAccountEntity user;
    private CourtroomEntity courtroom1;
    
    @BeforeEach
    void setupData() {
        swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
        swanseaCourthouse.setDisplayName("SWANSEA");

        londonCourthouse = someMinimalCourthouse();
        londonCourthouse.setCourthouseName("LONDON");
        londonCourthouse.setDisplayName("LONDON");

        CourtCaseEntity case1 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        CourtCaseEntity case2 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case2");
        case2.setDefendantList(new ArrayList<>(List.of(createDefendantForCaseWithName(case2, "Defendant2"))));

        CourtCaseEntity case3 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case3");

        CourtCaseEntity case4 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case4");

        CourtCaseEntity case5 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case5.setCaseNumber("case5");

        CourtCaseEntity case6 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case6.setCaseNumber("case6");

        CourtCaseEntity case7 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case7.setCaseNumber("case7");

        CourtCaseEntity case8 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case8.setCaseNumber("case8");

        CourtCaseEntity case9 = getCourtCaseTestData().createCaseAt(londonCourthouse);
        case9.setCaseNumber("Case9");

        CourtCaseEntity case10 = getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case10.setCaseNumber("case10");

        JudgeEntity judge = createJudgeWithName("aJudge");
        courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        HearingEntity hearing1a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);

        HearingEntity hearing1b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 21), judge);

        HearingEntity hearing1c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 22), judge);

        HearingEntity hearing2a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 20), judge);

        HearingEntity hearing2b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 21), judge);

        HearingEntity hearing2c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 22), judge);

        HearingEntity hearing3a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 20), judge);
        JudgeEntity judge3a = createJudgeWithName("Judge3a");
        hearing3a.addJudge(judge3a, false);

        HearingEntity hearing3b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 21), judge);

        HearingEntity hearing3c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 22), judge);

        CourtroomEntity courtroom2 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom2");
        HearingEntity hearing4a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case4, courtroom2, LocalDate.of(2023, 8, 20), judge);

        HearingEntity hearing4b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case4, courtroom1, LocalDate.of(2023, 8, 21), judge);

        HearingEntity hearing4c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case4, courtroom1, LocalDate.of(2023, 8, 22), judge);

        HearingEntity hearing5a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case5, courtroom2, LocalDate.of(2023, 9, 20), judge);

        HearingEntity hearing5b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case5, courtroom1, LocalDate.of(2023, 9, 21), judge);

        CourtroomEntity courtroom3 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom3");
        HearingEntity hearing5c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case5, courtroom3, LocalDate.of(2023, 9, 22), judge);

        HearingEntity hearing6a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case6, courtroom2, LocalDate.of(2023, 9, 20), judge);

        HearingEntity hearing6b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case6, courtroom3, LocalDate.of(2023, 9, 21), judge);
        hearing6b.addJudge(createJudgeWithName("Judge6b"), false);

        HearingEntity hearing6c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case6, courtroom1, LocalDate.of(2023, 9, 22), judge);

        HearingEntity hearing7a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case7, courtroom1, LocalDate.of(2023, 10, 21), judge);
        HearingEntity hearing7b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case7, courtroom1, LocalDate.of(2023, 10, 23), judge);
        HearingEntity hearing8 = PersistableFactory.getHearingTestData().createHearingWithDefaults(case8, courtroom1, LocalDate.of(2023, 10, 22), judge);

        CourtroomEntity courtroomLondon = createCourtRoomWithNameAtCourthouse(londonCourthouse, "courtroomLondon");
        HearingEntity hearing9 = PersistableFactory.getHearingTestData().createHearingWithDefaults(case9, courtroomLondon, LocalDate.of(2023, 5, 20), judge3a);

        CourtroomEntity courtroom4 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom4");
        HearingEntity hearing10a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case10, courtroom4, LocalDate.of(2023, 10, 23), judge);

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c,
                              hearing2a, hearing2b, hearing2c,
                              hearing3a, hearing3b, hearing3c,
                              hearing4a, hearing4b, hearing4c,
                              hearing5a, hearing5b, hearing5c,
                              hearing6a, hearing6b, hearing6c,
                              hearing7a, hearing7b,
                              hearing8,
                              hearing9,
                              hearing10a
        );

        EventEntity event4a = createEventWith("eventName", "event4a", hearing4a, OffsetDateTime.now());
        EventEntity event5b = createEventWith("eventName", "event5b", hearing5b, OffsetDateTime.now());
        dartsDatabase.saveAll(event4a, event5b);
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
    }

    @Test
    void multipleFieldsExcludingHearingDate() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCaseNumber("case3");
        request.setCourthouseIds(List.of(1, 2, 3, courtroom1.getCourthouse().getId()));
        request.setCourtroomName("courtroom1");

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/multipleFieldsExcludingHearingDate/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void hearingDateStartOnly() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setHearingStartAt(LocalDate.of(2023, 10, 1));

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/hearingDateStartOnly/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void hearingDateEndOnly() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setHearingEndAt(LocalDate.of(2023, 5, 21));

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/hearingDateEndOnly/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void hearingDateStartAndEndOnly() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setHearingStartAt(LocalDate.of(2023, 1, 1));
        request.setHearingEndAt(LocalDate.of(2023, 5, 21));

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/hearingDateStartAndEndOnly/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void noResults() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setHearingEndAt(LocalDate.of(2000, 5, 21));

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = "[]";
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void allResults() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setHearingStartAt(LocalDate.of(2000, 5, 21));

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/allResults/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void usingCourthouseIds() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCourthouseIds(List.of(swanseaCourthouse.getId(), londonCourthouse.getId()));

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/usingCourthouseIds/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void usingEmptyCourthouseIds() throws IOException {
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCourthouseIds(List.of());

        List<AdminCasesSearchResponseItem> resultList = service.adminCaseSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdminSearchTest/usingEmptyCourthouseIds/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void maxResults() {
        courtCaseStub.createCasesWithHearings(25, 1, 1);
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();

        var exception = assertThrows(
            DartsApiException.class,
            () -> service.adminCaseSearch(request)
        );

        assertEquals(CaseApiError.TOO_MANY_RESULTS, exception.getError());

    }

    private static void compareJson(String actualResponse, String expectedResponse) {
        try {
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
        } catch (AssertionError ae) {
            log.error("expected\r\n{}to match\r\n{}", expectedResponse, actualResponse);
            throw ae;
        }
    }
}