package uk.gov.hmcts.darts.arm.component;

import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;

public interface ArmAuthTokenCache {

    String getToken(ArmTokenRequest armTokenRequest);

    void evictToken();

}
