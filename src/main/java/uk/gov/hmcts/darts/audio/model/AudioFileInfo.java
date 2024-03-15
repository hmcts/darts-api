package uk.gov.hmcts.darts.audio.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.time.Instant;

@Value
@Builder
public class AudioFileInfo {

    @NotNull
    private Instant startTime;
    @NotNull
    private Instant endTime;
    @NotNull
    private Integer channel;
    private String mediaFile;
    @NotNull
    private Path path;
    private boolean isTrimmed;

}
