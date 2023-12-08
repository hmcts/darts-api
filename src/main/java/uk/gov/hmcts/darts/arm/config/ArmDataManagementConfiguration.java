package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class ArmDataManagementConfiguration {

    @Value("${darts.storage.arm.connection-string}")
    private String armStorageAccountConnectionString;

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;

    @Value("${darts.storage.arm.drop-zone}")
    private String armDropZone;

    @Value("${darts.storage.arm.max-retry-attempts}")
    private Integer maxRetryAttempts;
}
