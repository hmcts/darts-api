package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class EventControllerGetEventMappingByIdTest extends IntegrationBase {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/admin/event-mappings/{event_handler_id}";

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToGetEventMappingById(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var eventHandlerEntity = dartsDatabase.createEventHandlerData("8888");

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT, eventHandlerEntity.getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(eventHandlerEntity.getId())))
            .andExpect(jsonPath("$.type", Matchers.is("99999")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("8888")))
            .andExpect(jsonPath("$.name", Matchers.is("some-desc")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(false)))
            .andExpect(jsonPath("$.has_events", Matchers.is(false)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToGetEventMappingByIdWithEventsLinkedToHandler(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        var eventHandlerEntity = dartsDatabase.createEventHandlerData("8888");
        dartsDatabase.createEvent(hearingEntity, eventHandlerEntity.getId());

        MockHttpServletRequestBuilder requestBuilder = get(EVENT_MAPPINGS_ENDPOINT, eventHandlerEntity.getId());

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
            .andExpect(jsonPath("$.id", Matchers.is(eventHandlerEntity.getId())))
            .andExpect(jsonPath("$.type", Matchers.is("99999")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("8888")))
            .andExpect(jsonPath("$.name", Matchers.is("some-desc")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(false)))
            .andExpect(jsonPath("$.has_events", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToGetEventMappingById(SecurityRoleEnum role) throws Exception {
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

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type", Matchers.is("EVENT_101")))
            .andExpect(jsonPath("$.status", Matchers.is(404)))
            .andExpect(
                jsonPath("$.detail", Matchers.is("No event handler could be found in the database for event handler id: 1000099.")))
            .andExpect(jsonPath("$.title", Matchers.is("No event handler mapping found in database")));
    }
}