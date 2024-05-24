package uk.gov.hmcts.darts.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class AzureCopyUtil {

    @SneakyThrows
    public void copy(String source, String destination) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("/usr/bin/azcopy", "copy", source, destination);

            var startTime = Instant.now();
            log.debug("copy of blob started at {}", startTime);

            Process p = builder.start();
            int exitValue = p.waitFor();
            var endTime = Instant.now();
            log.debug("copy of blob completed at {}. Total duration in seconds: {}", endTime, Duration.between(startTime, endTime).getSeconds());

            if (exitValue != 0) {
                //limiting info included in the exception to avoid potential leak of the source and destination sas tokens
                throw new IOException("Failed to execute azcopy");
            }
        } catch (InterruptedException ie) {
            log.error("InterruptedException: ", ie);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new IOException("Failed to execute azcopy");
        }
    }
}
