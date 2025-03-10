package uk.gov.hmcts.darts.arm.client.model.rpo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonRootName("ARMAsyncSearchResponse")
public class ArmAsyncSearchResponse extends BaseRpoResponse {

    private String searchId;

}
