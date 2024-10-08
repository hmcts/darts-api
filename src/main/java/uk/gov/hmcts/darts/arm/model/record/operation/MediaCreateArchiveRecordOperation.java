package uk.gov.hmcts.darts.arm.model.record.operation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.CREATE_RECORD;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@SuppressWarnings({"PMD.FinalFieldCouldBeStatic"})
public class MediaCreateArchiveRecordOperation implements ArchiveRecordOperation {

    @JsonProperty("operation")
    private final String operation = CREATE_RECORD;
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("record_metadata")
    private RecordMetadata recordMetadata;

}
