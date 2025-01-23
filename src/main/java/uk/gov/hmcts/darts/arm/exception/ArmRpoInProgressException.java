package uk.gov.hmcts.darts.arm.exception;

import uk.gov.hmcts.darts.common.exception.DartsException;

public class ArmRpoInProgressException extends DartsException {

    public ArmRpoInProgressException(String rpoEndpoint, Integer executionId) {
        super(String.format("RPO endpoint %s is already in progress for execution id %s", rpoEndpoint, executionId));
    }
    
}
