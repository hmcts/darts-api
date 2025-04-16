package uk.gov.hmcts.darts.arm.service;

@FunctionalInterface
public interface BatchCleanupArmResponseFilesService {
    void cleanupResponseFiles(int batchSize);
}
