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
class EventControllerGetEventHandlersTest extends IntegrationBase  {

    private static final String EVENT_HANDLERS_ENDPOINT = "/admin/event-handlers";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToGetEventHandlers(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_HANDLERS_ENDPOINT);

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$[0]", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$[1]", Matchers.is("DarStopHandler")))
            .andExpect(jsonPath("$[2]", Matchers.is("DartsEventNullHandler")))
            .andExpect(jsonPath("$[3]", Matchers.is("InterpreterUsedHandler")))
            .andExpect(jsonPath("$[4]", Matchers.is("SentencingRemarksAndRetentionPolicyHandler")))
            .andExpect(jsonPath("$[5]", Matchers.is("SetReportingRestrictionEventHandler")))
            .andExpect(jsonPath("$[6]", Matchers.is("StandardEventHandler")))
            .andExpect(jsonPath("$[7]", Matchers.is("StopAndCloseHandler")));
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToGetEventHandlers(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_HANDLERS_ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andReturn();
    }

}
