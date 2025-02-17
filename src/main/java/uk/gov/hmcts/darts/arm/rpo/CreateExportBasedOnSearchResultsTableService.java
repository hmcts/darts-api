package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.arm.client.model.rpo.BaseRpoResponse;
import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.Duration;
import java.util.List;

public interface CreateExportBasedOnSearchResultsTableService {

    boolean createExportBasedOnSearchResultsTable(String bearerToken, Integer executionId,
                                                  List<MasterIndexFieldByRecordClassSchema> headerColumns, String uniqueProductionName,
                                                  Duration pollDuration, UserAccountEntity userAccount);

    boolean checkCreateExportBasedOnSearchResultsInProgress(UserAccountEntity userAccount,
                                                            BaseRpoResponse baseRpoResponse,
                                                            StringBuilder errorMessage, ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity,
                                                            Duration pollDuration);
}
