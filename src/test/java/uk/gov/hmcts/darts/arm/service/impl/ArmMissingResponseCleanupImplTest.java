package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmBatchCleanupConfiguration;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MISSING_RESPONSE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@ExtendWith(MockitoExtension.class)
class ArmMissingResponseCleanupImplTest {
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

    private ArmMissingResponseCleanupImpl armMissingResponseCleanup;
    private ObjectRecordStatusEntity armRawDataFailedObjectRecordStatus;

    @BeforeEach
    void beforeEach() {
        armRawDataFailedObjectRecordStatus = mock(ObjectRecordStatusEntity.class);
        when(objectRecordStatusRepository.getReferenceById(ARM_RAW_DATA_FAILED.getId()))
            .thenReturn(armRawDataFailedObjectRecordStatus);
        armMissingResponseCleanup = spy(new ArmMissingResponseCleanupImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            armDataManagementApi,
            userIdentity,
            batchCleanupConfiguration,
            armDataManagementConfiguration,
            currentTimeHelper,
            armResponseFileHelper
        ));
    }

    @Test
    void positiveConstructorTest() {
        verifyNoInteractions(armDataManagementConfiguration);

        assertThat(armMissingResponseCleanup)
            .hasFieldOrPropertyWithValue("externalObjectDirectoryRepository", externalObjectDirectoryRepository)
            .hasFieldOrPropertyWithValue("objectRecordStatusRepository", objectRecordStatusRepository)
            .hasFieldOrPropertyWithValue("externalLocationTypeRepository", externalLocationTypeRepository)
            .hasFieldOrPropertyWithValue("armDataManagementApi", armDataManagementApi)
            .hasFieldOrPropertyWithValue("userIdentity", userIdentity)
            .hasFieldOrPropertyWithValue("batchCleanupConfiguration", batchCleanupConfiguration)
            .hasFieldOrPropertyWithValue("armDataManagementConfiguration", armDataManagementConfiguration)
            .hasFieldOrPropertyWithValue("currentTimeHelper", currentTimeHelper)
            .hasFieldOrPropertyWithValue("armResponseFileHelper", armResponseFileHelper)
            .hasFieldOrPropertyWithValue("manifestFilePrefix", "ArmMissingResponseCleanup")
            .hasFieldOrPropertyWithValue("armRawDataFailed", armRawDataFailedObjectRecordStatus);
        verify(objectRecordStatusRepository).getReferenceById(ARM_RAW_DATA_FAILED.getId());
    }

    @Test
    void positiveGetStatusToSearchTest() {
        when(objectRecordStatusRepository.getReferencesByStatus(List.of(ARM_MISSING_RESPONSE)))
            .thenReturn(List.of(armRawDataFailedObjectRecordStatus));
        assertThat(armMissingResponseCleanup.getStatusToSearch())
            .isEqualTo(List.of(armRawDataFailedObjectRecordStatus));
    }

    @Test
    void positiveGetManifestFileNames() {
        int batchSize = 10;
        List<String> manifestFileNames = List.of("file1", "file2");
        OffsetDateTime dateTimeForDeletion = OffsetDateTime.now();
        doReturn(dateTimeForDeletion).when(armMissingResponseCleanup).getDateTimeForDeletion();

        try (MockedStatic<EodHelper> eodHelperMockedStatic = mockStatic(EodHelper.class)) {
            ExternalLocationTypeEntity armExternalLocationTypeEntity = mock(ExternalLocationTypeEntity.class);
            eodHelperMockedStatic.when(EodHelper::armLocation).thenReturn(armExternalLocationTypeEntity);

            doReturn(List.of(armRawDataFailedObjectRecordStatus))
                .when(armMissingResponseCleanup).getStatusToSearch();
            when(externalObjectDirectoryRepository.findBatchCleanupManifestFilenames(
                List.of(armRawDataFailedObjectRecordStatus),
                armExternalLocationTypeEntity,
                false,
                dateTimeForDeletion,
                Limit.of(batchSize)
            )).thenReturn(manifestFileNames);

            assertThat(armMissingResponseCleanup.getManifestFileNames(batchSize)).isEqualTo(manifestFileNames);
            verify(armMissingResponseCleanup).getDateTimeForDeletion();
            verify(externalObjectDirectoryRepository).findBatchCleanupManifestFilenames(
                List.of(armRawDataFailedObjectRecordStatus),
                armExternalLocationTypeEntity,
                false,
                dateTimeForDeletion,
                Limit.of(batchSize)
            );
            eodHelperMockedStatic.verify(() -> EodHelper.armLocation());
            verify(armMissingResponseCleanup).getStatusToSearch();
        }
    }

    @Test
    void positiveSetResponseCleaned() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        ExternalObjectDirectoryEntity externalObjectDirectory = mock(ExternalObjectDirectoryEntity.class);

        armMissingResponseCleanup.setResponseCleaned(userAccount, externalObjectDirectory);

        verify(externalObjectDirectory).setStatus(armRawDataFailedObjectRecordStatus);
        verify(externalObjectDirectory).setTransferAttempts(0);
        verify(externalObjectDirectory).setResponseCleaned(true);
        verify(externalObjectDirectory).setLastModifiedBy(userAccount);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectory);
    }
}
