package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.task.config.CaseExpiryDeletionAutomatedTaskConfig;

public interface CaseExpiryDeleter {

    void delete(CaseExpiryDeletionAutomatedTaskConfig config, Integer batchSize);

}
