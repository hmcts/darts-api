package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;

public interface UnstructuredToArmProcessorFactory {
    UnstructuredToArmProcessor createUnstructuredToArmProcessor(boolean batchMode);
}
