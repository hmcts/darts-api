package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.DATE;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.testutils.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.testutils.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createListOfJudgesForHearing;
import static uk.gov.hmcts.darts.testutils.data.ProsecutorTestData.createListOfProsecutor;

@AutoConfigureMockMvc
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseControllerTest extends IntegrationBase {

    public static final String EXPECTED_RESPONSE_FILE = "tests/cases/CaseControllerTest/casesGetEndpoint/expectedResponse.json";
    public static final String HEARING_DATE = "2023-06-20";
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

        dartsDatabase.saveAll(hearingForCase1, hearingForCase2, hearingForCase3);
    }

    @Test
    void casesGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get("/cases")
            .queryParam(COURTHOUSE, "SWANSEA")
            .queryParam(COURTROOM, "1")
            .queryParam(DATE, HEARING_DATE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(EXPECTED_RESPONSE_FILE);
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
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
