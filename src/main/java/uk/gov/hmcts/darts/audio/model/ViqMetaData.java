package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class ViqMetaData {

    @NonNull
    private String courthouse;
    private String raisedBy;
    @NonNull
    private ZonedDateTime startTime;
    @NonNull
    private ZonedDateTime endTime;
    private String type;

}
