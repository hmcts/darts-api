package uk.gov.hmcts.darts.arm.service.impl;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmBatchCleanupConfiguration;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MISSING_RESPONSE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@Service
public class ArmMissingResponseCleanupImpl extends BatchCleanupArmResponseFilesServiceCommon {
    private final ObjectRecordStatusEntity armRawDataFailed;

    public ArmMissingResponseCleanupImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                         ObjectRecordStatusRepository objectRecordStatusRepository,
                                         ExternalLocationTypeRepository externalLocationTypeRepository,
                                         ArmDataManagementApi armDataManagementApi,
                                         UserIdentity userIdentity,
                                         ArmBatchCleanupConfiguration batchCleanupConfiguration,
                                         ArmDataManagementConfiguration armDataManagementConfiguration,
                                         CurrentTimeHelper currentTimeHelper,
                                         ArmResponseFileHelper armResponseFileHelper) {
        super(externalObjectDirectoryRepository, objectRecordStatusRepository, externalLocationTypeRepository, armDataManagementApi, userIdentity,
              batchCleanupConfiguration, armDataManagementConfiguration, currentTimeHelper, armResponseFileHelper,
              "ArmMissingResponseCleanup");
        this.armRawDataFailed = objectRecordStatusRepository.getReferenceById(ARM_RAW_DATA_FAILED.getId());
    }

    @Override
    protected List<ObjectRecordStatusEntity> getStatusToSearch() {
        return objectRecordStatusRepository.getReferencesByStatus(
            List.of(ARM_MISSING_RESPONSE));
    }

    @Override
    protected List<String> getManifestFileNames(int batchsize) {
        return externalObjectDirectoryRepository.findBatchCleanupManifestFilenames(
            getStatusToSearch(),
            EodHelper.armLocation(),
            false,
            getDateTimeForDeletion(),
            Limit.of(batchsize)
        );
    }

    @Override
    protected void setResponseCleaned(UserAccountEntity userAccount, ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setStatus(armRawDataFailed);
        externalObjectDirectory.setTransferAttempts(0);
        super.setResponseCleaned(userAccount, externalObjectDirectory);
    }
}