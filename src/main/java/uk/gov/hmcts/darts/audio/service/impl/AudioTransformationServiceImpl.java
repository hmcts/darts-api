package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGenerator;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.helper.TransformedMediaHelper;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REQUEST_ID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"}) // DMP-715 to resolve
public class AudioTransformationServiceImpl implements AudioTransformationService {

    public static final String NO_DEFENDANTS = "There are no defendants for this hearing";
    public static final String NOT_AVAILABLE = "N/A";
    private final MediaRequestService mediaRequestService;
    private final OutboundFileProcessor outboundFileProcessor;
    private final OutboundFileZipGenerator outboundFileZipGenerator;

    private final TransientObjectDirectoryService transientObjectDirectoryService;
    private final FileOperationService fileOperationService;

    private final MediaRepository mediaRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final DataManagementApi dataManagementApi;
    private final NotificationApi notificationApi;
    private final UserAccountRepository userAccountRepository;

    private final TransformedMediaHelper transformedMediaHelper;

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
    public List<MediaEntity> getMediaMetadata(Integer hearingId) {
        return mediaRepository.findAllByHearingId(hearingId);
    }

    @Override
    public Optional<UUID> getMediaLocation(MediaEntity media) {
        Optional<UUID> externalLocation = Optional.empty();

        ObjectRecordStatusEntity objectDirectoryStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
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
    public void handleKedaInvocationForMediaRequests() {
        var openRequests = mediaRequestService.getOldestMediaRequestByStatus(MediaRequestStatus.OPEN);

        if (openRequests.isEmpty()) {
            log.info("No open requests found for ATS to process.");
        } else {
            openRequests.ifPresent(openMediaRequests -> processAudioRequest(openMediaRequests.getId()));
        }
    }

    /**
     * For all audio related to a given AudioRequest, download, transform and upload the processed file to outbound
     * storage.
     *
     * @param requestId The id of the AudioRequest to be processed.
     */
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidRethrowingException"})
    private void processAudioRequest(Integer requestId) {

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

            List<MediaEntity> mediaEntitiesForHearing = getMediaMetadata(hearingEntity.getId());

            if (mediaEntitiesForHearing.isEmpty()) {
                throw new DartsApiException(
                    AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST,
                    "No media present to process"
                );
            }

            List<MediaEntity> filteredMediaEntities = filterMediaByMediaRequestTimeframe(mediaEntitiesForHearing, mediaRequestEntity);

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
            for (AudioFileInfo generatedAudioFile : generatedAudioFiles) {
                final String fileName = hearingEntity.getCourtCase().getCaseNumber() + "_" + hearingEntity.getHearingDate().format(formatter) + "_"
                    + index++ + ".mp3";

                try (InputStream inputStream = Files.newInputStream(generatedAudioFile.getPath())) {
                    blobId = transformedMediaHelper.saveToStorage(mediaRequestEntity, BinaryData.fromStream(inputStream), fileName, generatedAudioFile);
                } catch (NoSuchFileException nsfe) {
                    log.error("No file found when trying to save to storage. {}", generatedAudioFile.getPath());
                    throw nsfe;
                }

                mediaRequestService.updateAudioRequestCompleted(mediaRequestEntity, fileName, audioRequestOutputFormat);
                log.debug(
                    "Completed upload of file to storage for mediaRequestId {}. File ''{}'' successfully uploaded with blobId: {}",
                    requestId,
                    fileName,
                    blobId
                );
            }

            log.debug("Completed processing for requestId {}.", requestId);

        } catch (Exception e) {
            log.error(
                "Exception occurred for request id {}.",
                requestId,
                e
            );
            mediaRequestService.updateAudioRequestStatus(requestId, FAILED);

            if (mediaRequestEntity != null && hearingEntity != null) {
                notifyUser(mediaRequestEntity, hearingEntity.getCourtCase(),
                           NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString()
                );
            }

            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, e);
        }

        notifyUser(mediaRequestEntity, hearingEntity.getCourtCase(), NotificationApi.NotificationTemplate.REQUESTED_AUDIO_AVAILABLE.toString());
    }

    List<MediaEntity> filterMediaByMediaRequestTimeframe(List<MediaEntity> mediaEntitiesForRequest, MediaRequestEntity mediaRequestEntity) {
        Comparator<MediaEntity> mediaStartTimeChannelComparator = (media1, media2) -> {
            if (media1.getStart().equals(media2.getStart())) {
                return media1.getChannel().compareTo(media2.getChannel());
            } else {
                return media1.getStart().compareTo(media2.getStart());
            }
        };
        return mediaEntitiesForRequest.stream()
            .filter(media -> (mediaRequestEntity.getStartTime()).isBefore(media.getEnd()))
            .filter(media -> media.getStart().isBefore(mediaRequestEntity.getEndTime()))
            .sorted(mediaStartTimeChannelComparator)
            .collect(Collectors.toList());
    }

    private Map<MediaEntity, Path> downloadAndSaveMediaToWorkspace(List<MediaEntity> mediaEntitiesForRequest)
        throws IOException {
        Map<MediaEntity, Path> downloadedMedias = new LinkedHashMap<>();
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

        log.debug("Downloading audio blob for {} from unstructured datastore", id);
        BinaryData binaryData = getUnstructuredAudioBlob(id);
        log.debug("Download audio blob complete for {}", id);

        Path downloadPath = saveBlobDataToTempWorkspace(binaryData, id.toString());
        log.debug("Saved audio blob {} to {}", id, downloadPath);
        return downloadPath;
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

        AudioFileInfo zipAudioFileInfo = new AudioFileInfo();
        zipAudioFileInfo.setStartTime(mediaRequestEntity.getStartTime().toInstant());
        zipAudioFileInfo.setEndTime(mediaRequestEntity.getEndTime().toInstant());
        zipAudioFileInfo.setPath(outboundFileZipGenerator.generateAndWriteZip(processedAudio, mediaRequestEntity));

        return Collections.singletonList(zipAudioFileInfo);
    }

    private List<AudioFileInfo> handlePlaybacks(Map<MediaEntity, Path> downloadedMedias, MediaRequestEntity mediaRequestEntity)
        throws ExecutionException, InterruptedException, IOException {
        return handlePlaybacks(downloadedMedias, mediaRequestEntity.getStartTime(), mediaRequestEntity.getEndTime());
    }

    public List<AudioFileInfo> handlePlaybacks(Map<MediaEntity, Path> downloadedMedias, OffsetDateTime startTime,
                                               OffsetDateTime endTime)
        throws ExecutionException, InterruptedException, IOException {

        List<AudioFileInfo> audioFileInfos = outboundFileProcessor.processAudioForPlaybacks(
            downloadedMedias,
            startTime,
            endTime
        );

        return audioFileInfos;
    }

    public void notifyUser(MediaRequestEntity mediaRequestEntity,
                           CourtCaseEntity courtCase,
                           String notificationTemplateName) {
        log.info("Scheduling notification for template name {}, request id {} and court case id {}", notificationTemplateName, mediaRequestEntity.getId(),
                 courtCase.getId()
        );

        Optional<UserAccountEntity> userAccount = userAccountRepository.findById(mediaRequestEntity.getRequestor().getId());

        if (userAccount.isPresent()) {
            Map<String, String> templateParams = new HashMap<>();

            if (notificationTemplateName.equals(NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString())) {

                String defendants = String.join(", ", mediaRequestEntity.getHearing().getCourtCase().getDefendantStringList());

                if (StringUtils.isBlank(defendants)) {
                    defendants = NO_DEFENDANTS;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

                String courthouseName = mediaRequestEntity.getHearing().getCourtCase().getCourthouse().getCourthouseName() != null
                    ? mediaRequestEntity.getHearing().getCourtCase().getCourthouse().getCourthouseName() : NOT_AVAILABLE;

                String hearingDate = getFormattedHearingDate(mediaRequestEntity.getHearing().getHearingDate());

                String audioStartTime = mediaRequestEntity.getStartTime() != null
                    ? mediaRequestEntity.getStartTime().format(formatter) : NOT_AVAILABLE;

                String audioEndTime = mediaRequestEntity.getEndTime() != null
                    ? mediaRequestEntity.getEndTime().format(formatter) : NOT_AVAILABLE;

                templateParams.put(REQUEST_ID, String.valueOf(mediaRequestEntity.getId()));
                templateParams.put(COURTHOUSE, courthouseName);
                templateParams.put(DEFENDANTS, defendants);
                templateParams.put(HEARING_DATE, hearingDate);
                templateParams.put(AUDIO_START_TIME, audioStartTime);
                templateParams.put(AUDIO_END_TIME, audioEndTime);
            }

            var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
                .eventId(notificationTemplateName)
                .caseId(courtCase.getId())
                .emailAddresses(userAccount.get().getEmailAddress())
                .templateValues(templateParams)
                .build();

            notificationApi.scheduleNotification(saveNotificationToDbRequest);

            log.debug("Notification scheduled successfully for request id {} and court case {}", mediaRequestEntity.getId(), courtCase.getId());
        } else {
            log.error("No notification scheduled for request id {} and court case {} ", mediaRequestEntity.getId(), courtCase.getId());
        }
    }

    private static String getFormattedHearingDate(LocalDate dateOfHearing) {

        if (dateOfHearing != null) {
            int day = dateOfHearing.getDayOfMonth();
            Month month = dateOfHearing.getMonth();
            int year = dateOfHearing.getYear();

            String strMonth = Pattern.compile("^.").matcher(month.toString().toLowerCase(Locale.UK)).replaceFirst(m -> m.group().toUpperCase(Locale.UK));
            return day + getNthNumber(day) + " " + strMonth + " " + year;
        } else {
            return NOT_AVAILABLE;
        }
    }

    public static String getNthNumber(int day) {
        if (day > 3 && day < 21) {
            return "th";
        }

        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

}
