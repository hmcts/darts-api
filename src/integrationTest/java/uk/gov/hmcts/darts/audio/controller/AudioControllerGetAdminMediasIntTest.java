package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseCase;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseCourthouse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseCourtroom;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseHearing;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
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
import static org.junit.jupiter.api.Assertions.fail;
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
    @MockitoBean
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
        MediaEntity preMediaEntityBefore = mediaEntitiesLst.get(DATE_BEFORE_INDEX);
        MediaEntity preMediaEntityNow = mediaEntitiesLst.get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("hearing_ids",
                                                              mediaStub.getHearingId(preMediaEntityBefore.getId())
                                                                  + "," + mediaStub.getHearingId(preMediaEntityNow.getId()))
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        dartsDatabase.getTransactionalUtil().executeInTransaction(() -> {
            try {
                MediaEntity mediaEntityBefore = dartsDatabase.getMediaRepository().findById(preMediaEntityBefore.getId()).get();
                MediaEntity mediaEntityNow = dartsDatabase.getMediaRepository().findById(preMediaEntityNow.getId()).get();

                GetAdminMediaResponseItem[] responseItems = objectMapper.readValue(mvcResult
                                                                                       .getResponse().getContentAsString(), GetAdminMediaResponseItem[].class);

                String expectedJsonBefore = getExpectedJson(mediaEntityBefore.getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getCourtroom().getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getCourtCase().getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getId(), mediaEntityBefore.getStart(),
                                                            mediaEntityBefore.getEnd());

                String expectedJsonNow = getExpectedJson(mediaEntityNow.getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtroom().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtCase().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getId(), mediaEntityNow.getStart(),
                                                         mediaEntityNow.getEnd());

                JSONAssert.assertEquals(expectedJsonBefore, objectMapper.writeValueAsString(responseItems[0]), JSONCompareMode.NON_EXTENSIBLE);
                JSONAssert.assertEquals(expectedJsonNow, objectMapper.writeValueAsString(responseItems[1]), JSONCompareMode.NON_EXTENSIBLE);
            } catch (Exception e) {
                fail("Exception occurred in test: " + e.getMessage());
            }
        });
    }

    @Test
    void getMediaIsSuccessfulWithMediaStartDate() throws Exception {

        // given
        transformedMedia = setupData();

        List<MediaEntity> mediaEntitiesLst = setupMediaBeforeAndAfter(transformedMedia);
        MediaEntity mediaEntityNowBefore = mediaEntitiesLst.get(DATE_NOW_INDEX);
        MediaEntity mediaEntityAfterBefore = mediaEntitiesLst.get(DATE_AFTER_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", mediaEntityNowBefore.getStart().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        dartsDatabase.getTransactionalUtil().executeInTransaction(() -> {
            try {
                MediaEntity mediaEntityNow = dartsDatabase.getMediaRepository().findById(mediaEntityNowBefore.getId()).get();
                MediaEntity mediaEntityAfter = dartsDatabase.getMediaRepository().findById(mediaEntityAfterBefore.getId()).get();

                String expectedJsonNow = getExpectedJson(mediaEntityNow.getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtroom().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtCase().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getId(), mediaEntityNow.getStart(),
                                                         mediaEntityNow.getEnd());

                String expectedJsonAfter = getExpectedJson(mediaEntityAfter.getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getCourtroom().getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getCourtCase().getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getId(), mediaEntityAfter.getStart(),
                                                           mediaEntityAfter.getEnd());
                GetAdminMediaResponseItem[] responseItems = objectMapper.readValue(mvcResult.getResponse()
                                                                                       .getContentAsString(), GetAdminMediaResponseItem[].class);

                JSONAssert.assertEquals(expectedJsonNow, objectMapper.writeValueAsString(responseItems[0]), JSONCompareMode.NON_EXTENSIBLE);
                JSONAssert.assertEquals(expectedJsonAfter, objectMapper.writeValueAsString(responseItems[1]), JSONCompareMode.NON_EXTENSIBLE);
            } catch (Exception e) {
                fail("Exception occurred in test: " + e.getMessage());
            }
        });
    }

    @Test
    void getMediaIsSuccessfulWithMediaEndDate() throws Exception {

        // given
        transformedMedia = setupData();
        List<MediaEntity> mediaEntitiesLst = setupMediaBeforeAndAfter(transformedMedia);
        MediaEntity preMediaEntityBefore = mediaEntitiesLst.get(DATE_BEFORE_INDEX);
        MediaEntity preMediaEntityNow = mediaEntitiesLst.get(DATE_NOW_INDEX);
        MediaEntity preMediaEntityAfter = mediaEntitiesLst.get(DATE_AFTER_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("end_at", preMediaEntityAfter.getEnd().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        dartsDatabase.getTransactionalUtil().executeInTransaction(() -> {
            try {
                MediaEntity mediaEntityBefore = dartsDatabase.getMediaRepository().findById(preMediaEntityBefore.getId()).get();
                MediaEntity mediaEntityNow = dartsDatabase.getMediaRepository().findById(preMediaEntityNow.getId()).get();
                MediaEntity mediaEntityAfter = dartsDatabase.getMediaRepository().findById(preMediaEntityAfter.getId()).get();

                String expectedJsonBefore = getExpectedJson(mediaEntityBefore.getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getCourtroom().getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getCourtCase().getId(),
                                                            mediaEntityBefore.getHearingList().getFirst().getId(),
                                                            mediaEntityBefore.getStart(), mediaEntityBefore.getEnd());

                String expectedJsonNow = getExpectedJson(mediaEntityNow.getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtroom().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getCourtCase().getId(),
                                                         mediaEntityNow.getHearingList().getFirst().getId(), mediaEntityNow.getStart(),
                                                         mediaEntityNow.getEnd());

                String expectedJsonAfter = getExpectedJson(mediaEntityAfter.getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getCourtroom().getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getCourtCase().getId(),
                                                           mediaEntityAfter.getHearingList().getFirst().getId(), mediaEntityAfter.getStart(),
                                                           mediaEntityAfter.getEnd());
                GetAdminMediaResponseItem[] responseItems = objectMapper.readValue(mvcResult
                                                                                       .getResponse().getContentAsString(), GetAdminMediaResponseItem[].class);

                JSONAssert.assertEquals(expectedJsonNow, objectMapper.writeValueAsString(responseItems[1]), JSONCompareMode.NON_EXTENSIBLE);
                JSONAssert.assertEquals(expectedJsonAfter, objectMapper.writeValueAsString(responseItems[2]), JSONCompareMode.NON_EXTENSIBLE);
                JSONAssert.assertEquals(expectedJsonBefore, objectMapper.writeValueAsString(responseItems[0]), JSONCompareMode.NON_EXTENSIBLE);
            } catch (Exception e) {
                fail("Exception occurred in test: " + e.getMessage());
            }
        });
    }

    @Test
    void getMediaIsSuccessfulWithMediaStartDateAndEndDate() throws Exception {

        // given
        transformedMedia = setupData();
        MediaEntity preMediaEntityNow = setupMediaBeforeAndAfter(transformedMedia).get(DATE_NOW_INDEX);

        // when
        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL)
                                                  .queryParam("start_at", preMediaEntityNow.getStart().toString())
                                                  .queryParam("end_at", preMediaEntityNow.getEnd().toString())
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        dartsDatabase.getTransactionalUtil().executeInTransaction(() -> {
            try {
                MediaEntity mediaEntityNow = dartsDatabase.getMediaRepository().findById(preMediaEntityNow.getId()).get();

                String actualJson = mvcResult.getResponse().getContentAsString();
                String expectedJsonNow = getExpectedJsonRoot(mediaEntityNow.getId(),
                                                             mediaEntityNow.getHearingList().getFirst().getCourtroom().getCourthouse().getId(),
                                                             mediaEntityNow.getHearingList().getFirst().getCourtroom().getId(),
                                                             mediaEntityNow.getHearingList().getFirst().getCourtCase().getId(),
                                                             mediaEntityNow.getHearingList().getFirst().getId(), mediaEntityNow.getStart(),
                                                             mediaEntityNow.getEnd());


                JSONAssert.assertEquals(expectedJsonNow, actualJson, JSONCompareMode.NON_EXTENSIBLE);
            } catch (Exception e) {
                fail("Exception occurred in test: " + e.getMessage());
            }
        });
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
        dartsDatabase.save(hearing);
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
        GetAdminMediaResponseItem responseItem = new GetAdminMediaResponseItem();
        responseItem.setId(id);
        responseItem.setChannel(1);
        responseItem.setStartAt(startDate);
        responseItem.setEndAt(endDate);
        responseItem.setIsHidden(false);
        responseItem.setIsCurrent(true);

        GetAdminMediaResponseHearing hearing = new GetAdminMediaResponseHearing();
        hearing.setId(hearingId);
        hearing.setHearingDate(LocalDate.of(2023, 6, 10));
        responseItem.setHearing(hearing);

        GetAdminMediaResponseCourthouse courthouseResponse = new GetAdminMediaResponseCourthouse();
        courthouseResponse.setId(courthouseId);
        courthouseResponse.setDisplayName("NEWCASTLE");
        responseItem.setCourthouse(courthouseResponse);

        GetAdminMediaResponseCourtroom courtroomResponse = new GetAdminMediaResponseCourtroom();
        courtroomResponse.setId(courtroomId);
        courtroomResponse.setDisplayName("INT TEST COURTROOM 2");
        responseItem.setCourtroom(courtroomResponse);

        GetAdminMediaResponseCase caseResponse = new GetAdminMediaResponseCase();
        caseResponse.setId(caseId);
        caseResponse.setCaseNumber("2");
        responseItem.setCase(caseResponse);


        return objectMapper.writeValueAsString(responseItem);
    }
}