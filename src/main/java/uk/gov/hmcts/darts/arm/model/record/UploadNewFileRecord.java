package uk.gov.hmcts.darts.arm.model.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.arm.model.ArchiveRecordOperation;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class UploadNewFileRecord implements ArchiveRecordOperation {

    @JsonProperty("operation")
    private String operation;
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("file_metadata")
    private UploadNewFileRecordMetadata fileMetadata;

}
