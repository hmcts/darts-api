package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.AudioMessageDigest;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioUploadService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
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
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.common.util.MediaEntityTreeNodeImpl;
import uk.gov.hmcts.darts.common.util.Tree;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioUploadServiceImpl implements AudioUploadService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final MediaRepository mediaRepository;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final HearingRepository hearingRepository;
    private final AddAudioRequestMapper mapper;
    private final DataManagementApi dataManagementApi;
    private final UserIdentity userIdentity;
    private final FileContentChecksum fileContentChecksum;
    private final CourtLogEventRepository courtLogEventRepository;
    private final AudioConfigurationProperties audioConfigurationProperties;
    private final LogApi logApi;
    private final AudioMessageDigest audioDigest;

    @Override
    @Transactional
    public void addAudio(MultipartFile audioFileStream, AddAudioMetadataRequest addAudioMetadataRequest) {

        log.info("Adding audio using metadata {}", addAudioMetadataRequest.toString());

        Collection<MediaEntity> identifiedDuplicate = getDuplicateMediaFile(addAudioMetadataRequest);
        Optional<Collection<MediaEntity>> audioToVersion = findChangedSizeAudioFilesFromDuplicates(identifiedDuplicate,
                                                                                                   addAudioMetadataRequest.getFileSize());

        if (identifiedDuplicate.isEmpty() || audioToVersion.isPresent()) {

            UUID externalLocation = null;
            String checksum = null;

            // upload to the blob store
            ObjectRecordStatusEntity objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(STORED.getId());
            Optional<AudioBlobUploadDetails> detailsOption = saveAudioToInbound(audioFileStream);
            if (detailsOption.isEmpty()) {
                objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND.getId());
            }

            // if we have not found any duplicate audio files to process lets add a new one
            if (audioToVersion.isEmpty()) {
                List<MediaEntity> audioFileToProcess = new ArrayList<>();
                MediaEntity newEntity = mapper.mapToMedia(addAudioMetadataRequest);

                audioFileToProcess.add(newEntity);
                audioToVersion = Optional.of(audioFileToProcess);

                log.info("No duplicates found. Uploading new file");
            } else {
                log.info("Duplicate audio file has been found with difference in file size");
            }

            // version the file upload to the database
            versionUpload(audioToVersion.get(), addAudioMetadataRequest,
                          externalLocation, checksum, objectRecordStatusEntity);
        } else {
            log.info("Duplicate audio upload detected with no difference in file size. Returning 200 with no changes ");
            for (MediaEntity entity : identifiedDuplicate) {
                log.info("Duplicate media id {}", entity.getId());
            }
        }
    }

    private Optional<AudioBlobUploadDetails> saveAudioToInbound(MultipartFile audioFileStream) {
        try (var digestInputStream = new DigestInputStream(new BufferedInputStream(audioFileStream.getInputStream()), audioDigest.getMessageDigest())) {
            if (!audioFileStream.isEmpty()) {
                UUID externalLocation = dataManagementApi.saveBlobDataToInboundContainer(digestInputStream);
                String checksum = fileContentChecksum.calculate(digestInputStream);
                return Optional.of(new AudioBlobUploadDetails(externalLocation, checksum));
            }
        } catch (IOException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
        }

        return Optional.empty();
    }

    private void versionUpload(Collection<MediaEntity> audioToVersion,
                               AddAudioMetadataRequest addAudioMetadataRequest,
                               UUID externalLocation, String checksum,
                               ObjectRecordStatusEntity objectRecordStatusEntity) {
        for (MediaEntity entity : audioToVersion) {

            MediaEntity saveEntity = entity;

            // if the media already exists in the database then create a new media file
            if (entity.getId() != null) {
                saveEntity = mapper.mapToMedia(addAudioMetadataRequest);

                saveEntity.setChronicleId(entity.getChronicleId());
                saveEntity.setAntecedentId(entity.getId().toString());

                log.info("Uploading version of duplicate filename {} with antecedent media id {}", entity.getMediaFile(), entity.getId().toString());
            } else {
                log.info("New file uploaded {} with filename", entity.getMediaFile());

                saveEntity = mediaRepository.save(saveEntity);
                saveEntity.setChronicleId(saveEntity.getId().toString());
            }

            log.info("Saved media id {}", entity.getId());

            saveEntity.setChecksum(checksum);
            mediaRepository.save(saveEntity);

            linkAudioToHearingInMetadata(addAudioMetadataRequest, entity.equals(saveEntity) ? null : entity, saveEntity);
            linkAudioToHearingByEvent(addAudioMetadataRequest, saveEntity);

            saveExternalObjectDirectory(
                externalLocation,
                checksum,
                userIdentity.getUserAccount(),
                saveEntity,
                objectRecordStatusEntity
            );
        }

        logApi.audioUploaded(addAudioMetadataRequest);
    }

    private Optional<Collection<MediaEntity>> findChangedSizeAudioFilesFromDuplicates(Collection<MediaEntity> duplicates, long size) {
        Collection<MediaEntity> mediaEntitiesToVersion = new ArrayList<>();
        for (MediaEntity entity : duplicates) {

            // if file size is not expected
            if (size != entity.getFileSize()) {
                mediaEntitiesToVersion.add(entity);
            }
        }

        if (!mediaEntitiesToVersion.isEmpty()) {
            return Optional.of(mediaEntitiesToVersion);
        }

        return Optional.empty();
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.LooseCoupling"})
    private List<MediaEntity> getCaseRelatedMediaEntities(List<String> cases, List<MediaEntity> entities) {
        ArrayList<MediaEntity> entityForCaseLst = new ArrayList<>();
        for (MediaEntity entity : entities) {
            List<String> casesForEntity = entity.getCaseNumberList();

            if (cases.size() == casesForEntity.size() && new HashSet<>(casesForEntity).containsAll(cases)) {
                entityForCaseLst.add(entity);
            }
        }

        return entityForCaseLst;
    }

    @SuppressWarnings({"PMD.LooseCoupling"})
    private Collection<MediaEntity> getDuplicateMediaFile(AddAudioMetadataRequest addAudioMetadataRequest) {
        List<MediaEntity> mediaEntities =  mediaRepository.findMediaByDetails(
            addAudioMetadataRequest.getCourthouse(),
            addAudioMetadataRequest.getCourtroom(),
            addAudioMetadataRequest.getChannel(),
            addAudioMetadataRequest.getFilename(),
            addAudioMetadataRequest.getStartedAt(),
            addAudioMetadataRequest.getEndedAt());

        // gets any media entities that relate to the cases.
        mediaEntities = getCaseRelatedMediaEntities(addAudioMetadataRequest.getCases(), mediaEntities);

        // now lets get the lowest level media objects so that they can act as a basis for the antecedent
        Tree<MediaEntityTreeNodeImpl> tree = new Tree<>();
        mediaEntities.stream().forEach(entry ->
            tree.addNode(new MediaEntityTreeNodeImpl(entry))
        );

        return tree.getLowestLevelDescendants().stream().map(MediaEntityTreeNodeImpl::getEntity).toList();
    }

    @Override
    public void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity mediaToReplace, MediaEntity newMedia) {
        for (String caseNumber : addAudioMetadataRequest.getCases()) {
            HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                addAudioMetadataRequest.getCourthouse(),
                addAudioMetadataRequest.getCourtroom(),
                caseNumber,
                DateConverterUtil.toLocalDateTime(addAudioMetadataRequest.getStartedAt()),
                userIdentity.getUserAccount()
            );

            if (mediaToReplace != null) {
                // delete the existing media entity hearing link and link to the new media entity instead
                hearing.getMediaList().removeIf(me -> me.getId().equals(mediaToReplace.getId()));
            }

            // add the new media
            hearing.addMedia(newMedia);

            hearingRepository.saveAndFlush(hearing);
        }
    }


    @Override
    public void linkAudioToHearingByEvent(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity savedMedia) {

        if (addAudioMetadataRequest.getTotalChannels() == 1
            && audioConfigurationProperties.getHandheldAudioCourtroomNumbers().contains(addAudioMetadataRequest.getCourtroom())) {
            return;
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
            .toList();

        for (var hearing : associatedHearings) {
            if (!hearing.getMediaList().contains(savedMedia)) {
                hearing.addMedia(savedMedia);
                hearingRepository.saveAndFlush(hearing);
            }
        }
    }

    private void saveExternalObjectDirectory(UUID externalLocation,
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
        externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
    }

    record AudioBlobUploadDetails(UUID uuid, String checksum) {}
}