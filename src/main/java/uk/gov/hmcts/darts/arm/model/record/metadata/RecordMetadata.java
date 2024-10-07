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
public class RecordMetadata implements ArchiveMetadata {
    @JsonProperty("record_class")
    private String recordClass;
    @JsonProperty("publisher")
    private String publisher;
    @JsonProperty("region")
    private String region;
    @JsonProperty("recordDate")
    private String recordDate;
    @JsonProperty("event_date")
    private String eventDate;
    @JsonProperty("title")
    private String title;
    @JsonProperty("client_identifier")
    private String clientId;
    @JsonProperty("contributor")
    private String contributor;
    @JsonProperty("ret_conf_score")
    private Integer retentionConfidenceScore;
    @JsonProperty("ret_conf_reason")
    private String retentionConfidenceReason;

    @JsonProperty("bf_001")
    private String bf001;
    @JsonProperty("bf_002")
    private String bf002;
    @JsonProperty("bf_003")
    private String bf003;
    @JsonProperty("bf_004")
    private String bf004;
    @JsonProperty("bf_005")
    private String bf005;
    @JsonProperty("bf_006")
    private String bf006;
    @JsonProperty("bf_007")
    private String bf007;
    @JsonProperty("bf_008")
    private String bf008;
    @JsonProperty("bf_009")
    private String bf009;
    @JsonProperty("bf_010")
    private String bf010;
    @JsonProperty("bf_011")
    private String bf011;
    @JsonProperty("bf_012")
    private Integer bf012;
    @JsonProperty("bf_013")
    private Integer bf013;
    @JsonProperty("bf_014")
    private Integer bf014;
    @JsonProperty("bf_015")
    private Integer bf015;
    @JsonProperty("bf_016")
    private String bf016;
    @JsonProperty("bf_017")
    private String bf017;
    @JsonProperty("bf_018")
    private String bf018;
    @JsonProperty("bf_019")
    private String bf019;
    @JsonProperty("bf_020")
    private String bf020;

}
