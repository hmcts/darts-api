package uk.gov.hmcts.darts.audio.service.impl;

import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AudioRequestSummaryResult(
    Integer mediaRequestId,
    Integer caseId,
    String caseNumber,
    String courthouseName,
    LocalDate hearingDate,
    Integer hearingId,
    AudioRequestType requestType,
    OffsetDateTime mediaRequestStartTs,
    OffsetDateTime mediaRequestEndTs,
    OffsetDateTime mediaRequestExpiryTs,
    AudioRequestStatus mediaRequestStatus,
    OffsetDateTime lastAccessedTs,
    String outputFilename,
    AudioRequestOutputFormat outputFormat
) {

}
