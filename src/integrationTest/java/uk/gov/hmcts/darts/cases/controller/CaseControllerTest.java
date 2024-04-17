package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTHOUSE;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.COURTROOM;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetCasesParams.DATE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.testutils.TestUtils.substituteHearingDateWithToday;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.testutils.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.testutils.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createListOfJudges;
import static uk.gov.hmcts.darts.testutils.data.ProsecutorTestData.createListOfProsecutor;

@AutoConfigureMockMvc
@Transactional
class CaseControllerTest extends IntegrationBase {

    public static final String EXPECTED_RESPONSE_FILE = "tests/cases/CaseControllerTest/casesGetEndpoint/expectedResponse.json";
    public static final String HEARING_DATE = "2023-06-20";
    public static final String BASE_PATH = "/cases";
    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;

    @MockBean
    LogApi logApi;

    private HearingEntity setupHearingForCase1(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000001");
        case1.setDefendantList(createListOfDefendantsForCase(2, case1));
        case1.setDefenceList(createListOfDefenceForCase(2, case1));
        case1.setProsecutorList(createListOfProsecutor(2, case1));

        var hearingForCase1 = createHearingWith(case1, swanseaCourtroom1);
        hearingForCase1.addJudges(createListOfJudges(1, case1));
        hearingForCase1.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase1;
    }

    private HearingEntity setupHearingForCase2(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case2 = createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case0000002");
        case2.setDefendantList(createListOfDefendantsForCase(2, case2));
        case2.setDefenceList(createListOfDefenceForCase(2, case2));
        case2.setProsecutorList(createListOfProsecutor(2, case2));

        var hearingForCase2 = createHearingWith(case2, swanseaCourtroom1);
        hearingForCase2.addJudges(createListOfJudges(1, case2));
        hearingForCase2.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase2.setScheduledStartTime(LocalTime.parse("10:00"));
        return hearingForCase2;
    }

    private HearingEntity setupHearingForCase3(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case3 = createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case0000003");
        case3.setDefendantList(createListOfDefendantsForCase(2, case3));
        case3.setDefenceList(createListOfDefenceForCase(2, case3));
        case3.setProsecutorList(createListOfProsecutor(2, case3));

        var hearingForCase3 = createHearingWith(case3, swanseaCourtroom1);
        hearingForCase3.addJudges(createListOfJudges(1, case3));
        hearingForCase3.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase3.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase3;
    }

    private HearingEntity createCaseWithHearingToday(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case4 = createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case0000004");
        case4.setDefendantList(createListOfDefendantsForCase(2, case4));
        case4.setDefenceList(createListOfDefenceForCase(2, case4));
        case4.setProsecutorList(createListOfProsecutor(2, case4));

        var hearingForCase3 = createHearingWith(case4, swanseaCourtroom1);
        hearingForCase3.addJudges(createListOfJudges(1, hearingForCase3.getCourtCase()));
        hearingForCase3.setHearingDate(LocalDate.now());
        hearingForCase3.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase3;
    }

    @BeforeEach
    void setupData() {
        var swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
        swanseaCourthouse.setDisplayName("SWANSEA");

        var swanseaCourtroom1 = someMinimalCourtRoom();
        swanseaCourtroom1.setName("1");
        swanseaCourtroom1.setCourthouse(swanseaCourthouse);

        HearingEntity hearingForCase1 = setupHearingForCase1(swanseaCourthouse, swanseaCourtroom1);
        HearingEntity hearingForCase2 = setupHearingForCase2(swanseaCourthouse, swanseaCourtroom1);
        HearingEntity hearingForCase3 = setupHearingForCase3(swanseaCourthouse, swanseaCourtroom1);
        HearingEntity hearingForCase4 = createCaseWithHearingToday(swanseaCourthouse, swanseaCourtroom1);


        dartsDatabase.saveAll(hearingForCase1, hearingForCase2, hearingForCase3, hearingForCase4);
    }

    @Test
    void casesGetEndpoint() throws Exception {
        setupExternalDarPcUserForCourthouse(null);
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
        setupExternalMidTierUserForCourthouse(null);

        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile("tests/cases/CaseControllerTest/casesPostEndpoint/requestBody.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = TestUtils.removeTags(List.of("case_id"), response.getResponse().getContentAsString());

        String expectedResponse = substituteHearingDateWithToday(getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponse.json"));
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
    void casesPostWithExistingCaseButNoHearing() throws Exception {
        setupExternalMidTierUserForCourthouse(null);

        dartsDatabase.createCase("EDINBURGH", "case1");
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyForCaseWithoutHearing.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = TestUtils.removeTags(List.of("case_id"), response.getResponse().getContentAsString());

        String expectedResponse = substituteHearingDateWithToday(getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseNoHearing.json"));
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostUpdateExistingCase() throws Exception {
        setupExternalMidTierUserForCourthouse(null);

        MockHttpServletRequestBuilder requestBuilder = post("/cases")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyCaseUpdate.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = TestUtils.removeTags(List.of("case_id"), response.getResponse().getContentAsString());

        String expectedResponse = substituteHearingDateWithToday(getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponseCaseUpdate.json"));
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesPostDefendant() throws Exception {
        setupExternalMidTierUserForCourthouse(null);

        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile("tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyDefendantNameOverflow.json"));
        mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        verify(logApi, times(2)).defendantNameOverflow(any());
    }

    private void setupExternalMidTierUserForCourthouse(CourthouseEntity courthouse) {
        String guid = UUID.randomUUID().toString();
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().createMidTierExternalUser(guid, courthouse);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        when(mockUserIdentity.userHasGlobalAccess(Set.of(MID_TIER))).thenReturn(true);
    }

    private void setupExternalDarPcUserForCourthouse(CourthouseEntity courthouse) {
        String guid = UUID.randomUUID().toString();
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().createDarPcExternalUser(guid, courthouse);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        when(mockUserIdentity.userHasGlobalAccess(Set.of(DAR_PC))).thenReturn(true);
    }
}
