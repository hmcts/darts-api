package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class MediaRequestCaseDocument extends CreatedModifiedCaseDocument {

    private Integer id;
    private Integer currentOwner;
    private Integer requestor;
    private MediaRequestStatus status;
    private AudioRequestType requestType;
    private Integer attempts;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}
