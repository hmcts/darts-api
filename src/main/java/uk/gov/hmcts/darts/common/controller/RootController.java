package uk.gov.hmcts.darts.common.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on. This is the endpoint to deal with that and therefore silence the unnecessary 404s as a
     * response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        return ok(String.format("Welcome to darts-api (%s)", offsetDateTime));
    }
}
