package uk.gov.hmcts.darts.testutils.stubs;

import jakarta.persistence.EntityManager;
import lombok.Getter;
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
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
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
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.GodClass")
@Getter
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
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    public ExternalObjectDirectoryEntity createAndSaveEod(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum
    ) {
        String uuid = UUID.randomUUID().toString();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        return dartsDatabaseSaveStub.save(eod);
    }

    public ExternalObjectDirectoryEntity createAndSaveEod(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum,
                                                          OffsetDateTime lastModified
    ) {
        String uuid = UUID.randomUUID().toString();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        eod.setLastModifiedDateTime(lastModified);
        return dartsDatabaseSaveStub.save(eod);
    }

    /**
     * Creates an ExternalObjectDirectoryEntity. Passes the created EOD to the client for further customisations before saving
     */
    public ExternalObjectDirectoryEntity createAndSaveEod(MediaEntity media,
                                                          ObjectRecordStatusEnum objectRecordStatusEnum,
                                                          ExternalLocationTypeEnum externalLocationTypeEnum,
                                                          Consumer<ExternalObjectDirectoryEntity> createdEodConsumer) {
        String uuid = UUID.randomUUID().toString();
        var eod = createExternalObjectDirectory(media, objectRecordStatusEnum, externalLocationTypeEnum, uuid);
        createdEodConsumer.accept(eod);
        return dartsDatabaseSaveStub.save(eod);
    }

    public ExternalObjectDirectoryEntity createAndSaveEod(AnnotationDocumentEntity annotationDocument,
                                                          ObjectRecordStatusEnum objectRecordStatus,
                                                          ExternalLocationTypeEnum externalLocationType,
                                                          Consumer<ExternalObjectDirectoryEntity> createdEodConsumer) {
        String uuid = UUID.randomUUID().toString();
        var eod = createExternalObjectDirectory(annotationDocument, getStatus(objectRecordStatus), getLocation(externalLocationType), uuid);
        createdEodConsumer.accept(eod);
        return dartsDatabaseSaveStub.save(eod);
    }

    public ExternalObjectDirectoryEntity createAndSaveEod(AnnotationDocumentEntity annotationDocument,
                                                          TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                          ObjectRecordStatusEnum objectRecordStatus,
                                                          ExternalLocationTypeEnum externalLocationType,
                                                          Consumer<ExternalObjectDirectoryEntity> createdEodConsumer) {
        String uuid = UUID.randomUUID().toString();
        var eod = createExternalObjectDirectory(annotationDocument, getStatus(objectRecordStatus), getLocation(externalLocationType), uuid);

        if (transcriptionDocumentEntity != null) {
            eod.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        }
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
                                                                       String externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        externalObjectDirectory.setMedia(mediaEntity);
        externalObjectDirectory = eodRepository.save(externalObjectDirectory);
        eodRepository.flush();

        return externalObjectDirectory;
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(CaseDocumentEntity caseDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       String externalLocation) {
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
                                                                       String externalLocation) {

        return createExternalObjectDirectory(media, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(CaseDocumentEntity caseDocumentEntity,
                                                                       ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                       ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                       String externalLocation) {

        ExternalObjectDirectoryEntity externalObjectDirectory = createExternalObjectDirectory(
            caseDocumentEntity, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);

        externalObjectDirectory.setCaseDocument(caseDocumentEntity);

        return externalObjectDirectory;
    }


    public ExternalObjectDirectoryEntity createExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                                                       ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                       ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                       String externalLocation) {

        ExternalObjectDirectoryEntity externalObjectDirectory = createExternalObjectDirectory(
            annotationDocumentEntity, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);

        externalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);

        return eodRepository.save(externalObjectDirectory);
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(AnnotationDocumentEntity annotationDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       String externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = createMinimalExternalObjectDirectory(
            objectRecordStatusEntity,
            externalLocationTypeEntity,
            externalLocation
        );

        if (annotationStub != null) {
            externalObjectDirectory.setAnnotationDocumentEntity(annotationDocumentEntity);
        }
        return externalObjectDirectory;
    }


    public ExternalObjectDirectoryEntity createExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                       ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                       ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                       String externalLocation) {

        ExternalObjectDirectoryEntity externalObjectDirectory = createExternalObjectDirectory(
            transcriptionDocumentEntity, getStatus(objectRecordStatusEnum), getLocation(externalLocationTypeEnum), externalLocation);

        externalObjectDirectory.setTranscriptionDocumentEntity(transcriptionDocumentEntity);

        return eodRepository.save(externalObjectDirectory);
    }

    public ExternalObjectDirectoryEntity createExternalObjectDirectory(TranscriptionDocumentEntity transcriptionDocumentEntity,
                                                                       ObjectRecordStatusEntity objectRecordStatusEntity,
                                                                       ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                       String externalLocation) {
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
            UUID.randomUUID().toString()
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
                                                                               String externalLocation) {
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

    public ExternalLocationTypeEntity getLocation(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    public ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum objectRecordStatusEnum) {
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
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndTranscriptionOrAnnotationAndLocation(
        ExternalLocationTypeEnum externalLocationTypeEnum, ObjectRecordStatusEnum objectRecordStatusEnum,
        int numberOfObjectDirectory, Optional<OffsetDateTime> dateToSet)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        Random randomiseAnnotationOrTranscription = new Random();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            var user = userAccountStub.getIntegrationTestUserAccountEntity();
            int random = randomiseAnnotationOrTranscription.nextInt(10);

            AnnotationEntity annotationEntity;
            AnnotationDocumentEntity annotationDocumentEntity = null;
            TranscriptionDocumentEntity transcriptionDocumentEntity = null;
            if (random % 2 == 0) {
                annotationEntity = annotationStub.createAndSaveAnnotationEntityWith(user, "test annotation");
                annotationDocumentEntity = annotationStub.createAndSaveAnnotationDocumentEntity(
                    userAccountStub, annotationEntity);
            } else {
                transcriptionDocumentEntity
                    = transcriptionDocumentStub.createTranscriptionDocumentForTranscription(userAccountStub, dartsDatabaseComposable,
                                                                                            transcriptionStubComposable, courthouseStubComposable, user);
            }

            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(
                annotationDocumentEntity, transcriptionDocumentEntity, objectRecordStatusEnum, externalLocationTypeEnum, e -> {
                });

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(externalObjectDirectory, dateToSet.get());
            }

            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndAnnotationAndLocation(
        ExternalLocationTypeEnum externalLocationTypeEnum, ObjectRecordStatusEnum objectRecordStatusEnum,
        int numberOfObjectDirectory, Optional<OffsetDateTime> dateToSet)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            var user = userAccountStub.getIntegrationTestUserAccountEntity();

            AnnotationEntity annotationEntity = annotationStub.createAndSaveAnnotationEntityWith(user, "test annotation");
            AnnotationDocumentEntity annotationDocumentEntity = annotationStub.createAndSaveAnnotationDocumentEntity(
                userAccountStub, annotationEntity);

            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(
                annotationDocumentEntity, null, objectRecordStatusEnum, externalLocationTypeEnum, e -> {
                });

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(externalObjectDirectory, dateToSet.get());
            }

            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndTranscriptionAndLocation(
        ExternalLocationTypeEnum externalLocationTypeEnum, ObjectRecordStatusEnum objectRecordStatusEnum,
        int numberOfObjectDirectory, Optional<OffsetDateTime> dateToSet)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            var user = userAccountStub.getIntegrationTestUserAccountEntity();

            TranscriptionDocumentEntity transcriptionDocumentEntity
                = transcriptionDocumentStub.createTranscriptionDocumentForTranscription(userAccountStub, dartsDatabaseComposable,
                                                                                        transcriptionStubComposable, courthouseStubComposable, user);

            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(
                null, transcriptionDocumentEntity, objectRecordStatusEnum, externalLocationTypeEnum, e -> {
                });

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(externalObjectDirectory, dateToSet.get());
            }

            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndMediaLocation(ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                                  ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                  int numberOfObjectDirectory, Optional<OffsetDateTime> dateToSet)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (int i = 0; i < numberOfObjectDirectory; i++) {
            MediaEntity media = mediaStub.createAndSaveMedia(courthouseStubComposable, courtroomStubComposable);
            ExternalObjectDirectoryEntity externalObjectDirectory = createAndSaveEod(media, objectRecordStatusEnum, externalLocationTypeEnum);

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(externalObjectDirectory, dateToSet.get());
            }

            entityListResult.add(externalObjectDirectory);
        }

        return entityListResult;
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndMediaAndArmLocation(List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
                                                                                        Optional<OffsetDateTime> dateToSet)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {
            ExternalObjectDirectoryEntity newExternalObjectDirectory = createAndSaveEod(externalObjectDirectory.getMedia(), STORED, ARM, OffsetDateTime.now());

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(newExternalObjectDirectory, dateToSet.get());
            }

            newExternalObjectDirectory = eodRepository.getReferenceById(newExternalObjectDirectory.getId());
            entityListResult.add(newExternalObjectDirectory);
        }
        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndTranscriptionAndAnnotationAndLocation(
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
        Optional<OffsetDateTime> dateToSet, ExternalLocationTypeEnum externalLocationType)

        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {

            ExternalObjectDirectoryEntity newExternalObjectDirectory
                = createAndSaveEod(externalObjectDirectory.getAnnotationDocumentEntity(),
                                   externalObjectDirectory.getTranscriptionDocumentEntity(), STORED, externalLocationType, e -> {
                });

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(newExternalObjectDirectory, dateToSet.get());
            }

            newExternalObjectDirectory = eodRepository.getReferenceById(newExternalObjectDirectory.getId());

            entityListResult.add(newExternalObjectDirectory);
        }
        return entityListResult;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities,
        Optional<OffsetDateTime> dateToSet)

        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<ExternalObjectDirectoryEntity> entityListResult = new ArrayList<>();

        for (ExternalObjectDirectoryEntity externalObjectDirectory : externalObjectDirectoryEntities) {

            ExternalObjectDirectoryEntity newExternalObjectDirectory
                = createAndSaveEod(externalObjectDirectory.getAnnotationDocumentEntity(),
                                   externalObjectDirectory.getTranscriptionDocumentEntity(), STORED, ARM, e -> {
                });

            if (dateToSet.isPresent()) {
                dateConfigurer.setLastModifiedDate(newExternalObjectDirectory, dateToSet.get());
            }

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
                || !List.of(SystemUsersEnum.INBOUND_AUDIO_DELETER_AUTOMATED_TASK.getId(),
                            SystemUsersEnum.INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK.getId()).contains(
                objectDirectoryEntity.getLastModifiedBy().getId())) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public boolean areObjectDirectoriesMarkedForDeletionWithSystemUser(List<Integer> entities) {
        for (Integer entity : entities) {
            ExternalObjectDirectoryEntity objectDirectoryEntity = eodRepository.getReferenceById(entity);

            if (!ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId().equals(objectDirectoryEntity.getStatus().getId())
                || SystemUsersEnum.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK.getId() != objectDirectoryEntity.getLastModifiedBy().getId()) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public boolean areObjectDirectoriesMarkedForDeletionWithUser(List<Integer> entities, String userEmail) {
        for (Integer entity : entities) {
            ExternalObjectDirectoryEntity objectDirectoryEntity = eodRepository.getReferenceById(entity);

            if (!ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId().equals(objectDirectoryEntity.getStatus().getId())
                || SystemUsersEnum.UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK.getId() != objectDirectoryEntity.getLastModifiedBy().getId()) {
                return false;
            }
            if (!objectDirectoryEntity.getLastModifiedBy().getEmailAddress().equals(userEmail)) {
                return false;
            }
        }

        return true;
    }

    public List<ExternalObjectDirectoryEntity> findAllFor(CaseDocumentEntity caseDocumentEntity) {
        return eodRepository.findByCaseDocument(caseDocumentEntity);
    }

    public List<ExternalObjectDirectoryEntity> findAllFor(AnnotationDocumentEntity annotationDocument) {
        return eodRepository.findByAnnotationDocumentEntity(annotationDocument);
    }

    public List<ExternalObjectDirectoryEntity> findAllFor(MediaEntity mediaEntity) {
        return eodRepository.findByMedia(mediaEntity);
    }

    public List<ExternalObjectDirectoryEntity> findAllFor(TranscriptionDocumentEntity transcriptionDocumentEntity) {
        return eodRepository.findByTranscriptionDocumentEntity(transcriptionDocumentEntity);
    }
}