package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;

public interface AutomatedTaskProcessorFactory {

    ArmResponseFilesProcessor createArmResponseFilesProcessor(int batchSize);

    UnstructuredToArmProcessor createUnstructuredToArmProcessor(int batchSize);

    GenerateCaseDocumentProcessor createGenerateCaseDocumentProcessor(int batchSize);

    CleanupCurrentFlagEventProcessor createCleanupCurrentFlagEventProcessor(int batchSize);
}