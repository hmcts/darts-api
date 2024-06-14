package uk.gov.hmcts.darts.arm.service;

public interface BatchCleanupArmResponseFilesService {
    void cleanupResponseFiles(int batchSize);
}
