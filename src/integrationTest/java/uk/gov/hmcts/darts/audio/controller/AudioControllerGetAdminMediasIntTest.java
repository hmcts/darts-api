package uk.gov.hmcts.darts.audio.controller;


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

    TransformedMediaEntity transformedMedia;

    MediaEntity mediaEntity;

    @Test
    void getMediaIsSuccessfulWithTransformedMediaId() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", transformedMedia.getId().toString()))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(
                                              mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulNoParam() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulAllParameters() throws Exception {

        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", transformedMedia.getId().toString())
                                                  .queryParam("hearing_ids", transformedMedia.getMediaRequest()
                                                      .getHearing().getId() + "," + transformedMedia.getMediaRequest().getHearing().getId())
                                                  .queryParam("start_at", transformedMedia.getStartTime().toString())
                                                  .queryParam("end_at", transformedMedia.getEndTime().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse()
                                                  .getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithHearingId() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("hearing_ids",
                                                              transformedMedia.getMediaRequest()
                                                                  .getHearing().getId() + "," + transformedMedia.getMediaRequest()
                                                                  .getHearing().getId())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithTransformedMediaIdAndHearing() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", transformedMedia.getId().toString())
                                                  .queryParam("hearing_ids", transformedMedia.getMediaRequest()
                                                      .getHearing().getId() + "," + transformedMedia.getMediaRequest().getHearing().getId())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithTransformedMediaStartDate() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", transformedMedia.getStartTime().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithTransformedMediaEndDate() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("end_at", transformedMedia.getEndTime().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithTransformedMediaStartDateAndEndDate() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", transformedMedia.getStartTime().toString())
                                                  .queryParam("end_at", transformedMedia.getEndTime().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJson(mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithTransformedMediaOutsideRange() throws Exception {

        // given
        transformedMedia = setupData();
        mediaEntity = setupMedia(transformedMedia);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("end_at", transformedMedia.getStartTime().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals("[]", actualJson, JSONCompareMode.NON_EXTENSIBLE);
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

    private TransformedMediaEntity setupData() {
        var user = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        return createAndSaveTransformedMedia(
            user,
            "testOutputFileName",
            AudioRequestOutputFormat.MP3,
            1000);
    }

    private MediaEntity setupMedia(TransformedMediaEntity transformedMedia) {
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

        return mediaEntity;
    }

    private String getExpectedJson(Integer id, Integer courthouseId, Integer courtroomId, Integer caseId, Integer hearingId) {
        return """
            [
               {
                 "id": ${ID},
                 "channel": 1,
                 "start_at": "2023-06-26T13:00:00Z",
                 "end_at": "2023-06-26T13:45:00Z",
                 "hearing": {
                   "id": ${HEARING_ID},
                   "hearing_date": "2023-06-10"
                 },
                 "courthouse": {
                   "id": ${COURTHOUSE_ID},
                   "display_name": "NEWCASTLE"
                 },
                 "courtroom": {
                   "id": ${COURTROOM_ID},
                   "display_name": "Int Test Courtroom 2"
                 },
                 "case": {
                   "id": ${CASE_ID},
                   "case_number": "2"
                 }
               }
             ]
              """
            .replace("${ID}", id.toString())
            .replace("${COURTHOUSE_ID}", courthouseId.toString())
            .replace("${COURTROOM_ID}", courtroomId.toString())
            .replace("${CASE_ID}", caseId.toString())
            .replace("${HEARING_ID}", hearingId.toString());
    }

}