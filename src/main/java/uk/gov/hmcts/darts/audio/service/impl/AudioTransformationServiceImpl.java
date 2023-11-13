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
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
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
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"}) // DMP-715 to resolve
public class AudioTransformationServiceImpl implements AudioTransformationService {

    private final MediaRequestService mediaRequestService;
    private final OutboundFileProcessor outboundFileProcessor;
    private final OutboundFileZipGenerator outboundFileZipGenerator;

    private final TransientObjectDirectoryService transientObjectDirectoryService;
    private final FileOperationService fileOperationService;

    private final MediaRepository mediaRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final DataManagementApi dataManagementApi;
    private final NotificationApi notificationApi;

    @Override
    public BinaryData getUnstructuredAudioBlob(UUID location) {
        return dataManagementApi.getBlobDataFromUnstructuredContainer(location);
    }

    @Override
    public BinaryData getOutboundAudioBlob(UUID location) {
        return dataManagementApi.getBlobDataFromOutboundContainer(location);
    }

    @Override
    public UUID saveAudioBlobData(BinaryData binaryData) {
        return dataManagementApi.saveBlobDataToOutboundContainer(binaryData);
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

        return blobId;
    }

    @Override
    public void handleKedaInvocationForMediaRequests() {
        mediaRequestService.getOldestMediaRequestByStatus(AudioRequestStatus.OPEN)
            .ifPresent(openMediaRequests -> processAudioRequest(openMediaRequests.getId()));
    }

    /**
     * For all audio related to a given AudioRequest, download, process and upload the processed file to outbound
     * storage.
     *
     * @param requestId The id of the AudioRequest to be processed.
     * @return The blob storage id representing the location/name of the file uploaded to the outbound data store.
     */
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidRethrowingException"})
    private UUID processAudioRequest(Integer requestId) {

        log.info("Starting processing for audio request id: {}", requestId);
        mediaRequestService.updateAudioRequestStatus(requestId, PROCESSING);

        MediaRequestEntity mediaRequestEntity = null;
        HearingEntity hearingEntity = null;
        UUID blobId;

        try {
            mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);
            hearingEntity = mediaRequestEntity.getHearing();

            AudioRequestOutputFormat audioRequestOutputFormat = AudioRequestOutputFormat.MP3;
            if (mediaRequestEntity.getRequestType().equals(DOWNLOAD)) {
                audioRequestOutputFormat = AudioRequestOutputFormat.ZIP;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MMM_uuuu");
            final String fileName = hearingEntity.getCourtCase().getCaseNumber() + "_" + hearingEntity.getHearingDate().format(formatter);

            List<MediaEntity> mediaEntitiesForRequest = getMediaMetadata(hearingEntity.getId());

            if (mediaEntitiesForRequest.isEmpty()) {
                throw new DartsApiException(
                    AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST,
                    "No media present to process"
                );
            }

            Map<MediaEntity, Path> downloadedMedias = downloadAndSaveMediaToWorkspace(mediaEntitiesForRequest);

            Path generatedFilePath;
            try {
                generatedFilePath = generateFileForRequestType(mediaRequestEntity, downloadedMedias);
            } catch (ExecutionException | InterruptedException e) {
                // For Sonar rule S2142
                throw e;
            }

            try (InputStream inputStream = Files.newInputStream(generatedFilePath)) {
                blobId = saveProcessedData(mediaRequestEntity, BinaryData.fromStream(inputStream));
            }

            mediaRequestService.updateAudioRequestCompleted(requestId, fileName, audioRequestOutputFormat);

        } catch (Exception e) {
            log.error(
                "Exception occurred for request id {}. Exception message: {}",
                requestId,
                e.getMessage()
            );
            mediaRequestService.updateAudioRequestStatus(requestId, FAILED);

            if (mediaRequestEntity != null && hearingEntity != null) {
                notifyUser(mediaRequestEntity, hearingEntity.getCourtCase(),
                           NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString());
            }

            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, e);
        }

        log.debug(
            "Completed processing for requestId {}. Zip successfully uploaded with blobId: {}",
            requestId,
            blobId
        );

        notifyUser(mediaRequestEntity, hearingEntity.getCourtCase(), NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString());

        return blobId;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Map<MediaEntity, Path> downloadAndSaveMediaToWorkspace(List<MediaEntity> mediaEntitiesForRequest)
        throws IOException {
        Map<MediaEntity, Path> downloadedMedias = new HashMap<>();
        for (MediaEntity mediaEntity : mediaEntitiesForRequest) {
            Path downloadPath = saveMediaToWorkspace(mediaEntity);

            downloadedMedias.put(mediaEntity, downloadPath);
        }
        return downloadedMedias;
    }

    @Override
    public Path saveMediaToWorkspace(MediaEntity mediaEntity) throws IOException {
        UUID id = getMediaLocation(mediaEntity).orElseThrow(
            () -> new RuntimeException(String.format("Could not locate UUID for media: %s", mediaEntity.getId()
            )));

        log.debug("Downloading audio blob for {}", id);
        BinaryData binaryData = getUnstructuredAudioBlob(id);
        log.debug("Download audio blob complete for {}", id);

        Path downloadPath = saveBlobDataToTempWorkspace(binaryData, id.toString());
        log.debug("Saved audio blob {} to {}", id, downloadPath);
        return downloadPath;
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private Path generateFileForRequestType(MediaRequestEntity mediaRequestEntity,
                                            Map<MediaEntity, Path> downloadedMedias)
        throws ExecutionException, InterruptedException, IOException {

        var requestType = mediaRequestEntity.getRequestType();

        if (DOWNLOAD.equals(requestType)) {
            return handleDownload(downloadedMedias, mediaRequestEntity);
        } else if (AudioRequestType.PLAYBACK.equals(requestType)) {
            return handlePlayback(downloadedMedias, mediaRequestEntity);
        } else {
            throw new NotImplementedException(
                String.format("No handler exists for request type %s", requestType));
        }
    }

    private Path handleDownload(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException, IOException {

        List<List<AudioFileInfo>> processedAudio = outboundFileProcessor.processAudioForDownload(
            downloadedMedias,
            mediaRequestEntity.getStartTime(),
            mediaRequestEntity.getEndTime()
        );

        return outboundFileZipGenerator.generateAndWriteZip(processedAudio, mediaRequestEntity);
    }

    public Path handlePlayback(Map<MediaEntity, Path> downloadedMedias, OffsetDateTime startTime,
                               OffsetDateTime endTime)
        throws ExecutionException, InterruptedException, IOException {

        AudioFileInfo audioFileInfo = outboundFileProcessor.processAudioForPlayback(
            downloadedMedias,
            startTime,
            endTime
        );

        return Path.of(audioFileInfo.getFileName());
    }

    private Path handlePlayback(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException, IOException {
        return handlePlayback(downloadedMedias, mediaRequestEntity.getStartTime(), mediaRequestEntity.getEndTime());
    }

    private void notifyUser(MediaRequestEntity mediaRequestEntity,
                            CourtCaseEntity courtCase,
                            String notificationTemplateName) {
        log.debug("Scheduling notification for template name: {}...", notificationTemplateName);

        var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
            .eventId(notificationTemplateName)
            .caseId(courtCase.getId())
            .emailAddresses(mediaRequestEntity.getRequestor().getEmailAddress())
            .build();

        notificationApi.scheduleNotification(saveNotificationToDbRequest);

        log.debug("Notification scheduled successfully");
    }

}
