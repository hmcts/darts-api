package uk.gov.hmcts.darts.audio.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ViqMetaData {

    private String courthouse;
    private String raisedBy;
    private String startTime;
    private String endTime;
    private String type;
}
