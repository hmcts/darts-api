package uk.gov.hmcts.darts.event.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EventsControllerPatchAdminEventByIdIntTest extends IntegrationBase {
    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DartsDatabaseStub databaseStub;

    private static final URI ENDPOINT = URI.create("/admin/events/");
    private static final String COURTHOUSE_NAME = "TESTCOURTHOUSE";
    private static final String COURTROOM_NAME = "TESTCOURTROOM";
    private static final String CASE_NUMBER = "testCaseNumber";
    private static final OffsetDateTime HEARING_START_AT = OffsetDateTime.parse("2024-01-01T12:10:10Z");
    private static final OffsetDateTime EVENT_TIMESTAMP = HEARING_START_AT;

    @Test
    void shouldUpdateEventToIsCurrentTrue_whenPayloadHasThisSetToTrue_andResetAllOtherAssociatedEventIsCurrentToFalse() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        var eventEntity1 = createAndSaveEventEntity(1234, false);
        var eventEntity2 = createAndSaveEventEntity(1234, true);
        var eventEntity3 = createAndSaveEventEntity(1234, false);
        var eventEntity4 = createAndSaveEventEntity(12, true);

        assertEventIsCurrentStatus(eventEntity1.getId(), false);
        assertEventIsCurrentStatus(eventEntity2.getId(), true);
        assertEventIsCurrentStatus(eventEntity3.getId(), false);
        assertEventIsCurrentStatus(eventEntity4.getId(), true);

        // When
        mockMvc.perform(patch(ENDPOINT.resolve(String.valueOf(eventEntity1.getId())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createPayload(true)))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        assertEventIsCurrentStatus(eventEntity1.getId(), true);
        assertEventIsCurrentStatus(eventEntity2.getId(), false);
        assertEventIsCurrentStatus(eventEntity3.getId(), false);
        assertEventIsCurrentStatus(eventEntity4.getId(), true);//Should be true as different eventId

        List<AuditEntity> auditEntityList = dartsDatabase.getAuditRepository().findAll();

        assertThat(auditEntityList).hasSize(1);
        AuditEntity auditEntity = auditEntityList.get(0);
        assertThat(auditEntity.getAuditActivity().getId()).isEqualTo(AuditActivity.CURRENT_EVENT_VERSION_UPDATED.getId());
        assertThat(auditEntity.getAdditionalData()).isEqualTo("eve_id: 1 was made current replacing eve_id: [2]");
    }

    @Test
    void shouldReturn400_whenIsCurrentIsSetToFalse() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT.resolve("123456789"))
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(createPayload(false)))
            .andExpect(status().isBadRequest())
            .andReturn();
        assertStandardErrorJsonResponse(mvcResult, CommonApiError.BAD_REQUEST, "is_current must be set to true");
    }

    @Test
    void shouldReturn400_whenIsCurrentIsSetToNull() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        mockMvc.perform(patch(ENDPOINT.resolve("123456789"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createPayload(null)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409_whenEventIsAlreadyCurrent() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        var eventEntity = createAndSaveEventEntity(123, true);

        // When
        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT.resolve(String.valueOf(eventEntity.getId())))
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(createPayload(true)))
            .andExpect(status().isConflict())
            .andReturn();
        assertStandardErrorJsonResponse(mvcResult, EventError.EVENT_ALREADY_CURRENT);
    }


    @Test
    void shouldReturn404_whenEventRecordDoesNotExist() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        // When
        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT.resolve("123456789"))
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(createPayload(true)))
            .andExpect(status().isNotFound())
            .andReturn();

        // Then
        assertStandardErrorJsonResponse(mvcResult, CommonApiError.NOT_FOUND, "Event with id 123456789 not found");
    }


    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = EXCLUDE)
    void shouldThrowUnauthorisedError_whenUserIsNotAuthenticatedAtTheRightLevel(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        // When
        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT.resolve("123456789"))
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(createPayload(true)))
            .andExpect(status().isForbidden())
            .andReturn();

        // Then
        assertStandardErrorJsonResponse(mvcResult, AuthorisationError.USER_NOT_AUTHORISED_FOR_ENDPOINT);
    }

    private EventEntity createAndSaveEventEntity(Integer eventId, boolean isCurrent) {
        CourtCaseEntity courtCaseEntity = databaseStub.createCase(
            COURTHOUSE_NAME,
            CASE_NUMBER
        );

        EventEntity eventEntity = PersistableFactory.getEventTestData().someMinimal();
        eventEntity.setTimestamp(EVENT_TIMESTAMP);
        eventEntity.setEventId(eventId);
        eventEntity.setIsCurrent(isCurrent);
        eventEntity.setCourtroom(databaseStub.createCourtroomUnlessExists(COURTHOUSE_NAME, COURTROOM_NAME));
        dartsPersistence.save(eventEntity);

        databaseStub.createEventLinkedCase(
            eventEntity,
            courtCaseEntity
        );

        transactionalUtil.executeInTransaction(() -> {
            HearingEntity hearing = databaseStub.createHearing(
                COURTHOUSE_NAME,
                COURTROOM_NAME,
                CASE_NUMBER,
                HEARING_START_AT.toLocalDateTime()
            );
            if (isCurrent) {
                eventEntity.addHearing(hearing);
            }
            dartsDatabase.getHearingRepository().saveAndFlush(hearing);
        });
        return databaseStub.getEventRepository().saveAndFlush(eventEntity);
    }

    private String createPayload(Boolean isCurrent) {
        return "{\"is_current\": %s}".formatted(isCurrent);
    }

    private void assertEventIsCurrentStatus(long eventId, boolean isCurrent) {
        transactionalUtil.executeInTransaction(() -> {
            EventEntity event = databaseStub.getEventRepository().findById(eventId).orElseThrow();
            assertThat(event.getIsCurrent()).isEqualTo(isCurrent);
            if (isCurrent) {
                assertThat(event.getHearingEntities()).isNotEmpty();
            } else {
                assertThat(event.getHearingEntities()).isEmpty();
            }
        });
    }
}
