package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
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
import uk.gov.hmcts.darts.task.config.AssociatedObjectDataExpiryDeletionAutomatedTaskConfig;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssociatedObjectDataExpiryDeletionAutomatedTaskTest {
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
    @Mock
    private AuditApi auditApi;
    @Mock
    private AssociatedObjectDataExpiryDeletionAutomatedTaskConfig config;
    @Mock
    private TransactionTemplate transactionTemplate;

    private final AtomicInteger idAddition = new AtomicInteger(123);


    private AssociatedObjectDataExpiryDeletionAutomatedTask associatedObjectDataExpiryDeletionAutomatedTask;

    @BeforeEach
    void beforeEach() {
        this.associatedObjectDataExpiryDeletionAutomatedTask = spy(
            new AssociatedObjectDataExpiryDeletionAutomatedTask(
                null, config, userIdentity, null, null, currentTimeHelper,
                transcriptionDocumentRepository, mediaRepository, annotationDocumentRepository,
                caseDocumentRepository,
                externalObjectDirectoryRepository, inboundDeleter, unstructuredDeleter,
                auditApi, 100, transactionTemplate)
        );
        lenient().doCallRealMethod().when(transactionTemplate).executeWithoutResult(any());
        lenient().when(transactionTemplate.execute(any()))
            .thenAnswer(invocation -> {
                TransactionCallback transactionCallback = invocation.getArgument(0);
                transactionCallback.doInTransaction(null);
                return null;
            });
    }

    @Test
    void positiveRunTask() {
        Duration duration = Duration.ofHours(24);
        when(config.getBufferDuration()).thenReturn(duration);

        UserAccountEntity userAccount = new UserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime time = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(time);
        doReturn(5).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .getAutomatedTaskBatchSize();
        OffsetDateTime maxRetentionDate = time.minus(duration);
        doNothing().when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteTranscriptionDocumentEntity(any(), any(), any());
        doNothing().when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteMediaEntity(any(), any(), any());
        doNothing().when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteAnnotationDocumentEntity(any(), any(), any());
        doNothing().when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteCaseDocumentEntity(any(), any(), any());

        associatedObjectDataExpiryDeletionAutomatedTask.runTask();

        Limit limit = Limit.of(5);
        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteTranscriptionDocumentEntity(userAccount, maxRetentionDate, limit);
        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteMediaEntity(userAccount, maxRetentionDate, limit);
        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteAnnotationDocumentEntity(userAccount, maxRetentionDate, limit);
        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteCaseDocumentEntity(userAccount, maxRetentionDate, limit);

        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .getAutomatedTaskBatchSize();

        verify(userIdentity).getUserAccount();
        verify(currentTimeHelper).currentOffsetDateTime();
    }


    @Test
    void positiveDeleteTranscriptionDocumentEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
            transcriptionDocumentEntity.setId(getNextId());
            externalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        final List<TranscriptionDocumentEntity> transcriptionDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getTranscriptionDocumentEntity)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredTranscriptionDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());
        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(any());

        associatedObjectDataExpiryDeletionAutomatedTask.deleteTranscriptionDocumentEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> {
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .deleteFromExternalDataStore(externalObjectDirectoryEntity);
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .shouldDeleteFilter(externalObjectDirectoryEntity.getTranscriptionDocumentEntity());
        });

        verify(externalObjectDirectoryRepository)
            .deleteAll(data);

        verify(transcriptionDocumentRepository)
            .softDeleteAll(transcriptionDocumentEntities, userAccount);

        verify(auditApi)
            .record(AuditActivity.TRANSCRIPT_EXPIRED, userAccount, "123");
    }

    @Test
    void positiveDeleteMediaEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            MediaEntity mediaEntity = new MediaEntity();
            mediaEntity.setId(getNextId());
            externalObjectDirectoryEntity.setMedia(mediaEntity);
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        final List<MediaEntity> mediaEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getMedia)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredMediaEntries(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());
        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(any());

        associatedObjectDataExpiryDeletionAutomatedTask.deleteMediaEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> {
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .deleteFromExternalDataStore(externalObjectDirectoryEntity);
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .shouldDeleteFilter(externalObjectDirectoryEntity.getMedia());
        });

        verify(externalObjectDirectoryRepository)
            .deleteAll(data);

        verify(mediaRepository)
            .softDeleteAll(mediaEntities, userAccount);

        verify(auditApi)
            .record(AuditActivity.AUDIO_EXPIRED, userAccount, "123");
    }

    private Integer getNextId() {
        return idAddition.getAndIncrement();
    }

    @Test
    void positiveDeleteAnnotationDocumentEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            AnnotationDocumentEntity annotationDocumentEntity = new AnnotationDocumentEntity();
            annotationDocumentEntity.setId(getNextId());
            externalObjectDirectoryEntity.setAnnotationDocumentEntity(annotationDocumentEntity);
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        final List<AnnotationDocumentEntity> annotationDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getAnnotationDocumentEntity)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredAnnotationDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());
        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(any());

        associatedObjectDataExpiryDeletionAutomatedTask.deleteAnnotationDocumentEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> {
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .deleteFromExternalDataStore(externalObjectDirectoryEntity);
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .shouldDeleteFilter(externalObjectDirectoryEntity.getAnnotationDocumentEntity());
        });

        verify(externalObjectDirectoryRepository)
            .deleteAll(data);

        verify(annotationDocumentRepository)
            .softDeleteAll(annotationDocumentEntities, userAccount);

        verify(auditApi)
            .record(AuditActivity.ANNOTATION_EXPIRED, userAccount, "123");
    }

    @Test
    void positiveDeleteCaseDocumentEntity() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            CaseDocumentEntity caseDocumentEntity = new CaseDocumentEntity();
            caseDocumentEntity.setId(getNextId());
            externalObjectDirectoryEntity.setCaseDocument(caseDocumentEntity);
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        final List<CaseDocumentEntity> caseDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getCaseDocument)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredCaseDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(any());
        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(any());

        associatedObjectDataExpiryDeletionAutomatedTask.deleteCaseDocumentEntity(userAccount, maxRetentionDate, batchSize);


        data.forEach(externalObjectDirectoryEntity -> {
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .deleteFromExternalDataStore(externalObjectDirectoryEntity);
            verify(associatedObjectDataExpiryDeletionAutomatedTask)
                .shouldDeleteFilter(externalObjectDirectoryEntity.getCaseDocument());
        });

        verify(externalObjectDirectoryRepository)
            .deleteAll(data);

        verify(caseDocumentRepository)
            .softDeleteAll(caseDocumentEntities, userAccount);

        verify(auditApi)
            .record(AuditActivity.CASE_DOCUMENT_EXPIRED, userAccount, "123");
    }

    @Test
    void positiveDeleteExternalObjectDirectoryEntityWithFailedDeletions() {
        UserAccountEntity userAccount = new UserAccountEntity();
        OffsetDateTime maxRetentionDate = OffsetDateTime.now();
        Limit batchSize = Limit.of(5);

        Supplier<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitySupplier = () -> {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
            CaseDocumentEntity caseDocumentEntity = new CaseDocumentEntity();
            caseDocumentEntity.setId(getNextId());
            externalObjectDirectoryEntity.setCaseDocument(caseDocumentEntity);
            return externalObjectDirectoryEntity;
        };

        List<ExternalObjectDirectoryEntity> data
            = List.of(externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get(), externalObjectDirectoryEntitySupplier.get());


        final List<CaseDocumentEntity> caseDocumentEntities = data.stream()
            .map(ExternalObjectDirectoryEntity::getCaseDocument)
            .toList();

        when(externalObjectDirectoryRepository.findExpiredCaseDocuments(
            maxRetentionDate, batchSize)).thenReturn(data);


        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(data.get(0));

        doReturn(false).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(data.get(1));

        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(data.get(2));
        doReturn(true).when(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(any());

        associatedObjectDataExpiryDeletionAutomatedTask.deleteCaseDocumentEntity(userAccount, maxRetentionDate, batchSize);

        data.forEach(externalObjectDirectoryEntity -> verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .deleteFromExternalDataStore(externalObjectDirectoryEntity));

        verify(externalObjectDirectoryRepository)
            .deleteAll(List.of(data.get(0), data.get(2)));

        verify(caseDocumentRepository)
            .softDeleteAll(List.of(caseDocumentEntities.get(0), caseDocumentEntities.get(2)), userAccount);

        verify(auditApi)
            .record(AuditActivity.CASE_DOCUMENT_EXPIRED, userAccount, "123");
        verify(auditApi)
            .record(AuditActivity.CASE_DOCUMENT_EXPIRED, userAccount, "125");
        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(data.get(0).getCaseDocument());
        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .shouldDeleteFilter(data.get(2).getCaseDocument());
        verifyNoMoreInteractions(auditApi);
    }

    @Test
    void positiveDeleteFromExternalDataStoreInbound() {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(ExternalLocationTypeEnum.INBOUND.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        doReturn(true).when(inboundDeleter).delete(any(ExternalObjectDirectoryEntity.class));

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isTrue();
        verify(inboundDeleter).delete(externalObjectDirectoryEntity);
        verifyNoInteractions(unstructuredDeleter, auditApi);
    }

    @Test
    void positiveDeleteFromExternalDataStoreUnstructured() {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(ExternalLocationTypeEnum.UNSTRUCTURED.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        doReturn(true).when(unstructuredDeleter).delete(any(ExternalObjectDirectoryEntity.class));

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isTrue();
        verify(unstructuredDeleter).delete(externalObjectDirectoryEntity);
        verifyNoInteractions(inboundDeleter, auditApi);

    }

    @Test
    void positiveDeleteFromExternalDataStoreArm() {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(ExternalLocationTypeEnum.ARM.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isTrue();
        verifyNoInteractions(inboundDeleter, unstructuredDeleter, auditApi);
    }

    @ParameterizedTest
    @EnumSource(value = ExternalLocationTypeEnum.class, names = {"INBOUND", "UNSTRUCTURED", "ARM"},
        mode = EnumSource.Mode.EXCLUDE)
    void negativeDeleteFromExternalDataStoreUnknown(ExternalLocationTypeEnum externalLocationTypeEnum) {
        ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
        externalLocationTypeEntity.setId(externalLocationTypeEnum.getId());

        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeEntity);

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.deleteFromExternalDataStore(externalObjectDirectoryEntity))
            .isFalse();
        verifyNoInteractions(inboundDeleter, unstructuredDeleter, auditApi);
    }


    private ExternalObjectDirectoryEntity createEodWithExternalLocationType(ExternalLocationTypeEnum externalLocationTypeEnum) {
        ExternalLocationTypeEntity et = new ExternalLocationTypeEntity();
        et.setId(externalLocationTypeEnum.getId());
        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setExternalLocationType(et);
        eod.setEventDateTs(OffsetDateTime.now());
        return eod;
    }


    @Test
    void positiveShouldDeleteFilterTrue() {
        ExternalObjectDirectoryEntity inboundEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.INBOUND);
        ExternalObjectDirectoryEntity unstructuredEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.UNSTRUCTURED);
        ExternalObjectDirectoryEntity armEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.ARM);

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setRetainUntilTs(OffsetDateTime.now().minusYears(100));
        transcriptionDocumentEntity.setExternalObjectDirectoryEntities(List.of(inboundEod, unstructuredEod, armEod));


        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.shouldDeleteFilter(transcriptionDocumentEntity))
            .isTrue();
    }

    @Test
    void negativeShouldDeleteFilterFalseArmNotFound() {
        ExternalObjectDirectoryEntity inboundEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.INBOUND);
        ExternalObjectDirectoryEntity unstructuredEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.UNSTRUCTURED);

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setRetainUntilTs(OffsetDateTime.now().minusYears(100));
        transcriptionDocumentEntity.setExternalObjectDirectoryEntities(List.of(inboundEod, unstructuredEod));

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.shouldDeleteFilter(transcriptionDocumentEntity))
            .isFalse();
    }

    @Test
    void negativeShouldDeleteFilterFalseEventDateTsNotFound() {
        ExternalObjectDirectoryEntity inboundEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.INBOUND);
        ExternalObjectDirectoryEntity unstructuredEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.UNSTRUCTURED);
        ExternalObjectDirectoryEntity armEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.ARM);
        armEod.setEventDateTs(null);

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setRetainUntilTs(OffsetDateTime.now().minusYears(100));
        transcriptionDocumentEntity.setExternalObjectDirectoryEntities(List.of(inboundEod, unstructuredEod, armEod));


        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.shouldDeleteFilter(transcriptionDocumentEntity))
            .isFalse();
    }


    @Test
    void negativeShouldDeleteFilterFalseEventDateTsToRetainMissmatchTooOld() {
        ExternalObjectDirectoryEntity inboundEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.INBOUND);
        ExternalObjectDirectoryEntity unstructuredEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.UNSTRUCTURED);
        ExternalObjectDirectoryEntity armEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.ARM);

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setRetainUntilTs(OffsetDateTime.now().minusYears(100).minusDays(1));
        transcriptionDocumentEntity.setExternalObjectDirectoryEntities(List.of(inboundEod, unstructuredEod, armEod));

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.shouldDeleteFilter(transcriptionDocumentEntity))
            .isFalse();
    }


    @Test
    void negativeShouldDeleteFilterFalseEventDateTsToRetainMissmatchTooYoung() {
        ExternalObjectDirectoryEntity inboundEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.INBOUND);
        ExternalObjectDirectoryEntity unstructuredEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.UNSTRUCTURED);
        ExternalObjectDirectoryEntity armEod = createEodWithExternalLocationType(ExternalLocationTypeEnum.ARM);

        TranscriptionDocumentEntity transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setRetainUntilTs(OffsetDateTime.now().minusYears(100).plusDays(1));
        transcriptionDocumentEntity.setExternalObjectDirectoryEntities(List.of(inboundEod, unstructuredEod, armEod));

        assertThat(associatedObjectDataExpiryDeletionAutomatedTask.shouldDeleteFilter(transcriptionDocumentEntity))
            .isFalse();
    }
}
