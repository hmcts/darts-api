package uk.gov.hmcts.darts.audio.controller;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.HiddenReason;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerGetAdminMediasByIdIntTest extends IntegrationBase {

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DartsDatabaseStub databaseStub;

    private static final URI ENDPOINT = URI.create("/admin/medias/");
    private static final String COURTHOUSE_NAME = "TESTCOURTHOUSE";
    private static final String COURTROOM_NAME = "TESTCOURTROOM";
    private static final String CASE_NUMBER = "testCaseNumber";
    private static final OffsetDateTime HEARING_START_AT = OffsetDateTime.parse("2024-01-01T12:10:10Z");
    private static final OffsetDateTime MEDIA_START_AT = HEARING_START_AT;
    private static final OffsetDateTime MEDIA_END_AT = MEDIA_START_AT.plusHours(1);
    private static final String HIDE_DELETE_AT = "2024-02-01T00:00:00Z";
    private static final String RETAIN_UNTIL = "2200-02-01T00:00:00Z";

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_USER", "SUPER_ADMIN"}, mode = INCLUDE)
    void shouldReturnExpectedMediaObjectAndChildren(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var hearingEntity = databaseStub.createHearing(COURTHOUSE_NAME, COURTROOM_NAME, CASE_NUMBER, HEARING_START_AT.toLocalDateTime());

        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        var mediaEntity = createAndSaveMediaEntity(hearingEntity, userAccountEntity, false);

        createAndSaveAdminActionEntity(mediaEntity, userAccountEntity);

        // When
        mockMvc.perform(get(ENDPOINT.resolve(String.valueOf(mediaEntity.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.start_at").value(MEDIA_START_AT.toString()))
            .andExpect(jsonPath("$.end_at").value(MEDIA_END_AT.toString()))
            .andExpect(jsonPath("$.channel").value(2))
            .andExpect(jsonPath("$.total_channels").value(2))
            .andExpect(jsonPath("$.media_type").value("A"))
            .andExpect(jsonPath("$.media_format").value("mp2"))
            .andExpect(jsonPath("$.file_size_bytes").value(1000))
            .andExpect(jsonPath("$.filename").value("a-media-file"))
            .andExpect(jsonPath("$.media_object_id").value("object-id-value"))
            .andExpect(jsonPath("$.content_object_id").value("content-id-value"))
            .andExpect(jsonPath("$.clip_id").value("clip-id-value"))
            .andExpect(jsonPath("$.checksum").value("7017013d05bcc5032e142049081821d6"))
            .andExpect(jsonPath("$.media_status").value("media-status-value"))
            .andExpect(jsonPath("$.is_hidden").value(true))
            .andExpect(jsonPath("$.is_deleted").value(false))
            .andExpect(jsonPath("$.is_current").value(true))

            .andExpect(jsonPath("$.admin_action").exists())
            .andExpect(jsonPath("$.admin_action.id").isNumber())
            .andExpect(jsonPath("$.admin_action.reason_id").isNumber())
            .andExpect(jsonPath("$.admin_action.hidden_by_id").isNumber())
            .andExpect(jsonPath("$.admin_action.hidden_at").value(HIDE_DELETE_AT))
            .andExpect(jsonPath("$.admin_action.is_marked_for_manual_deletion").value(true))
            .andExpect(jsonPath("$.admin_action.marked_for_manual_deletion_by_id").isNumber())
            .andExpect(jsonPath("$.admin_action.marked_for_manual_deletion_at").value(HIDE_DELETE_AT))
            .andExpect(jsonPath("$.admin_action.ticket_reference").value("ticket-reference-value"))
            .andExpect(jsonPath("$.admin_action.comments").value("comments-value"))

            .andExpect(jsonPath("$.version").value("version-label-value"))
            .andExpect(jsonPath("$.chronicle_id").value("chronicle-value"))
            .andExpect(jsonPath("$.antecedent_id").value("antecedent-value"))
            .andExpect(jsonPath("$.retain_until").value(RETAIN_UNTIL))
            .andExpect(jsonPath("$.created_at").isString())
            .andExpect(jsonPath("$.created_by_id").isNumber())
            .andExpect(jsonPath("$.last_modified_at").isString())
            .andExpect(jsonPath("$.last_modified_by_id").isNumber())

            .andExpect(jsonPath("$.courthouse").exists())
            .andExpect(jsonPath("$.courthouse.id").isNumber())
            .andExpect(jsonPath("$.courthouse.display_name").value(COURTHOUSE_NAME))

            .andExpect(jsonPath("$.courtroom").exists())
            .andExpect(jsonPath("$.courtroom.id").isNumber())
            .andExpect(jsonPath("$.courtroom.name").value(COURTROOM_NAME))

            .andExpect(jsonPath("$.hearings", hasSize(1)))
            .andExpect(jsonPath("$.hearings.[0].id").isNumber())
            .andExpect(jsonPath("$.hearings.[0].hearing_date").value(HEARING_START_AT.toLocalDate().toString()))
            .andExpect(jsonPath("$.hearings.[0].case_id").isNumber())

            .andReturn();

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_USER", "SUPER_ADMIN"}, mode = INCLUDE)
    void shouldReturnExpectedMediaObjectAndChildrenWithHiddenAndCurrentSetFalse(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var hearingEntity = databaseStub.createHearing(COURTHOUSE_NAME, COURTROOM_NAME, CASE_NUMBER, HEARING_START_AT.toLocalDateTime());

        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        var mediaEntity = createAndSaveMediaEntityWithHiddenAndDeletedAndCurrentSetFalse(hearingEntity, userAccountEntity);

        createAndSaveAdminActionEntity(mediaEntity, userAccountEntity);

        // When
        mockMvc.perform(get(ENDPOINT.resolve(String.valueOf(mediaEntity.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.start_at").value(MEDIA_START_AT.toString()))
            .andExpect(jsonPath("$.end_at").value(MEDIA_END_AT.toString()))
            .andExpect(jsonPath("$.channel").value(2))
            .andExpect(jsonPath("$.total_channels").value(2))
            .andExpect(jsonPath("$.media_type").value("A"))
            .andExpect(jsonPath("$.media_format").value("mp2"))
            .andExpect(jsonPath("$.file_size_bytes").value(1000))
            .andExpect(jsonPath("$.filename").value("a-media-file"))
            .andExpect(jsonPath("$.media_object_id").value("object-id-value"))
            .andExpect(jsonPath("$.content_object_id").value("content-id-value"))
            .andExpect(jsonPath("$.clip_id").value("clip-id-value"))
            .andExpect(jsonPath("$.checksum").value("7017013d05bcc5032e142049081821d6"))
            .andExpect(jsonPath("$.media_status").value("media-status-value"))
            .andExpect(jsonPath("$.is_hidden").value(false))
            .andExpect(jsonPath("$.is_deleted").value(false))
            .andExpect(jsonPath("$.is_current").value(false))

            .andExpect(jsonPath("$.admin_action").exists())
            .andExpect(jsonPath("$.admin_action.id").isNumber())
            .andExpect(jsonPath("$.admin_action.reason_id").isNumber())
            .andExpect(jsonPath("$.admin_action.hidden_by_id").isNumber())
            .andExpect(jsonPath("$.admin_action.hidden_at").value(HIDE_DELETE_AT))
            .andExpect(jsonPath("$.admin_action.is_marked_for_manual_deletion").value(true))
            .andExpect(jsonPath("$.admin_action.marked_for_manual_deletion_by_id").isNumber())
            .andExpect(jsonPath("$.admin_action.marked_for_manual_deletion_at").value(HIDE_DELETE_AT))
            .andExpect(jsonPath("$.admin_action.ticket_reference").value("ticket-reference-value"))
            .andExpect(jsonPath("$.admin_action.comments").value("comments-value"))

            .andExpect(jsonPath("$.version").value("version-label-value"))
            .andExpect(jsonPath("$.chronicle_id").value("chronicle-value"))
            .andExpect(jsonPath("$.antecedent_id").value("antecedent-value"))
            .andExpect(jsonPath("$.retain_until").value(RETAIN_UNTIL))
            .andExpect(jsonPath("$.created_at").isString())
            .andExpect(jsonPath("$.created_by_id").isNumber())
            .andExpect(jsonPath("$.last_modified_at").isString())
            .andExpect(jsonPath("$.last_modified_by_id").isNumber())

            .andExpect(jsonPath("$.courthouse").exists())
            .andExpect(jsonPath("$.courthouse.id").isNumber())
            .andExpect(jsonPath("$.courthouse.display_name").value(COURTHOUSE_NAME))

            .andExpect(jsonPath("$.courtroom").exists())
            .andExpect(jsonPath("$.courtroom.id").isNumber())
            .andExpect(jsonPath("$.courtroom.name").value(COURTROOM_NAME))

            .andReturn();

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_USER", "SUPER_ADMIN"}, mode = INCLUDE)
    void shouldReturn404WhenMediaRecordDoesNotExist(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT.resolve("123456789")))
            .andExpect(status().isNotFound())
            .andReturn();

        // Then
        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUDIO_102",
                                      "title": "The requested media cannot be found",
                                      "status": 404
                                    }
                                    """, jsonString, JSONCompareMode.STRICT);
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_USER", "SUPER_ADMIN"}, mode = INCLUDE)
    void shouldReturn404WhenMediaRecordIsDeleted(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        var hearingEntity = databaseStub.createHearing(COURTHOUSE_NAME, COURTROOM_NAME, CASE_NUMBER, HEARING_START_AT.toLocalDateTime());

        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        var mediaEntity = createAndSaveMediaEntity(hearingEntity, userAccountEntity, true);

        createAndSaveAdminActionEntity(mediaEntity, userAccountEntity);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT.resolve(String.valueOf(mediaEntity.getId()))))
            .andExpect(status().isNotFound())
            .andReturn();

        // Then
        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUDIO_102",
                                      "title": "The requested media cannot be found",
                                      "status": 404
                                    }
                                    """, jsonString, JSONCompareMode.STRICT);

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_USER", "SUPER_ADMIN"}, mode = EXCLUDE)
    void shouldDenyAccess(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT.resolve("123456789")))
            .andExpect(status().isForbidden())
            .andReturn();

        // Then
        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUTHORISATION_109",
                                      "title": "User is not authorised for this endpoint",
                                      "status": 403
                                    }
                                    """, jsonString, JSONCompareMode.STRICT);
    }

    @Test
    void getMediaById_shouldReturnCaseWithCasIdAndActualHearing_whenMediaHasLinkedCase() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_USER);
        UserAccountEntity userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        // Create hearing and mark it as actual
        var hearingEntity = databaseStub.createHearing(COURTHOUSE_NAME, COURTROOM_NAME, CASE_NUMBER, HEARING_START_AT.toLocalDateTime());
        hearingEntity.setHearingIsActual(true);
        databaseStub.getHearingRepository().save(hearingEntity);

        // Create media and link it to hearing
        var mediaEntity = createAndSaveMediaEntity(hearingEntity, userAccountEntity, false);

        // Create media linked case with court case
        databaseStub.createMediaLinkedCase(mediaEntity, hearingEntity.getCourtCase());

        // When
        mockMvc.perform(get(ENDPOINT.resolve(String.valueOf(mediaEntity.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cases", hasSize(1)))
            .andExpect(jsonPath("$.cases[0].id").value(hearingEntity.getCourtCase().getId()))
            .andExpect(jsonPath("$.cases[0].case_number").value(CASE_NUMBER))
            .andExpect(jsonPath("$.cases[0].source").value("Legacy"))
            .andExpect(jsonPath("$.cases[0].courthouse.id").value(hearingEntity.getCourtCase().getCourthouse().getId()))
            .andExpect(jsonPath("$.cases[0].courthouse.display_name").value(COURTHOUSE_NAME))
            .andExpect(jsonPath("$.hearings", hasSize(1)))
            .andExpect(jsonPath("$.hearings[0].id").value(hearingEntity.getId()))
            .andExpect(jsonPath("$.hearings[0].case_id").value(hearingEntity.getCourtCase().getId()))
            .andExpect(jsonPath("$.hearings[0].case_number").value(CASE_NUMBER))
            .andExpect(jsonPath("$.hearings[0].hearing_date").value(HEARING_START_AT.toLocalDate().toString()))
            .andExpect(jsonPath("$.hearings[0].courthouse.id").value(hearingEntity.getCourtCase().getCourthouse().getId()))
            .andExpect(jsonPath("$.hearings[0].courthouse.display_name").value(COURTHOUSE_NAME))
            .andExpect(jsonPath("$.hearings[0].courtroom.id").value(hearingEntity.getCourtroom().getId()))
            .andExpect(jsonPath("$.hearings[0].courtroom.name").value(COURTROOM_NAME));
    }

    @Test
    void getMediaById_shouldReturnCaseWithBaseDetails_whenCaseHasNoCourtCaseRecord() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_USER);
        UserAccountEntity userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        // Create media without hearing
        var mediaEntity = createAndSaveMediaEntity(null, userAccountEntity, false);

        // Create media linked case without court case
        databaseStub.createMediaLinkedCase(
            mediaEntity,
            "MIGRATED_CASE",
            COURTHOUSE_NAME
        );

        // When
        mockMvc.perform(get(ENDPOINT.resolve(String.valueOf(mediaEntity.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cases", hasSize(1)))
            .andExpect(jsonPath("$.cases[0].id").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.cases[0].case_number").value("MIGRATED_CASE"))
            .andExpect(jsonPath("$.cases[0].source").value("Legacy"))
            .andExpect(jsonPath("$.cases[0].courthouse.display_name").value(COURTHOUSE_NAME))
            .andExpect(jsonPath("$.cases[0].courthouse.id").doesNotExist());
    }

    private MediaEntity createAndSaveMediaEntity(HearingEntity hearingEntity, UserAccountEntity userAccountEntity, boolean isDeleted) {
        MediaEntity mediaEntity = databaseStub.createMediaEntity(COURTHOUSE_NAME,
                                                                 COURTROOM_NAME,
                                                                 MEDIA_START_AT,
                                                                 MEDIA_END_AT,
                                                                 2);
        mediaEntity.setLegacyObjectId("object-id-value");
        mediaEntity.setContentObjectId("content-id-value");
        mediaEntity.setClipId("clip-id-value");
        mediaEntity.setMediaStatus("media-status-value");
        mediaEntity.setHidden(true);
        mediaEntity.setDeleted(isDeleted);
        mediaEntity.setLegacyVersionLabel("version-label-value");
        mediaEntity.setChronicleId("chronicle-value");
        mediaEntity.setAntecedentId("antecedent-value");
        mediaEntity.setRetainUntilTs(OffsetDateTime.parse(RETAIN_UNTIL));
        mediaEntity.setCreatedBy(userAccountEntity);
        mediaEntity.setLastModifiedBy(userAccountEntity);
        mediaEntity.setIsCurrent(true);

        mediaEntity.setHearingList(Collections.singletonList(hearingEntity));
        if (hearingEntity != null) {
            mediaEntity.setHearingList(Collections.singletonList(hearingEntity));
            hearingEntity.setMediaList(Collections.singletonList(mediaEntity));
            databaseStub.getHearingRepository()
                .save(hearingEntity);
        }

        return databaseStub.getMediaRepository()
            .saveAndFlush(mediaEntity);
    }

    private MediaEntity createAndSaveMediaEntityWithHiddenAndDeletedAndCurrentSetFalse(HearingEntity hearingEntity, UserAccountEntity userAccountEntity) {
        var mediaEntity = createAndSaveMediaEntity(hearingEntity, userAccountEntity, false);

        mediaEntity.setHidden(false);
        mediaEntity.setIsCurrent(false);

        return databaseStub.getMediaRepository().save(mediaEntity);
    }

    private ObjectAdminActionEntity createAndSaveAdminActionEntity(MediaEntity mediaEntity, UserAccountEntity userAccountEntity) {
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setMedia(mediaEntity);
        objectAdminActionEntity.setHiddenBy(userAccountEntity);
        objectAdminActionEntity.setHiddenDateTime(OffsetDateTime.parse(HIDE_DELETE_AT));
        objectAdminActionEntity.setMarkedForManualDeletion(true);
        objectAdminActionEntity.setMarkedForManualDelBy(userAccountEntity);
        objectAdminActionEntity.setMarkedForManualDelDateTime(OffsetDateTime.parse(HIDE_DELETE_AT));
        objectAdminActionEntity.setTicketReference("ticket-reference-value");
        objectAdminActionEntity.setComments("comments-value");

        int reasonId = HiddenReason.OTHER_DELETE.getId();
        var hiddenReasonEntity = databaseStub.getObjectHiddenReasonRepository().findById(reasonId)
            .orElseThrow();
        objectAdminActionEntity.setObjectHiddenReason(hiddenReasonEntity);

        return databaseStub.getObjectAdminActionRepository()
            .saveAndFlush(objectAdminActionEntity);
    }

}
