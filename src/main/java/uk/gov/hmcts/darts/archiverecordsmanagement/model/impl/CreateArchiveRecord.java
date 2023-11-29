package uk.gov.hmcts.darts.archiverecordsmanagement.model.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveMetadata;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveRecord;

import static uk.gov.hmcts.darts.archiverecordsmanagement.util.ARMConstants.CREATE_RECORD;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CreateArchiveRecord implements ArchiveRecord {
    @JsonProperty("relation_id")
    private String relationId;
    @JsonProperty("record_metadata")
    private CreateArchiveRecordMetadata archiveMetadata;

    @Override
    public String getOperation() {
        return CREATE_RECORD;
    }

    @Override
    public String getRelationId() {
        return relationId;
    }

    @Override
    @JsonProperty("record_metadata")
    public ArchiveMetadata getMetadata() {
        return archiveMetadata;
    }

}
