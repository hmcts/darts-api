package uk.gov.hmcts.darts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@ComponentScan({"uk.gov.hmcts.darts"})
@EnableConfigurationProperties(AudioTransformConfigurationProperties.class)
@Slf4j
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    @Autowired
    private AudioTransformConfigurationProperties audioTransformConfigurationProperties;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        log.info(audioTransformConfigurationProperties.toString());
    }

}
