package uk.gov.hmcts.darts.audio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
@Setter
public class TransformedMediaDetailsDto {

    private Integer mediaRequestId;

    private Integer transformedMediaId;

    private Integer caseId;

    private Integer hearingId;

    private AudioRequestType requestType;

    private String caseNumber;

    private String courthouseName;

    private LocalDate hearingDate;

    private OffsetDateTime startTs;

    private OffsetDateTime endTs;

    private OffsetDateTime transformedMediaExpiryTs;

    private MediaRequestStatus mediaRequestStatus;

    private String transformedMediaFilename;

    private String transformedMediaFormat;

    private OffsetDateTime lastAccessedTs;

}
