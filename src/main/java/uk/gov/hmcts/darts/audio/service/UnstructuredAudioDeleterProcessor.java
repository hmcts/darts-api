package uk.gov.hmcts.darts.audio.service;

@FunctionalInterface
public interface UnstructuredAudioDeleterProcessor {
    void markForDeletion(Integer batchSize);
}
