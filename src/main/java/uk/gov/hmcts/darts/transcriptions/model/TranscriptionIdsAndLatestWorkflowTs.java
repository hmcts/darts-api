package uk.gov.hmcts.darts.transcriptions.model;

import java.time.OffsetDateTime;

public record TranscriptionIdsAndLatestWorkflowTs(Long transcriptionId, OffsetDateTime latestWorkflowTs) {
}
