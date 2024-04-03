package uk.gov.hmcts.darts.authentication.controller;

import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.darts.authorisation.model.UserState;

public interface AuthenticationCommonController {

    @GetMapping("/userstate")
    UserState getUserState();

}
