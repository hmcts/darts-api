package uk.gov.hmcts.darts.testutils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;
import uk.gov.hmcts.darts.testutils.stubs.ArmServiceStubImpl;
import uk.gov.hmcts.darts.testutils.stubs.DataManagementServiceStubImpl;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

@TestConfiguration
public class IntegrationTestConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final String BLOB_STORAGE_PROFILE = "blobTest";

    @Bean
    @Primary
    public ArmService armService() {
        return new ArmServiceStubImpl();
    }

    @Bean
    @Primary
    public DataManagementService getDartsDataManagementService(DataManagementConfiguration configuration,
                                                               DataManagementAzureClientFactory factory,
                                                               AzureCopyUtil azureCopyUtil) {
        String[] profiles = applicationContext.getEnvironment().getActiveProfiles();

        for (String profile: profiles) {
            if (BLOB_STORAGE_PROFILE.equals(profile)) {
                return new DataManagementServiceImpl(configuration, factory, azureCopyUtil);
            }
        }

        return new DataManagementServiceStubImpl(configuration);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}