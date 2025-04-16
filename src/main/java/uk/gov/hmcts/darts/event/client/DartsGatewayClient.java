package uk.gov.hmcts.darts.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;

@FeignClient(name = "darts-gateway-client",
    url = "${darts.gateway.url}")
@FunctionalInterface
public interface DartsGatewayClient {

    @PostMapping(value = "${darts.gateway.events-dar-notify-path}",
        consumes = {MediaType.APPLICATION_JSON_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    void darNotify(DarNotifyEvent darNotifyEvent);

}
