package uk.gov.hmcts.darts.authentication.model;

public record JwtValidationResult(boolean valid, String reason) {

}
