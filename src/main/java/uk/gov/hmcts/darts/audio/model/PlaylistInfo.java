package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class PlaylistInfo {

    private String caseNumber;
    private ZonedDateTime startTime;
    private String fileLocation;

}
