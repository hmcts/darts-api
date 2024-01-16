package uk.gov.hmcts.darts.arm.model.record.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.darts.arm.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class UploadNewFileRecordMetadata implements ArchiveMetadata {

    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("dz_file_name")
    private String dzFilename;
    @JsonProperty("file_tag")
    private String fileTag;

}
