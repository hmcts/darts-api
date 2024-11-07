package uk.gov.hmcts.darts.audio.service;

public interface InboundAudioDeleterProcessor {

    void markForDeletion(int batchSize);
}
