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
public class MasterIndexFieldByRecordClassSchemaResponse extends BaseRpoResponse {

    private List<MasterIndexField> masterIndexFields;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class MasterIndexField {

        @JsonProperty("masterIndexFieldID")
        private String masterIndexFieldId;
        private String propertyName;
        private String propertyType;
        private String displayName;
        private Boolean isMasked;

    }


}


