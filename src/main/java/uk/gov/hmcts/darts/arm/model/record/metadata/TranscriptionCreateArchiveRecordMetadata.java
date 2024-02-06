package uk.gov.hmcts.darts.arm.model.record.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.arm.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
//TODO fill in fields
public class TranscriptionCreateArchiveRecordMetadata implements ArchiveMetadata {

    @JsonProperty("publisher")
    String publisher;
}
