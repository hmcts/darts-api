package uk.gov.hmcts.darts.audio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class EnhancedMediaRequestInfo {

    Integer mediaRequestId;
    Integer caseId;
    String caseNumber;
    String courthouseName;
    LocalDate hearingDate;
    Integer hearingId;
    AudioRequestType requestType;
    OffsetDateTime mediaRequestStartTs;
    OffsetDateTime mediaRequestEndTs;
    AudioRequestStatus mediaRequestStatus;
}
