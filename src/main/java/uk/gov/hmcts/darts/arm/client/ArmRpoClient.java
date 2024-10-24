package uk.gov.hmcts.darts.arm.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "arm-rpo-client",
    url = "${darts.storage.arm-rpo.url}"
)
public interface ArmRpoClient {


}
