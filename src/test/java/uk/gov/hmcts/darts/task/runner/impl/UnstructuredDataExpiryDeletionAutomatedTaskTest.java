package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredDataExpiryDeletionAutomatedTaskTest {
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private AnnotationDocumentRepository annotationDocumentRepository;
    @Mock
    private CaseDocumentRepository caseDocumentRepository;
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ExternalInboundDataStoreDeleter inboundDeleter;
    @Mock
    private ExternalUnstructuredDataStoreDeleter unstructuredDeleter;


    private UnstructuredDataExpiryDeletionAutomatedTask unstructuredDataExpiryDeletionAutomatedTask;

    @BeforeEach
    void beforeEach() {
        this.unstructuredDataExpiryDeletionAutomatedTask = spy(
            new UnstructuredDataExpiryDeletionAutomatedTask(null, null, userIdentity, null, null, currentTimeHelper,
                                                            transcriptionDocumentRepository, mediaRepository, annotationDocumentRepository,
                                                            caseDocumentRepository,
                                                            externalObjectDirectoryRepository, inboundDeleter, unstructuredDeleter)
        );
    }

    @Test
    void positiveRunTask() {
        UserAccountEntity userAccount = new UserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime time = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(time);
        doReturn(5).when(unstructuredDataExpiryDeletionAutomatedTask)
            .getAutomatedTaskBatchSize();

        doNothing().when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteTranscriptionDocumentEntity(any(), any(), any());
        doNothing().when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteMediaEntity(any(), any(), any());
        doNothing().when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteAnnotationDocumentEntity(any(), any(), any());
        doNothing().when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteCaseDocumentEntity(any(), any(), any());

        unstructuredDataExpiryDeletionAutomatedTask.runTask();

        Limit limit = Limit.of(5);
        verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteTranscriptionDocumentEntity(userAccount, time, limit);
        verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteMediaEntity(userAccount, time, limit);
        verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteAnnotationDocumentEntity(userAccount, time, limit);
        verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteCaseDocumentEntity(userAccount, time, limit);

        verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .getAutomatedTaskBatchSize();

        verify(userIdentity, times(1)).getUserAccount();
        verify(currentTimeHelper, times(1)).currentOffsetDateTime();
    }


    @Test
    void positiveDeleteTranscriptionDocumentEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            externalObjectDirectoryEntity.setTranscriptionDocumentEntity(new TranscriptionDocumentEntity());
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        List<TranscriptionDocumentEntity> transcriptionDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getTranscriptionDocumentEntity)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredTranscriptionDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());

        unstructuredDataExpiryDeletionAutomatedTask.deleteTranscriptionDocumentEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteFromExternalDataStore(externalObjectDirectoryEntity));

        verify(externalObjectDirectoryRepository, times(1))
            .deleteAll(data);

        verify(transcriptionDocumentRepository, times(1))
            .saveAll(transcriptionDocumentEntities);

        transcriptionDocumentEntities.forEach(transcriptionDocumentEntity -> {
            assertThat(transcriptionDocumentEntity.isDeleted()).isTrue();
            assertThat(transcriptionDocumentEntity.getDeletedBy()).isEqualTo(userAccount);
            assertThat(transcriptionDocumentEntity.getDeletedTs()).isCloseTo(OffsetDateTime.now(), within(4, ChronoUnit.SECONDS));
        });
    }

    @Test
    void positiveDeleteMediaEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            externalObjectDirectoryEntity.setMedia(new MediaEntity());
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        List<MediaEntity> mediaEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getMedia)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredMediaEntries(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());

        unstructuredDataExpiryDeletionAutomatedTask.deleteMediaEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteFromExternalDataStore(externalObjectDirectoryEntity));

        verify(externalObjectDirectoryRepository, times(1))
            .deleteAll(data);

        verify(mediaRepository, times(1))
            .saveAll(mediaEntities);

        mediaEntities.forEach(transcriptionDocumentEntity -> {
            assertThat(transcriptionDocumentEntity.isDeleted()).isTrue();
            assertThat(transcriptionDocumentEntity.getDeletedBy()).isEqualTo(userAccount);
            assertThat(transcriptionDocumentEntity.getDeletedTs()).isCloseTo(OffsetDateTime.now(), within(4, ChronoUnit.SECONDS));
        });
    }

    @Test
    void positiveDeleteAnnotationDocumentEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            externalObjectDirectoryEntity.setAnnotationDocumentEntity(new AnnotationDocumentEntity());
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        List<AnnotationDocumentEntity> annotationDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getAnnotationDocumentEntity)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredAnnotationDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());

        unstructuredDataExpiryDeletionAutomatedTask.deleteAnnotationDocumentEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteFromExternalDataStore(externalObjectDirectoryEntity));

        verify(externalObjectDirectoryRepository, times(1))
            .deleteAll(data);

        verify(annotationDocumentRepository, times(1))
            .saveAll(annotationDocumentEntities);

        annotationDocumentEntities.forEach(transcriptionDocumentEntity -> {
            assertThat(transcriptionDocumentEntity.isDeleted()).isTrue();
            assertThat(transcriptionDocumentEntity.getDeletedBy()).isEqualTo(userAccount);
            assertThat(transcriptionDocumentEntity.getDeletedTs()).isCloseTo(OffsetDateTime.now(), within(4, ChronoUnit.SECONDS));
        });
    }

    @Test
    void positiveDeleteCaseDocumentEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            externalObjectDirectoryEntity.setCaseDocument(new CaseDocumentEntity());
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        List<CaseDocumentEntity> caseDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getCaseDocument)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredCaseDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());

        unstructuredDataExpiryDeletionAutomatedTask.deleteCaseDocumentEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteFromExternalDataStore(externalObjectDirectoryEntity));

        verify(externalObjectDirectoryRepository, times(1))
            .deleteAll(data);

        verify(caseDocumentRepository, times(1))
            .saveAll(caseDocumentEntities);

        caseDocumentEntities.forEach(transcriptionDocumentEntity -> {
            assertThat(transcriptionDocumentEntity.isDeleted()).isTrue();
            assertThat(transcriptionDocumentEntity.getDeletedBy()).isEqualTo(userAccount);
            assertThat(transcriptionDocumentEntity.getDeletedTs()).isCloseTo(OffsetDateTime.now(), within(4, ChronoUnit.SECONDS));
        });
    }

    @Test
    void positiveDeleteExternalObjectDirectoryEntityWithFailedDeletions() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            externalObjectDirectoryEntity.setCaseDocument(new CaseDocumentEntity());
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        final List<CaseDocumentEntity> caseDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getCaseDocument)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredCaseDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(data.get(0));

        doReturn(false).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(data.get(1));

        doReturn(true).when(unstructuredDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(data.get(2));

        unstructuredDataExpiryDeletionAutomatedTask.deleteCaseDocumentEntity(userAccount, maxRetentionDate, batchSize);

        data.forEach(externalObjectDirectoryEntity -> verify(unstructuredDataExpiryDeletionAutomatedTask, times(1))
            .deleteFromExternalDataStore(externalObjectDirectoryEntity));

        verify(externalObjectDirectoryRepository, times(1))
            .deleteAll(List.of(data.get(0), data.get(2)));


        List.of(caseDocumentEntities.get(0), caseDocumentEntities.get(2)).forEach(transcriptionDocumentEntity -> {
            assertThat(transcriptionDocumentEntity.isDeleted()).isTrue();
            assertThat(transcriptionDocumentEntity.getDeletedBy()).isEqualTo(userAccount);
            assertThat(transcriptionDocumentEntity.getDeletedTs()).isCloseTo(OffsetDateTime.now(), within(4, ChronoUnit.SECONDS));
        });
        assertThat(caseDocumentEntities.get(1).isDeleted()).isFalse();
        assertThat(caseDocumentEntities.get(1).getDeletedBy()).isNull();
        assertThat(caseDocumentEntities.get(1).getDeletedTs()).isNull();
    }

    @Test
    void positiveDeleteFromExternalDataStoreInbound() {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(ExternalLocationTypeEnum.INBOUND.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        doReturn(true).when(inboundDeleter).delete(any());

        assertThat(unstructuredDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isTrue();
        verify(inboundDeleter, times(1)).delete(externalObjectDirectoryEntity);
        verifyNoInteractions(unstructuredDeleter);
    }

    @Test
    void positiveDeleteFromExternalDataStoreUnstructured() {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(ExternalLocationTypeEnum.UNSTRUCTURED.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        doReturn(true).when(unstructuredDeleter).delete(any());

        assertThat(unstructuredDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isTrue();
        verify(unstructuredDeleter, times(1)).delete(externalObjectDirectoryEntity);
        verifyNoInteractions(inboundDeleter);

    }

    @Test
    void positiveDeleteFromExternalDataStoreArm() {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(ExternalLocationTypeEnum.ARM.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        assertThat(unstructuredDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isTrue();
        verifyNoInteractions(inboundDeleter, unstructuredDeleter);
    }

    @ParameterizedTest
    @EnumSource(value = ExternalLocationTypeEnum.class, names = {"INBOUND", "UNSTRUCTURED", "ARM"},
        mode = EnumSource.Mode.EXCLUDE)
    void negativeDeleteFromExternalDataStoreUnknown(ExternalLocationTypeEnum externalLocationTypeEnum) {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(externalLocationTypeEnum.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        assertThat(unstructuredDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isFalse();
        verifyNoInteractions(inboundDeleter, unstructuredDeleter);
    }
}
