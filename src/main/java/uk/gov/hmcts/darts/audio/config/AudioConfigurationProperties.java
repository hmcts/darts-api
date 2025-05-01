package uk.gov.hmcts.darts.audio.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("darts.audio")
@Getter
@Setter
@ToString
@Validated
public class AudioConfigurationProperties {

    @NotEmpty
    private String ffmpegExecutable;
    @NotEmpty
    private String concatWorkspace;
    @NotEmpty
    private String mergeWorkspace;
    @NotEmpty
    private String trimWorkspace;
    @NotEmpty
    private String reEncodeWorkspace;
    @NotEmpty
    private String tempBlobWorkspace;

    private Duration allowableAudioGapDuration;
    private Duration preAmbleDuration;
    private Duration postAmbleDuration;
    @NotEmpty
    private List<String> handheldAudioCourtroomNumbers;

    @NotNull
    private Integer maxHandheldAudioFiles;
}
