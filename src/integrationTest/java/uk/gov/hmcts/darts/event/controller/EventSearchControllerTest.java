package uk.gov.hmcts.darts.event.controller;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.service.impl.AdminEventsSearchGivensBuilder;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void adminEventSearch_ShouldReturnErrorTooManyResults() throws Exception {
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
    void adminEventSearch_ShouldForbidNonSuperUsers(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                            .content("{}")
                            .contentType("application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.INCLUDE)
    void adminEventSearch_ShouldAllowSuperUsers() throws Exception {
        List<EventEntity> entity = eventsGivensBuilder.persistedEventsWithHearings(1, 1);
        CourtCaseEntity courtCaseEntity = entity.getFirst().getHearingEntity().getCourtCase();
        courtCaseEntity.setDataAnonymisedTs(now());
        dartsDatabase.save(courtCaseEntity);
        AdminEventSearch request = new AdminEventSearch();
        request.setCaseNumber(courtCaseEntity.getCaseNumber());

        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void adminEventSearch_ShouldReturnOK_WithCaseNumber() throws Exception {
        int eventHearingsCount = 2;
        int eventsCount = 2;
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        List<EventEntity> entity = eventsGivensBuilder.persistedEventsWithHearings(eventsCount, eventHearingsCount);
        CourtCaseEntity courtCaseEntity = entity.getFirst().getHearingEntity().getCourtCase();
        courtCaseEntity.setDataAnonymisedTs(now());
        dartsDatabase.save(courtCaseEntity);
        AdminEventSearch request = new AdminEventSearch();
        request.setCaseNumber(courtCaseEntity.getCaseNumber());

        var mvcResult = mockMvc.perform(post(EVENT_SEARCH_ENDPOINT)
                                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        var response = json.from(mvcResult.getResponse().getContentAsString());
        assertThat(response).hasJsonPathNumberValue("[0].id");
        assertThat(response).hasJsonPathStringValue("[0].event_ts");
        assertThat(response).hasJsonPathNumberValue("[0].courthouse.id");
        assertThat(response).hasJsonPathStringValue("[0].courthouse.display_name");
        assertThat(response).hasJsonPathNumberValue("[0].courtroom.id");
        assertThat(response).hasJsonPathStringValue("[0].courtroom.name");
        assertThat(response).hasJsonPathBooleanValue("[0].is_data_anonymised");

        DocumentContext documentContext = JsonPath.parse(mvcResult.getResponse().getContentAsString());
        assertEquals(1, (Integer) documentContext.read("$[0].id"));
    }
}
