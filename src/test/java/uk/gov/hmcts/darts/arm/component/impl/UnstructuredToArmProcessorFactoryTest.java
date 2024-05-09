package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@Slf4j
class UnstructuredToArmProcessorFactoryTest {
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;
    @Mock
    private ExternalObjectDirectoryService eodService;
    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;
    @Mock
    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Mock
    private AnnotationDocumentRepository annotationDocumentRepository;
    @Mock
    private CaseDocumentRepository caseDocumentRepository;

    @Test
    void whenBatchModeTrueBatchProcessorCreated() {
        AutomatedTaskProcessorFactory automatedTaskProcessorFactory =
            new AutomatedTaskProcessorFactoryImpl(externalObjectDirectoryRepository,
                                                  objectRecordStatusRepository,
                                                  externalLocationTypeRepository,
                                                  dataManagementApi,
                                                  armDataManagementApi,
                                                  userIdentity,
                                                  armDataManagementConfiguration,
                                                  fileOperationService,
                                                  archiveRecordService,
                                                  eodService,
                                                  archiveRecordFileGenerator,
                                                  armResponseFilesProcessSingleElement,
                                                  objectMapper,
                                                  currentTimeHelper,
                                                  mediaRepository,
                                                  transcriptionDocumentRepository,
                                                  annotationDocumentRepository,
                                                  caseDocumentRepository);

        var unstructuredToArmProcessor = automatedTaskProcessorFactory.createUnstructuredToArmProcessor(true);

        assertEquals(UnstructuredToArmBatchProcessorImpl.class, unstructuredToArmProcessor.getClass());
    }

    @Test
    void whenBatchModeFalseSingleArmResponseFilesProcessorCreated() {
        AutomatedTaskProcessorFactory automatedTaskProcessorFactory =
            new AutomatedTaskProcessorFactoryImpl(externalObjectDirectoryRepository,
                                                  objectRecordStatusRepository,
                                                  externalLocationTypeRepository,
                                                  dataManagementApi,
                                                  armDataManagementApi,
                                                  userIdentity,
                                                  armDataManagementConfiguration,
                                                  fileOperationService,
                                                  archiveRecordService,
                                                  eodService,
                                                  archiveRecordFileGenerator,
                                                  armResponseFilesProcessSingleElement,
                                                  objectMapper,
                                                  currentTimeHelper,
                                                  mediaRepository,
                                                  transcriptionDocumentRepository,
                                                  annotationDocumentRepository,
                                                  caseDocumentRepository);

        var armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(false);

        assertEquals(ArmResponseFilesProcessorImpl.class, armResponseFilesProcessor.getClass());
    }

    @Test
    void whenBatchModeTrueArmResponseFilesBatchProcessorCreated() {
        AutomatedTaskProcessorFactory automatedTaskProcessorFactory =
            new AutomatedTaskProcessorFactoryImpl(externalObjectDirectoryRepository,
                                                  objectRecordStatusRepository,
                                                  externalLocationTypeRepository,
                                                  dataManagementApi,
                                                  armDataManagementApi,
                                                  userIdentity,
                                                  armDataManagementConfiguration,
                                                  fileOperationService,
                                                  archiveRecordService,
                                                  eodService,
                                                  archiveRecordFileGenerator,
                                                  armResponseFilesProcessSingleElement,
                                                  objectMapper,
                                                  currentTimeHelper,
                                                  mediaRepository,
                                                  transcriptionDocumentRepository,
                                                  annotationDocumentRepository,
                                                  caseDocumentRepository);

        var armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(true);

        assertEquals(ArmBatchProcessResponseFilesImpl.class, armResponseFilesProcessor.getClass());
    }
}
