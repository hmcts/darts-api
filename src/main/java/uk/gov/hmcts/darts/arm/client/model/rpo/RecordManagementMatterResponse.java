package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecordManagementMatterResponse extends BaseRpoResponse {

    private RecordManagementMatter recordManagementMatter;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class RecordManagementMatter {
        private Integer matterCategory;
        @JsonProperty("matterID")
        private String matterId;
        private String name;
        private boolean isQuickSearch;
        @JsonProperty("isUsedForRM")
        private boolean isUsedForRm;
        private String description;
        private OffsetDateTime createdDate;
        private Integer type;
        private Integer status;
        @JsonProperty("userID")
        private String userId;
        @JsonProperty("backgroundJobID")
        private String backgroundJobId;
        private boolean isClosed;
    }
}
