package uk.gov.hmcts.darts.transcriptions.service;

@FunctionalInterface
public interface TranscriptionsProcessor {

    void closeTranscriptions(Integer batchSize);

}
