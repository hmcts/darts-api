package uk.gov.hmcts.darts.cases.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCaseEntityAt;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtHouse;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtroomAt;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aDefendant;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aEvent;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aHearingForCaseInRoom;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aJudge;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.VariableDeclarationUsageDistance", "PMD.NcssCount"})
class CaseServiceAdvancedSearchTest extends IntegrationBase {

    @Autowired
    CaseService service;
    CourthouseEntity swanseaCourthouse;
    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @BeforeEach
    void setupData() {
        swanseaCourthouse = aCourtHouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");

        CourtroomEntity courtroom1 = aCourtroomAt(swanseaCourthouse, "courtroom1");
        CourtroomEntity courtroom2 = aCourtroomAt(swanseaCourthouse, "courtroom2");
        CourtroomEntity courtroom3 = aCourtroomAt(swanseaCourthouse, "courtroom3");


        CourtCaseEntity case1 = aCaseEntityAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        CourtCaseEntity case2 = aCaseEntityAt(swanseaCourthouse);
        case2.setCaseNumber("Case2");
        case2.setDefendantList(List.of(aDefendant(case2, "Defendant2")));

        CourtCaseEntity case3 = aCaseEntityAt(swanseaCourthouse);
        case3.setCaseNumber("Case3");

        CourtCaseEntity case4 = aCaseEntityAt(swanseaCourthouse);
        case4.setCaseNumber("Case4");

        CourtCaseEntity case5 = aCaseEntityAt(swanseaCourthouse);
        case5.setCaseNumber("case5");

        CourtCaseEntity case6 = aCaseEntityAt(swanseaCourthouse);
        case6.setCaseNumber("case6");

        HearingEntity hearing1a = aHearingForCaseInRoom(case1, courtroom1);
        hearing1a.setHearingDate(LocalDate.of(2023, 5, 20));

        HearingEntity hearing1b = aHearingForCaseInRoom(case1, courtroom1);
        hearing1b.setHearingDate(LocalDate.of(2023, 5, 21));

        HearingEntity hearing1c = aHearingForCaseInRoom(case1, courtroom1);
        hearing1c.setHearingDate(LocalDate.of(2023, 5, 22));

        HearingEntity hearing2a = aHearingForCaseInRoom(case2, courtroom1);
        hearing2a.setHearingDate(LocalDate.of(2023, 6, 20));

        HearingEntity hearing2b = aHearingForCaseInRoom(case2, courtroom1);
        hearing2b.setHearingDate(LocalDate.of(2023, 6, 21));

        HearingEntity hearing2c = aHearingForCaseInRoom(case2, courtroom1);
        hearing2c.setHearingDate(LocalDate.of(2023, 6, 22));

        HearingEntity hearing3a = aHearingForCaseInRoom(case3, courtroom1);
        hearing3a.setHearingDate(LocalDate.of(2023, 7, 20));
        hearing3a.setJudgeList(List.of(aJudge(hearing3a, "Judge3a")));

        HearingEntity hearing3b = aHearingForCaseInRoom(case3, courtroom1);
        hearing3b.setHearingDate(LocalDate.of(2023, 7, 21));

        HearingEntity hearing3c = aHearingForCaseInRoom(case3, courtroom1);
        hearing3c.setHearingDate(LocalDate.of(2023, 7, 22));

        HearingEntity hearing4a = aHearingForCaseInRoom(case4, courtroom2);
        hearing4a.setHearingDate(LocalDate.of(2023, 8, 20));

        HearingEntity hearing4b = aHearingForCaseInRoom(case4, courtroom1);
        hearing4b.setHearingDate(LocalDate.of(2023, 8, 21));

        HearingEntity hearing4c = aHearingForCaseInRoom(case4, courtroom1);
        hearing4c.setHearingDate(LocalDate.of(2023, 8, 22));
        hearing4c.setJudgeList(List.of(aJudge(hearing3a, "Judge6b")));

        HearingEntity hearing5a = aHearingForCaseInRoom(case5, courtroom2);
        hearing5a.setHearingDate(LocalDate.of(2023, 9, 20));

        HearingEntity hearing5b = aHearingForCaseInRoom(case5, courtroom1);
        hearing5b.setHearingDate(LocalDate.of(2023, 9, 21));

        HearingEntity hearing5c = aHearingForCaseInRoom(case5, courtroom3);
        hearing5c.setHearingDate(LocalDate.of(2023, 9, 22));

        HearingEntity hearing6a = aHearingForCaseInRoom(case6, courtroom2);
        hearing6a.setHearingDate(LocalDate.of(2023, 9, 20));

        HearingEntity hearing6b = aHearingForCaseInRoom(case6, courtroom3);
        hearing6b.setHearingDate(LocalDate.of(2023, 9, 21));
        hearing6b.setJudgeList(List.of(aJudge(hearing6b, "Judge6b")));

        HearingEntity hearing6c = aHearingForCaseInRoom(case6, courtroom1);
        hearing6c.setHearingDate(LocalDate.of(2023, 9, 22));

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c,
                              hearing2a, hearing2b, hearing2c,
                              hearing3a, hearing3b, hearing3c,
                              hearing4a, hearing4b, hearing4c,
                              hearing5a, hearing5b, hearing5c,
                              hearing6a, hearing6b, hearing6c

        );

        EventEntity event4a = aEvent(hearing4a, "event4a");
        EventEntity event5b = aEvent(hearing5b, "event5b");
        dartsDatabase.saveAll(event4a, event5b);
    }

    @Test
    void getWithCaseNumber() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber("sE1")
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCaseNumber/expectedResponse.json"));

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getWithDateRangeFrom() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 7, 21))
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeFrom/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getWithDateRangeTo() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateTo(LocalDate.of(2023, 6, 21))
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeTo/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getWithDateRangeFromTo() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 6, 21))
            .dateTo(LocalDate.of(2023, 7, 21))
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeFromTo/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getWithJudge() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .judgeName("3A")
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithJudge/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getWithCourtroom() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroom("roOm2")
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCourtroom/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getWithEventText() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .eventTextContains("nT5b")
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithEventText/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    void getWithCourtroomJudge() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroom("roOm3")
            .judgeName("dGe6B")
            .build();


        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCourtroomJudge/expectedResponse.json"));
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}
