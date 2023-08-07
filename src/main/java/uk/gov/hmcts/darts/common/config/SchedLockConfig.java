package uk.gov.hmcts.darts.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.sql.DataSource;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
@RequiredArgsConstructor
@Slf4j
public class SchedLockConfig {

    @Value("${spring.datasource.schema}")
    private String schema;

    private final Environment environment;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        String tablename = "shedlock";
        if (StringUtils.isNotBlank(schema)) {
            tablename = schema + "." + tablename;
        }
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withLockedByValue(getLockedBy())
                .usingDbTime()
                .withTableName(tablename)
                .build());
    }

    // TODO this will need to be revised to see if this works in the azure pods
    private String getLockedBy() {
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Unable to get hostname for lock: {}", e.getMessage());
        }
        return hostname + ":" + environment.getProperty("server.port");

    }
}
