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
import java.util.List;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseControllerTest extends IntegrationBase {

    public static final String EXPECTED_RESPONSE_FILE = "tests/cases/CaseControllerTest/casesGetEndpoint/expectedResponse.json";
    public static final String HEARING_DATE = "2023-06-20";
    @Autowired
    private transient MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        var swanseaCourthouse = aCourtHouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");

        var swanseaCourtroom1 = aCourtroom();
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
        var case1 = aCaseEntityAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000001");
        case1.setDefendants(List.of("Mr Defendant0000001 Bloggs1", "Mr Defendant0000001 Bloggs2"));
        case1.setDefenders(List.of("Defence00000011", "Defence00000012"));
        case1.setProsecutors(List.of("Prosecutor00000011", "Prosecutor00000012"));

        var hearingForCase1 = aHearingForCaseInRoom(case1, swanseaCourtroom1);
        hearingForCase1.setJudges(List.of("{Judge1}"));
        hearingForCase1.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase1;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase2(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case1 = aCaseEntityAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000002");
        case1.setDefendants(List.of("Mr Defendant0000002 Bloggs1", "Mr Defendant0000002 Bloggs2"));
        case1.setDefenders(List.of("Defence00000021", "Defence00000022"));
        case1.setProsecutors(List.of("Prosecutor00000021", "Prosecutor00000022"));

        var hearingForCase1 = aHearingForCaseInRoom(case1, swanseaCourtroom1);
        hearingForCase1.setJudges(List.of("{Judge1}"));
        hearingForCase1.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("10:00"));
        return hearingForCase1;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase3(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case1 = aCaseEntityAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000003");
        case1.setDefendants(List.of("Mr Defendant0000003 Bloggs1", "Mr Defendant0000003 Bloggs2"));
        case1.setDefenders(List.of("Defence00000031", "Defence00000032"));
        case1.setProsecutors(List.of("Prosecutor00000031", "Prosecutor00000032"));

        var hearingForCase1 = aHearingForCaseInRoom(case1, swanseaCourtroom1);
        hearingForCase1.setJudges(List.of("{Judge1}"));
        hearingForCase1.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase1;
    }
}
