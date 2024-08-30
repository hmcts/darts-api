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
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@Slf4j
class UnstructuredToArmProcessorFactoryTest {

    private static final int CASE_DOCUMENT_GENERATION_DAYS = 44;

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
    private CaseRepository caseRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private GenerateCaseDocumentSingleCaseProcessor generateCaseDocumentSingleCaseProcessor;
    @Mock
    private GenerateCaseDocumentForRetentionDateProcessor generateCaseDocumentForRetentionDateBatchProcessor;
    @Mock
    private LogApi logApi;

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
                                                  caseRepository,
                                                  generateCaseDocumentSingleCaseProcessor,
                                                  eventRepository,
                                                  generateCaseDocumentForRetentionDateBatchProcessor,
                                                  logApi,
                                                  CASE_DOCUMENT_GENERATION_DAYS);

        var unstructuredToArmProcessor = automatedTaskProcessorFactory.createUnstructuredToArmProcessor(10);

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
                                                  caseRepository,
                                                  generateCaseDocumentSingleCaseProcessor,
                                                  eventRepository,
                                                  generateCaseDocumentForRetentionDateBatchProcessor,
                                                  logApi,
                                                  CASE_DOCUMENT_GENERATION_DAYS);

        var armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(0);

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
                                                  caseRepository,
                                                  generateCaseDocumentSingleCaseProcessor,
                                                  eventRepository,
                                                  generateCaseDocumentForRetentionDateBatchProcessor,
                                                  logApi,
                                                  CASE_DOCUMENT_GENERATION_DAYS);

        var armResponseFilesProcessor = automatedTaskProcessorFactory.createArmResponseFilesProcessor(10);

        assertEquals(ArmBatchProcessResponseFilesImpl.class, armResponseFilesProcessor.getClass());
    }
}