package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;

public interface AutomatedTaskProcessorFactory {

    ArmResponseFilesProcessor createArmResponseFilesProcessor(int batchSize);

    UnstructuredToArmProcessor createUnstructuredToArmProcessor(int batchSize);
}
