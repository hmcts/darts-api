package uk.gov.hmcts.darts.log.service;

public interface DeletionLoggerService {

    void mediaDeleted(Long mediaId);

    void transcriptionDeleted(Long transcriptionId);
}
