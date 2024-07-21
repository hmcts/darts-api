package uk.gov.hmcts.darts.common.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetrieveCoreObjectServiceIntTest extends IntegrationBase {
    @Test
    /*
    Tests that the service can create a hearing with CaseNumber `casenumber` when `CaseNumber` already exists with a hearing.
     */
    void useExistingCaseDifferentCaseNumberCase() throws Exception {
        dartsDatabase.createCourthouseUnlessExists("swansea");
        LocalDateTime hearingDate = LocalDateTime.of(2020, 10, 10, 10, 0, 0, 0);
        UserAccountEntity userAccount = new UserAccountEntity();
        //TODO remove
        userAccount.setId(0);
        HearingEntity existingHearing = dartsDatabase.getRetrieveCoreObjectService().retrieveOrCreateHearing("swansea",
                                                                                                             "1", "CaseNumber",
                                                                                                             hearingDate,
                                                                                                             userAccount);

        HearingEntity newHearing = dartsDatabase.getRetrieveCoreObjectService().retrieveOrCreateHearing("swansea",
                                                                                                        "1", "casenumber",
                                                                                                        hearingDate,
                                                                                                        userAccount);

        //Should be the same as case numbers should be case-insensitive.
        assertEquals(existingHearing.getId(), newHearing.getId());
    }

}
