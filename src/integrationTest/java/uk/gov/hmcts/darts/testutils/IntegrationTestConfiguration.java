package uk.gov.hmcts.darts.testutils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;
import uk.gov.hmcts.darts.testutils.stubs.ArmServiceStubImpl;
import uk.gov.hmcts.darts.testutils.stubs.DataManagementServiceStubImpl;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

@Configuration
public class TestConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public ArmService armService() {
        return new ArmServiceStubImpl();
    }

    @Bean
    public DataManagementService getDartsDataManagementService(DataManagementConfiguration configuration,
                                                               DataManagementAzureClientFactory factory,
                                                               AzureCopyUtil azureCopyUtil) {
        String profiles[] = applicationContext.getEnvironment().getActiveProfiles();

        for (String profile: profiles) {
            if (profile.equals("blobTest")) {
                return new DataManagementServiceImpl(configuration, factory, azureCopyUtil);
            }
        }

        return new DataManagementServiceStubImpl(configuration);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}