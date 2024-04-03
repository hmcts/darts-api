package uk.gov.hmcts.darts.authentication.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.darts.authorisation.model.UserState;

public interface AuthenticationCommonController {

    @Operation(
        tags = {"Authentication"}
    )
    @GetMapping("/userstate")
    UserState getUserState();

}
