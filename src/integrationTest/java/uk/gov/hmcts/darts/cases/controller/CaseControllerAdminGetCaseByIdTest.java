package uk.gov.hmcts.darts.cases.controller;

import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCase;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCase;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;

@Slf4j
@AutoConfigureMockMvc
class CaseControllerAdminGetCaseByIdTest extends IntegrationBase {

    private static final String endpointUrl = "/admin/cases/{id}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    private HearingEntity hearingEntity;

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private UserAccountEntity user;

    private CourthouseEntity swanseaCourthouse;


    @BeforeEach
    void setupData() {
        swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
        swanseaCourthouse.setDisplayName("SWANSEA");

        CourtCaseEntity case1 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        JudgeEntity judge = createJudgeWithName("aJudge");
        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");

        HearingEntity hearing1a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);

        HearingEntity hearing1b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 21), judge);

        HearingEntity hearing1c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 22), judge);

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c);

        EventEntity event1 = createEventWith("eventName", "event1", hearing1a, OffsetDateTime.now());
        dartsDatabase.save(event1);

        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
    }

    @Test
    void adminGetCaseById_ShouldReturnForbiddenError() throws Exception {
        // given
        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        // when
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        // then
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }

    @Test
    void adminGetCaseById_Success() throws Exception {
        // given
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        // then
        String actualResponse = mvcResult.getResponse().getContentAsString();
        log.info("actualJson: {}", actualResponse);
        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminGetCaseByIdTest/testOk/expectedResponse.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminGetCaseById_IsAnonymised() throws Exception {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            "123",
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setDataAnonymised(true);
        OffsetDateTime dataAnonymisedTs = OffsetDateTime.parse("2023-01-01T12:00:00Z");
        courtCase.setDataAnonymisedTs(dataAnonymisedTs);
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        dartsDatabase.save(courtCase);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId("123", SOME_COURTHOUSE));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"case_id":<case-id>,
            "courthouse_id":<courthouse-id>,
            "courthouse":"SOME-COURTHOUSE",
            "case_number":"123",
            "defendants":["some-defendant"],
            "judges":["123JUDGE1"],
            "prosecutors":["some-prosecutor"],
            "defenders":["aDefence"],
            "reporting_restrictions":[],
            "is_data_anonymised":true,
            "data_anonymised_at":"2023-01-01T12:00:00Z"
            }
            """;

        expectedJson = expectedJson.replace("<case-id>", hearingEntity.getCourtCase().getId().toString());
        expectedJson = expectedJson.replace("<courthouse-id>", hearingEntity.getCourtCase().getCourthouse().getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminGetCaseById_CheckListsAreCorrectSize() throws Exception {
        setupData();
        final Integer caseId = getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE);
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, caseId);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.case_id", Matchers.is(caseId)))
            .andExpect(jsonPath("$.judges", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.judges[0]", Matchers.is("1JUDGE1")))
            .andExpect(jsonPath("$.prosecutors", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.prosecutors[0]", Matchers.is("some-prosecutor")))
            .andExpect(jsonPath("$.defendants", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.defendants[0]", Matchers.is("some-defendant")))
            .andExpect(jsonPath("$.defenders", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.defenders[0]", Matchers.is("aDefence")));

    }

    @Test
    void adminGetCaseById_CaseNotFound() throws Exception {
        setupData();
        mockMvc.perform(get(endpointUrl, 25))
            .andExpect(status().isNotFound());
    }

    private Integer getCaseId(String caseNumber, String courthouse) {
        return dartsDatabase.createCase(courthouse, caseNumber).getId();
    }

    private void setupUserAccountAndSecurityGroup() {
        var securityGroup = SecurityGroupTestData.createGroupForRole(SUPER_ADMIN);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
        userAccountRepository.save(user);
    }
}
