package uk.gov.hmcts.darts.arm.component.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.UnstructuredToArmProcessorFactory;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
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

    @Test
    void whenBatchModeTrueBatchProcessorCreated() {
        UnstructuredToArmProcessorFactory unstructuredToArmProcessorFactory =
            new UnstructuredToArmProcessorFactoryImpl(externalObjectDirectoryRepository,
                                                      objectRecordStatusRepository,
                                                      externalLocationTypeRepository,
                                                      dataManagementApi,
                                                      armDataManagementApi,
                                                      userIdentity,
                                                      armDataManagementConfiguration,
                                                      fileOperationService,
                                                      archiveRecordService,
                                                      eodService,
                                                      archiveRecordFileGenerator);

        var unstructuredToArmProcessor = unstructuredToArmProcessorFactory.createUnstructuredToArmProcessor(true);

        assertEquals(UnstructuredToArmBatchProcessorImpl.class, unstructuredToArmProcessor.getClass());
    }

    @Test
    void whenBatchModeFalseSingleProcessorCreated() {
        UnstructuredToArmProcessorFactory unstructuredToArmProcessorFactory =
            new UnstructuredToArmProcessorFactoryImpl(externalObjectDirectoryRepository,
                                                      objectRecordStatusRepository,
                                                      externalLocationTypeRepository,
                                                      dataManagementApi,
                                                      armDataManagementApi,
                                                      userIdentity,
                                                      armDataManagementConfiguration,
                                                      fileOperationService,
                                                      archiveRecordService,
                                                      eodService,
                                                      archiveRecordFileGenerator);

        var unstructuredToArmProcessor = unstructuredToArmProcessorFactory.createUnstructuredToArmProcessor(false);

        assertEquals(UnstructuredToArmProcessorImpl.class, unstructuredToArmProcessor.getClass());
    }
}
