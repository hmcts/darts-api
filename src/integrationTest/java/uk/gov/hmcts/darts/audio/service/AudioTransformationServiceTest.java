package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioTransformationServiceTest extends IntegrationBase {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    @Autowired
    private AudioTransformationService audioTransformationService;

    @Autowired
    private AudioConfigurationProperties audioConfigurationProperties;

    @Autowired
    private AudioTransformationServiceGivenBuilder given;

    @MockitoBean
    FileOperationService mockFileOperationService;

    @Autowired
    DartsPersistence dartsPersistence;

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    void getMediaByHearingIdShouldReturnExpectedMediaEntitiesWhenHearingIdHasRelatedMedia() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity1());
        Integer hearingIdWithMedia = given.getHearingEntityWithMedia1().getId();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaByHearingId(hearingIdWithMedia);

        assertEquals(2, mediaEntities.size());

        List<Integer> mediaIds = mediaEntities.stream().map(MediaEntity::getId).collect(toList());
        assertTrue(mediaIds.contains(given.getMediaEntity1().getId()));
        assertTrue(mediaIds.contains(given.getMediaEntity2().getId()));

        assertFalse(mediaIds.contains(given.getMediaEntity3().getId()));
    }

    @Test
    void getMediaByHearingIdShouldNotReturnMediaEntitiesWhenMediaIsHidden() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity4());
        Integer hearingIdWithMedia = given.getHearingEntityWithMedia1().getId();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaByHearingId(hearingIdWithMedia);

        assertEquals(2, mediaEntities.size());

        List<Integer> mediaIds = mediaEntities.stream().map(MediaEntity::getId).collect(toList());
        assertTrue(mediaIds.contains(given.getMediaEntity1().getId()));
        assertTrue(mediaIds.contains(given.getMediaEntity2().getId()));
    }

    @Test
    void getMediaByHearingIdShouldReturnEmptyListWhenHearingIdHasNoRelatedMedia() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity1());
        Integer hearingIdWithNoRelatedMedia = given.getHearingEntityWithoutMedia().getId();

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaByHearingId(hearingIdWithNoRelatedMedia);

        assertEquals(0, mediaEntities.size());
    }

    @Test
    void getMediaByHearingIdShouldReturnEmptyListWhenHearingIdDoesNotExist() {
        given.setupTest();
        given.externalObjectDirForMedia(given.getMediaEntity1());

        List<MediaEntity> mediaEntities = audioTransformationService.getMediaByHearingId(123_456);

        assertEquals(0, mediaEntities.size());
    }

    @Test
    void shouldSaveAudioBlobDataUsingTempWorkSpace() throws IOException {
        String fileName = "caseAudioFile.pdf";
        String tempWorkspace = audioConfigurationProperties.getTempBlobWorkspace();
        Path filePath = Path.of(tempWorkspace).resolve(fileName);

        InputStream inputStream = new ByteArrayInputStream(TEST_BINARY_STRING.getBytes());

        when(mockFileOperationService.saveFileToTempWorkspace(
            inputStream,
            fileName
        )).thenReturn(filePath);

        Path actualFilePath = audioTransformationService.saveBlobDataToTempWorkspace(inputStream, fileName);

        assertEquals(filePath, actualFilePath);
        verify(mockFileOperationService).saveFileToTempWorkspace(
            inputStream,
            fileName
        );
        verifyNoMoreInteractions(mockFileOperationService);
    }

}