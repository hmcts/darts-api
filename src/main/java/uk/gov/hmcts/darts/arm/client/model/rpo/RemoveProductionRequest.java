package uk.gov.hmcts.darts.arm.client.model.rpo;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class RemoveProductionRequest {

    private String productionId;
    private boolean deleteSearch;

}
