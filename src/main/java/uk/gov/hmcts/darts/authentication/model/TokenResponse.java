package uk.gov.hmcts.darts.authentication.model;

public record TokenResponse(String accessToken, String refreshToken) {
}
