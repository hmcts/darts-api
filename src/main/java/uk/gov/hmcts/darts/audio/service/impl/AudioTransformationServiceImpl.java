package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGenerator;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequest.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.ExcessiveImports")
public class AudioTransformationServiceImpl implements AudioTransformationService {

    private final MediaRequestService mediaRequestService;
    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final TransientObjectDirectoryService transientObjectDirectoryService;

    private final FileOperationService fileOperationService;

    private final MediaRepository mediaRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final OutboundFileProcessor outboundFileProcessor;
    private final OutboundFileZipGenerator outboundFileZipGenerator;

    /**
     * For all audio related to a given AudioRequest, download, process and upload the processed file to outbound
     * storage.
     *
     * @param requestId The id of the AudioRequest to be processed.
     * @return The blob storage id representing the location/name of the file uploaded to the outbound data store.
     */
    @Override
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidRethrowingException"})
    public UUID processAudioRequest(Integer requestId) {

        log.debug("Starting processing for audio request id: {}", requestId);
        mediaRequestService.updateAudioRequestStatus(requestId, PROCESSING);

        try {
            var mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);

            Integer hearingId = mediaRequestEntity.getHearing().getId();
            List<MediaEntity> mediaEntitiesForRequest = getMediaMetadata(hearingId);

            if (mediaEntitiesForRequest.isEmpty()) {
                throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, "No media present to process");
            }

            Map<MediaEntity, Path> downloadedMedias = new HashMap<>();
            for (MediaEntity mediaEntity : mediaEntitiesForRequest) {
                UUID id = getMediaLocation(mediaEntity).orElseThrow(
                    () -> new RuntimeException(String.format("Could not locate UUID for media: %s", mediaEntity.getId()
                    )));

                log.debug("Downloading audio blob for {}", id);
                BinaryData binaryData = getAudioBlobData(id);
                log.debug("Download audio blob complete for {}", id);

                Path downloadPath = saveBlobDataToTempWorkspace(binaryData, id.toString());
                log.debug("Saved audio blob {} to {}", id, downloadPath);

                downloadedMedias.put(mediaEntity, downloadPath);
            }

            Path generatedFilePath;
            try {
                generatedFilePath = generateFileForRequestType(mediaRequestEntity, downloadedMedias);
            } catch (ExecutionException | InterruptedException e) {
                // For Sonar rule S2142
                throw e;
            }

            UUID blobId;
            try (InputStream inputStream = Files.newInputStream(generatedFilePath)) {
                blobId = saveProcessedData(mediaRequestEntity, BinaryData.fromStream(inputStream));
            }

            log.debug(
                "Completed processing for requestId {}. Zip successfully uploaded with blobId: {}",
                requestId,
                blobId
            );
            return blobId;

        } catch (Exception e) {
            log.error(
                "Exception occurred for request id {}. Exception message: {}",
                requestId,
                e.getMessage()
            );
            mediaRequestService.updateAudioRequestStatus(requestId, FAILED);

            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, e);
        }
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private Path generateFileForRequestType(MediaRequestEntity mediaRequestEntity,
                                            Map<MediaEntity, Path> downloadedMedias)
        throws ExecutionException, InterruptedException {

        var requestType = mediaRequestEntity.getRequestType();

        if (AudioRequestType.DOWNLOAD.equals(requestType)) {
            return handleDownload(downloadedMedias, mediaRequestEntity);
        } else if (AudioRequestType.PLAYBACK.equals(requestType)) {
            return handlePlayback(downloadedMedias, mediaRequestEntity);
        } else {
            throw new NotImplementedException(
                String.format("No handler exists for request type %s", requestType));
        }
    }

    @Override
    public BinaryData getAudioBlobData(UUID location) {
        return dataManagementService.getBlobData(dataManagementConfiguration.getUnstructuredContainerName(), location);
    }

    @Override
    public UUID saveAudioBlobData(BinaryData binaryData) {
        return dataManagementService.saveBlobData(
            dataManagementConfiguration.getOutboundContainerName(),
            binaryData
        );
    }

    @Override
    public TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest,
                                                                    UUID externalLocation) {
        return transientObjectDirectoryService.saveTransientDataLocation(mediaRequest, externalLocation);
    }

    @Override
    public List<MediaEntity> getMediaMetadata(Integer hearingId) {
        return mediaRepository.findAllByHearingId(hearingId);
    }

    @Override
    public Optional<UUID> getMediaLocation(MediaEntity media) {
        Optional<UUID> externalLocation = Optional.empty();

        ObjectDirectoryStatusEntity objectDirectoryStatus = objectDirectoryStatusRepository.getReferenceById(STORED.getId());
        ExternalLocationTypeEntity externalLocationType = externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId());
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityList = externalObjectDirectoryRepository.findByMediaStatusAndType(
            media, objectDirectoryStatus, externalLocationType
        );

        if (!externalObjectDirectoryEntityList.isEmpty()) {
            if (externalObjectDirectoryEntityList.size() != 1) {
                log.warn(
                    "Only one External Object Directory expected, but found {} for mediaId={}, statusEnum={}, externalLocationTypeId={}",
                    externalObjectDirectoryEntityList.size(),
                    media.getId(),
                    STORED,
                    externalLocationType.getId()
                );
            }
            externalLocation = Optional.ofNullable(externalObjectDirectoryEntityList.get(0).getExternalLocation());
        }

        return externalLocation;
    }

    @Override
    public Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException {

        return fileOperationService.saveFileToTempWorkspace(mediaFile, fileName);
    }

    @Override
    @Transactional
    public UUID saveProcessedData(MediaRequestEntity mediaRequest, BinaryData binaryData) {
        UUID blobId = saveAudioBlobData(binaryData);
        saveTransientDataLocation(mediaRequest, blobId);

        mediaRequestService.updateAudioRequestStatus(mediaRequest.getId(), COMPLETED);

        return blobId;
    }

    private Path handleDownload(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException {

        List<List<AudioFileInfo>> processedAudio;
        processedAudio = outboundFileProcessor.processAudioForDownload(
            downloadedMedias,
            mediaRequestEntity.getStartTime(),
            mediaRequestEntity.getEndTime()
        );

        return outboundFileZipGenerator.generateAndWriteZip(processedAudio);
    }

    private Path handlePlayback(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException {

        AudioFileInfo audioFileInfo = outboundFileProcessor.processAudioForPlayback(
            downloadedMedias,
            mediaRequestEntity.getStartTime(),
            mediaRequestEntity.getEndTime()
        );

        return Path.of(audioFileInfo.getFileName());
    }

}
