package uk.gov.hmcts.darts;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

@Component
@Slf4j
public class TomcatThreadPoolMetricsBinder implements MeterBinder {


    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName threadPoolMBean = new ObjectName("Tomcat:type=ThreadPool,name=\"http-nio-4550\"");

            // Thread pool usage: active threads
            registry.gauge("tomcat.threads.busy", Tags.empty(), mbs, m -> safeGetIntAttribute(m, threadPoolMBean, "currentThreadsBusy"));

            // Thread pool limit: max threads
            registry.gauge("tomcat.threads.config.max", Tags.empty(), mbs, m -> safeGetIntAttribute(m, threadPoolMBean, "maxThreads"));

            // Active HTTP connections (approximated via current thread count)
            registry.gauge("tomcat.connections.current", Tags.empty(), mbs, m -> safeGetIntAttribute(m, threadPoolMBean, "currentThreadCount"));

        } catch (Exception e) {
            // Log but don't crash application startup
            log.error("Failed to register Tomcat metrics: " + e.getMessage());
        }
    }

    private double safeGetIntAttribute(MBeanServer mbs, ObjectName name, String attribute) {
        try {
            Object val = mbs.getAttribute(name, attribute);
            return val instanceof Number ? ((Number) val).doubleValue() : Double.NaN;
        } catch (Exception ex) {
            return Double.NaN;
        }
    }
}
