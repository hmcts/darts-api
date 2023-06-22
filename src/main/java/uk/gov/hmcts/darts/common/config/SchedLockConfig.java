package uk.gov.hmcts.darts.common.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
@Profile("!intTest")
public class SchedLockConfig {

    @Value("${spring.datasource.schema}")
    private String schema;


    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        String tablename = "shedlock";
        if (StringUtils.isNotBlank(schema)) {
            tablename = schema + "." + tablename;
        }
        return new JdbcTemplateLockProvider(dataSource, tablename);
    }
}
