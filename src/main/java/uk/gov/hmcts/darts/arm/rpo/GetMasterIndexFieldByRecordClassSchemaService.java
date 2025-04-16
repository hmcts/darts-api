package uk.gov.hmcts.darts.arm.rpo;

import uk.gov.hmcts.darts.arm.model.rpo.MasterIndexFieldByRecordClassSchema;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@FunctionalInterface
public interface GetMasterIndexFieldByRecordClassSchemaService {

    List<MasterIndexFieldByRecordClassSchema> getMasterIndexFieldByRecordClassSchema(String bearerToken, Integer executionId, ArmRpoStateEntity rpoStateEntity,
                                                                                     UserAccountEntity userAccount);

}
