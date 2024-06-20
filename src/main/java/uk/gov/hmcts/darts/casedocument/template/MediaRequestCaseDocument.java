package uk.gov.hmcts.darts.casedocument.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class MediaRequestCaseDocument extends CreatedModifiedCaseDocument {

    private final Integer id;
    private final Integer currentOwner;
    private final Integer requestor;
    private final MediaRequestStatus status;
    private final AudioRequestType requestType;
    private final Integer attempts;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
}
