package uk.gov.hmcts.darts;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
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
import uk.gov.hmcts.darts.audio.util.AudioUtil;

import java.util.concurrent.ExecutionException;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@ComponentScan({"uk.gov.hmcts.darts"})
@EnableConfigurationProperties(AudioTransformConfigurationProperties.class)
@Slf4j
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    @Autowired
    private AudioTransformConfigurationProperties audioTransformConfigurationProperties;
    @Autowired
    private AudioUtil audioUtil;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    void ffmpegShowVersionCommand() throws ExecutionException, InterruptedException {
        CommandLine command = new CommandLine(audioTransformConfigurationProperties.getFfmpegExecutable());
        command.addArgument("-version");
        audioUtil.execute(command);
    }

}
