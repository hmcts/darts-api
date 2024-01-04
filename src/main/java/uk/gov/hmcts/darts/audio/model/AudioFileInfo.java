package uk.gov.hmcts.darts.audio.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.nio.file.Path;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class AudioFileInfo {

    @NotNull
    private Instant startTime;
    @NotNull
    private Instant endTime;
    @NotEmpty
    private String fileName;
    @NotNull
    private Integer channel;
    @NotNull
    private Path path;

}
