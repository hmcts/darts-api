package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
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
class AudioControllerPatchAdminMediasByIdIntTest extends IntegrationBase {

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

    @Test
    void shouldUpdateMediaToIsCurrentTrue_whenPayloadHasThisSetToTrue_andResetAllOtherAssociatedMediaIsCurrentToFalse() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();


        var mediaEntity1 = createAndSaveMediaEntity(userAccountEntity, "chronicleId", false, false);
        var mediaEntity2 = createAndSaveMediaEntity(userAccountEntity, "chronicleId", true, false);
        var mediaEntity3 = createAndSaveMediaEntity(userAccountEntity, "chronicleId", false, false);
        var mediaEntity4 = createAndSaveMediaEntity(userAccountEntity, "chronicleId2", true, false);

        assertMediaIsCurrentStatus(mediaEntity1.getId(), false);
        assertMediaIsCurrentStatus(mediaEntity2.getId(), true);
        assertMediaIsCurrentStatus(mediaEntity3.getId(), false);
        assertMediaIsCurrentStatus(mediaEntity4.getId(), true);

        // When
        mockMvc.perform(patch(ENDPOINT.resolve(String.valueOf(mediaEntity1.getId())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createPayload(true)))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        assertMediaIsCurrentStatus(mediaEntity1.getId(), true);
        assertMediaIsCurrentStatus(mediaEntity2.getId(), false);
        assertMediaIsCurrentStatus(mediaEntity3.getId(), false);
        assertMediaIsCurrentStatus(mediaEntity4.getId(), true);//Should be true as different chronicleId

        List<AuditEntity> auditEntityList = dartsDatabase.getAuditRepository().findAll();

        assertThat(auditEntityList).hasSize(1);
        AuditEntity auditEntity = auditEntityList.get(0);
        assertThat(auditEntity.getAuditActivity().getId()).isEqualTo(AuditActivity.CURRENT_MEDIA_VERSION_UPDATED.getId());
        assertThat(auditEntity.getAdditionalData()).isEqualTo("med_id: 1 was made current replacing med_id: [2]");

    }

    @Test
    void shouldReturn422_whenIsCurrentIsSetToFalse() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        mockMvc.perform(patch(ENDPOINT.resolve("123456789"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createPayload(false)))
            .andExpect(status().isUnprocessableEntity());
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
    void shouldReturn409_whenMediaIsAlreadyCurrent() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        var mediaEntity = createAndSaveMediaEntity(userAccountEntity, false);

        // When
        mockMvc.perform(patch(ENDPOINT.resolve(String.valueOf(mediaEntity.getId())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createPayload(true)))
            .andExpect(status().isConflict())
            .andReturn();
    }


    @Test
    void shouldReturn404_whenMediaRecordDoesNotExist() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        // When
        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT.resolve("123456789"))
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(createPayload(true)))
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

    @Test
    void shouldReturn404_whenMediaRecordIsDeleted() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();

        var mediaEntity = createAndSaveMediaEntity(userAccountEntity, true);

        // When
        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT.resolve(String.valueOf(mediaEntity.getId())))
                                                  .contentType(MediaType.APPLICATION_JSON)
                                                  .content(createPayload(true)))
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
        var jsonString = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUTHORISATION_109",
                                      "title": "User is not authorised for this endpoint",
                                      "status": 403
                                    }
                                    """, jsonString, JSONCompareMode.STRICT);
    }

    private MediaEntity createAndSaveMediaEntity(UserAccountEntity userAccountEntity, boolean isDeleted) {
        return createAndSaveMediaEntity(userAccountEntity, "chronicle-value", true, isDeleted);
    }

    private MediaEntity createAndSaveMediaEntity(UserAccountEntity userAccountEntity, String chronicleId, boolean isCurrent, boolean isDeleted) {
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
        mediaEntity.setChronicleId(chronicleId);
        mediaEntity.setAntecedentId("antecedent-value");
        mediaEntity.setRetainUntilTs(OffsetDateTime.parse(RETAIN_UNTIL));
        mediaEntity.setCreatedBy(userAccountEntity);
        mediaEntity.setLastModifiedBy(userAccountEntity);
        mediaEntity.setIsCurrent(isCurrent);

        CourtCaseEntity courtCaseEntity = databaseStub.createCase(
            COURTHOUSE_NAME,
            CASE_NUMBER
        );
        databaseStub.createMediaLinkedCase(
            mediaEntity,
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
                hearing.addMedia(mediaEntity);
                mediaEntity.addHearing(hearing);
            }
            dartsDatabase.getHearingRepository().saveAndFlush(hearing);
        });
        return databaseStub.getMediaRepository().saveAndFlush(mediaEntity);
    }


    private String createPayload(Boolean isCurrent) {
        return "{\"is_current\": %s}".formatted(isCurrent);
    }

    private void assertMediaIsCurrentStatus(int mediaId, boolean isCurrent) {
        transactionalUtil.executeInTransaction(() -> {
            MediaEntity media = databaseStub.getMediaRepository().findById(mediaId).orElseThrow();
            assertThat(media.getIsCurrent()).isEqualTo(isCurrent);
            if (isCurrent) {
                assertThat(media.getHearings()).isNotEmpty();
            } else {
                assertThat(media.getHearings()).isEmpty();
            }
        });
    }
}
