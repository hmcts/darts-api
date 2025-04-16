package uk.gov.hmcts.darts.authorisation.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.darts.authorisation.model.UserState;

@FunctionalInterface
public interface AuthorisationController {

    @Operation(
        summary = "Gets the User State for the currently logged in user",
        description = "Gets the User State for the currently logged in user. The User State details the Users roles and permissions. " +
            "This allows the screen to refresh the roles and permissions of a user easily.",
        tags = {"Authorisation"}
    )
    @GetMapping(value = "/userstate", produces = "application/json")
    UserState getUserState();

}
