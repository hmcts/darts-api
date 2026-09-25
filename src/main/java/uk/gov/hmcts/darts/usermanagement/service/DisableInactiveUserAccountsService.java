package uk.gov.hmcts.darts.usermanagement.service;

@FunctionalInterface
public interface DisableInactiveUserAccountsService {

    void process(int batchSize);
}
