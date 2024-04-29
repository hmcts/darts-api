package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGenerator;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.helper.TransformedMediaHelper;
import uk.gov.hmcts.darts.audio.helper.UnstructuredDataHelper;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.notification.api.NotificationApi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioTransformationServiceImpl implements AudioTransformationService {
    private final MediaRequestService mediaRequestService;
    private final OutboundFileProcessor outboundFileProcessor;
    private final OutboundFileZipGenerator outboundFileZipGenerator;

    private final FileOperationService fileOperationService;

    private final MediaRepository mediaRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ExternalObjectDirectoryService eodService;

    private final DataManagementApi dataManagementApi;

    private final TransformedMediaHelper transformedMediaHelper;
    private final LogApi logApi;
    private final DataManagementFacade dataManagementFacade;
    private final UnstructuredDataHelper unstructuredDataHelper;

    private static final Comparator<MediaEntity> MEDIA_START_TIME_CHANNEL_COMPARATOR = (media1, media2) -> {
        if (media1.getStart().equals(media2.getStart())) {
            return media1.getChannel().compareTo(media2.getChannel());
        } else {
            return media1.getStart().compareTo(media2.getStart());
        }
    };

    @Override
    public BinaryData getUnstructuredAudioBlob(UUID location) {
        return dataManagementApi.getBlobDataFromUnstructuredContainer(location);
    }

    @Override
    public List<MediaEntity> getMediaMetadata(Integer hearingId) {
        return mediaRepository.findAllByHearingId(hearingId);
    }

    @Override
    public Optional<UUID> getMediaLocation(MediaEntity media, Integer containerLocationId) {
        Optional<UUID> externalLocation = Optional.empty();

        ObjectRecordStatusEntity objectRecordStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        ExternalLocationTypeEntity externalLocationType = externalLocationTypeRepository.getReferenceById(containerLocationId);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityList = externalObjectDirectoryRepository.findByMediaStatusAndType(
            media, objectRecordStatus, externalLocationType
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
    public Path saveBlobDataToTempWorkspace(InputStream mediaFile, String fileName) throws IOException {

        return fileOperationService.saveFileToTempWorkspace(mediaFile, fileName);
    }

    @Override
    public void handleKedaInvocationForMediaRequests() {
        var openRequests = mediaRequestService.getOldestMediaRequestByStatus(OPEN);

        if (openRequests.isEmpty()) {
            log.info("No open requests found for ATS to process.");
        } else {
            openRequests.ifPresent(openMediaRequests -> processAudioRequest(openMediaRequests.getId()));
        }

        unstructuredDataHelper.waitForAllJobsToFinish();
    }

    /**
     * For all audio related to a given AudioRequest, download, transform and upload the processed file to outbound
     * storage.
     *
     * @param requestId The id of the AudioRequest to be processed.
     */
    @SuppressWarnings("PMD.AvoidRethrowingException")
    private void processAudioRequest(Integer requestId) {

        log.info("Starting processing for audio request id: {}", requestId);
        mediaRequestService.updateAudioRequestStatus(requestId, PROCESSING);

        MediaRequestEntity mediaRequestEntity = null;
        HearingEntity hearingEntity = null;
        UUID blobId;

        try {
            mediaRequestEntity = mediaRequestService.getMediaRequestById(requestId);
            hearingEntity = mediaRequestEntity.getHearing();

            logApi.atsProcessingUpdate(mediaRequestEntity);

            AudioRequestOutputFormat audioRequestOutputFormat = AudioRequestOutputFormat.MP3;
            if (mediaRequestEntity.getRequestType().equals(DOWNLOAD)) {
                audioRequestOutputFormat = AudioRequestOutputFormat.ZIP;
            }

            List<MediaEntity> mediaEntitiesForHearing = getMediaMetadata(hearingEntity.getId());

            if (mediaEntitiesForHearing.isEmpty()) {
                throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, "No media present to process");
            }

            List<MediaEntity> filteredMediaEntities = filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(
                mediaEntitiesForHearing,
                mediaRequestEntity
            );

            if (filteredMediaEntities.isEmpty()) {
                throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, "No filtered media present to process");
            }

            boolean hasAllMediaBeenCopiedFromInboundStorage = eodService.hasAllMediaBeenCopiedFromInboundStorage(filteredMediaEntities);

            if (!hasAllMediaBeenCopiedFromInboundStorage) {
                mediaRequestService.updateAudioRequestStatus(requestId, OPEN);
                return;
            }

            Map<MediaEntity, Path> downloadedMedias = downloadAndSaveMediaToWorkspace(filteredMediaEntities);

            List<AudioFileInfo> generatedAudioFiles;
            try {
                generatedAudioFiles = generateFilesForRequestType(mediaRequestEntity, downloadedMedias);
            } catch (ExecutionException | InterruptedException e) {
                // For Sonar rule S2142
                throw e;
            }

            int index = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MMM_uuuu");
            final String fileNamePrefix = String.format(
                "%s_%s_",
                hearingEntity.getCourtCase().getCaseNumber(),
                hearingEntity.getHearingDate().format(formatter)
            );
            for (AudioFileInfo generatedAudioFile : generatedAudioFiles) {
                final String fileName = String.format(
                    "%s%d.%s",
                    fileNamePrefix,
                    index++,
                    audioRequestOutputFormat.getExtension()
                );

                try (InputStream inputStream = Files.newInputStream(generatedAudioFile.getPath())) {
                    blobId = transformedMediaHelper.saveToStorage(mediaRequestEntity, BinaryData.fromStream(inputStream), fileName, generatedAudioFile);
                } catch (NoSuchFileException nsfe) {
                    log.error("No file found when trying to save to storage. {}", generatedAudioFile.getPath());
                    throw nsfe;
                }

                mediaRequestService.updateAudioRequestCompleted(mediaRequestEntity, fileName, audioRequestOutputFormat);
                log.debug("Completed upload of file to storage for mediaRequestId {}. File ''{}'' successfully uploaded with blobId: {}",
                          requestId, fileName, blobId);
            }

            logApi.atsProcessingUpdate(mediaRequestEntity);

            transformedMediaHelper.notifyUser(
                mediaRequestEntity,
                hearingEntity.getCourtCase(),
                NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString()
            );

        } catch (Exception e) {
            log.error("Exception occurred for request id {}.", requestId, e);
            var updatedMediaRequest = mediaRequestService.updateAudioRequestStatus(requestId, FAILED);

            if (mediaRequestEntity != null && hearingEntity != null) {
                transformedMediaHelper.notifyUser(updatedMediaRequest, hearingEntity.getCourtCase(),
                           NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString()
                );
            }

            logApi.atsProcessingUpdate(updatedMediaRequest);
        }
    }

    List<MediaEntity> filterMediaByMediaRequestTimeframeAndSortByStartTimeAndChannel(List<MediaEntity> mediaEntitiesForRequest,
                                                                                     MediaRequestEntity mediaRequestEntity) {
        return mediaEntitiesForRequest.stream()
            .filter(media -> mediaRequestEntity.getStartTime().isBefore(media.getEnd()))
            .filter(media -> media.getStart().isBefore(mediaRequestEntity.getEndTime()))
            .sorted(MEDIA_START_TIME_CHANNEL_COMPARATOR)
            .collect(Collectors.toList());
    }

    private Map<MediaEntity, Path> downloadAndSaveMediaToWorkspace(List<MediaEntity> mediaEntitiesForRequest)
        throws IOException {
        Map<MediaEntity, Path> downloadedMedias = new LinkedHashMap<>();
        for (MediaEntity mediaEntity : mediaEntitiesForRequest) {
            Path downloadPath = retrieveFromStorageAndSaveToTempWorkspace(mediaEntity);

            downloadedMedias.put(mediaEntity, downloadPath);
        }
        return downloadedMedias;
    }

    @Override
    public Path retrieveFromStorageAndSaveToTempWorkspace(MediaEntity mediaEntity) throws IOException {

        try (DownloadResponseMetaData downloadResponseMetaData = dataManagementFacade.retrieveFileFromStorage(mediaEntity)) {
            UUID id = downloadResponseMetaData.getEodEntity().getExternalLocation();

            var mediaData = downloadResponseMetaData.getInputStream();
            return saveBlobDataToTempWorkspace(mediaData, id.toString());
        } catch (FileNotDownloadedException e) {
            throw new RuntimeException("Retrieval from storage failed for MediaId " + mediaEntity.getId(), e);
        }
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private List<AudioFileInfo> generateFilesForRequestType(MediaRequestEntity mediaRequestEntity,
                                                            Map<MediaEntity, Path> downloadedMedias)
        throws ExecutionException, InterruptedException, IOException {

        var requestType = mediaRequestEntity.getRequestType();

        if (DOWNLOAD.equals(requestType)) {
            return handleDownloads(downloadedMedias, mediaRequestEntity);
        } else if (AudioRequestType.PLAYBACK.equals(requestType)) {
            return handlePlaybacks(downloadedMedias, mediaRequestEntity);
        } else {
            throw new NotImplementedException(
                String.format("No handler exists for request type %s", requestType));
        }
    }

    private List<AudioFileInfo> handleDownloads(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException, IOException {

        List<List<AudioFileInfo>> processedAudio = outboundFileProcessor.processAudioForDownload(
            downloadedMedias,
            mediaRequestEntity.getStartTime(),
            mediaRequestEntity.getEndTime()
        );

        var downloadZip = outboundFileZipGenerator.generateAndWriteZip(processedAudio, mediaRequestEntity);
        return Collections.singletonList(
            AudioFileInfo.builder()
                .startTime(mediaRequestEntity.getStartTime().toInstant())
                .endTime(mediaRequestEntity.getEndTime().toInstant())
                .channel(0)
                .path(downloadZip)
                .build()
        );
    }

    private List<AudioFileInfo> handlePlaybacks(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException, IOException {

        return outboundFileProcessor.processAudioForPlaybacks(
            downloadedMedias,
            mediaRequestEntity.getStartTime(),
            mediaRequestEntity.getEndTime()
        );
    }
}
