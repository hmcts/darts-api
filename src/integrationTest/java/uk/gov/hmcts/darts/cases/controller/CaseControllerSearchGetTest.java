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
import uk.gov.hmcts.darts.common.util.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.util.List;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.DATE_TO;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.ENDPOINT_URL;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCaseEntityAt;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtHouse;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtroomAt;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aDefendant;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aEvent;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aHearingForCaseInRoom;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aJudge;

@AutoConfigureMockMvc
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.VariableDeclarationUsageDistance", "PMD.NcssCount"})
class CaseControllerSearchGetTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        CourthouseEntity swanseaCourthouse = aCourtHouse();
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
