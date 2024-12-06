package uk.gov.hmcts.darts.arm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.log.api.LogApi;


@Slf4j
@Component
public class ArmBatchProcessResponseFilesImpl extends AbstractArmBatchProcessResponseFiles {

    public ArmBatchProcessResponseFilesImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                            ArmDataManagementApi armDataManagementApi,
                                            FileOperationService fileOperationService, ArmDataManagementConfiguration armDataManagementConfiguration,
                                            ObjectMapper objectMapper, UserIdentity userIdentity, CurrentTimeHelper currentTimeHelper,
                                            ExternalObjectDirectoryService externalObjectDirectoryService,
                                            LogApi logApi) {
        super(externalObjectDirectoryRepository,
              armDataManagementApi,
              fileOperationService,
              armDataManagementConfiguration,
              objectMapper,
              userIdentity,
              currentTimeHelper,
              externalObjectDirectoryService,
              logApi);
    }

    @Override
    public String getManifestFilePrefix() {
        return armDataManagementConfiguration.getManifestFilePrefix();
    }
}