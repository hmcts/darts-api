package uk.gov.hmcts.darts.audio.service;

public interface UnstructuredAudioDeleterProcessor {
    void markForDeletion(Integer batchSize);
}
