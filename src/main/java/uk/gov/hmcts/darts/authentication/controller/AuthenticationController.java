package uk.gov.hmcts.darts.authentication.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface AuthenticationController {

    @GetMapping("/login-or-refresh")
    ModelAndView loginOrRefresh(
        @RequestHeader(value = "Authorization", required = false) String authHeaderValue,
        @RequestParam(value = "redirect_uri", required = false) String redirectUri
    );

    @Operation(
        tags = {"Authentication"}
    )
    @PostMapping("/handle-oauth-code")
    SecurityToken handleOauthCode(
        @RequestParam("code") String code,
        @RequestParam(value = "redirect_uri", required = false) String redirectUri
    );

    @Operation(
        tags = {"Authentication"}
    )
    @PostMapping("/refresh-access-token")
    SecurityToken refreshAccessToken(@RequestParam("refresh_token") String refreshToken);

    @GetMapping("/logout")
    ModelAndView logout(
        @RequestHeader("Authorization") String authHeaderValue,
        @RequestParam(value = "redirect_uri", required = false) String redirectUri
    );

    @GetMapping("/reset-password")
    ModelAndView resetPassword(@RequestParam(value = "redirect_uri", required = false) String redirectUri);


}
