package uk.gov.hmcts.darts.audio.service;

@FunctionalInterface
public interface InboundAudioDeleterProcessor {
    void markForDeletion(int batchSize);
}
