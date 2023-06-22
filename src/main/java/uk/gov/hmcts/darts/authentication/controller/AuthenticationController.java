package uk.gov.hmcts.darts.authentication.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

public interface AuthenticationController {

    @GetMapping("/login-or-refresh")
    ModelAndView loginOrRefresh(HttpSession session);

    @PostMapping("/handle-oauth-code")
    String handleOauthCode(HttpSession session, @RequestParam("code") String code);

    @GetMapping("/logout")
    ModelAndView logout(HttpSession session);

    @PostMapping("/invalidate-session")
    void invalidateSession(HttpSession session);

}
