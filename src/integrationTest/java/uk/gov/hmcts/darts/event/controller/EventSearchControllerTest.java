package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.event.service.impl.AdminEventsSearchGivensBuilder;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@TestPropertySource(properties = {"darts.events.admin-search.max-results=5"})
@AutoConfigureMockMvc
class EventSearchControllerTest extends IntegrationBase {

    private static final String EVENT_SEARCH_ENDPOINT = "/admin/events/search";

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void returnsErrorIfTooManyResults() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        eventsGivensBuilder.persistedEvents(6);

        var mvcResult = mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                                            .content("{}")
                                            .contentType("application/json"))
            .andExpect(status().isBadRequest())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).extractingJsonPathStringValue("type", "EVENT_107");
        assertThat(response).extractingJsonPathStringValue("detail", "Number of results exceeded 5 please narrow your search.");
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.EXCLUDE)
    void forbidsNonSuperUsers(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                            .content("{}")
                            .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.INCLUDE)
    void allowsSuperUsers() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                            .content("{}")
                            .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void returnsAllFieldsCorrectly() throws Exception {
        int eventHearingsCount = 2;
        int eventsCount = 2;
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        List<EventEntity> entity = eventsGivensBuilder.persistedEventsWithHearings(eventsCount, eventHearingsCount);

        var mvcResult = mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                                            .content("{}")
                                            .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).hasJsonPathNumberValue("$.length()", eventHearingsCount + eventsCount);

        // assert the event and hearings
        for (int i = 0; i < eventsCount; i++) {
            for (int j = 0; j < eventHearingsCount; j++) {
                assertThat(response).hasJsonPathNumberValue("[" + i + "].id", entity.get(i).getId());
                assertThat(response).hasJsonPathStringValue("[" + i + "].event_ts", entity.get(i).getTimestamp().toString());
                assertThat(response).hasJsonPathNumberValue("[" + i + "].courthouse.id", entity.get(i).getCourtroom().getCourthouse().getId());
                assertThat(response).hasJsonPathStringValue("[" + i + "].courthouse.display_name",
                                                            entity.get(i).getCourtroom().getCourthouse().getDisplayName());
                assertThat(response).hasJsonPathNumberValue("[" + i + "].courtroom.id", entity.get(i).getCourtroom().getId());
                assertThat(response).hasJsonPathStringValue("[" + i + "].courtroom.name", entity.get(i).getCourtroom().getName());
                assertThat(response).hasJsonPathBooleanValue("[" + i + "].is_data_anonymised", entity.get(i).isDataAnonymised());
                assertThat(response).hasJsonPathBooleanValue("[" + i + "].is_case_expired",
                                                             entity.get(i).getHearingEntities().get(j).getCourtCase().isDataAnonymised());
            }
        }
    }
}