package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.testutils.DatabaseDateSetter;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.helper.SystemUserHelper.HOUSEKEEPING;

@Component
@RequiredArgsConstructor
public class ExternalObjectDirectoryStub {

    private final UserAccountStub userAccountStub;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalObjectDirectoryRepository eodRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final MediaStubComposable mediaStub;
    private final CourthouseStubComposable courthouseStubComposable;
    private final CourtroomStubComposable courtroomStubComposable;
    private final EntityManager em;
    private final DatabaseDateSetter dateConfigurer;
    private final SystemUserHelper systemUserHelper;
    private final CurrentTimeHelper currentTimeHelper;

    public ExternalObjectDirectoryEntity createAndSaveEod(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum
    ) {
        UUID uuid = UUID.randomUUID();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        return eodRepository.save(eod);
    }

    public ExternalObjectDirectoryEntity createAndSaveEod(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum,
                                                          OffsetDateTime lastModified
    ) {
        UUID uuid = UUID.randomUUID();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        eod.setLastModifiedDateTime(lastModified);
        return eodRepository.save(eod);
    }

    /**
     * Creates an ExternalObjectDirectoryEntity. Passes the created EOD to the client for further customisations before saving
     */
    public ExternalObjectDirectoryEntity createAndSaveEod(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum,
                                                          Consumer<ExternalObjectDirectoryEntity> createdEodConsumer) {
        UUID uuid = UUID.randomUUID();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        createdEodConsumer.accept(eod);
        return eodRepository.save(eod);
    }

    public ExternalObjectDirectoryEntity createAndSaveEod(AnnotationDocumentEntity annotationDocument,
                                                          ObjectRecordStatusEnum objectRecordStatus,
                                                          ExternalLocationTypeEnum externalLocationType,
                                                          Consumer<ExternalObjectDirectoryEntity> createdEodConsumer) {
        UUID uuid = UUID.randomUUID();
        var eod = createExternalObjectDirectory(annotationDocument, getStatus(objectRecordStatus), getLocation(externalLocationType), uuid);
        createdEodConsumer.accept(eod);
        return eodRepository.save(eod);
    }

    /**
     * Creates an ExternalObjectDirectoryEntity.
     *
     * @deprecated Use
     *     {@link ExternalObjectDirectoryStub#createExternalObjectDirectory(MediaEntity, ObjectRecordStatusEnum, ExternalLocationTypeEnum, UUID)} instead.
     */
    @Deprecated
    public ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity mediaEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setMedia(mediaEntity);
        eodRepository.save(externalObjectDirectory);
        eodRepository.flush();

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(CaseDocumentEntity caseDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setCaseDocument(caseDocumentEntity);
        eodRepository.save(externalObjectDirectory);
        eodRepository.flush();

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(MediaEntity media,
                                                                       ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                       ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                       UUID externalLocation) {

        return createExternalObjectDirectory(media, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(CaseDocumentEntity caseDocumentEntity,
                                                                       ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                       ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                       UUID externalLocation) {

        ExternalObjectDirectoryEntity externalObjectDirectory = createExternalObjectDirectory(
            caseDocumentEntity, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);

        externalObjectDirectory.setCaseDocument(caseDocumentEntity);

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       UUID externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setTranscriptionDocumentEntity(transcriptionDocumentEntity);

        return externalObjectDirectory;
    }

    @Transactional
    public ExternalObjectDirectoryEntity createAndSaveExternalObjectDirectory(Integer transcriptionDocumentId,
                                                                              ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                              ExternalLocationTypeEntity externalLocationTypeEntity) {
        TranscriptionDocumentEntity transcriptionDocument = transcriptionDocumentRepository.findById(transcriptionDocumentId).orElseThrow();
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            UUID.randomUUID()
        );

        externalObjectDirectory.setTranscriptionDocumentEntity(transcriptionDocument);

        return eodRepository.saveAndFlush(externalObjectDirectory);
    }

    public List<ExternalObjectDirectoryEntity> findByMediaStatusAndType(MediaEntity media,
                                                                        ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                        ExternalLocationTypeEnum externalLocationTypeEnum) {

        return eodRepository.findByMediaStatusAndType(media, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum));
    }

    private ExternalObjectDirectoryEntity createMinimalExternalObjectDirectory(ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                               ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                               UUID externalLocation) {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setStatus(objectRecordStatusEntity);
        externalObjectDirectory.setExternalLocationType(externalLocationTypeEntity);
        externalObjectDirectory.setExternalLocation(externalLocation);
        externalObjectDirectory.setChecksum(null);
        externalObjectDirectory.setTransferAttempts(1);
        externalObjectDirectory.setVerificationAttempts(1);

        var user = userAccountStub.getIntegrationTestUserAccountEntity();
        externalObjectDirectory.setCreatedBy(user);
        externalObjectDirectory.setLastModifiedBy(user);
        return externalObjectDirectory;
    }

    private ExternalLocationTypeEntity getLocation(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    private ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum objectRecordStatusEnum) {
        return objectRecordStatusRepository.getReferenceById(objectRecordStatusEnum.getId());
    }

    public ExternalObjectDirectoryEntity createEodWithRandomValues() {
        EasyRandomParameters parameters = new EasyRandomParameters()
            .randomize(Integer.class, new IntegerRangeRandomizer(1, 100))
            .collectionSizeRange(1, 1)
            .overrideDefaultInitialization(true);

        EasyRandom generator = new EasyRandom(parameters);
        return generator.nextObject(ExternalObjectDirectoryEntity.class);
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndInboundLocation(ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                     int numberOfObjectDirectory) {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            MediaEntity media = mediaStub.createAndSaveMedia(courthouseStubComposable, courtroomStubComposable);
            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(media, objectRecordStatusEnum, INBOUND);
            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndUnstructuredLocation(ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                    int numberOfObjectDirectory) {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            MediaEntity media = mediaStub.createAndSaveMedia(courthouseStubComposable, courtroomStubComposable);
            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(media, objectRecordStatusEnum, UNSTRUCTURED);
            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndMediaAndArmLocation(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
                                                                                         int hoursBehindCurrentTime)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            hoursBehindCurrentTime,
            ChronoUnit.HOURS
        );

        return generateWithStatusAndMediaAndArmLocation(externalObjectDirectoryEntities, lastModifiedBefore);
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndMediaAndArmLocation(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
                                                                                        OffsetDateTime lastModifiedDate)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {
            ExternalObjectDirectoryEntity newExternalObjectDirectory = createAndSaveEod(externalObjectDirectory.getMedia(), STORED, ARM, OffsetDateTime.now());

            dateConfigurer.setLastModifiedDate(newExternalObjectDirectory, lastModifiedDate);

            newExternalObjectDirectory = eodRepository.getReferenceById(newExternalObjectDirectory.getId());
            entityListResult.add(newExternalObjectDirectory);
        }
        return entityListResult;
    }

    @Transactional
    public List<Integer> updateExternalDirectoryWithMarkedForDeletionUsingHouseKeeperUser(List<Integer> eodIds) {
        ObjectRecordStatusEntity status = objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
        UserAccountEntity entity = systemUserHelper.getHousekeepingUser();
        eodRepository.updateStatusAndUserOfObjectDirectory(eodIds, status, entity);
        List<Integer> externalObjectDirectoryEntities = new ArrayList<>();
        for (Integer eodId : eodIds) {
            externalObjectDirectoryEntities.add(eodRepository.getReferenceById(eodId).getId());
        }

        return externalObjectDirectoryEntities;
    }

    @Transactional
    public void checkNotMarkedForDeletion(List<ExternalObjectDirectoryEntity> checkUnchangedEntities) {
        for (ExternalObjectDirectoryEntity checkUnchangedEntity : checkUnchangedEntities) {
            ExternalObjectDirectoryEntity objectDirectoryEntity = eodRepository.getReferenceById(checkUnchangedEntity.getId());

            Assertions.assertNotEquals(objectDirectoryEntity.getStatus(),
                                       ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId().equals(objectDirectoryEntity.getStatus().getId()));
        }
    }

    @Transactional
    public boolean areObjectDirectoriesMarkedForDeletionWithHousekeeper(List<Integer> entities) {
        for (Integer entity : entities) {
            ExternalObjectDirectoryEntity objectDirectoryEntity = eodRepository.getReferenceById(entity);

            if (!ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId().equals(objectDirectoryEntity.getStatus().getId())
                || !systemUserHelper.findSystemUserGuid(HOUSEKEEPING).equals(objectDirectoryEntity.getLastModifiedBy().getAccountGuid())) {
                return false;
            }
        }

        return true;
    }
}