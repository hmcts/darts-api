package uk.gov.hmcts.darts.common.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = { "uk.gov.hmcts.darts" })
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class FeignCloudConfiguration {
}
