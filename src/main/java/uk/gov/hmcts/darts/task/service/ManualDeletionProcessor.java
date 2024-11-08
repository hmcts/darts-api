package uk.gov.hmcts.darts.task.service;

public interface ManualDeletionProcessor {

    void process(Integer batchSize);
}
