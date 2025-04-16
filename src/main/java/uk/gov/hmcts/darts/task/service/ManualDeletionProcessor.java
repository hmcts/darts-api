package uk.gov.hmcts.darts.task.service;

@FunctionalInterface
public interface ManualDeletionProcessor {
    void process(Integer batchSize);
}
