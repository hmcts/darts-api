package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class PlaylistInfo {

    private String caseNumber;
    private OffsetDateTime startTime;
    private String fileLocation;

}
