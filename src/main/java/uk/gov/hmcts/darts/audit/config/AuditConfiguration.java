package uk.gov.hmcts.darts.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.audit.AuditSearchQueryValidator;

@Configuration
public class AuditConfiguration {

    @Bean
    public AuditSearchQueryValidator validator() {
        return new AuditSearchQueryValidator();
    }
}
