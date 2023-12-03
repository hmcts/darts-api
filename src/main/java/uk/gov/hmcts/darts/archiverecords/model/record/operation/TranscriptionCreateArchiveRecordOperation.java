package uk.gov.hmcts.darts.archiverecords.model.record.recordoperation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecords.model.ArchiveRecordOperation;

import static uk.gov.hmcts.darts.archiverecords.util.ArchiveConstants.ArchiveRecordOperationValues.CREATE_RECORD;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class TranscriptionCreateArchiveRecordOperation implements ArchiveRecordOperation {
    @JsonProperty("operation")
    private final String operation = CREATE_RECORD;
    @JsonProperty("relation_id")
    private String relationId;
}
