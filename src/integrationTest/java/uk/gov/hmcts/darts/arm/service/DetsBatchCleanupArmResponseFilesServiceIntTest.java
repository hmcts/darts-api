package uk.gov.hmcts.darts.arm.service;


import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.service.impl.DetsBatchCleanupArmResponseFilesServiceImpl;

@SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive tests done via inheritance
class DetsBatchCleanupArmResponseFilesServiceIntTest extends AbstractBatchCleanupArmResponseFilesServiceIntTest {

    @Autowired
    private DetsBatchCleanupArmResponseFilesServiceImpl cleanupArmResponseFilesService;

    protected DetsBatchCleanupArmResponseFilesServiceIntTest() {
        super("DETS");
    }

    @Override
    protected BatchCleanupArmResponseFilesService getCleanupArmResponseFilesService() {
        return cleanupArmResponseFilesService;
    }
}