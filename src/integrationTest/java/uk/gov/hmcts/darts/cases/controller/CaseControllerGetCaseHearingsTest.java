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
import uk.gov.hmcts.darts.cases.model.Hearing;
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
    void caseHearingsGetEndpointShouldReturnForbidden() throws Exception {
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
    void caseHearingsGetEndpointNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, "25");

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());

    }

    @Test
    void caseHearingsGetEndpointOneObjectReturned() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", Matchers.is(1)))
            .andExpect(jsonPath("$[0].id", Matchers.is(hearingEntity.getId())))
            .andExpect(jsonPath("$[0].date", Matchers.is(DateConverterUtil.toLocalDateTime(SOME_DATE_TIME).toLocalDate().toString())))
            .andExpect(jsonPath("$[0].judges", Matchers.is(Matchers.notNullValue())))
            .andExpect(jsonPath("$[0].courtroom", Matchers.is(SOME_COURTROOM.toUpperCase(Locale.ROOT))));
    }

    @Test
    void caseHearingsGetEndpoint_shouldBeOrderedByHearingDate() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();

        HearingEntity hearingEntity2 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME.plusDays(1))
        );

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", Matchers.is(2)))
            .andExpect(jsonPath("$[0].id", Matchers.is(hearingEntity2.getId())))
            .andExpect(jsonPath("$[0].date", Matchers.is(hearingEntity2.getHearingDate().toString())))
            .andExpect(jsonPath("$[1].id", Matchers.is(hearingEntity.getId())))
            .andExpect(jsonPath("$[1].date", Matchers.is(hearingEntity.getHearingDate().toString())));

        HearingEntity hearingEntity3 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME.minusDays(1))
        );

        requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", Matchers.is(3)))
            .andExpect(jsonPath("$[0].id", Matchers.is(hearingEntity2.getId())))
            .andExpect(jsonPath("$[0].date", Matchers.is(hearingEntity2.getHearingDate().toString())))
            .andExpect(jsonPath("$[1].id", Matchers.is(hearingEntity.getId())))
            .andExpect(jsonPath("$[1].date", Matchers.is(hearingEntity.getHearingDate().toString())))
            .andExpect(jsonPath("$[2].id", Matchers.is(hearingEntity3.getId())))
            .andExpect(jsonPath("$[2].date", Matchers.is(hearingEntity3.getHearingDate().toString())));
    }

    @Test
    void caseHearingsMultipleWithTranscripts() throws Exception {
        var otherCourtroom = "CR1";
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            otherCourtroom,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
        HearingEntity hearingEntity2 = dartsDatabase.getHearingRepository().findAll().get(1);

        hearingEntity.addJudge(dartsDatabase.createSimpleJudge("hearing1Judge"), false);
        hearingEntity2.addJudge(dartsDatabase.createSimpleJudge("hearing2Judge"), false);

        dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            testUser, hearingEntity.getCourtCase(), hearingEntity2, SOME_DATE_TIME, false
        );

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        Hearing[] hearingResultList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Hearing[].class);
        assertEquals(2, hearingResultList.length);

        var hearing1 = hearingResultList[0];
        assertEquals(hearingEntity.getId(), hearing1.getId());
        assertEquals(SOME_COURTROOM.toUpperCase(Locale.ROOT), hearing1.getCourtroom());
        assertEquals(1, hearing1.getJudges().size());
        assertEquals("1JUDGE1", hearing1.getJudges().getFirst());
        assertEquals(DateConverterUtil.toLocalDateTime(SOME_DATE_TIME).toLocalDate().toString(), hearing1.getDate().toString());
        assertEquals(0, hearing1.getTranscriptCount());

        var hearing2 = hearingResultList[1];
        assertEquals(hearingEntity2.getId(), hearing2.getId());
        assertEquals(otherCourtroom, hearing2.getCourtroom());
        assertEquals(1, hearing2.getJudges().size());
        assertEquals("1JUDGE1", hearing2.getJudges().getFirst());
        assertEquals(DateConverterUtil.toLocalDateTime(SOME_DATE_TIME).toLocalDate().toString(), hearing2.getDate().toString());
        assertEquals(1, hearing2.getTranscriptCount());
    }

    @Test
    void caseHearingsMultipleWithTranscriptsWithHiddenDocument() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();

        hearingEntity.addJudge(dartsDatabase.createSimpleJudge("hearing1Judge"), false);

        dartsDatabase.getTranscriptionStub().createAndSaveCompletedTranscriptionWithDocument(
            testUser, hearingEntity.getCourtCase(), hearingEntity, SOME_DATE_TIME, true
        );

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        Hearing[] hearingResultList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Hearing[].class);
        var hearing1 = hearingResultList[0];
        assertEquals(1, hearingResultList.length);
        assertEquals(0, hearing1.getTranscriptCount());
    }

    @Test
    void caseHearingsEmptyHearingList() throws Exception {

        String courthouseName = "25";
        CourthouseEntity courthouseEntity = dartsDatabase.createCourthouseUnlessExists(courthouseName);
        dartsDatabase.getUserAccountStub().createUnauthorisedIntegrationTestUser();
        dartsDatabase.getUserAccountStub().createAuthorisedIntegrationTestUser(courthouseEntity);

        CourtCaseEntity courtCase = dartsDatabase.createCase(courthouseName, "Test");

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, courtCase.getId());

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        Hearing[] hearingResultList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Hearing[].class);

        assertEquals(0, hearingResultList.length);
    }

    @Test
    void caseHearingsWithInactiveUser() throws Exception {

        testUser.setActive(false);
        userAccountRepository.save(testUser);

        MockHttpServletRequestBuilder requestBuilder = get(endpointUrl, hearingEntity.getCourtCase().getId());

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andExpect(jsonPath("$.type").value(
            AuthorisationError.USER_DETAILS_INVALID.getType()));
    }

}