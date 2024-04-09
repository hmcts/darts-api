package uk.gov.hmcts.darts.common.repository;

import java.time.OffsetDateTime;

public record TranscriptionIdsAndLatestWorkflowTs(Integer transcriptionId, OffsetDateTime latestWorkflowTs) {
}
