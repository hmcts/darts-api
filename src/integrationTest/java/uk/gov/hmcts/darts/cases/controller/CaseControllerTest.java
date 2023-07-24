package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.DATE;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCaseEntityAt;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtHouse;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtroom;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aHearingForCaseInRoom;

@AutoConfigureMockMvc
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.TooManyMethods"})
class CaseControllerTest extends IntegrationBase {

    public static final String EXPECTED_RESPONSE_FILE = "tests/cases/CaseControllerTest/casesGetEndpoint/expectedResponse.json";
    public static final String HEARING_DATE = "2023-06-20";
    public static final String BASE_PATH = "/cases";
    @Autowired
    private transient MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        var swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");

        var swanseaCourtroom1 = someMinimalCourtRoom();
        swanseaCourtroom1.setName("1");
        swanseaCourtroom1.setCourthouse(swanseaCourthouse);

        HearingEntity hearingForCase1 = setupCase1(swanseaCourthouse, swanseaCourtroom1);
        HearingEntity hearingForCase2 = setupCase2(swanseaCourthouse, swanseaCourtroom1);
        HearingEntity hearingForCase3 = setupCase3(swanseaCourthouse, swanseaCourtroom1);
        HearingEntity hearingForCase4 = setupCase4(swanseaCourthouse, swanseaCourtroom1);


        dartsDatabase.saveAll(hearingForCase1, hearingForCase2, hearingForCase3, hearingForCase4);
    }

    @Test
    void casesGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(BASE_PATH)
            .queryParam(COURTHOUSE, "SWANSEA")
            .queryParam(COURTROOM, "1")
            .queryParam(DATE, HEARING_DATE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(EXPECTED_RESPONSE_FILE);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostWithoutExistingCase() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile("tests/cases/CaseControllerTest/casesPostEndpoint/requestBody.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponse.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostWithoutExistingCaseAndCourtroomMissing() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyWithoutCourtroom.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseWithoutCourtroomAndJudge.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostCaseNumberMissing() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyCaseNumberMissing.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseCaseNumberMissing_400.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostCourthouseMissing() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyCourthouseMissing.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseCourthouseMissing_400.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostWithNonExistingCourtroom() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyWithNonExistingCourtroom.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseForNonExistingCourtroom.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostOnlyCaseNumberAndCourthouseProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyOnlyCourthouseProvided.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseOnlyCourthouseProvided.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostWithExistingCaseButNoHearing() throws Exception {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom("case1", "EDINBURGH", "1");
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyForCaseWithoutHearing.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseNoHearing.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostUpdateExistingCase() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/cases")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyCaseUpdate.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseCaseUpdate.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase1(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000001");
        case1.setDefendantList(createListOfDefendantsForCase(2, case1));
        case1.setDefenceList(createListOfDefenceForCase(2, case1));
        case1.setProsecutorList(createListOfProsecutor(2, case1));

        var hearingForCase = createHearingWith(case1, swanseaCourtroom1);
        hearingForCase.setJudgeList(createListOfJudgesForHearing(1, hearingForCase));
        hearingForCase.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase2(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case2 = createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case0000002");
        case2.setDefendantList(createListOfDefendantsForCase(2, case2));
        case2.setDefenceList(createListOfDefenceForCase(2, case2));
        case2.setProsecutorList(createListOfProsecutor(2, case2));

        var hearingForCase = createHearingWith(case2, swanseaCourtroom1);
        hearingForCase.setJudgeList(createListOfJudgesForHearing(1, hearingForCase));
        hearingForCase.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase.setScheduledStartTime(LocalTime.parse("10:00"));
        return hearingForCase;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase3(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case3 = createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case0000003");
        case3.setDefendantList(createListOfDefendantsForCase(2, case3));
        case3.setDefenceList(createListOfDefenceForCase(2, case3));
        case3.setProsecutorList(createListOfProsecutor(2, case3));

        var hearingForCase = createHearingWith(case3, swanseaCourtroom1);
        hearingForCase.setJudgeList(createListOfJudgesForHearing(1, hearingForCase));
        hearingForCase.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase;
    }
}
