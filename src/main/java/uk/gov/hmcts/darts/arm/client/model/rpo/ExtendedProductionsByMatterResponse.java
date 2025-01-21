package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExtendedProductionsByMatterResponse extends BaseRpoResponse {

    private List<Productions> productions;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Productions {

        @JsonProperty("productionID")
        private String productionId;
        @JsonProperty("name")
        private String name;
        @JsonProperty("startProductionTime")
        private String startProductionTime;
        @JsonProperty("endProductionTime")
        private String endProductionTime;

    }
}
