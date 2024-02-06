package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.AudioRequestBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.model.AudioRequestBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.util.StreamingResponseEntityUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.sse.SentServerEventsHeartBeatEmitter;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioServiceImpl implements AudioService {

    private static final String AUDIO_RESPONSE_EVENT_NAME = "audio response";

    private final AudioTransformationService audioTransformationService;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final MediaRepository mediaRepository;
    private final AudioOperationService audioOperationService;
    private final FileOperationService fileOperationService;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final HearingRepository hearingRepository;
    private final AddAudioRequestMapper mapper;
    private final DataManagementApi dataManagementApi;
    private final UserIdentity userIdentity;
    private final FileContentChecksum fileContentChecksum;
    private final CourtLogEventRepository courtLogEventRepository;
    private final AudioConfigurationProperties audioConfigurationProperties;
    private final SentServerEventsHeartBeatEmitter heartBeatEmitter;
    private final AudioRequestBeingProcessedFromArchiveQuery audioRequestBeingProcessedFromArchiveQuery;

    private AudioFileInfo createAudioFileInfo(MediaEntity mediaEntity, Path downloadPath) {
        return AudioFileInfo.builder()
                .startTime(mediaEntity.getStart().toInstant())
                .endTime(mediaEntity.getEnd().toInstant())
                .channel(mediaEntity.getChannel())
                .mediaFile(mediaEntity.getMediaFile())
                .path(downloadPath)
                .build();
    }

    private static SseEmitter.SseEventBuilder createPreviewSse(String range, InputStream audioMediaFile) throws IOException {
        ResponseEntity<byte[]> response;
        response = StreamingResponseEntityUtil.createResponseEntity(audioMediaFile, range);

        return SseEmitter.event()
                .data(response)
                .name(AUDIO_RESPONSE_EVENT_NAME);
    }

    @Override
    public List<MediaEntity> getAudioMetadata(Integer hearingId, Integer channel) {
        return mediaRepository.findAllByHearingIdAndChannel(hearingId, channel);
    }

    @Override
    public InputStream preview(Integer mediaId) {
        MediaEntity mediaEntity = mediaRepository.findById(mediaId).orElseThrow(
                () -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));
        BinaryData mediaBinaryData;
        try {
            Path downloadPath = audioTransformationService.saveMediaToWorkspace(mediaEntity);

            AudioFileInfo audioFileInfo = createAudioFileInfo(mediaEntity, downloadPath);

            AudioFileInfo encodedAudioFileInfo;
            try {
                encodedAudioFileInfo = audioOperationService.reEncode(UUID.randomUUID().toString(), audioFileInfo);
            } catch (ExecutionException | InterruptedException e) {
                // For Sonar rule S2142
                throw e;
            }
            Path encodedAudioPath = encodedAudioFileInfo.getPath();

            mediaBinaryData = fileOperationService.convertFileToBinaryData(encodedAudioPath.toFile().getAbsolutePath());
        } catch (Exception exception) {
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return mediaBinaryData.toStream();
    }

    @Override
    @Transactional
    public void addAudio(MultipartFile audioFileStream, AddAudioMetadataRequest addAudioMetadataRequest) {
        final UUID externalLocation;
        final String checksum;

        try {
            BinaryData binaryData = BinaryData.fromStream(audioFileStream.getInputStream());
            checksum = fileContentChecksum.calculate(binaryData.toBytes());
            externalLocation = dataManagementApi.saveBlobDataToInboundContainer(binaryData);
        } catch (IOException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
        }

        MediaEntity savedMedia = mediaRepository.save(mapper.mapToMedia(addAudioMetadataRequest));
        savedMedia.setChecksum(checksum);
        linkAudioToHearingInMetadata(addAudioMetadataRequest, savedMedia);
        linkAudioToHearingByEvent(addAudioMetadataRequest, savedMedia);

        saveExternalObjectDirectory(
                externalLocation,
                checksum,
                userIdentity.getUserAccount(),
                savedMedia
        );
    }

    @Override
    public void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia) {
        for (String caseNumber : addAudioMetadataRequest.getCases()) {
            HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                    addAudioMetadataRequest.getCourthouse(),
                    addAudioMetadataRequest.getCourtroom(),
                    caseNumber,
                    addAudioMetadataRequest.getStartedAt().toLocalDate()
            );
            hearing.addMedia(savedMedia);
            hearingRepository.saveAndFlush(hearing);
        }
    }

    @Override
    public void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia) {

        if (addAudioMetadataRequest.getTotalChannels() == 1) {
            if (audioConfigurationProperties.getHandheldAudioCourtroomNumbers()
                    .contains(addAudioMetadataRequest.getCourtroom())) {
                return;
            }
        }

        String courthouse = addAudioMetadataRequest.getCourthouse();
        String courtroom = addAudioMetadataRequest.getCourtroom();
        OffsetDateTime start = addAudioMetadataRequest.getStartedAt().minusMinutes(audioConfigurationProperties.getPreAmbleDuration());
        OffsetDateTime end = addAudioMetadataRequest.getEndedAt().plusMinutes(audioConfigurationProperties.getPostAmbleDuration());
        var courtLogs = courtLogEventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
                courthouse,
                courtroom,
                start,
                end
        );

        var associatedHearings = courtLogs.stream()
                .flatMap(h -> h.getHearingEntities().stream())
                .distinct()
                .collect(Collectors.toList());

        for (var hearing : associatedHearings) {
            if (!hearing.getMediaList().contains(savedMedia)) {
                hearing.addMedia(savedMedia);
                hearingRepository.saveAndFlush(hearing);
            }
        }
    }

    private ExternalObjectDirectoryEntity saveExternalObjectDirectory(UUID externalLocation,
                                                                      String checksum,
                                                                      UserAccountEntity userAccountEntity,
                                                                      MediaEntity mediaEntity) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setMedia(mediaEntity);
        externalObjectDirectoryEntity.setStatus(objectRecordStatusRepository.getReferenceById(
                ObjectRecordStatusEnum.STORED.getId()));
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(INBOUND.getId()));
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(checksum);
        externalObjectDirectoryEntity.setVerificationAttempts(1);
        externalObjectDirectoryEntity.setCreatedBy(userAccountEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccountEntity);
        externalObjectDirectoryEntity = externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
        return externalObjectDirectoryEntity;
    }

    @Override
    public SseEmitter startStreamingPreview(Integer mediaId, String range, SseEmitter emitter) {
        heartBeatEmitter.startHeartBeat(emitter);
        ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
        previewExecutor.execute(() -> this.sendPreview(emitter, mediaId, range));

        return emitter;
    }

    @Override
    public void setIsArchived(List<AudioMetadata> audioMetadata) {
        List<Integer> mediaIds = audioMetadata.stream().map(AudioMetadata::getId).toList();
        List<AudioRequestBeingProcessedFromArchiveQueryResult> archivedArmRecords =
            audioRequestBeingProcessedFromArchiveQuery.getResultsByMediaIds(mediaIds);

        for (AudioMetadata audioMetadataItem : audioMetadata) {
            if (archivedArmRecords.stream().anyMatch(archived -> audioMetadataItem.getId().equals(archived.mediaId()))) {
                audioMetadataItem.setIsArchived(true);
            }
        }
    }

    private void sendPreview(SseEmitter emitter, Integer mediaId, String range) {
        try {
            InputStream audioMediaFile = preview(mediaId);
            SseEmitter.SseEventBuilder event = createPreviewSse(range, audioMediaFile);
            emitter.send(event);
            emitter.complete();
        } catch (IOException e) {
            DartsApiException dartsApiException = new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST);
            log.error("Error when creating preview SSE", e);
            emitter.completeWithError(dartsApiException);
            throw dartsApiException;
        } catch (DartsApiException e) {
            emitter.completeWithError(e);
            throw e;
        }
    }
}
