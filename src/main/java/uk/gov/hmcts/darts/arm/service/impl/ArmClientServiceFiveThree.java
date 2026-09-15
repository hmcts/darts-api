package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.RemoveProductionResponse;
import uk.gov.hmcts.darts.arm.client.version.fivethree.ArmApiBaseClientFiveThree;
import uk.gov.hmcts.darts.arm.client.version.fivethree.ArmAuthClientFiveThree;

@Component
@ConditionalOnProperty(
    prefix = "darts.storage.arm-api",
    name = "active-version",
    havingValue = "v5_3"
)
@AllArgsConstructor
public class ArmClientServiceFiveThree {

    private final ArmAuthClientFiveThree armAuthClient;
    private final ArmApiBaseClientFiveThree armApiBaseClient;

    public ArmTokenResponse getToken(ArmTokenRequest armTokenRequest) {
        return armAuthClient.getToken(armTokenRequest);
    }

    public RemoveProductionResponse removeProduction(String bearerToken, RemoveProductionRequest removeProductionRequest) {
        return armApiBaseClient.removeProduction(bearerToken, removeProductionRequest);
    }
}
