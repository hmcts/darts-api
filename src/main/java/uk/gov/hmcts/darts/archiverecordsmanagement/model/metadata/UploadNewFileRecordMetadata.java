package uk.gov.hmcts.darts.archiverecordsmanagement.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class UploadNewFileRecordMetadata implements ArchiveMetadata {

    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("dz_file_name")
    private String dzFilename;
    @JsonProperty("file_tag")
    private String fileTag;

}
