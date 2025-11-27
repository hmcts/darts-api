package uk.gov.hmcts.darts.arm.exception;

import uk.gov.hmcts.darts.common.exception.DartsException;

public class ArmRpoSearchNoResultsException extends DartsException {

    public ArmRpoSearchNoResultsException(Integer executionId) {
        super(String.format("RPO endpoint search returned no results for execution id %s", executionId));
    }

}
