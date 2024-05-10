package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EventControllerGetEventsMappingTest extends IntegrationBase  {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/event-mappings/{event_handler_id}";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Test
    void eventMappingsGetEndpoint() throws Exception {

        var entity = dartsDatabase.createEventHandlerData();

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

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

    @Test
    void eventMappingsEndpointShouldReturnForbiddenError() throws Exception {
        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT, 1);

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void eventMappingsEndpointShouldReturn404ErrorWhenEventMappingDoesNotExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

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
