package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.DATE_TO;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.ENDPOINT_URL;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.testutils.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createJudgeWithNameForHearing;


@AutoConfigureMockMvc
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.VariableDeclarationUsageDistance", "PMD.NcssCount"})
class CaseControllerSearchGetTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        CourthouseEntity swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");

        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        CourtroomEntity courtroom2 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom2");
        CourtroomEntity courtroom3 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom3");


        CourtCaseEntity case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        CourtCaseEntity case2 = createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case2");
        case2.setDefendantList(Arrays.asList(createDefendantForCaseWithName(case2, "Defendant2")));

        CourtCaseEntity case3 = createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case3");

        CourtCaseEntity case4 = createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case4");

        CourtCaseEntity case5 = createCaseAt(swanseaCourthouse);
        case5.setCaseNumber("case5");

        CourtCaseEntity case6 = createCaseAt(swanseaCourthouse);
        case6.setCaseNumber("case6");

        HearingEntity hearing1a = createHearingWith(case1, courtroom1, LocalDate.of(2023, 5, 20));

        HearingEntity hearing1b = createHearingWith(case1, courtroom1, LocalDate.of(2023, 5, 21));

        HearingEntity hearing1c = createHearingWith(case1, courtroom1, LocalDate.of(2023, 5, 22));

        HearingEntity hearing2a = createHearingWith(case2, courtroom1, LocalDate.of(2023, 6, 20));

        HearingEntity hearing2b = createHearingWith(case2, courtroom1);
        hearing2b.setHearingDate(LocalDate.of(2023, 6, 21));

        HearingEntity hearing2c = createHearingWith(case2, courtroom1);
        hearing2c.setHearingDate(LocalDate.of(2023, 6, 22));

        HearingEntity hearing3a = createHearingWith(case3, courtroom1);
        hearing3a.setHearingDate(LocalDate.of(2023, 7, 20));
        hearing3a.setJudgeList(Arrays.asList(createJudgeWithNameForHearing("Judge3a", hearing3a)));

        HearingEntity hearing3b = createHearingWith(case3, courtroom1);
        hearing3b.setHearingDate(LocalDate.of(2023, 7, 21));

        HearingEntity hearing3c = createHearingWith(case3, courtroom1);
        hearing3c.setHearingDate(LocalDate.of(2023, 7, 22));

        HearingEntity hearing4a = createHearingWith(case4, courtroom2);
        hearing4a.setHearingDate(LocalDate.of(2023, 8, 20));

        HearingEntity hearing4b = createHearingWith(case4, courtroom1);
        hearing4b.setHearingDate(LocalDate.of(2023, 8, 21));

        HearingEntity hearing4c = createHearingWith(case4, courtroom1);
        hearing4c.setHearingDate(LocalDate.of(2023, 8, 22));
        hearing4c.setJudgeList(Arrays.asList(createJudgeWithNameForHearing("Judge6b", hearing3a)));

        HearingEntity hearing5a = createHearingWith(case5, courtroom2);
        hearing5a.setHearingDate(LocalDate.of(2023, 9, 20));

        HearingEntity hearing5b = createHearingWith(case5, courtroom1);
        hearing5b.setHearingDate(LocalDate.of(2023, 9, 21));

        HearingEntity hearing5c = createHearingWith(case5, courtroom3);
        hearing5c.setHearingDate(LocalDate.of(2023, 9, 22));

        HearingEntity hearing6a = createHearingWith(case6, courtroom2);
        hearing6a.setHearingDate(LocalDate.of(2023, 9, 20));

        HearingEntity hearing6b = createHearingWith(case6, courtroom3);
        hearing6b.setHearingDate(LocalDate.of(2023, 9, 21));
        hearing6b.setJudgeList(Arrays.asList(createJudgeWithNameForHearing("Judge6b", hearing6b)));

        HearingEntity hearing6c = createHearingWith(case6, courtroom1);
        hearing6c.setHearingDate(LocalDate.of(2023, 9, 22));

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c,
                              hearing2a, hearing2b, hearing2c,
                              hearing3a, hearing3b, hearing3c,
                              hearing4a, hearing4b, hearing4c,
                              hearing5a, hearing5b, hearing5c,
                              hearing6a, hearing6b, hearing6c

        );

        EventEntity event4a = createEventWith("eventName", "event4a", hearing4a, OffsetDateTime.now());
        EventEntity event5b = createEventWith("eventName", "event5b", hearing5b, OffsetDateTime.now());
        dartsDatabase.saveAll(event4a, event5b);

    }


    @Test
    void casesSearchGetEndpoint() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL)
            .queryParam(COURTHOUSE, "SWANSEA")
            .queryParam(COURTROOM, "1")
            .queryParam(DATE_TO, "2023-05-20");
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponse.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}
