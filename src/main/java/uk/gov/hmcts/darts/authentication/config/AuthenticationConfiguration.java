package uk.gov.hmcts.darts.authentication.config;

import groovy.util.logging.Slf4j;
import lombok.Getter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties
@Getter
public class AuthenticationConfiguration {
}
