package uk.gov.hmcts.darts.transcriptions.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
@Getter
public class TranscriptionDocumentResponse {
    private Integer transcriptionDocumentId;
    private Integer transcriptionId;
    private Integer caseId;
    private String caseNumber;
    private Integer courthouseId;
    private String courthouseDisplayName;
    private Integer hearingId;
    private LocalDate hearingDate;
    private boolean isManualTranscription;
    private boolean isHidden;

}