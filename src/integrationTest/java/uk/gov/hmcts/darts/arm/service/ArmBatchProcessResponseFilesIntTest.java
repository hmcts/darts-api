package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
class ArmBatchProcessResponseFilesIntTest extends AbstractArmBatchProcessResponseFilesIntTest {

    @BeforeEach
    void setupData() {

        armBatchProcessResponseFiles = new ArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            objectStateRecordRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            externalObjectDirectoryService,
            logApi
        );
    }

    @Override
    protected String prefix() {
        return "DARTS";
    }
}