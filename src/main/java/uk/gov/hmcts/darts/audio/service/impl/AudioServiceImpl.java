package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.AudioBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AudioBeingProcessedFromArchiveQueryResult;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.service.AudioService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
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
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.common.util.Version;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

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
    private final CourtLogEventRepository courtLogEventRepository;
    private final AudioConfigurationProperties audioConfigurationProperties;
    private final AudioBeingProcessedFromArchiveQuery audioBeingProcessedFromArchiveQuery;
    private final LogApi logApi;
    private final EntityManager em;

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
    public List<MediaEntity> getAudioMetadata(Integer hearingId, Integer channel) {
        return mediaRepository.findAllByHearingIdAndChannel(hearingId, channel);
    }

    public BinaryData encode(Integer mediaId) {
        MediaEntity mediaEntity = mediaRepository.findById(mediaId).orElseThrow(
            () -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));
        BinaryData mediaBinaryData;
        try {
            Path downloadPath = audioTransformationService.retrieveFromStorageAndSaveToTempWorkspace(mediaEntity);

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

        return mediaBinaryData;
    }

    @Override
    @Transactional
    public void addAudio(MultipartFile audioFileStream, AddAudioMetadataRequest addAudioMetadataRequest) {

        log.info("Adding audio using metadata {}", addAudioMetadataRequest.toString());

        Optional<Collection<MediaEntity>> identifiedDuplicate = getDuplicateMediaFile(addAudioMetadataRequest);
        Optional<Collection<MediaEntity>> audioToVersion = findAudioToVersionAccordingToSize(identifiedDuplicate,
                                                                                                  addAudioMetadataRequest.getFileSize());

        if (identifiedDuplicate.isEmpty() || audioToVersion.isPresent()) {

            // if we have not found any duplicate audio files to process lets add a new one
            if (audioToVersion.isEmpty()) {
                List<MediaEntity> audioFileToProcess = new ArrayList<MediaEntity>();
                MediaEntity newEntity = mapper.mapToMedia(addAudioMetadataRequest);

                audioFileToProcess.add(newEntity);
                audioToVersion = Optional.of(audioFileToProcess);

                log.info("No duplicates found. Uploading new file");
            } else {
                log.info("Identified media ids that require duplicate audio");
            }

            MessageDigest md5Digest;
            try {
                md5Digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
            }

            UUID externalLocation = null;
            String checksum = null;

            ObjectRecordStatusEntity objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(STORED.getId());
            try (var digestInputStream = new DigestInputStream(new BufferedInputStream(audioFileStream.getInputStream()), md5Digest)) {
                if (audioFileStream.isEmpty()) {
                    objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND.getId());
                } else {
                    externalLocation = dataManagementApi.saveBlobDataToInboundContainer(digestInputStream);
                    checksum = fileContentChecksum.calculate(digestInputStream);
                }
            } catch (IOException e) {
                throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
            }

            // version the file upload to the database
            versionUpload(audioToVersion.get(), addAudioMetadataRequest,
                           externalLocation, checksum, objectRecordStatusEntity);
        } else {
            log.info("Duplicate audio upload detected with no difference in file size. Returning 200 with no changes ");
            for (MediaEntity entity : identifiedDuplicate.get()) {
                log.info("Duplicate media id {}", entity.getId());
            }
        }
    }

    private void versionUpload(Collection<MediaEntity> audioToVersion,
                                AddAudioMetadataRequest addAudioMetadataRequest,
                                UUID externalLocation, String checksum,
                                ObjectRecordStatusEntity objectRecordStatusEntity) {
        for (MediaEntity entity : audioToVersion) {

            LockModeType modeType = null;
            if (entity.getId() != null) {
                modeType = em.getLockMode(entity);
            }
            if (entity.getId() != null) {

                // lock the entity record so that nothing can write to it. We need
                // this to ensure version doesnt change if a concurrent add audio request comes through
                em.lock(entity, LockModeType.PESSIMISTIC_WRITE);

                // ensure that we refresh the entity
                em.refresh(entity);

                Version version = new Version(entity.getLegacyVersionLabel());
                entity = mapper.mapToMedia(addAudioMetadataRequest);

                //Increase the major number for now
                version.increaseMajor();

                setVersion(version, entity);
                log.info("Uploading version of duplicate filename {} under version number {}", entity.getMediaFile(), entity.getLegacyVersionLabel());
            } else {
                log.info("New file uploaded {} with version number {}", entity.getMediaFile(), entity.getLegacyVersionLabel());

                setVersion(new Version(), entity);
            }
            MediaEntity savedMedia = mediaRepository.save(entity);

            // set the lock mode back to what is was originally
            if (modeType != null) {
                em.lock(entity, modeType);
            }

            log.info("Saved media id {}", entity.getId());

            savedMedia.setChecksum(checksum);
            linkAudioToHearingInMetadata(addAudioMetadataRequest, savedMedia);
            linkAudioToHearingByEvent(addAudioMetadataRequest, savedMedia);

            saveExternalObjectDirectory(
                externalLocation,
                checksum,
                userIdentity.getUserAccount(),
                savedMedia,
                objectRecordStatusEntity
            );
        }

        logApi.audioUploaded(addAudioMetadataRequest);
    }

    //TODO: Clarify how the version is set in the DB
    private void setVersion(Version version, MediaEntity entity) {
        entity.setLegacyVersionLabel(version.getVersionString());
    }

    private Optional<Collection<MediaEntity>> findAudioToVersionAccordingToSize(Optional<Collection<MediaEntity>> duplicates,
                                                                                long size) {
        Collection<MediaEntity> mediaEntitiesToVersion = new ArrayList<>();

        if (duplicates.isPresent()) {
            for (MediaEntity entity : duplicates.get()) {

                // if file size is not expected
                if (size != entity.getFileSize()) {
                    mediaEntitiesToVersion.add(entity);
                }
            }

            if (!mediaEntitiesToVersion.isEmpty()) {
                return Optional.of(mediaEntitiesToVersion);
            }
        }

        return Optional.empty();
    }

    private Optional<Collection<MediaEntity>> getDuplicateMediaFile(AddAudioMetadataRequest addAudioMetadataRequest) {
        Map<List<String>, MediaEntity> casesToLatestMedia = new HashMap<>();

        List<MediaEntity> mediaEntities = mediaRepository.findMediaByDetails(
                                                                             addAudioMetadataRequest.getCourthouse(),
                                                                             addAudioMetadataRequest.getCourtroom(),
                                                                             addAudioMetadataRequest.getChannel(),
                                                                             addAudioMetadataRequest.getFilename(),
                                                                             addAudioMetadataRequest.getStartedAt(),
                                                                             addAudioMetadataRequest.getEndedAt());
        mediaEntities = getCaseRelatedEntities(addAudioMetadataRequest.getCases(), mediaEntities);

        // we now need to get the latest media entity for each type so we can increment the version
        // for a media entity. To determine this we match on the cases number list
        for (MediaEntity entity : mediaEntities) {
            MediaEntity fndMediaEntity = casesToLatestMedia.get(entity.getCaseNumberList());
            if (fndMediaEntity != null) {
                int compareVersion = new Version(entity.getLegacyVersionLabel()).compareTo(new Version(fndMediaEntity.getLegacyVersionLabel()));
                if (compareVersion > 0) {
                    casesToLatestMedia.put(entity.getCaseNumberList(), fndMediaEntity);
                }
            } else {
                casesToLatestMedia.put(entity.getCaseNumberList(), entity);
            }
        }

        if (!casesToLatestMedia.isEmpty()) {
            return Optional.of(casesToLatestMedia.values());
        }

        return Optional.empty();
    }

    private List<MediaEntity> getCaseRelatedEntities(List<String> cases, List<MediaEntity> entities) {
        ArrayList<MediaEntity> entityForCaseLst = new ArrayList<>();
        for (MediaEntity entity : entities) {
            List<String> casesForEntity = entity.getCaseNumberList();

            // lets determine if any of the cases we are targeting exist against the media entities
            casesForEntity.retainAll(cases);

            if (!casesForEntity.isEmpty()) {
                entityForCaseLst.add(entity);
            }
        }

        return entityForCaseLst;
    }

    @Override
    public void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia) {
        for (String caseNumber : addAudioMetadataRequest.getCases()) {
            HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                addAudioMetadataRequest.getCourthouse(),
                addAudioMetadataRequest.getCourtroom(),
                caseNumber,
                DateConverterUtil.toLocalDateTime(addAudioMetadataRequest.getStartedAt()),
                userIdentity.getUserAccount()
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
                                                                      MediaEntity mediaEntity,
                                                                      ObjectRecordStatusEntity objectRecordStatusEntity) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setMedia(mediaEntity);
        externalObjectDirectoryEntity.setStatus(objectRecordStatusEntity);
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
    public void setIsArchived(List<AudioMetadata> audioMetadata, Integer hearingId) {
        List<AudioBeingProcessedFromArchiveQueryResult> archivedArmRecords =
            audioBeingProcessedFromArchiveQuery.getResults(hearingId);

        for (AudioMetadata audioMetadataItem : audioMetadata) {
            if (archivedArmRecords.stream().anyMatch(archived -> audioMetadataItem.getId().equals(archived.mediaId()))) {
                audioMetadataItem.setIsArchived(true);
            } else {
                audioMetadataItem.setIsArchived(false);
            }
        }
    }

    /*
    Set the isAvailable flag if the media is available in the unstructured datastore.
     */
    @Override
    public void setIsAvailable(List<AudioMetadata> audioMetadataList) {
        List<Integer> mediaIdList = audioMetadataList.stream().map(AudioMetadata::getId).toList();
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        ExternalLocationTypeEntity unstructuredLocationType = externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId());
        List<Integer> mediaIdsStoredInUnstructured = externalObjectDirectoryRepository.findMediaIdsByInMediaIdStatusAndType(mediaIdList, storedStatus,
                                                                                                                            unstructuredLocationType);

        for (AudioMetadata audioMetadataItem : audioMetadataList) {
            audioMetadataItem.setIsAvailable(mediaIdsStoredInUnstructured.contains(audioMetadataItem.getId()));
        }
    }
}