package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
class ArmBatchProcessResponseFilesIntTest extends AbstractArmBatchProcessResponseFilesIntTest {

    @BeforeEach
    void setupData() {
        armBatchProcessResponseFiles = new ArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            externalObjectDirectoryService,
            logApi,
            deleteArmResponseFilesHelper
        );
    }

    @Override
    protected String prefix() {
        return "DARTS";
    }
}