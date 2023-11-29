package uk.gov.hmcts.darts.archiverecordsmanagement.model.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.gov.hmcts.darts.archiverecordsmanagement.model.ArchiveMetadata;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CreateArchiveRecordMetadata implements ArchiveMetadata {
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
    @JsonProperty("MediaFile")
    private String mediaFile;
    @JsonProperty("MediaFormat")
    private String mediaFormat;
    @JsonProperty("StartDateTime")
    private String startDateTime;
    @JsonProperty("EndDateTime")
    private String endDateTime;
    @JsonProperty("CreatedDateTime")
    private String createdDateTime;
    @JsonProperty("CaseNumbers")
    private String caseNumbers;
}
