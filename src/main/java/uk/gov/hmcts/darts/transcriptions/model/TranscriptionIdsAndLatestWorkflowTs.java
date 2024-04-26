package uk.gov.hmcts.darts.transcriptions.model;

import java.time.OffsetDateTime;

public record TranscriptionIdsAndLatestWorkflowTs(Integer transcriptionId, OffsetDateTime latestWorkflowTs) {
}
