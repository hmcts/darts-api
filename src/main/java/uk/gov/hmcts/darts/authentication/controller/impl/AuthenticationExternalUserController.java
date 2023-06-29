package uk.gov.hmcts.darts.authentication.controller.impl;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/external-user")
@RequiredArgsConstructor
public class AuthenticationExternalUserController implements AuthenticationController {

    private final AuthenticationService authenticationService;

    @Override
    public ModelAndView loginOrRefresh(HttpSession session) {
        URI url = authenticationService.loginOrRefresh(session.getId());
        return new ModelAndView("redirect:" + url.toString());
    }

    @Override
    public String handleOauthCode(HttpSession session, String code) {
        return authenticationService.handleOauthCode(session.getId(), code);
    }

    @Override
    public ModelAndView logout(HttpSession session) {
        URI url = authenticationService.logout(session.getId());
        return new ModelAndView("redirect:" + url.toString());
    }

    @Override
    public void invalidateSession(HttpSession session) {
        authenticationService.invalidateSession(session.getId());
    }

    @Override
    public ModelAndView resetPassword(HttpSession session) {
        URI url = authenticationService.resetPassword(session.getId());
        return new ModelAndView("redirect:" + url.toString());
    }

}
