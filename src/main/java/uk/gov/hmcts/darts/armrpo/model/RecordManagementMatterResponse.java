package uk.gov.hmcts.darts.armrpo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class RecordManagementMatterResponse extends AbstractMatterResponse {
}
