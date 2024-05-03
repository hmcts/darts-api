package uk.gov.hmcts.darts.arm.component.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.UnstructuredToArmProcessorFactory;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

@Component
@RequiredArgsConstructor
public class UnstructuredToArmProcessorFactoryImpl implements UnstructuredToArmProcessorFactory {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final DataManagementApi dataManagementApi;
    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final FileOperationService fileOperationService;
    private final ArchiveRecordService archiveRecordService;
    private final ExternalObjectDirectoryService eodService;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;

    @Override
    public UnstructuredToArmProcessor createUnstructuredToArmProcessor(boolean batchMode) {
        if (batchMode) {
            return new UnstructuredToArmBatchProcessorImpl(
                externalObjectDirectoryRepository,
                objectRecordStatusRepository,
                externalLocationTypeRepository,
                dataManagementApi,
                armDataManagementApi,
                userIdentity,
                armDataManagementConfiguration,
                fileOperationService,
                archiveRecordService,
                eodService,
                archiveRecordFileGenerator
            );
        }

        return new UnstructuredToArmProcessorImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            dataManagementApi,
            armDataManagementApi,
            userIdentity,
            armDataManagementConfiguration,
            fileOperationService,
            archiveRecordService
        );
    }
}
