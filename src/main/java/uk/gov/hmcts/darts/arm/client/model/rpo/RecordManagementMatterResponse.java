package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
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
        // These tests read our OpenAPI YAML directly and do not need springdoc.
        // Exclude springdoc's Swagger jars from the test runtime to avoid duplicate Swagger classes.
        @Getter(onMethod_ = @JsonProperty("isUsedForRM"))
        @Setter(onMethod_ = @JsonProperty("isUsedForRM"))
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
