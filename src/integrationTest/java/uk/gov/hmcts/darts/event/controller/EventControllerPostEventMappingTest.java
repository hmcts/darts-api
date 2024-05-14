package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class EventControllerPostEventMappingTest extends IntegrationBase  {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/admin/event-mappings";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPost.json"));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type", Matchers.is("12345")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("9876")))
            .andExpect(jsonPath("$.name", Matchers.is("My Test Event")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappingsGivenTypeExistsAndSubtypeDoesNotExist(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingWithExistingTypeAndNewSubtypePost.json"));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type", Matchers.is("40750")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("9876")))
            .andExpect(jsonPath("$.name", Matchers.is("My Test Event")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappingsWithIsRevisionFalse(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT + "?is_revision=false")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingRevisionFalsePost.json"));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type", Matchers.is("123456")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("9876")))
            .andExpect(jsonPath("$.name", Matchers.is("My Test Event")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToGetEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void eventMappingsPostEndpointShouldReturn400ErrorWhenMissingTypeRequiredFieldPayload() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPayloadMissingTypeRequiredFieldPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void eventMappingsPostEndpointShouldReturn400ErrorWhenMissingHasRestrictionsRequiredFieldPayload() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPayloadMissingHasRestrictionsRequiredFieldPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void eventMappingsPostEndpointShouldReturn400ErrorWhenMissingNameRequiredFieldPayload() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPayloadMissingNameRequiredFieldPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void allowSuperAdminToPostEventMappingsWithMinimumRequiredDataPopulated() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPayloadMinimumRequiredFieldsPost.json"));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type", Matchers.is("123456")))
            .andExpect(jsonPath("$.sub_type").doesNotExist())
            .andExpect(jsonPath("$.name", Matchers.is("My Test Event")))
            .andExpect(jsonPath("$.handler", Matchers.is("DartsEventNullHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void eventMappingsPostEndpointShouldReturn409ErrorWhenDuplicateEventMapping() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingDuplicatePost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andReturn();
    }

    @Test
    void eventMappingsPostEndpointShouldReturn400ErrorWhenHandlerDoesNotExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingUnknownHandlerPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();
    }
}
