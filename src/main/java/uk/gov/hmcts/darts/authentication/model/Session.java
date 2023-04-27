package uk.gov.hmcts.darts.authentication.model;

import java.util.UUID;

public record Session(UUID sessionId, String accessToken, String refreshToken) {
}
