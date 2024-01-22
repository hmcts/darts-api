package uk.gov.hmcts.darts.arm.model.record.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.darts.arm.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
@ToString
public class MediaCreateArchiveRecordMetadata implements ArchiveMetadata {

    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("record_class")
    private String recordClass;
    @JsonProperty("recordDate")
    private String recordDate;
    @JsonProperty("region")
    private String region;
    @JsonProperty("Id")
    private String id;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Channel")
    private String channel;
    @JsonProperty("MaxChannels")
    private String maxChannels;
    @JsonProperty("Courthouse")
    private String courthouse;
    @JsonProperty("Courtroom")
    private String courtroom;
    @JsonProperty("FileName")
    private String fileName;
    @JsonProperty("FileFormat")
    private String fileFormat;
    @JsonProperty("FileType")
    private String fileType;
    @JsonProperty("StartDateTime")
    private String startDateTime;
    @JsonProperty("EndDateTime")
    private String endDateTime;
    @JsonProperty("CreatedDateTime")
    private String createdDateTime;
    @JsonProperty("CaseNumbers")
    private String caseNumbers;

}
