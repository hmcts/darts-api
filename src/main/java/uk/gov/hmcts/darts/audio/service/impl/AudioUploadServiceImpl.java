package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.component.AudioMessageDigest;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioUploadService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
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

        //remove duplicate cases as they can appear more than once, e.g. if they broke for lunch.
        List<String> distinctCaseList = addAudioMetadataRequest.getCases().stream().distinct().toList();
        addAudioMetadataRequest.setCases(distinctCaseList);

        List<MediaEntity> duplicatesToBeSuperseded = getLatestDuplicateMediaFiles(addAudioMetadataRequest);
        List<MediaEntity> duplicateMediaWithDifferentFileSizes = filterAudioFilesWithDifferentFileSizes(duplicatesToBeSuperseded,
                                                                                                        addAudioMetadataRequest.getFileSize());
        if (isNotEmpty(duplicatesToBeSuperseded) && duplicateMediaWithDifferentFileSizes.isEmpty()) {
            log.info("Duplicate audio upload detected with no difference in file size. Returning 200 with no changes ");
            for (MediaEntity entity : duplicatesToBeSuperseded) {
                log.info("Duplicate media id {}", entity.getId());
            }
            return;
        }

        UUID externalLocation = null;
        String checksum = null;

        // upload to the blob store
        ObjectRecordStatusEntity objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(STORED.getId());
        Optional<AudioBlobUploadDetails> detailsOption = saveAudioToInbound(audioFileStream);
        if (detailsOption.isEmpty()) {
            objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND.getId());
        } else {
            externalLocation = detailsOption.get().uuid();
            checksum = detailsOption.get().checksum();
        }

        UserAccountEntity currentUser = userIdentity.getUserAccount();

        List<MediaEntity> mediaToSupersede = new ArrayList<>();

        // if we have not found any duplicate audio files to process lets add a new one
        if (duplicateMediaWithDifferentFileSizes.isEmpty()) {
            log.info("No duplicates found. Uploading new file");
        } else {
            mediaToSupersede.addAll(duplicateMediaWithDifferentFileSizes);
            log.info("Duplicate audio file has been found with difference in file size");
        }

        // version the file upload to the database
        versionUpload(mediaToSupersede, addAudioMetadataRequest,
                      externalLocation, checksum, objectRecordStatusEntity, currentUser);

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

    private void versionUpload(List<MediaEntity> mediaToReplace,
                               AddAudioMetadataRequest addAudioMetadataRequest,
                               UUID externalLocation, String checksum,
                               ObjectRecordStatusEntity objectRecordStatusEntity,
                               UserAccountEntity userAccount) {

        MediaEntity newMediaEntity = mapper.mapToMedia(addAudioMetadataRequest, userAccount);
        newMediaEntity.setChecksum(checksum);
        if (mediaToReplace.isEmpty()) {
            log.info("New file uploaded with filename {}", newMediaEntity.getMediaFile());

            mediaRepository.save(newMediaEntity);
            newMediaEntity.setChronicleId(String.valueOf(newMediaEntity.getId()));
        } else {
            MediaEntity oldMediaEntity = mediaToReplace.stream().max(Comparator.comparing(MediaEntity::getCreatedDateTime)).get();
            newMediaEntity.setChronicleId(oldMediaEntity.getChronicleId());
            newMediaEntity.setAntecedentId(String.valueOf(oldMediaEntity.getId()));
            log.info("Uploading new version of duplicate filename {} with antecedent media id {}", newMediaEntity.getMediaFile(),
                     newMediaEntity.getId().toString());
        }
        mediaRepository.save(newMediaEntity);
        log.info("Saved media id {}", newMediaEntity.getId());

        linkAudioToHearingInMetadata(addAudioMetadataRequest, newMediaEntity);
        linkAudioToHearingByEvent(addAudioMetadataRequest, newMediaEntity);

        saveExternalObjectDirectory(
            externalLocation,
            checksum,
            userIdentity.getUserAccount(),
            newMediaEntity,
            objectRecordStatusEntity
        );
        for (MediaEntity mediaEntity : mediaToReplace) {
            deleteMediaLinking(mediaEntity);
        }

        logApi.audioUploaded(addAudioMetadataRequest);
    }

    private List<MediaEntity> filterAudioFilesWithDifferentFileSizes(Collection<MediaEntity> mediaEntities, long expectedFileSize) {
        List<MediaEntity> mediaEntitiesToVersion = new ArrayList<>();
        for (MediaEntity mediaEntity : mediaEntities) {

            // if file size is not expected
            if (expectedFileSize != mediaEntity.getFileSize()) {
                mediaEntitiesToVersion.add(mediaEntity);
            }
        }

        return mediaEntitiesToVersion;
    }

    private List<MediaEntity> filterMediaEntitiesWithIdenticalCaseList(List<String> caseNumbersToLookFor, List<MediaEntity> mediaEntities) {
        ArrayList<MediaEntity> resultList = new ArrayList<>();
        for (MediaEntity mediaEntity : mediaEntities) {
            List<String> mediaCaseNumbers = mediaEntity.getCases().stream().map(CourtCaseEntity::getCaseNumber).toList();
            if (caseNumbersToLookFor.size() == mediaCaseNumbers.size() && CollectionUtils.containsAll(mediaCaseNumbers, caseNumbersToLookFor)) {
                resultList.add(mediaEntity);
            }
        }

        return resultList;
    }


    private List<MediaEntity> getLatestDuplicateMediaFiles(AddAudioMetadataRequest addAudioMetadataRequest) {
        CourtroomEntity courtroomEntity = retrieveCoreObjectService.retrieveOrCreateCourtroom(addAudioMetadataRequest.getCourthouse(),
                                                                                              addAudioMetadataRequest.getCourtroom(),
                                                                                              userIdentity.getUserAccount());
        List<MediaEntity> identicalMediaEntities = mediaRepository.findMediaByDetails(
            courtroomEntity,
            addAudioMetadataRequest.getChannel(),
            addAudioMetadataRequest.getFilename(),
            addAudioMetadataRequest.getStartedAt(),
            addAudioMetadataRequest.getEndedAt());

        // filter on any media entities that relate to the same cases.
        identicalMediaEntities = filterMediaEntitiesWithIdenticalCaseList(addAudioMetadataRequest.getCases(), identicalMediaEntities);

        // now lets get the lowest level media objects so that they can act as a basis for the antecedent
        Tree<MediaEntityTreeNodeImpl> tree = new Tree<>();
        identicalMediaEntities.stream().forEach(entry ->
                                                    tree.addNode(new MediaEntityTreeNodeImpl(entry))
        );
        return tree.getLowestLevelDescendants().stream().map(MediaEntityTreeNodeImpl::getEntity).toList();
    }

    @Override
    public void linkAudioToHearingInMetadata(AddAudioMetadataRequest addAudioMetadataRequest, MediaEntity mediaEntity) {
        List<String> casesInMetadata = addAudioMetadataRequest.getCases();
        for (String caseNumber : casesInMetadata) {
            HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
                addAudioMetadataRequest.getCourthouse(),
                addAudioMetadataRequest.getCourtroom(),
                caseNumber,
                DateConverterUtil.toLocalDateTime(addAudioMetadataRequest.getStartedAt()),
                userIdentity.getUserAccount()
            );

            // add the new media
            hearing.addMedia(mediaEntity);

            hearingRepository.saveAndFlush(hearing);
        }
    }

    private void deleteMediaLinking(MediaEntity mediaEntity) {
        List<HearingEntity> hearingList = mediaEntity.getHearingList();
        for (HearingEntity hearing : hearingList) {
            mediaEntity.removeHearing(hearing);
        }
        mediaRepository.save(mediaEntity);
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

    record AudioBlobUploadDetails(UUID uuid, String checksum) {
    }
}