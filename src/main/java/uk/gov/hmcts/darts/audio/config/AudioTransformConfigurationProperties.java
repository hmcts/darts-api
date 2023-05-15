package uk.gov.hmcts.darts.audio.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("audiotransform")
@Getter
@Setter
@ToString
public class AudioTransformConfigurationProperties {

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

}
