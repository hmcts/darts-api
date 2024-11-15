package uk.gov.hmcts.darts.arm.service;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.service.impl.DartsBatchCleanupArmResponseFilesServiceImpl;

@SuppressWarnings("PMD.TestClassWithoutTestCases")//False positive tests done via inheritanc
class DartsBatchCleanupArmResponseFilesServiceIntTest extends AbstractBatchCleanupArmResponseFilesServiceIntTest {


    @Autowired
    @Getter
    private DartsBatchCleanupArmResponseFilesServiceImpl cleanupArmResponseFilesService;

    protected DartsBatchCleanupArmResponseFilesServiceIntTest() {
        super("DARTS");
    }
}