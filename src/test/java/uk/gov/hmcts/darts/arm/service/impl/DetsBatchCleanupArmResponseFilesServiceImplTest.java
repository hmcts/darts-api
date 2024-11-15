package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmBatchCleanupConfiguration;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DetsBatchCleanupArmResponseFilesServiceImplTest {
    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private ArmBatchCleanupConfiguration batchCleanupConfiguration;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private ArmResponseFileHelper armResponseFileHelper;

    private static final String MANIFEST_FILE_PREFIX = "TEST";

    @Test
    void positiveConstructorTest() {
        DetsBatchCleanupArmResponseFilesServiceImpl cleanupArmResponseFilesService = new DetsBatchCleanupArmResponseFilesServiceImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            armDataManagementApi,
            userIdentity,
            batchCleanupConfiguration,
            armDataManagementConfiguration,
            currentTimeHelper,
            armResponseFileHelper,
            MANIFEST_FILE_PREFIX
        );

        verifyNoInteractions(armDataManagementConfiguration);

        assertThat(cleanupArmResponseFilesService)
            .hasFieldOrPropertyWithValue("externalObjectDirectoryRepository", externalObjectDirectoryRepository)
            .hasFieldOrPropertyWithValue("objectRecordStatusRepository", objectRecordStatusRepository)
            .hasFieldOrPropertyWithValue("externalLocationTypeRepository", externalLocationTypeRepository)
            .hasFieldOrPropertyWithValue("armDataManagementApi", armDataManagementApi)
            .hasFieldOrPropertyWithValue("userIdentity", userIdentity)
            .hasFieldOrPropertyWithValue("batchCleanupConfiguration", batchCleanupConfiguration)
            .hasFieldOrPropertyWithValue("armDataManagementConfiguration", armDataManagementConfiguration)
            .hasFieldOrPropertyWithValue("currentTimeHelper", currentTimeHelper)
            .hasFieldOrPropertyWithValue("armResponseFileHelper", armResponseFileHelper)
            .hasFieldOrPropertyWithValue("manifestFilePrefix", MANIFEST_FILE_PREFIX);
    }
}
