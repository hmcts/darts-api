package uk.gov.hmcts.darts.authentication.controller;

import groovy.util.logging.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@RestController
@RequestMapping("/external-user")
public class AuthenticationExternalUserController extends AuthenticationController {

    @GetMapping("/login-or-refresh")
    public ModelAndView loginOrRefresh() {

        return new ModelAndView();
    }
}
