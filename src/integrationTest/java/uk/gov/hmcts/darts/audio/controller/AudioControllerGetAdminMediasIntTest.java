package uk.gov.hmcts.darts.audio.controller;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SuppressWarnings("VariableDeclarationUsageDistance")
class AudioControllerGetAdminMediasIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/medias";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    SuperAdminUserStub superAdminUserStub;
    @Autowired
    UserAccountStub userAccountStub;
    @MockBean
    UserIdentity userIdentity;
    @Autowired
    MediaRequestStub mediaRequestStub;
    @Autowired
    TransformedMediaRepository transformedMediaRepository;

    @Test
    void getMediaIsSuccessful() throws Exception {

        // given
        var user = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var transformedMedia = createAndSaveTransformedMedia(
            user,
            "testOutputFileName",
            AudioRequestOutputFormat.MP3,
            1000);

        HearingEntity hearing = transformedMedia.getMediaRequest().getHearing();
        MediaEntity mediaEntity = dartsDatabase.createMediaEntity(hearing.getCourtroom().getCourthouse().getDisplayName(),
                                                                  hearing.getCourtroom().getName(),
                                                                  transformedMedia.getStartTime(),
                                                                  transformedMedia.getEndTime(),
                                                                  1);
        hearing.addMedia(mediaEntity);

        MediaEntity mediaEntityBefore = dartsDatabase.createMediaEntity(hearing.getCourtroom().getCourthouse().getDisplayName(),
                                                                        hearing.getCourtroom().getName(),
                                                                        transformedMedia.getStartTime().minusHours(1),
                                                                        transformedMedia.getStartTime().minusSeconds(1),
                                                                        1);
        hearing.addMedia(mediaEntityBefore);

        MediaEntity mediaEntityAfter = dartsDatabase.createMediaEntity(hearing.getCourtroom().getCourthouse().getDisplayName(),
                                                                       hearing.getCourtroom().getName(),
                                                                       transformedMedia.getEndTime().plusSeconds(1),
                                                                       transformedMedia.getEndTime().plusHours(1),
                                                                       1);
        hearing.addMedia(mediaEntityAfter);

        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", transformedMedia.getId().toString()))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [
               {
                 "id": 1,
                 "channel": 1,
                 "start_at": "2023-06-26T13:00:00Z",
                 "end_at": "2023-06-26T13:45:00Z",
                 "hearing": {
                   "id": 1,
                   "hearing_date": "2023-06-10"
                 },
                 "courthouse": {
                   "id": 4,
                   "display_name": "NEWCASTLE"
                 },
                 "courtroom": {
                   "id": 1,
                   "display_name": "Int Test Courtroom 2"
                 },
                 "case": {
                   "id": 1,
                   "case_number": "2"
                 }
               }
             ]
              """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void transformedMediaIsDoesNotExist() throws Exception {

        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", "-1"))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [
               
             ]
              """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void transformedMediaNotProvided() throws Exception {

        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", ""))
            .andExpect(status().isBadRequest())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        assertTrue(StringUtils.contains(actualJson, "Either transformed_media_id or transcription_document_id must be provided in the request, but not both."));
    }

    @Test
    void wrongPermissions() throws Exception {

        // given
        userAccountStub.givenUserIsAuthorisedJudge(userIdentity);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", "1"))
            .andExpect(status().isForbidden())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
              """;
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }


    private TransformedMediaEntity createAndSaveTransformedMedia(
        UserAccountEntity user,
        String fileName,
        AudioRequestOutputFormat format,
        Integer fileSize
    ) {
        var mediaRequestEntity = mediaRequestStub.createAndSaveMediaRequestEntity(user);

        var transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setMediaRequest(mediaRequestEntity);
        transformedMediaEntity.setOutputFilename(fileName);
        transformedMediaEntity.setOutputFormat(format);
        transformedMediaEntity.setOutputFilesize(fileSize);
        transformedMediaEntity.setStartTime(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        transformedMediaEntity.setEndTime(OffsetDateTime.parse("2023-06-26T13:45:00Z"));
        transformedMediaEntity.setCreatedBy(user);
        transformedMediaEntity.setLastModifiedBy(user);
        return transformedMediaRepository.save(transformedMediaEntity);
    }

}
