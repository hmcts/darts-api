package uk.gov.hmcts.darts.transcriptions.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("darts.transcription")
@Getter
@Setter
@ToString
@Validated
public class TranscriptionConfigurationProperties {

    private List<String> allowedExtensions = new ArrayList<>();
    private List<String> allowedContentTypes = new ArrayList<>();

    @NotNull
    private Duration maxCreatedByDuration;

}
