package uk.gov.hmcts.darts.audio.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseCase;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseCourthouse;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseCourtroom;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseHearing;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseItem;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.MediaStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;
import uk.gov.hmcts.darts.transcriptions.model.Problem;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    MediaStub mediaStub;
    @Autowired
    TransformedMediaRepository transformedMediaRepository;

    TransformedMediaEntity transformedMedia;


    private static final int DATE_BEFORE_INDEX = 0;
    private static final int DATE_NOW_INDEX = 1;
    private static final int DATE_AFTER_INDEX = 2;

    @Test
    void getMediaIsSuccessfulWithTransformedMediaId() throws Exception {

        // given
        transformedMedia = setupData();
        MediaEntity mediaEntity = setupMediaBeforeAndAfter(transformedMedia).get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", transformedMedia.getId().toString()))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = getExpectedJsonRoot(
                                              mediaEntity.getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtroom().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                                              transformedMedia.getMediaRequest().getHearing().getId(), mediaEntity.getStart(), mediaEntity.getEnd());

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsBadRequestFailureAllParameters() throws Exception {

        transformedMedia = setupData();
        setupMediaBeforeAndAfter(transformedMedia).get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", transformedMedia.getId().toString())
                                                  .queryParam("hearing_ids", transformedMedia.getMediaRequest()
                                                      .getHearing().getId() + "," + transformedMedia.getMediaRequest().getHearing().getId())
                                                  .queryParam("start_at", transformedMedia.getStartTime().toString())
                                                  .queryParam("end_at", transformedMedia.getEndTime().toString())
            )
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE.getType(), problemResponse.getType());
    }

    @Test
    void getMediaIsBadRequestFailureNoParameters() throws Exception {

        transformedMedia = setupData();
        setupMediaBeforeAndAfter(transformedMedia).get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Problem problemResponse = objectMapper.readValue(content, Problem.class);
        assertEquals(AudioApiError.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE.getType(), problemResponse.getType());
    }

    @Test
    void getMediaIsSuccessfulWithHearingId() throws Exception {

        // given
        transformedMedia = setupData();
        List<MediaEntity> mediaEntitiesLst = setupMediaBeforeAndAfter(transformedMedia);
        MediaEntity mediaEntityBefore = mediaEntitiesLst.get(DATE_BEFORE_INDEX);
        MediaEntity mediaEntityNow = mediaEntitiesLst.get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("hearing_ids",
                                                              mediaStub.getHearingId(mediaEntityBefore.getId())
                                                                  + "," + mediaStub.getHearingId(mediaEntityNow.getId()))
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        AdminMediaSearchResponseItem[] responseItems = objectMapper.readValue(mvcResult
                                                                                  .getResponse().getContentAsString(), AdminMediaSearchResponseItem[].class);

        String expectedJsonBefore = getExpectedJson(mediaEntityBefore.getId(),
                                              mediaEntityBefore.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                              mediaEntityBefore.getHearingList().get(0).getCourtroom().getId(),
                                              mediaEntityBefore.getHearingList().get(0).getCourtCase().getId(),
                                              mediaEntityBefore.getHearingList().get(0).getId(), mediaEntityBefore.getStart(), mediaEntityBefore.getEnd());

        String expectedJsonNow = getExpectedJson(mediaEntityNow.getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtroom().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtCase().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getId(), mediaEntityNow.getStart(), mediaEntityNow.getEnd());

        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[0]), expectedJsonBefore, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[1]), expectedJsonNow, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithMediaStartDate() throws Exception {

        // given
        transformedMedia = setupData();

        List<MediaEntity> mediaEntitiesLst = setupMediaBeforeAndAfter(transformedMedia);
        MediaEntity mediaEntityNow = mediaEntitiesLst.get(DATE_NOW_INDEX);
        MediaEntity mediaEntityAfter = mediaEntitiesLst.get(DATE_AFTER_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", mediaEntityNow.getStart().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String expectedJsonNow = getExpectedJson(mediaEntityNow.getId(),
                                                    mediaEntityNow.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                    mediaEntityNow.getHearingList().get(0).getCourtroom().getId(),
                                                    mediaEntityNow.getHearingList().get(0).getCourtCase().getId(),
                                                    mediaEntityNow.getHearingList().get(0).getId(), mediaEntityNow.getStart(), mediaEntityNow.getEnd());

        String expectedJsonAfter = getExpectedJson(mediaEntityAfter.getId(),
                                                 mediaEntityAfter.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                 mediaEntityAfter.getHearingList().get(0).getCourtroom().getId(),
                                                 mediaEntityAfter.getHearingList().get(0).getCourtCase().getId(),
                                                 mediaEntityAfter.getHearingList().get(0).getId(), mediaEntityAfter.getStart(), mediaEntityAfter.getEnd());
        AdminMediaSearchResponseItem[] responseItems = objectMapper.readValue(mvcResult.getResponse()
                                                                                  .getContentAsString(), AdminMediaSearchResponseItem[].class);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[0]), expectedJsonNow, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[1]), expectedJsonAfter,  JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithMediaEndDate() throws Exception {

        // given
        transformedMedia = setupData();
        List<MediaEntity> mediaEntitiesLst = setupMediaBeforeAndAfter(transformedMedia);
        MediaEntity mediaEntityBefore = mediaEntitiesLst.get(DATE_BEFORE_INDEX);
        MediaEntity mediaEntityNow = mediaEntitiesLst.get(DATE_NOW_INDEX);
        MediaEntity mediaEntityAfter = mediaEntitiesLst.get(DATE_AFTER_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("end_at", mediaEntityAfter.getEnd().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String expectedJsonBefore = getExpectedJson(mediaEntityBefore.getId(),
                                                    mediaEntityBefore.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                    mediaEntityBefore.getHearingList().get(0).getCourtroom().getId(),
                                                    mediaEntityBefore.getHearingList().get(0).getCourtCase().getId(),
                                                    mediaEntityBefore.getHearingList().get(0).getId(),
                                                    mediaEntityBefore.getStart(), mediaEntityBefore.getEnd());

        String expectedJsonNow = getExpectedJson(mediaEntityNow.getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtroom().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtCase().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getId(), mediaEntityNow.getStart(), mediaEntityNow.getEnd());

        String expectedJsonAfter = getExpectedJson(mediaEntityAfter.getId(),
                                                   mediaEntityAfter.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                   mediaEntityAfter.getHearingList().get(0).getCourtroom().getId(),
                                                   mediaEntityAfter.getHearingList().get(0).getCourtCase().getId(),
                                                   mediaEntityAfter.getHearingList().get(0).getId(), mediaEntityAfter.getStart(), mediaEntityAfter.getEnd());
        AdminMediaSearchResponseItem[] responseItems = objectMapper.readValue(mvcResult
                                                                                  .getResponse().getContentAsString(), AdminMediaSearchResponseItem[].class);

        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[1]), expectedJsonNow, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[2]), expectedJsonAfter,  JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(objectMapper.writeValueAsString(responseItems[0]), expectedJsonBefore, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithMediaStartDateAndEndDate() throws Exception {

        // given
        transformedMedia = setupData();
        MediaEntity mediaEntityNow = setupMediaBeforeAndAfter(transformedMedia).get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", mediaEntityNow.getStart().toString())
                                                  .queryParam("end_at", mediaEntityNow.getEnd().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJsonNow = getExpectedJsonRoot(mediaEntityNow.getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtroom().getCourthouse().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtroom().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getCourtCase().getId(),
                                                 mediaEntityNow.getHearingList().get(0).getId(), mediaEntityNow.getStart(), mediaEntityNow.getEnd());


        JSONAssert.assertEquals(expectedJsonNow, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getMediaIsSuccessfulWithMediaOutsideDateRange() throws Exception {

        // given
        transformedMedia = setupData();
        MediaEntity mediaEntityAfter = setupMediaBeforeAndAfter(transformedMedia).get(DATE_AFTER_INDEX);
        OffsetDateTime endDateNoResults = mediaEntityAfter.getEnd().plusDays(1);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", endDateNoResults.toString())
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
        mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("transformed_media_id", "1"))
            .andExpect(status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("AUTHORISATION_109")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(403)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("User is not authorised for this endpoint")));
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


    private List<MediaEntity> setupMediaBeforeAndAfter(TransformedMediaEntity transformedMedia) {
        List<MediaEntity> createdBeforeAndAfterLst = new ArrayList<>();

        HearingEntity hearing = transformedMedia.getMediaRequest().getHearing();

        MediaEntity mediaEntityBefore = dartsDatabase.createMediaEntity(hearing.getCourtroom().getCourthouse().getDisplayName(),
                                                                        hearing.getCourtroom().getName(),
                                                                        transformedMedia.getStartTime().minusHours(1),
                                                                        transformedMedia.getStartTime().minusSeconds(1),
                                                                        1);
        hearing.addMedia(mediaEntityBefore);
        createdBeforeAndAfterLst.add(mediaEntityBefore);

        MediaEntity mediaEntity = dartsDatabase.createMediaEntity(hearing.getCourtroom().getCourthouse().getDisplayName(),
                                                                  hearing.getCourtroom().getName(),
                                                                  transformedMedia.getStartTime(),
                                                                  transformedMedia.getEndTime(),
                                                                  1);
        hearing.addMedia(mediaEntity);
        createdBeforeAndAfterLst.add(mediaEntity);

        MediaEntity mediaEntityAfter = dartsDatabase.createMediaEntity(hearing.getCourtroom().getCourthouse().getDisplayName(),
                                                                       hearing.getCourtroom().getName(),
                                                                       transformedMedia.getEndTime().plusSeconds(1),
                                                                       transformedMedia.getEndTime().plusHours(1),
                                                                       1);
        hearing.addMedia(mediaEntityAfter);
        createdBeforeAndAfterLst.add(mediaEntityAfter);
        dartsDatabase.getHearingRepository().saveAndFlush(hearing);

        return createdBeforeAndAfterLst;
    }

    private String getExpectedJsonRoot(Integer id, Integer courthouseId, Integer courtroomId,
                                       Integer caseId, Integer hearingId, OffsetDateTime startDate, OffsetDateTime endDate)
        throws JsonProcessingException {
        String body = getExpectedJson(id, courthouseId, courtroomId, caseId, hearingId, startDate, endDate);
        return """
            [
               ${RESPONSE_BODY}
            ]
              """.replace("${RESPONSE_BODY}", body);
    }

    private String getExpectedJson(Integer id, Integer courthouseId, Integer courtroomId,
                                   Integer caseId, Integer hearingId,
                                   OffsetDateTime startDate, OffsetDateTime endDate) throws JsonProcessingException {
        AdminMediaSearchResponseItem responseItem = new AdminMediaSearchResponseItem();
        responseItem.setId(id);
        responseItem.setChannel(1);
        responseItem.setStartAt(startDate);
        responseItem.setEndAt(endDate);

        AdminMediaSearchResponseHearing hearing = new AdminMediaSearchResponseHearing();
        hearing.setId(hearingId);
        hearing.setHearingDate(LocalDate.of(2023, 6, 10));
        responseItem.setHearing(hearing);

        AdminMediaSearchResponseCourthouse courthouseResponse = new AdminMediaSearchResponseCourthouse();
        courthouseResponse.setId(courthouseId);
        courthouseResponse.setDisplayName("NEWCASTLE");
        responseItem.setCourthouse(courthouseResponse);

        AdminMediaSearchResponseCourtroom courtroomResponse = new AdminMediaSearchResponseCourtroom();
        courtroomResponse.setId(courtroomId);
        courtroomResponse.setDisplayName("Int Test Courtroom 2");
        responseItem.setCourtroom(courtroomResponse);

        AdminMediaSearchResponseCase caseResponse = new AdminMediaSearchResponseCase();
        caseResponse.setId(caseId);
        caseResponse.setCaseNumber("2");
        responseItem.setCase(caseResponse);


        return objectMapper.writeValueAsString(responseItem);
    }
}