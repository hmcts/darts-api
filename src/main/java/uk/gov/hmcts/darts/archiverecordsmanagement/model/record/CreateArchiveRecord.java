package uk.gov.hmcts.darts.archiverecordsmanagement.model.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveRecord;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.metadata.CreateArchiveRecordMetadata;

import static uk.gov.hmcts.darts.archiverecordsmanagement.util.ArchiveConstants.ArchiveRecordOperationValues.CREATE_RECORD;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class CreateArchiveRecord implements ArchiveRecord {

    @JsonProperty("operation")
    private final String operation = CREATE_RECORD;
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("record_metadata")
    private CreateArchiveRecordMetadata recordMetadata;

}
