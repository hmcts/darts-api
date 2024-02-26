package uk.gov.hmcts.darts.annotation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnnotationConfigurationProperties.class)
public class AnnotationConfiguration {

}
