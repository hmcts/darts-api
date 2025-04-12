package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.unstructured.ExternalUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.datamanagement.service.impl.AssociatedObjectDataExpiryDeleterServiceImpl;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AssociatedObjectDataExpiryDeletionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssociatedObjectDataExpiryDeletionAutomatedTaskTest {
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AssociatedObjectDataExpiryDeletionAutomatedTaskConfig automatedTaskConfigurationProperties;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
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

    private AssociatedObjectDataExpiryDeleterServiceImpl associatedObjectDataExpiryDeleter;

    private final AtomicInteger idAddition = new AtomicInteger(123);


    private AssociatedObjectDataExpiryDeletionAutomatedTask associatedObjectDataExpiryDeletionAutomatedTask;

    @BeforeEach
    void beforeEach() {
        associatedObjectDataExpiryDeleter = spy(new AssociatedObjectDataExpiryDeleterServiceImpl(
            userIdentity,
            currentTimeHelper,
            transcriptionDocumentRepository,
            mediaRepository,
            annotationDocumentRepository,
            caseDocumentRepository,
            externalObjectDirectoryRepository,
            config,
            transactionTemplate,
            inboundDeleter,
            unstructuredDeleter,
            auditApi
        ));

        this.associatedObjectDataExpiryDeletionAutomatedTask = spy(
            new AssociatedObjectDataExpiryDeletionAutomatedTask(automatedTaskRepository,
                                                                automatedTaskConfigurationProperties,
                                                                logApi, lockService,
                                                                associatedObjectDataExpiryDeleter)
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

        associatedObjectDataExpiryDeletionAutomatedTask.runTask();

        Limit limit = Limit.of(5);

        verify(associatedObjectDataExpiryDeletionAutomatedTask)
            .getAutomatedTaskBatchSize();

        verify(userIdentity).getUserAccount();
        verify(currentTimeHelper).currentOffsetDateTime();
    }

}
