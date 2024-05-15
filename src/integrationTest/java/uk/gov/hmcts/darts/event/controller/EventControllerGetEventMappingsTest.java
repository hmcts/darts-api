package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EventControllerGetEventMappingsTest extends IntegrationBase  {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/admin/event-mappings";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToGetEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT);

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", Matchers.is(1)))
            .andExpect(jsonPath("$[0].type", Matchers.is("1000")))
            .andExpect(jsonPath("$[0].sub_type", Matchers.is("1002")))
            .andExpect(jsonPath("$[0].name", Matchers.is("Proceedings in chambers")))
            .andExpect(jsonPath("$[0].handler", Matchers.is("StandardEventHandler")))
            .andExpect(jsonPath("$[0].is_active", Matchers.is(true)))
            .andExpect(jsonPath("$[0].has_restrictions", Matchers.is(false)))
            .andExpect(jsonPath("$[0].created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToGetEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();
    }
}
