package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.impl.DetsToArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;

public interface AutomatedTaskProcessorFactory {

    ArmResponseFilesProcessor createArmResponseFilesProcessor(int batchSize);

    DetsToArmBatchProcessResponseFilesImpl createDetsToArmResponseFilesProcessor(int batchSize);

    GenerateCaseDocumentProcessor createGenerateCaseDocumentProcessor(int batchSize);

    GenerateCaseDocumentForRetentionDateProcessor createGenerateCaseDocumentForRetentionDateProcessor(int batchSize);

    CloseOldCasesProcessor createCloseOldCasesProcessor(int batchSize);
}