package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.PostCaseResponse;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.TestUtils.substituteHearingDateWithToday;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createListOfJudges;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createListOfProsecutor;

@AutoConfigureMockMvc
class CaseControllerTest extends IntegrationBase {

    public static final String EXPECTED_RESPONSE_FILE = "tests/cases/CaseControllerTest/casesGetEndpoint/expectedResponse.json";
    public static final String HEARING_DATE = "2023-06-20";
    public static final String BASE_PATH = "/cases";

    @Autowired
    private transient MockMvc mockMvc;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @MockitoBean
    LogApi logApi;

    private HearingEntity setupHearingForCase1(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case1 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000001");
        case1.setDefendantList(createListOfDefendantsForCase(2, case1));
        case1.setDefenceList(createListOfDefenceForCase(2, case1));
        case1.setProsecutorList(createListOfProsecutor(2, case1));

        var hearingForCase1 = PersistableFactory.getHearingTestData().createHearingWith(case1, swanseaCourtroom1);
        hearingForCase1.addJudges(createListOfJudges(1, case1));
        hearingForCase1.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase1;
    }

    private HearingEntity setupHearingForCase2(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case2 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case0000002");
        case2.setDefendantList(createListOfDefendantsForCase(2, case2));
        case2.setDefenceList(createListOfDefenceForCase(2, case2));
        case2.setProsecutorList(createListOfProsecutor(2, case2));

        var hearingForCase2 = PersistableFactory.getHearingTestData().createHearingWith(case2, swanseaCourtroom1);
        hearingForCase2.addJudges(createListOfJudges(1, case2));
        hearingForCase2.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase2.setScheduledStartTime(LocalTime.parse("10:00"));
        return hearingForCase2;
    }

    private HearingEntity setupHearingForCase3(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case3 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case0000003");
        case3.setDefendantList(createListOfDefendantsForCase(2, case3));
        case3.setDefenceList(createListOfDefenceForCase(2, case3));
        case3.setProsecutorList(createListOfProsecutor(2, case3));

        var hearingForCase3 = PersistableFactory.getHearingTestData().createHearingWith(case3, swanseaCourtroom1);
        hearingForCase3.addJudges(createListOfJudges(1, case3));
        hearingForCase3.setHearingDate(LocalDate.parse(HEARING_DATE));
        hearingForCase3.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase3;
    }

    private HearingEntity createCaseWithHearingToday(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case4 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case0000004");
        case4.setDefendantList(createListOfDefendantsForCase(2, case4));
        case4.setDefenceList(createListOfDefenceForCase(2, case4));
        case4.setProsecutorList(createListOfProsecutor(2, case4));

        var hearingForCase3 = PersistableFactory.getHearingTestData().createHearingWith(case4, swanseaCourtroom1);
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

        dartsPersistence.saveAll(hearingForCase1, hearingForCase2, hearingForCase3, hearingForCase4);
    }

    @BeforeEach
    void startHibernateSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
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
    void getCase_courthouseNotFound_404ShouldBeReturned() throws Exception {
        setupExternalDarPcUserForCourthouse(null);
        MockHttpServletRequestBuilder requestBuilder = get(BASE_PATH)
            .queryParam(COURTHOUSE, "SOME_UNKNOWN_HOUSE")
            .queryParam(COURTROOM, "1")
            .queryParam(DATE, HEARING_DATE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        String content = response.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        Assertions.assertEquals(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST.getType(), problemResponse.getType());
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
    void casesPostWithoutExistingCaseWithLeadingAndTrailingSpacesInNames() throws Exception {
        setupExternalMidTierUserForCourthouse(null);

        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile("tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyWithSpaces.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = TestUtils.removeTags(List.of("case_id"), response.getResponse().getContentAsString());

        String expectedResponse = substituteHearingDateWithToday(getContentsFromFile(
            "tests/cases/CaseControllerTest/casesPostEndpoint/expectedResponse.json"));
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
    void casesPost_courthouseNotFound_404ShouldBeReturned() throws Exception {
        setupExternalMidTierUserForCourthouse(null);
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile("tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyCourthouseNotFound.json"));

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        String content = response.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        Assertions.assertEquals(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST.getType(), problemResponse.getType());
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
    void casesPostWithType() throws Exception {
        setupExternalMidTierUserForCourthouse(null);

        dartsDatabase.createCase("EDINBURGH", "case1");
        MockHttpServletRequestBuilder requestBuilder = post(BASE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyForCaseWithType.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();
        PostCaseResponse postCaseResponse = objectMapper.readValue(actualResponse, PostCaseResponse.class);
        Optional<CourtCaseEntity> savedCase = dartsDatabase.getCaseRepository().findById(postCaseResponse.getCaseId());
        String caseType = savedCase.get().getCaseType();
        Assertions.assertEquals("1", caseType);
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
            .content(getContentsFromFile("tests/cases/CaseControllerTest/casesPostEndpoint/requestBodyDefendantNameIssues.json"));
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();

        PostCaseResponse postCaseResponse = objectMapper.readValue(response.getResponse().getContentAsString(), PostCaseResponse.class);

        Assertions.assertEquals(2, postCaseResponse.getDefendants().size());
        verify(logApi, times(2)).defendantNameOverflow(any(AddCaseRequest.class));
        verify(logApi, times(1)).defendantNotAdded("U20240603-103622, U20240603-03622", "CASE1001");
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