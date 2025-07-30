package uk.gov.hmcts.darts;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConnectionPoolMonitor {

    private final HikariDataSource hikariDataSource;

    public ConnectionPoolMonitor(HikariDataSource hikariDataSource) {
        this.hikariDataSource = hikariDataSource;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkConnections() {
        int active = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
        int max = hikariDataSource.getHikariConfigMXBean().getMaximumPoolSize();
        int idle = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
        int pending = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

        double usage = ((double) active / max) * 100;
        log.info("[METRIC] [DB POOL] usage: {}/{} active ({}%), pending: {}, idle: {}",
                 active, max, String.format("%.1f", usage), pending, idle);
    }
}
