package uk.gov.hmcts.reform.darts.controllers.authentication;

import groovy.util.logging.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@RestController
@RequestMapping("/internal-user")
public class AuthInternalUserController extends AuthController {

    @GetMapping("/login-or-refresh")
    public ModelAndView loginOrRefresh() {

        return new ModelAndView();
    }
}
