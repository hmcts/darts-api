package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@FunctionalInterface
public interface GetProductionOutputFilesService {

    List<String> getProductionOutputFiles(String bearerToken, Integer executionId, UserAccountEntity userAccount);

}
