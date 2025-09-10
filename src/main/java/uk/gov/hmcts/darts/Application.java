package uk.gov.hmcts.darts;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.gov.hmcts.darts.audio.api.AudioApi;

import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@EnableTransactionManagement
@Slf4j
@RequiredArgsConstructor
public class Application implements CommandLineRunner {

    private final AudioApi audioApi;

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        log.info("Default TimeZone: {}", TimeZone.getDefault().getID());
    }

    public static void main(final String[] args) {
        final var application = new SpringApplication(Application.class);
        application.run(args);
        if (System.getenv("ATS_MODE") != null) {
            log.info("ATS_MODE found, closing instance");
            System.exit(0);//Let the shutdown hooks do its job
        }
    }

    @Override
    public void run(String... args) {
        if (System.getenv("ATS_MODE") != null) {
            log.info("ATS_MODE activated");
            audioApi.handleKedaInvocationForMediaRequests();
        }
    }

}
