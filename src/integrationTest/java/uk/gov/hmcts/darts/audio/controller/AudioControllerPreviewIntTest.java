package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.audio.model.AudioPreview;
import uk.gov.hmcts.darts.audio.service.AudioTransformationServiceGivenBuilder;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.service.RedisService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static java.util.Objects.nonNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus.READY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.test.common.AwaitabilityUtil.waitForMaxWithOneSecondPoll;

@AutoConfigureMockMvc
@TestPropertySource(properties = {"darts.audio.transformation.service.audio.file=tests/audio/WithViqHeader/viq0001min.mp2"})
class AudioControllerPreviewIntTest extends IntegrationBase {

    @Value("${darts.audio.preview.redis-folder}")
    private String folder;

    @Autowired
    private AudioTransformationServiceGivenBuilder given;

    @MockBean
    private Authorisation authorisation;

    private MediaEntity mediaEntity;

    @Autowired
    private RedisService<AudioPreview> binaryDataRedisService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setupData() {
        given.setupTest();
        mediaEntity = given.getMediaEntity1();
        given.externalObjectDirForMedia(mediaEntity);
        doNothing().when(authorisation).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );

    }

    @AfterEach
    void cleanup() {
        if (nonNull(mediaEntity)) {
            binaryDataRedisService.deleteFromRedis(folder, mediaEntity.getId().toString());
        }
    }

    @Test
    void previewWithRangeFromStartShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId()))).header("Range", "bytes=0-1023");

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful());
        waitUntilPreviewEncodedAndCached();
        mockMvc.perform(requestBuilder).andExpect(status().isPartialContent());

        verify(authorisation, times(2)).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
    }

    @Test
    void previewWithRangeFromStartWithNoEndShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId()))).header("Range", "bytes=0-");

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful());
        waitUntilPreviewEncodedAndCached();
        mockMvc.perform(requestBuilder).andExpect(status().isPartialContent());

        verify(authorisation, times(2)).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
    }

    @Test
    void previewWithRangeShouldReturnSuccess() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(URI.create(
            String.format("/audio/preview/%d", mediaEntity.getId()))).header("Range", "bytes=1024-2047");

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful());
        waitUntilPreviewEncodedAndCached();
        mockMvc.perform(requestBuilder).andExpect(status().isPartialContent());

        verify(authorisation, times(2)).authoriseByMediaId(
            mediaEntity.getId(),
            Set.of(JUDICIARY, REQUESTER, APPROVER, TRANSCRIBER, TRANSLATION_QA)
        );
    }

    private void waitUntilPreviewEncodedAndCached() {
        waitForMaxWithOneSecondPoll(() -> {
            var cachedAudioPreview = binaryDataRedisService.readFromRedis(folder, mediaEntity.getId().toString());
            return cachedAudioPreview.getStatus().equals(READY);
        }, Duration.ofSeconds(20));
    }
}
