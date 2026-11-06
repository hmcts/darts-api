package uk.gov.hmcts.darts.audio.service;

@FunctionalInterface
public interface FindFilesService {

    void findFiles(int batchSize);
}
