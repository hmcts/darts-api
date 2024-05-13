package uk.gov.hmcts.darts.usermanagement.service;

import uk.gov.hmcts.darts.usermanagement.model.TranscriptionDetails;

import java.time.OffsetDateTime;

public interface TranscriptionService {
    TranscriptionDetails getTranscriptionsForUser(Integer userId, OffsetDateTime requestedAtFrom);
}