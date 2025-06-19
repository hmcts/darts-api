package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
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
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCase;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createProsecutorForCase;

@AutoConfigureMockMvc
class CaseControllerGetCaseByIdTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/{case_id}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    private HearingEntity hearingEntity;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private void setupData() {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor(createProsecutorForCase(courtCase));
        courtCase.addDefendant(createDefendantForCase(courtCase));
        courtCase.addDefence("aDefence");
        dartsDatabase.save(courtCase);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void casesSearchGetEndpoint_shouldReturn401Error_whenUserNotFound() throws Exception {
        setupData();
        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        mockMvc.perform(requestBuilder).andExpect(status().isUnauthorized());
    }

    @Test
    void casesSearchGetEndpoint() throws Exception {
        setupData();
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, getCaseId(SOME_CASE_NUMBER, SOME_COURTHOUSE));

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"case_id":<case-id>,
            "courthouse_id":<courthouse-id>,
            "courthouse":"SOME-COURTHOUSE",
            "case_number":"1",
            "defendants":["some-defendant"],
            "judges":["1JUDGE1"],
            "prosecutors":["some-prosecutor"],
            "defenders":["aDefence"],
            "reporting_restrictions":[],
            "is_data_anonymised":false
            }
            """;

        expectedJson = expectedJson.replace("<case-id>", hearingEntity.getCourtCase().getId().toString());
        expectedJson = expectedJson.replace("<courthouse-id>", hearingEntity.getCourtCase().getCourthouse().getId().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void casesSearchGetEndpointIsAnonymised() throws Exception {
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

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

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
    void casesSearchGetEndpointCheckListsAreCorrectSize() throws Exception {
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
    void casesSearchGetEndpointCaseNotFound() throws Exception {
        setupData();
        mockMvc.perform(get(endpointUrl, 25))
            .andExpect(status().isNotFound());
    }

    private Integer getCaseId(String caseNumber, String courthouse) {
        return dartsDatabase.createCase(courthouse, caseNumber).getId();
    }

}
