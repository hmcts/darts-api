package uk.gov.hmcts.darts.authentication.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

public interface AuthenticationController {

    @GetMapping("/login-or-refresh")
    ModelAndView loginOrRefresh(@RequestHeader(value = "Authorization", required = false) String authHeaderValue);

    @PostMapping("/handle-oauth-code")
    String handleOauthCode(@RequestParam("code") String code);

    @GetMapping("/logout")
    ModelAndView logout(@RequestHeader("Authorization") String authHeaderValue);
}
