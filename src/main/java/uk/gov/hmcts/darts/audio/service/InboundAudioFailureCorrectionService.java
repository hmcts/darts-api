package uk.gov.hmcts.darts.audio.service;

@FunctionalInterface
public interface InboundAudioFailureCorrectionService {

    void correctAudioFailure(int batchSize);
    
}
