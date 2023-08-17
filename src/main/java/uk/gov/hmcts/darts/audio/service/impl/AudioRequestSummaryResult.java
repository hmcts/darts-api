package uk.gov.hmcts.darts.audio.service.impl;

import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AudioRequestSummaryResult(
    Integer mediaRequestId,
    String caseNumber,
    String courthouseName,
    LocalDate hearingDate,
    OffsetDateTime mediaRequestStartTs,
    OffsetDateTime mediaRequestEndTs,
    OffsetDateTime expiryTs,
    AudioRequestStatus mediaRequestStatus
) {

}
