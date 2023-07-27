package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PlaylistInfo {
    private String caseNumber;
    private OffsetDateTime startTime;
    private String fileLocation;
}
