package uk.gov.hmcts.darts.event.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.event.model.EventMapping;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class EventControllerPostEventMappingTest extends IntegrationBase {

    private static final String EVENT_MAPPINGS_ENDPOINT = "/admin/event-mappings";

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private EventHandlerRepository eventHandlerRepository;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        String typeToTest = "12345612";
        String subTypeToTest = "9876";

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPost.json")
                         .replace("${TYPE}", typeToTest).replace("${SUBTYPE}", subTypeToTest));

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        EventMapping mappingResult = objectMapper.readValue(result.getResponse().getContentAsString(), EventMapping.class);

        assertEquals(201, result.getResponse().getStatus());
        assertEquals(typeToTest, mappingResult.getType());
        assertEquals(subTypeToTest, mappingResult.getSubType());
        assertEquals("My Test Event", mappingResult.getName());
        assertEquals("DarStartHandler", mappingResult.getHandler());
        assertTrue(mappingResult.getIsActive());
        assertTrue(mappingResult.getHasRestrictions());
        assertNotNull(mappingResult.getCreatedAt());

        Integer mappingId = mappingResult.getId();

        assertEquals(1, auditRepository.findAll().size());
        AuditEntity auditEntity = auditRepository.findAll().getFirst();
        assertEquals(AuditActivity.ADDING_EVENT_MAPPING.getId(), auditEntity.getAuditActivity().getId());
        assertFalse(eventHandlerRepository.findRevisions(mappingId).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappingsThatAlreadyExistsWithRevision(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        String typeToTest = "12345";
        String subTypeToTest = "9876";

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPost.json")
                         .replace("${TYPE}", typeToTest).replace("${SUBTYPE}", subTypeToTest));

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        EventMapping mappingResult = objectMapper.readValue(result.getResponse().getContentAsString(), EventMapping.class);

        requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("is_revision", "true")
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPost.json")
                         .replace("${TYPE}", typeToTest).replace("${SUBTYPE}", subTypeToTest));


        assertEquals(201, result.getResponse().getStatus());
        assertEquals(typeToTest, mappingResult.getType());
        assertEquals(subTypeToTest, mappingResult.getSubType());
        assertEquals("My Test Event", mappingResult.getName());
        assertEquals("DarStartHandler", mappingResult.getHandler());
        assertTrue(mappingResult.getIsActive());
        assertTrue(mappingResult.getHasRestrictions());
        assertNotNull(mappingResult.getCreatedAt());

        MvcResult updatedResult = mockMvc.perform(requestBuilder).andReturn();

        EventMapping updatedMappingResult = objectMapper.readValue(updatedResult.getResponse().getContentAsString(), EventMapping.class);
        assertEquals(201, updatedResult.getResponse().getStatus());
        assertEquals(typeToTest, updatedMappingResult.getType());
        assertEquals(subTypeToTest, updatedMappingResult.getSubType());
        assertEquals("My Test Event", updatedMappingResult.getName());
        assertEquals("DarStartHandler", updatedMappingResult.getHandler());
        assertTrue(updatedMappingResult.getIsActive());
        assertTrue(updatedMappingResult.getHasRestrictions());
        assertNotNull(updatedMappingResult.getCreatedAt());

        Integer updatedMappingId = updatedMappingResult.getId();
        Integer mappingId = mappingResult.getId();
        assertNotEquals(mappingId, updatedMappingId);

        assertEquals(3, auditRepository.findAll().size());
        AuditEntity addedInitialEntity = auditRepository.findAll().getFirst();
        AuditEntity changedEntity = auditRepository.findAll().get(1);
        AuditEntity addedEntity = auditRepository.findAll().get(2);

        assertEquals(AuditActivity.ADDING_EVENT_MAPPING.getId(), addedInitialEntity.getAuditActivity().getId());
        assertEquals(AuditActivity.CHANGE_EVENT_MAPPING.getId(), changedEntity.getAuditActivity().getId());
        assertEquals(AuditActivity.ADDING_EVENT_MAPPING.getId(), addedEntity.getAuditActivity().getId());

        assertFalse(eventHandlerRepository.findRevisions(mappingId).isEmpty());
        assertFalse(eventHandlerRepository.findRevisions(updatedMappingId).isEmpty());
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

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("is_revision", "false")
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
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappingsWithIsRevisionTrueAndMakePreviousRevisionInactive(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var entity = dartsDatabase.createEventHandlerData("8888");

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("is_revision", "true")
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingRevisionTruePost.json"));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type", Matchers.is("99999")))
            .andExpect(jsonPath("$.sub_type", Matchers.is("8888")))
            .andExpect(jsonPath("$.name", Matchers.is("My Test Event")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());

        var updatedPreviousActiveMapping = dartsDatabase.findEventHandlerMappingFor(entity.getId());

        assertFalse(updatedPreviousActiveMapping.getActive());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.INCLUDE)
    void allowSuperAdminToPostEventMappingsWithIsRevisionTrueAndEmptySubtypeAndMakePreviousRevisionInactive(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var entity = dartsDatabase.createEventHandlerData(null);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("is_revision", "true")
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingWithEmptySubtypeAndRevisionTruePost.json"));

        mockMvc.perform(requestBuilder).andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.type", Matchers.is("99999")))
            .andExpect(jsonPath("$.sub_type").doesNotExist())
            .andExpect(jsonPath("$.name", Matchers.is("My Test Event")))
            .andExpect(jsonPath("$.handler", Matchers.is("DarStartHandler")))
            .andExpect(jsonPath("$.is_active", Matchers.is(true)))
            .andExpect(jsonPath("$.has_restrictions", Matchers.is(true)))
            .andExpect(jsonPath("$.created_at").exists());

        var updatedPreviousActiveMapping = dartsDatabase.findEventHandlerMappingFor(entity.getId());

        assertFalse(updatedPreviousActiveMapping.getActive());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToGetEventMappings(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingPost.json")
                         .replace("${TYPE}", "12345")
                         .replace("${SUBTYPE}", "9879"));

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
    void eventMappingsPostEndpointShouldReturn409ErrorWhenEventMappingDoesNotExistForIsRevisionTrue() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam("is_revision", "true")
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingUnknownMappingForRevisionPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isConflict())
            .andReturn();
    }

    @Test
    void eventMappingsPostEndpointShouldReturn422ErrorWhenHandlerDoesNotExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        MockHttpServletRequestBuilder requestBuilder = post(EVENT_MAPPINGS_ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(getContentsFromFile(
                "tests/events/EventControllerPostEventMappingTest/createEventMappingUnknownHandlerPost.json"));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isUnprocessableEntity())
            .andReturn();
    }
}