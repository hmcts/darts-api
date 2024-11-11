package uk.gov.hmcts.darts.cases.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CaseControllerGetCaseHearingsTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    private static String endpointUrl = "/cases/{case_id}/hearings";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    private HearingEntity hearingEntity;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    private Authentication authentication;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccountEntity testUser;

    @BeforeEach
    void setUp() {
        authentication = Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        CourthouseEntity courthouseEntity = hearingEntity.getCourtroom().getCourthouse();
        assertEquals(SOME_COURTHOUSE.toUpperCase(Locale.ROOT), courthouseEntity.getCourthouseName());

        testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(false, courthouseEntity);
        superAdminUserStub.setupUserAsAuthorised(authentication, testUser);
    }

    @Test
    void casesSearchGetEndpointShouldReturnForbidden() throws Exception {
        Mockito.reset(authentication);

        // a user that does not exist in the db
        UserAccountEntity userAccountEntity = new UserAccountEntity();
        userAccountEntity.setId(-1212);
        userAccountEntity.setEmailAddress("-1212");

        superAdminUserStub.setupUserAsAuthorised(authentication, userAccountEntity);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_106","title":"Could not obtain user details","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchGetEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, "25");

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());

    }

    @Test
    void casesSearchGetEndpointOneObjectReturned() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", Matchers.is(hearingEntity.getId())))
            .andExpect(jsonPath("$[0].date", Matchers.is(Matchers.notNullValue())))
            .andExpect(jsonPath("$[0].judges", Matchers.is(Matchers.notNullValue())))
            .andExpect(jsonPath("$[0].courtroom", Matchers.is(SOME_COURTROOM.toUpperCase(Locale.ROOT))));

    }

    @Test
    void casesSearchGetEndpointMultipleObjectsReturned() throws Exception {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            "CR1",
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);
        HearingEntity hearingEntity2 = dartsDatabase.getHearingRepository().findAll().get(1);

        hearingEntity.addJudge(dartsDatabase.createSimpleJudge("hearing1Judge"), false);
        hearingEntity2.addJudge(dartsDatabase.createSimpleJudge("hearing2Judge"), false);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", Matchers.is(hearingEntity.getId())))
            .andExpect(jsonPath("$[0].date", Matchers.is(Matchers.notNullValue())))
            .andExpect(jsonPath("$[0].judges", Matchers.is(Matchers.notNullValue())))
            .andExpect(jsonPath("$[0].courtroom", Matchers.is(SOME_COURTROOM.toUpperCase(Locale.ROOT))))
            .andExpect(jsonPath("$[1].id", Matchers.is(hearingEntity2.getId())))
            .andExpect(jsonPath("$[1].courtroom", Matchers.is("CR1")));

    }

    @Test
    void casesSearchGetEndpointNoObjectsReturned() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, "25");

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andExpect(
            jsonPath(
                "$[0]").doesNotExist());

    }

    @Test
    void casesSearchEmptyHearingListCaseIdExists() throws Exception {

        String courthouseName = "25";
        CourthouseEntity courthouseEntity = dartsDatabase.createCourthouseUnlessExists(courthouseName);
        dartsDatabase.getUserAccountStub().createUnauthorisedIntegrationTestUser();
        dartsDatabase.getUserAccountStub().createAuthorisedIntegrationTestUser(courthouseEntity);

        CourtCaseEntity courtCase = dartsDatabase.createCase(courthouseName, "Test");

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, courtCase.getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.case_id").doesNotExist());

    }

    @Test
    void casesSearchWithInactiveUser() throws Exception {

        testUser.setActive(false);
        userAccountRepository.save(testUser);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andExpect(jsonPath("$.type").value(
            AuthorisationError.USER_DETAILS_INVALID.getType()));
    }

}