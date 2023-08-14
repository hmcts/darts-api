package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class ViqMetaData {

    @NonNull
    private String courthouse;
    private String raisedBy;
    @NonNull
    private Date startTime;
    @NonNull
    private Date endTime;
    private String type;

}
