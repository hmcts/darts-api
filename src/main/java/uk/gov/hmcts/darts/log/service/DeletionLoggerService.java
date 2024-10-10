package uk.gov.hmcts.darts.log.service;

public interface DeletionLoggerService {

    void mediaDeleted(Integer mediaId);

    void transcriptionDeleted(Integer transcriptionId);
}
