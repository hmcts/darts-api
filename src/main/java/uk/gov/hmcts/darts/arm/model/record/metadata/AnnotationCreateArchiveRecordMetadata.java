package uk.gov.hmcts.darts.arm.model.record.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.arm.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
//TODO fill in fields
public class AnnotationCreateArchiveRecordMetadata implements ArchiveMetadata {

    @JsonProperty("publisher")
    private String publisher;
}
