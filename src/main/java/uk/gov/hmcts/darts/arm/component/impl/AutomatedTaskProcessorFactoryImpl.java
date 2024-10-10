package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.casedocument.service.impl.GenerateCaseDocumentBatchProcessorImpl;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;
import uk.gov.hmcts.darts.event.service.impl.CleanupCurrentFlagEventProcessorImpl;
import uk.gov.hmcts.darts.log.api.LogApi;

@Component
@RequiredArgsConstructor
@Slf4j
public class AutomatedTaskProcessorFactoryImpl implements AutomatedTaskProcessorFactory {
    private final HearingRepository hearingRepository;

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
    private final CaseRepository caseRepository;
    private final GenerateCaseDocumentSingleCaseProcessor generateCaseDocumentSingleCaseProcessor;
    private final EventRepository eventRepository;
    private final GenerateCaseDocumentForRetentionDateProcessor generateCaseDocumentForRetentionDateBatchProcessor;
    private final LogApi logApi;
    @Value("${darts.case-document.generation-days}")
    private final int caseDocumentGenerationDays;
    private final DataStoreToArmHelper unstructuredToArmHelper;
    private final CloseOldCasesProcessor closeOldCasesProcessor;

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
                batchSize,
                logApi
            );
        }

        return new ArmResponseFilesProcessorImpl(
            externalObjectDirectoryRepository,
            userIdentity,
            armResponseFilesProcessSingleElement
        );
    }

    @Override
    public GenerateCaseDocumentProcessor createGenerateCaseDocumentProcessor(int batchSize) {
        if (batchSize > 0) {
            return new GenerateCaseDocumentBatchProcessorImpl(
                batchSize, caseDocumentGenerationDays, caseRepository, generateCaseDocumentSingleCaseProcessor, currentTimeHelper);
        } else {
            throw new DartsException(String.format("Batch size not supported for case document generation: '%s'", batchSize));
        }
    }

    @Override
    public GenerateCaseDocumentForRetentionDateProcessor createGenerateCaseDocumentForRetentionDateProcessor(int batchSize) {
        if (batchSize > 0) {
            return generateCaseDocumentForRetentionDateBatchProcessor;
        } else {
            throw new DartsException(String.format("Batch size not supported for generating case document for retention date: '%s'", batchSize));
        }
    }

    @Override
    public CleanupCurrentFlagEventProcessor createCleanupCurrentFlagEventProcessor(int batchSize) {
        return new CleanupCurrentFlagEventProcessorImpl(batchSize, eventRepository, hearingRepository, userIdentity);
    }

    @Override
    public CloseOldCasesProcessor createCloseOldCasesProcessor(int batchSize) {
        if (batchSize > 0) {
            return closeOldCasesProcessor;
        } else {
            throw new DartsException(String.format("Batch size not supported for closing old cases: '%s'", batchSize));
        }
    }
}