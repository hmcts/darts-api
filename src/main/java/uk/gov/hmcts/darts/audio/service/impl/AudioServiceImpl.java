package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.component.AudioBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioServiceImpl implements AudioService {

    private final AudioTransformationService audioTransformationService;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final MediaRepository mediaRepository;
    private final AudioOperationService audioOperationService;
    private final FileOperationService fileOperationService;
    private final AudioBeingProcessedFromArchiveQuery audioBeingProcessedFromArchiveQuery;

    private AudioFileInfo createAudioFileInfo(MediaEntity mediaEntity, Path downloadPath) {
        return AudioFileInfo.builder()
            .startTime(mediaEntity.getStart().toInstant())
            .endTime(mediaEntity.getEnd().toInstant())
            .channel(mediaEntity.getChannel())
            .mediaFile(mediaEntity.getMediaFile())
            .path(downloadPath)
            .build();
    }

    @Override
    public List<MediaEntity> getMediaEntitiesByHearingAndLowestChannel(Integer hearingId) {
        return mediaRepository.findAllByHearingIdAndMinimumChannelAndIsCurrentTrue(hearingId);
    }

    @Override
    @SuppressWarnings({
        "PMD.ExceptionAsFlowControl",
        "PMD.DoNotUseThreads"//TODO - refactor to avoid using Thread.sleep() when this is next edited
    })
    public BinaryData encode(Long mediaId) {
        MediaEntity mediaEntity = mediaRepository.findById(mediaId).orElseThrow(
            () -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));
        BinaryData mediaBinaryData;
        try {
            Path downloadPath = audioTransformationService.retrieveFromStorageAndSaveToTempWorkspace(mediaEntity);

            AudioFileInfo audioFileInfo = createAudioFileInfo(mediaEntity, downloadPath);

            AudioFileInfo encodedAudioFileInfo;
            encodedAudioFileInfo = audioOperationService.reEncode(UUID.randomUUID().toString(), audioFileInfo);

            Path encodedAudioPath = encodedAudioFileInfo.getPath();

            mediaBinaryData = fileOperationService.convertFileToBinaryData(encodedAudioPath.toFile().getAbsolutePath());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        } catch (ExecutionException | IOException exception) {
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return mediaBinaryData;
    }

    @Override
    public void setIsArchived(List<AudioMetadata> audioMetadata, Integer hearingId) {
        if (!isEmpty(audioMetadata)) {
            List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords =
                audioBeingProcessedFromArchiveQuery.getResults(hearingId);

            for (AudioMetadata audioMetadataItem : audioMetadata) {
                boolean isArchived = isMediaArchived(audioMetadataItem, archivedArmRecords);
                audioMetadataItem.setIsArchived(isArchived);
            }
        }
    }

    /*
    Set the isAvailable flag if the media is available in the unstructured datastore.
     */
    @Override
    public void setIsAvailable(List<AudioMetadata> audioMetadataList) {
        if (!isEmpty(audioMetadataList)) {
            List<Long> mediaIdList = audioMetadataList.stream().map(AudioMetadata::getId).toList();
            List<Long> mediaIdsStoredInUnstructured = externalObjectDirectoryRepository.findMediaIdsByInMediaIdStatusAndType(
                mediaIdList,
                EodHelper.storedStatus(),
                EodHelper.unstructuredLocation(), EodHelper.detsLocation());

            for (AudioMetadata audioMetadataItem : audioMetadataList) {
                audioMetadataItem.setIsAvailable(mediaIdsStoredInUnstructured.contains(audioMetadataItem.getId()));
            }
        }
    }

    private boolean isMediaArchived(AudioMetadata audioMetadataItem, List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords) {
        return archivedArmRecords.stream().anyMatch(archived -> audioMetadataItem.getId().equals(archived.mediaId()));
    }
}