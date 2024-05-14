package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class EventControllerGetEventMappingTest extends IntegrationBase  {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/event-mappings/{event_handler_id}";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToGetEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var entity = dartsDatabase.createEventHandlerData();

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT, entity.getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(entity.getId())))
            .andExpect(jsonPath("$.type", Matchers.is("99999")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("8888")))
            .andExpect(jsonPath("$.name", Matchers.is("some-desc")))
            .andExpect(jsonPath("$.handler", Matchers.is("Dummy integration test handler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(false)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToGetEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT, 1);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void eventMappingsEndpointShouldReturn404ErrorWhenEventMappingDoesNotExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT, 1_000_099);

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "type":"EVENT_101",
              "title": "No event handler found in database",
              "status": 404,
              "detail": "No event handler could be found in the database for event handler id: 1000099."
            }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }
}
