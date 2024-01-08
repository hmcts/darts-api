package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobRange;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.helper.BlobRangeHelper;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.PreviewRange;
import uk.gov.hmcts.darts.audio.model.SavedAudioFileInfo;
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
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;

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
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final HearingRepository hearingRepository;
    private final AddAudioRequestMapper mapper;
    private final DataManagementApi dataManagementApi;
    private final UserIdentity userIdentity;
    private final FileContentChecksum fileContentChecksum;
    private final BlobRangeHelper blobRangeHelper;

    private static AudioFileInfo createAudioFileInfo(MediaEntity mediaEntity, Path downloadPath) {
        return new AudioFileInfo(
            mediaEntity.getStart().toInstant(),
            mediaEntity.getEnd().toInstant(),
            downloadPath.toFile().getAbsolutePath(),
            mediaEntity.getChannel()
        );
    }

    @Override
    public ResponseEntity<byte[]> preview(Integer mediaId, PreviewRange previewRange) throws IOException {

        MediaEntity mediaEntity = mediaRepository.findById(mediaId).orElseThrow(
            () -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));
        BinaryData mediaBinaryData;
        SavedAudioFileInfo mp2FileInfo;
        try {
            BlobRange range = new BlobRange(0, previewRange.getEndRange());
            mp2FileInfo = audioTransformationService.retrieveFromStorageAndSaveMediaToWorkspace(mediaEntity, range);

            AudioFileInfo mp2AudioFileInfo = createAudioFileInfo(mediaEntity, mp2FileInfo.getPath());

            AudioFileInfo mp3AudioFileInfo;
            try {
                mp3AudioFileInfo = audioOperationService.reEncode(UUID.randomUUID().toString(), mp2AudioFileInfo);
            } catch (ExecutionException | InterruptedException e) {
                // For Sonar rule S2142
                throw e;
            }
            Path encodedAudioPath = Path.of(mp3AudioFileInfo.getFileName());

            mediaBinaryData = fileOperationService.saveFileToBinaryData(encodedAudioPath.toFile().getAbsolutePath());
            previewRange.setContentLength(mp2FileInfo.getBlobClient().getProperties().getBlobSize());
        } catch (Exception exception) {
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return StreamingResponseEntityUtil.createResponseEntity(mediaBinaryData.toStream(), previewRange);
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
        linkAudioAndHearing(addAudioMetadataRequest, savedMedia);

        saveExternalObjectDirectory(
            externalLocation,
            checksum,
            userIdentity.getUserAccount(),
            savedMedia
        );
    }

    @Override
    public void linkAudioAndHearing(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia) {
        for (String caseId : addAudioMetadataRequest.getCases()) {
            HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                addAudioMetadataRequest.getCourthouse(),
                addAudioMetadataRequest.getCourtroom(),
                caseId,
                addAudioMetadataRequest.getStartedAt().toLocalDate()
            );
            hearing.addMedia(savedMedia);
            hearingRepository.saveAndFlush(hearing);
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
        externalObjectDirectoryEntity.setCreatedBy(userAccountEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccountEntity);
        externalObjectDirectoryEntity = externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
        return externalObjectDirectoryEntity;
    }

}
