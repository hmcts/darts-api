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
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.testutils.DatabaseDateSetter;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.helper.SystemUserHelper.HOUSEKEEPING;

@Component
@RequiredArgsConstructor
public class ExternalObjectDirectoryStub {

    private final UserAccountStubComposable userAccountStub;
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
    private final AnnotationStubComposable annotationStub;
    private final TranscriptionDocumentStubComposable transcriptionDocumentStub;
    private final DartsDatabaseComposable dartsDatabaseComposable;
    private final TranscriptionStubComposable transcriptionStubComposable;

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

    public ExternalObjectDirectoryEntity createAndSaveEod(AnnotationDocumentEntity annotationDocument,
                                                          TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                          ObjectRecordStatusEnum objectRecordStatus,
                                                          ExternalLocationTypeEnum externalLocationType,
                                                          Consumer<ExternalObjectDirectoryEntity> createdEodConsumer) {
        UUID uuid = UUID.randomUUID();
        var eod = createExternalObjectDirectory(annotationDocument, getStatus(objectRecordStatus), getLocation(externalLocationType), uuid);
        eod.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        createdEodConsumer.accept(eod);
        eodRepository.save(eod);
        eodRepository.flush();
        return eod;
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
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                     int numberOfObjectDirectory) {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            var user = userAccountStub.getIntegrationTestUserAccountEntity();
            AnnotationEntity annotationEntity = annotationStub.createAndSaveAnnotationEntityWith(user, "test annotation");
            TranscriptionDocumentEntity transcriptionDocumentEntity
                = transcriptionDocumentStub.createTranscriptionDocumentForTranscription(userAccountStub, dartsDatabaseComposable,
                                                                                        transcriptionStubComposable, courthouseStubComposable, user);

            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(
                annotationStub.createAndSaveAnnotationDocumentEntity(
                    userAccountStub, annotationEntity), transcriptionDocumentEntity, objectRecordStatusEnum, INBOUND, e -> { });
            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndMediaAndInboundLocation(ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                    int numberOfObjectDirectory) {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            MediaEntity media = mediaStub.createAndSaveMedia(courthouseStubComposable, courtroomStubComposable);
            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(media, objectRecordStatusEnum, INBOUND);
            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndMediaAndArmLocation(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
                                                                                        int hoursBehindCurrentTime)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {
            ExternalObjectDirectoryEntity newExternalObjectDirectory = createAndSaveEod(externalObjectDirectory.getMedia(), STORED, ARM, OffsetDateTime.now());

            dateConfigurer.setLastModifiedDate(newExternalObjectDirectory, OffsetDateTime.now().minusHours(hoursBehindCurrentTime));

            newExternalObjectDirectory = eodRepository.getReferenceById(newExternalObjectDirectory.getId());
            entityListResult.add(newExternalObjectDirectory);
        }
        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity>
    generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
                                                                                         int hoursBehindCurrentTime)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {

            ExternalObjectDirectoryEntity newExternalObjectDirectory
                = createAndSaveEod(externalObjectDirectory.getAnnotationDocumentEntity(),
                                    externalObjectDirectory.getTranscriptionDocumentEntity(), STORED, ARM, e -> { });

            dateConfigurer.setLastModifiedDate(newExternalObjectDirectory, OffsetDateTime.now().minusHours(hoursBehindCurrentTime));

            newExternalObjectDirectory = eodRepository.getReferenceById(newExternalObjectDirectory.getId());

            entityListResult.add(newExternalObjectDirectory);
        }
        return entityListResult;
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