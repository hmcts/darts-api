package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.MediaRequestStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SuppressWarnings("VariableDeclarationUsageDistance")
class AudioControllerGetTransformedMediaIntTest extends IntegrationBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    SuperAdminUserStub superAdminUserStub;
    @MockitoBean
    UserIdentity userIdentity;
    @Autowired
    MediaRequestStub mediaRequestStub;
    @Autowired
    TransformedMediaRepository transformedMediaRepository;

    @Test
    void getTransformedMediaIsSuccessful() throws Exception {

        // given
        var user = superAdminUserStub.givenUserIsAuthorised(userIdentity);

        var transformedMedia = createAndSaveTransformedMedia(
            user,
            "testOutputFileName",
            AudioRequestOutputFormat.MP3,
            1000);

        // when
        MvcResult mvcResult = mockMvc.perform(get("/admin/transformed-medias/" + transformedMedia.getId()))
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
          {
            "id": %s,
            "file_name": "testOutputFileName",
            "file_format": "mp3",
            "file_size_bytes": 1000,
            "case_id": %s,
            "media_request_id": %s,
          }
            """
            .formatted(transformedMedia.getId(),
                       transformedMedia.getMediaRequest().getHearing().getCourtCase().getId(),
                       transformedMedia.getMediaRequest().getId());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTransformedMediaReturns404WhenTransformedMediaDoesNotExist() throws Exception {

        // given
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // when/then
        mockMvc.perform(
            get("/admin/transformed-medias/44"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getTransformedMediaReturns403WhenUserIsNotAuthorised() throws Exception {

        // given
        UserAccountEntity nonAuthorisedUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(nonAuthorisedUser);

        // when/then
        mockMvc.perform(
                get("/admin/transformed-medias/44"))
            .andExpect(status().isForbidden());
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
        transformedMediaEntity.setStartTime(OffsetDateTime.now());
        transformedMediaEntity.setEndTime(OffsetDateTime.now().plusSeconds(2));
        transformedMediaEntity.setCreatedBy(user);
        transformedMediaEntity.setLastModifiedBy(user);
        return transformedMediaRepository.save(transformedMediaEntity);
    }

}
