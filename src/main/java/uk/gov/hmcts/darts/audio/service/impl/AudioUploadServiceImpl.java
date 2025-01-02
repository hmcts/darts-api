package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.audio.component.AddAudioRequestMapper;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.audio.service.AudioUploadService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
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
    private final LogApi logApi;
    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private final AudioAsyncService audioAsyncService;


    @Override
    public void addAudio(MultipartFile audioMultipartFile, AddAudioMetadataRequest addAudioMetadataRequest) {
        String incomingChecksum;
        try {
            incomingChecksum = fileContentChecksum.calculate(audioMultipartFile.getInputStream());
        } catch (IOException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, "Failed to compute incoming checksum", e);
        }
        //No need to delete guid on duplicate as the file is never uploaded to the blob store unless the duplicate check has passed in this flow
        addAudio(incomingChecksum, () -> saveAudioToInbound(audioMultipartFile), addAudioMetadataRequest, false);
    }

    @Override
    public void addAudio(UUID guid, AddAudioMetadataRequest addAudioMetadataRequest) {
        String checksum = dataManagementApi.getChecksum(DatastoreContainerType.INBOUND, guid);
        if (!checksum.equals(addAudioMetadataRequest.getChecksum())) {
            throw new DartsApiException(AudioApiError.FAILED_TO_ADD_AUDIO_META_DATA,
                                        String.format("Checksum for blob '%s' does not match the one passed in the API request '%s'.",
                                                      checksum, addAudioMetadataRequest.getChecksum()));
        }
        addAudio(checksum, () -> guid, addAudioMetadataRequest, true);
    }

    private void addAudio(String incomingChecksum,
                          Supplier<UUID> externalLocationSupplier,
                          AddAudioMetadataRequest addAudioMetadataRequest,
                          boolean deleteGuidOnDuplicate) {
        log.info("Adding audio using metadata {}", addAudioMetadataRequest.toString());

        //remove duplicate cases as they can appear more than once, e.g. if they broke for lunch.
        List<String> distinctCaseList = addAudioMetadataRequest.getCases().stream().distinct().toList();
        addAudioMetadataRequest.setCases(distinctCaseList);

        List<MediaEntity> duplicatesToBeSuperseded = getLatestDuplicateMediaFiles(addAudioMetadataRequest);

        List<MediaEntity> duplicatesWithDifferentChecksum = filterForMediaWithMismatchingChecksum(duplicatesToBeSuperseded, incomingChecksum);

        if (isNotEmpty(duplicatesToBeSuperseded) && isEmpty(duplicatesWithDifferentChecksum)) {
            if (deleteGuidOnDuplicate) {
                UUID uuid = externalLocationSupplier.get();
                try {
                    dataManagementApi.deleteBlobDataFromInboundContainer(uuid);
                } catch (AzureDeleteBlobException e) {
                    log.error("Failed to delete blob from inbound container with guid: ", uuid, e);
                }
            }
            if (log.isInfoEnabled()) {
                log.info("Exact duplicate detected based upon media metadata and checksum for media entity ids {}. Returning 200 with no changes.",
                         duplicatesToBeSuperseded.stream().map(MediaEntity::getId).toList());
            }
            return;
        }

        // upload to the blob store
        ObjectRecordStatusEntity objectRecordStatusEntity = objectRecordStatusRepository.getReferenceById(STORED.getId());
        UUID externalLocation = externalLocationSupplier.get();

        UserAccountEntity currentUser = userIdentity.getUserAccount();

        List<MediaEntity> mediaToSupersede = new ArrayList<>();

        // if we have not found any duplicate audio files to process lets add a new one
        if (isNotEmpty(duplicatesWithDifferentChecksum)) {
            mediaToSupersede.addAll(duplicatesWithDifferentChecksum);
            if (log.isInfoEnabled()) {
                log.info("Duplicate audio file has been found with difference in checksum for guid {} latest checksum {}. But found {}",
                         externalLocation, incomingChecksum,
                         duplicatesWithDifferentChecksum
                             .stream()
                             .map(mediaEntity -> "Meida Id " + mediaEntity.getId().toString() + " with checksum " + mediaEntity.getChecksum())
                             .toList());
            }
        }

        // version the file upload to the database
        versionUpload(mediaToSupersede, addAudioMetadataRequest,
                      externalLocation, incomingChecksum, objectRecordStatusEntity, currentUser);
    }

    private UUID saveAudioToInbound(MultipartFile audioFileStream) {
        try (var bufferedInputStream = new BufferedInputStream(audioFileStream.getInputStream())) {
            return dataManagementApi.saveBlobDataToInboundContainer(bufferedInputStream);
        } catch (IOException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
        }
    }

    private void versionUpload(List<MediaEntity> mediaToSupersede,
                               AddAudioMetadataRequest addAudioMetadataRequest,
                               UUID externalLocation, String checksum,
                               ObjectRecordStatusEntity objectRecordStatusEntity,
                               UserAccountEntity userAccount) {

        MediaEntity newMediaEntity = mapper.mapToMedia(addAudioMetadataRequest, userAccount);
        newMediaEntity.setChecksum(checksum);
        newMediaEntity.setIsCurrent(true);
        if (mediaToSupersede.isEmpty()) {
            mediaRepository.save(newMediaEntity);
            newMediaEntity.setChronicleId(String.valueOf(newMediaEntity.getId()));
            log.info("First version of media added with filename {}", newMediaEntity.getMediaFile());
        } else {
            MediaEntity oldMediaEntity = mediaToSupersede.stream().max(Comparator.comparing(MediaEntity::getCreatedDateTime)).get();
            newMediaEntity.setChronicleId(oldMediaEntity.getChronicleId());
            newMediaEntity.setAntecedentId(String.valueOf(oldMediaEntity.getId()));
            log.info("Revised version of media added with filename {} and antecedent media id {}", newMediaEntity.getMediaFile(),
                     newMediaEntity.getId().toString());
        }
        mediaRepository.save(newMediaEntity);
        log.info("Saved media id {}", newMediaEntity.getId());

        linkAudioToHearingInMetadata(addAudioMetadataRequest, newMediaEntity);
        audioAsyncService.linkAudioToHearingByEvent(addAudioMetadataRequest, newMediaEntity, userAccount);

        saveExternalObjectDirectory(
            externalLocation,
            checksum,
            userIdentity.getUserAccount(),
            newMediaEntity,
            objectRecordStatusEntity
        );
        for (MediaEntity mediaEntity : mediaToSupersede) {
            deleteMediaLinkingAndSetCurrentFalse(mediaEntity);
        }

        logApi.audioUploaded(addAudioMetadataRequest);
    }

    private List<MediaEntity> filterForMediaWithMismatchingChecksum(List<MediaEntity> mediaEntities, String checksum) {
        return mediaEntities.stream()
            .filter(mediaEntity -> !checksum.equals(mediaEntity.getChecksum()))
            .toList();
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
            hearing.setHearingIsActual(true);

            hearingRepository.saveAndFlush(hearing);
        }
    }

    private void deleteMediaLinkingAndSetCurrentFalse(MediaEntity mediaEntity) {
        List<HearingEntity> hearingList = mediaEntity.getHearingList();
        for (HearingEntity hearing : hearingList) {
            mediaEntity.removeHearing(hearing);
        }
        mediaEntity.setIsCurrent(false);
        mediaRepository.save(mediaEntity);
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

}