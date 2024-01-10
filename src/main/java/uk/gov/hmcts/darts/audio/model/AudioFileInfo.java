package uk.gov.hmcts.darts.audio.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.file.Path;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class AudioFileInfo {

    @NotNull
    private Instant startTime;
    @NotNull
    private Instant endTime;
    @NotNull
    private Integer channel;
    @NotNull
    private Path path;

}
