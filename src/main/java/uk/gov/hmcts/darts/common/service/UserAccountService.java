package uk.gov.hmcts.darts.common.service;

@FunctionalInterface
public interface UserAccountService {

    void updateLastLoginTime(Integer userId);

}
