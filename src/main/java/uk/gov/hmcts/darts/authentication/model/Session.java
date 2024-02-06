package uk.gov.hmcts.darts.authentication.model;

public record Session(String sessionId, String accessToken, long accessTokenExpiresIn) {

}
