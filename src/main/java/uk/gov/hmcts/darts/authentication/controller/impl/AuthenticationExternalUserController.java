package uk.gov.hmcts.darts.authentication.controller.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.controller.AuthenticationController;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/external-user")
@RequiredArgsConstructor
public class AuthenticationExternalUserController implements AuthenticationController {

    private final AuthenticationService authenticationService;

    @Override
    public ModelAndView loginOrRefresh(String authHeaderValue) {
        String accessToken = null;
        if (authHeaderValue != null) {
            accessToken = authHeaderValue.replace("Bearer ", "");
        }
        URI url = authenticationService.loginOrRefresh(accessToken);
        return new ModelAndView("redirect:" + url.toString());
    }

    @Override
    public SecurityToken handleOauthCode(String code) {
        return SecurityToken.builder()
            .accessToken(authenticationService.handleOauthCode(code))
            .build();
    }

    @Override
    public ModelAndView logout(String authHeaderValue) {
        String accessToken = authHeaderValue.replace("Bearer ", "");
        URI url = authenticationService.logout(accessToken);
        return new ModelAndView("redirect:" + url.toString());
    }

}
