package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class AudioControllerGetAdminMediaVersionsByIdIntTest extends IntegrationBase {
    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DartsDatabaseStub databaseStub;

    private static final String ENDPOINT_URL = "/admin/medias/{id}/versions";
    private static final String COURTHOUSE_NAME = "TESTCOURTHOUSE";
    private static final String COURTROOM_NAME = "TESTCOURTROOM";
    private static final OffsetDateTime HEARING_START_AT = OffsetDateTime.parse("2024-01-01T12:10:10Z");
    private static final OffsetDateTime MEDIA_START_AT = HEARING_START_AT;
    private static final OffsetDateTime MEDIA_END_AT = MEDIA_START_AT.plusHours(1);
    private static final String RETAIN_UNTIL = "2200-02-01T00:00:00Z";


    @Test
    void shouldReturn200_whenChronicleIdHasBothCurrentAndNonCurrentVersions() throws Exception {
        final String chronicleId = "chronicleId";
        MediaEntity currentMediaEntity = createAndSaveMediaEntity(true, chronicleId, Duration.ofDays(3));
        MediaEntity versionedMediaEntity1 = createAndSaveMediaEntity(false, chronicleId, Duration.ofDays(2));
        MediaEntity versionedMediaEntity2 = createAndSaveMediaEntity(false, chronicleId, Duration.ofDays(1));
        //Craeted unrelated media to ensure not returned
        createAndSaveMediaEntity(true, "unrealted");

        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL, currentMediaEntity.getId()))
            .andExpect(status().isOk())
            .andReturn();

        String expectedResponse = getContentsFromFile(
            "tests/audio/AudioControllerGetAdminMediaVersionsByIdIntTest/expectedResponseTypical.json")
            .replace("<created_at_current>", currentMediaEntity.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<versioned_at_current_1>", versionedMediaEntity2.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<versioned_at_current_2>", versionedMediaEntity1.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        JSONAssert.assertEquals(expectedResponse, mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void shouldReturn200_whenMediaIdPassedIsNotCurrent_shouldReturnTheCorrectCorrentVersion() throws Exception {
        final String chronicleId = "chronicleId";
        MediaEntity currentMediaEntity = createAndSaveMediaEntity(true, chronicleId, Duration.ofDays(3));
        MediaEntity versionedMediaEntity1 = createAndSaveMediaEntity(false, chronicleId, Duration.ofDays(2));
        MediaEntity versionedMediaEntity2 = createAndSaveMediaEntity(false, chronicleId, Duration.ofDays(1));
        //Craeted unrelated media to ensure not returned
        createAndSaveMediaEntity(true, "unrealted");

        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL, versionedMediaEntity1.getId()))
            .andExpect(status().isOk())
            .andReturn();

        String expectedResponse = getContentsFromFile(
            "tests/audio/AudioControllerGetAdminMediaVersionsByIdIntTest/expectedResponseTypical.json")
            .replace("<created_at_current>", currentMediaEntity.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<versioned_at_current_1>", versionedMediaEntity2.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<versioned_at_current_2>", versionedMediaEntity1.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        JSONAssert.assertEquals(expectedResponse, mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void shouldReturn200_whenChronicleIdHasOnlyCurrentAndNoVersions() throws Exception {
        final String chronicleId = "chronicleId";
        MediaEntity currentMediaEntity = createAndSaveMediaEntity(true, chronicleId, Duration.ofDays(3));
        //Craeted unrelated media to ensure not returned
        createAndSaveMediaEntity(true, "unrealted");

        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL, currentMediaEntity.getId()))
            .andExpect(status().isOk())
            .andReturn();

        String expectedResponse = getContentsFromFile(
            "tests/audio/AudioControllerGetAdminMediaVersionsByIdIntTest/expectedResponseNoVersions.json")
            .replace("<created_at_current>", currentMediaEntity.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
        JSONAssert.assertEquals(expectedResponse, mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void shouldReturn404_WhenMediaRecordDoesNotExist() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL, "123456789"))
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
    void shouldReturn404_WhenMediaRecordIsDeleted() throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);


        var mediaEntity = createAndSaveMediaEntity(true, true, "chronicleId", Duration.ofMillis(0));
        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL, String.valueOf(mediaEntity.getId())))
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
    void shouldDenyAccess_whenNotAuthorised(SecurityRoleEnum role) throws Exception {
        // Given
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        // When
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL, "123456789"))
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

    private MediaEntity createAndSaveMediaEntity(boolean isCurrent, String chronicleId) {
        return createAndSaveMediaEntity(false, isCurrent, chronicleId, Duration.ofMillis(0));
    }

    private MediaEntity createAndSaveMediaEntity(boolean isCurrent, String chronicleId, Duration timeOffset) {
        return createAndSaveMediaEntity(false, isCurrent, chronicleId, timeOffset);
    }

    private MediaEntity createAndSaveMediaEntity(boolean isDeleted, boolean isCurrent, String chronicleId, Duration timeOffset) {
        MediaEntity mediaEntity = databaseStub.createMediaEntity(COURTHOUSE_NAME,
                                                                 COURTROOM_NAME,
                                                                 MEDIA_START_AT.plus(timeOffset),
                                                                 MEDIA_END_AT.plus(timeOffset),
                                                                 2);
        var userAccountEntity = databaseStub.getUserAccountRepository().findAll().stream()
            .findFirst()
            .orElseThrow();
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

        return databaseStub.getMediaRepository()
            .saveAndFlush(mediaEntity);
    }
}
