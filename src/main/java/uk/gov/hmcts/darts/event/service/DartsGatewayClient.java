package uk.gov.hmcts.darts.event.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;

@FeignClient(name = "darts-gateway-client",
    url = "${darts.gateway.url}")
public interface DartsGatewayClient {

    @GetMapping
    ResponseEntity<String> getWelcome();

    @PostMapping(value = "/events/dar-notify",
        consumes = {"application/json"},
        produces = {"application/json"}
    )
    ResponseEntity<Void> notifyEvent(DarNotifyEvent darNotifyEvent);

}
