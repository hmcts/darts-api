package uk.gov.hmcts.reform.darts.config;

import groovy.util.logging.Slf4j;
import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties
@Getter
public class AuthServiceConfiguration {
}
