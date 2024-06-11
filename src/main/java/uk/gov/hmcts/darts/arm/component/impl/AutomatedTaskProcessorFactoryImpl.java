package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.service.impl.GenerateCaseDocumentBatchProcessorImpl;
import uk.gov.hmcts.darts.common.exception.DartsException;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class AutomatedTaskProcessorFactoryImpl implements AutomatedTaskProcessorFactory {

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
    private final ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;
    private final ObjectMapper objectMapper;
    private final CurrentTimeHelper currentTimeHelper;
    private final MediaRepository mediaRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final CaseDocumentRepository caseDocumentRepository;

    @Override
    public ArmResponseFilesProcessor createArmResponseFilesProcessor(int batchSize) {
        if (batchSize > 0) {
            return new ArmBatchProcessResponseFilesImpl(
                externalObjectDirectoryRepository,
                armDataManagementApi,
                fileOperationService,
                armDataManagementConfiguration,
                objectMapper,
                userIdentity,
                currentTimeHelper,
                eodService,
                mediaRepository,
                transcriptionDocumentRepository,
                annotationDocumentRepository,
                caseDocumentRepository,
                batchSize
            );
        }

        return new ArmResponseFilesProcessorImpl(
            externalObjectDirectoryRepository,
            userIdentity,
            armResponseFilesProcessSingleElement
        );
    }

    @Override
    public UnstructuredToArmProcessor createUnstructuredToArmProcessor(int batchSize) {
        if (batchSize > 0) {
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
                archiveRecordFileGenerator,
                batchSize
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
            archiveRecordService,
            batchSize
        );
    }

    @Override
    public GenerateCaseDocumentProcessor createGenerateCaseDocumentProcessor(int batchSize) {
        if (batchSize > 0) {
            return new GenerateCaseDocumentBatchProcessorImpl(batchSize);
        } else {
            throw new DartsException(String.format("batch size not supported: '%s'", batchSize));
        }
    }

}
