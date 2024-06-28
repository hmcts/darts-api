package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class EventControllerDeleteEventMappingTest extends IntegrationBase {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/admin/event-mappings/{event_handler_id}";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private EventHandlerRepository eventHandlerRepository;

    @Test
    void deleteOk() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var entity = dartsDatabase.createEventHandlerData("8888");
        MockHttpServletRequestBuilder requestBuilder = delete(EVENT_MAPPINGS_ENDPOINT, entity.getId());
        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        Assertions.assertFalse(eventHandlerRepository.findById(entity.getId()).isPresent());
        Assertions.assertEquals(1, auditRepository.findAll().size());
        AuditEntity auditEntity = auditRepository.findAll().get(0);
        Assertions.assertEquals(AuditActivity.DELETE_EVENT_MAPPING.getId(), auditEntity.getAuditActivity().getId());
        Assertions.assertFalse(eventHandlerRepository.findRevisions(entity.getId()).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void wrongPermission(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);
        var entity = dartsDatabase.createEventHandlerData("8888");
        MockHttpServletRequestBuilder requestBuilder = delete(EVENT_MAPPINGS_ENDPOINT, entity.getId());
        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("AUTHORISATION_109")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(403)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("User is not authorised for this endpoint")));
    }

    @Test
    void mappingInactive() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        var entity = dartsDatabase.createEventHandlerData("8888");
        entity.setActive(false);
        dartsDatabase.save(entity);
        MockHttpServletRequestBuilder requestBuilder = delete(EVENT_MAPPINGS_ENDPOINT, entity.getId());
        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("EVENT_105")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(409)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.is(("Event handler " +
                "mapping %s cannot be deleted because it is inactive.").formatted(entity.getId()))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("The mapping is inactive, so cannot be deleted")));
    }

    @Test
    void mappingHasEvents() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        LocalDateTime hearingDate = LocalDateTime.of(2020, 6, 6, 20, 0, 0);
        HearingEntity hearing = dartsDatabase.createHearing("Courthouse", "1", "12345", hearingDate);
        EventEntity event = dartsDatabase.createEvent(hearing);

        MockHttpServletRequestBuilder requestBuilder = delete(EVENT_MAPPINGS_ENDPOINT, event.getEventType().getId());
        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("EVENT_106")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(409)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.detail", Matchers.is("Event handler mapping 10 already has processed events, so cannot be deleted.")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("The mapping has already processed events, so cannot be deleted")));
    }

    @Test
    void mappingDoesNotExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = delete(EVENT_MAPPINGS_ENDPOINT, -1);
        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {
              "type":"EVENT_101",
              "title": "No event handler mapping found in database",
              "status": 404,
              "detail": "No event handler could be found in the database for event handler id: -1.",
              "instance":"/admin/event-mappings/-1"
            }
            """;

        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}