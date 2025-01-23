package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductionOutputFilesResponse extends BaseRpoResponse {

    @JsonProperty("productionID")
    private String productionId;

    @JsonProperty("productionExportFile")
    private List<ProductionExportFile> productionExportFiles;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class ProductionExportFile {
        @JsonProperty("productionExportFile")
        private ProductionExportFileDetail productionExportFileDetails;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class ProductionExportFileDetail {
        @JsonProperty("productionExportFileID")
        private String productionExportFileId;

        @JsonProperty("status")
        private Integer status;
        
    }

}
