package uk.gov.hmcts.darts;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;

import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@EnableTransactionManagement
@Slf4j
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application implements CommandLineRunner {

    @Autowired
    private MediaRequestService mediaRequestService;

    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        log.info("Default TimeZone: {}", TimeZone.getDefault().getID());
    }

    public static void main(final String[] args) {
        final var application = new SpringApplication(Application.class);
        final var instance = application.run(args);

        if (System.getenv("ATS_MODE") != null) {
            log.info("ATS_MODE found, closing instance");
            instance.close();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        if (System.getenv("ATS_MODE") != null) {
            log.info("ATS_MODE activated");
            // Temporary workaround to prevent many media requests in OPEN state
            var openAudioRequests = mediaRequestService.getMediaRequestsByStatus(AudioRequestStatus.OPEN);
            mediaRequestService.updateAudioRequestStatus(openAudioRequests.get(0).getId(), AudioRequestStatus.PROCESSING);
        }
    }

}
