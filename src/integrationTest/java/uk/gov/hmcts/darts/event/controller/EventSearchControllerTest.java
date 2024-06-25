package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.event.service.impl.AdminEventsSearchGivensBuilder;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class EventSearchControllerTest extends IntegrationBase {

    private static final String EVENT_SEARCH_ENDPOINT = "/admin/events/search";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private AdminEventsSearchGivensBuilder eventsGivensBuilder;

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    @BeforeEach
    void setUp() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void tearDown() {
        openInViewUtil.closeEntityManager();
    }


    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void forbidsNonSuperAdminUsers(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                            .content("{}")
                            .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void allowsSuperAdminUser() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
            .content("{}")
            .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void returnsAllFieldsCorrectly() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        eventsGivensBuilder.persistedEvents(1);

        var mvcResult = mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                                            .content("{}")
                                            .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).hasJsonPathNumberValue("events[0].id");
        assertThat(response).hasJsonPathStringValue("events[0].created_at");
        assertThat(response).hasJsonPathNumberValue("events[0].courthouse.id");
        assertThat(response).hasJsonPathStringValue("events[0].courthouse.display_name");
        assertThat(response).hasJsonPathNumberValue("events[0].courtroom.id");
        assertThat(response).hasJsonPathStringValue("events[0].courtroom.name");
    }
}