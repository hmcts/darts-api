package uk.gov.hmcts.darts.arm.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmBatchCleanupConfiguration;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

@Service
public class DetsBatchCleanupArmResponseFilesServiceImpl extends BatchCleanupArmResponseFilesServiceCommon {


    public DetsBatchCleanupArmResponseFilesServiceImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                       ObjectRecordStatusRepository objectRecordStatusRepository,
                                                       ExternalLocationTypeRepository externalLocationTypeRepository,
                                                       ArmDataManagementApi armDataManagementApi,
                                                       UserIdentity userIdentity, ArmBatchCleanupConfiguration batchCleanupConfiguration,
                                                       ArmDataManagementConfiguration armDataManagementConfiguration, CurrentTimeHelper currentTimeHelper,
                                                       ArmResponseFileHelper armResponseFileHelper,
                                                       @Value("${darts.storage.dets.dets-manifest-file-prefix}") String manifestFilePrefix) {
        super(externalObjectDirectoryRepository, objectRecordStatusRepository, externalLocationTypeRepository, armDataManagementApi, userIdentity,
              batchCleanupConfiguration, armDataManagementConfiguration, currentTimeHelper, armResponseFileHelper,
              manifestFilePrefix, manifestFilePrefix
        );
    }
}