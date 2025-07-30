package uk.gov.hmcts.darts;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpConnectionMonitor {

    private final MeterRegistry registry;

    public HttpConnectionMonitor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 5000)
    public void logHttpConnectionStats() {
        findAndLog(
            "tomcat.connections.current",
            "http.server.requests",
            "http.server.requests.active",
            "tomcat.sessions.active.current",
            "tomcat.sessions.active.max",
            "tomcat.sessions.alive.max",
            "tomcat.sessions.created",
            "tomcat.sessions.expired",
            "tomcat.sessions.rejected",
            "tomcat.threads.busy",
            "tomcat.threads.config.max",
            "system.cpu.count",
            "system.cpu.usage",
            "jvm.info",
            "jvm.memory.committed",
            "jvm.memory.max",
            "jvm.memory.usage.after.gc",
            "jvm.memory.used",
            "jvm.threads.daemon",
            "jvm.threads.live",
            "jvm.threads.peak",
            "jvm.threads.started",
            "jvm.threads.states",
            "disk.free",
            "disk.total",
            "executor.active",
            "executor.completed",
            "executor.pool.core",
            "executor.pool.max",
            "executor.pool.size",
            "jdbc.connections.active",
            "jdbc.connections.idle",
            "jdbc.connections.max",
            "jdbc.connections.min"
        );
    }

    private void findAndLog(String... data) {
        for (String name : data) {
            String value = getStr(name);
            if (value != null) {
                log.info("[METRIC] {}: {}", name, value);
            }
        }
    }

    private String getStr(String metricName) {
        try {
            var gauge = registry.find(metricName).gauge();
            return (gauge != null) ? String.valueOf(gauge.value()) : null;
        } catch (Exception ex) {
            log.warn("Failed to read metric '{}': {}", metricName, ex.getMessage());
            return null;
        }
    }

    private Double get(String metricName) {
        try {
            var gauge = registry.find(metricName).gauge();
            return (gauge != null) ? gauge.value() : null;
        } catch (Exception ex) {
            log.warn("Failed to read metric '{}': {}", metricName, ex.getMessage());
            return null;
        }
    }
}
