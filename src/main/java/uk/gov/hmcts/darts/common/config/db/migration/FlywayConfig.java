package uk.gov.hmcts.darts.common.config.db.migration;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfigureBefore(FlywayAutoConfiguration.class)
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "dbMigration", name = "runOnStartup", havingValue = "false")
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayNoOpStrategy();
    }
}
